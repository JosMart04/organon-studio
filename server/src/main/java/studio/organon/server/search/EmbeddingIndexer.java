package studio.organon.server.search;

import jakarta.annotation.PreDestroy;
import java.sql.Array;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;
import studio.organon.server.ai.OllamaAvailability;
import studio.organon.server.domain.NotebookChanged;
import studio.organon.server.search.dto.IndexStatusDto;
import studio.organon.server.search.dto.SearchHitType;

/**
 * Mantiene al dia los vectores de significado del cuaderno.
 *
 * <p>Trabaja en un unico hilo propio y en segundo plano: vectorizar en CPU tarda,
 * y guardar una nota no debe esperar a Ollama. Una rafaga de cambios se colapsa
 * en una sola pasada, y cada pasada solo manda al modelo lo que cambio desde la
 * anterior, comparando el hash del texto.
 *
 * <p>Si Ollama esta apagado, o falta el modelo, no hace nada: la busqueda sigue
 * funcionando por palabras y el indice se completa en cuanto el modelo vuelva.
 */
@Component
public class EmbeddingIndexer {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingIndexer.class);

    /** Textos por peticion: /api/embed los procesa juntos, pero sin mandar el cuaderno entero de golpe. */
    private static final int LOTE = 16;

    private final JdbcTemplate jdbc;
    private final SearchDocuments documents;
    private final ObjectProvider<EmbeddingModel> embeddingModel;
    private final OllamaAvailability availability;
    private final String model;
    private final boolean featureEnabled;
    private final boolean reindexOnStartup;

    private final ExecutorService ejecutor = Executors.newSingleThreadExecutor(tarea -> {
        Thread hilo = new Thread(tarea, "organon-indexador");
        hilo.setDaemon(true);
        return hilo;
    });

    private final AtomicBoolean programado = new AtomicBoolean();

    /** Vectores del modelo vigente en memoria. Se descartan cada vez que el indice cambia. */
    private volatile Map<String, float[]> vectores;

    public EmbeddingIndexer(JdbcTemplate jdbc,
                            SearchDocuments documents,
                            ObjectProvider<EmbeddingModel> embeddingModel,
                            OllamaAvailability availability,
                            @Value("${spring.ai.ollama.embedding.model:embeddinggemma}") String model,
                            @Value("${organon.ai.enabled:true}") boolean featureEnabled,
                            @Value("${organon.search.reindex-on-startup:true}") boolean reindexOnStartup) {
        this.jdbc = jdbc;
        this.documents = documents;
        this.embeddingModel = embeddingModel;
        this.availability = availability;
        this.model = model;
        this.featureEnabled = featureEnabled;
        this.reindexOnStartup = reindexOnStartup;
    }

    @EventListener(ApplicationReadyEvent.class)
    void alArrancar() {
        if (reindexOnStartup) {
            requestReindex();
        }
    }

    @EventListener(NotebookChanged.class)
    void alCambiarElCuaderno() {
        requestReindex();
    }

    @PreDestroy
    void alParar() {
        ejecutor.shutdownNow();
    }

    /** Programa una pasada. Si ya hay una esperando, no programa otra. */
    public void requestReindex() {
        if (programado.compareAndSet(false, true)) {
            ejecutor.execute(() -> {
                // Se baja antes de trabajar: un cambio durante la pasada programa la siguiente.
                programado.set(false);
                try {
                    reindexStale();
                } catch (RuntimeException e) {
                    log.warn("No se ha podido actualizar el indice de significado: {}", e.getMessage());
                    log.debug("Detalle del fallo al indexar", e);
                }
            });
        }
    }

    public boolean embeddingsAvailable() {
        return featureEnabled && embeddingModel.getIfAvailable() != null && availability.isModelInstalled(model);
    }

    public IndexStatusDto status() {
        boolean disponible = embeddingsAvailable();
        List<Preparado> preparados = preparar(documents.all());
        Map<String, String> guardados = hashesGuardados();
        int indexadas = (int) preparados.stream()
                .filter(p -> p.hash().equals(guardados.get(p.item().key())))
                .count();
        return new IndexStatusDto(indexadas, preparados.size() - indexadas, model, disponible, mensaje(disponible));
    }

    /** Sincrona: la llama el hilo del indexador, y las pruebas. */
    IndexReport reindexStale() {
        if (!embeddingsAvailable()) {
            return new IndexReport(0, 0);
        }

        List<Preparado> preparados = preparar(documents.all());
        Map<String, String> guardados = hashesGuardados();
        Set<String> vigentes = preparados.stream().map(p -> p.item().key()).collect(Collectors.toSet());

        List<String> retiradas = guardados.keySet().stream().filter(clave -> !vigentes.contains(clave)).toList();
        List<Preparado> pendientes = preparados.stream()
                .filter(p -> !p.hash().equals(guardados.get(p.item().key())))
                .toList();

        // Lo calculado con otro modelo no se puede comparar con las consultas de este.
        int deOtrosModelos = jdbc.update("DELETE FROM semantic_embedding WHERE model <> ?", model);
        borrar(retiradas);

        EmbeddingModel modelo = embeddingModel.getObject();
        for (int inicio = 0; inicio < pendientes.size(); inicio += LOTE) {
            List<Preparado> lote = pendientes.subList(inicio, Math.min(inicio + LOTE, pendientes.size()));
            guardar(lote, modelo.embed(lote.stream().map(Preparado::prompt).toList()));
        }

        if (!pendientes.isEmpty() || !retiradas.isEmpty() || deOtrosModelos > 0) {
            vectores = null;
            log.info("Indice de significado al dia: {} entradas nuevas o cambiadas, {} retiradas",
                    pendientes.size(), retiradas.size() + deOtrosModelos);
        }
        return new IndexReport(pendientes.size(), retiradas.size());
    }

    /** Vectores del modelo vigente, por clave «TIPO:id». Se cargan una vez y se reutilizan. */
    Map<String, float[]> vectors() {
        Map<String, float[]> actuales = vectores;
        if (actuales == null) {
            Map<String, float[]> cargados = new LinkedHashMap<>();
            jdbc.query("SELECT entity_type, entity_id, embedding FROM semantic_embedding WHERE model = ?",
                    (RowCallbackHandler) rs -> cargados.put(
                            IndexableItem.key(SearchHitType.valueOf(rs.getString("entity_type")), rs.getLong("entity_id")),
                            aFloats(rs.getArray("embedding"))),
                    model);
            actuales = Map.copyOf(cargados);
            vectores = actuales;
        }
        return actuales;
    }

    float[] embedQuery(String consulta) {
        return SearchMath.normalize(embeddingModel.getObject().embed(EmbeddingPrompts.query(model, consulta)));
    }

    private String mensaje(boolean disponible) {
        if (disponible) {
            return null;
        }
        if (!featureEnabled) {
            return "La búsqueda por significado está desactivada en esta instalación. La búsqueda por palabras funciona igual.";
        }
        if (!availability.isRunning()) {
            return "Ollama no está en marcha, así que busco solo por palabras. Arráncalo con `ollama serve` "
                    + "para buscar también por significado.";
        }
        return "Para buscar por significado falta el modelo `" + model + "`. Descárgalo con `ollama pull " + model
                + "`; mientras tanto busco solo por palabras.";
    }

    private List<Preparado> preparar(List<IndexableItem> items) {
        return items.stream().map(item -> {
            String prompt = EmbeddingPrompts.document(model, item.title(), item.text());
            // El modelo entra en el hash: cambiarlo obliga a recalcularlo todo.
            return new Preparado(item, prompt, SearchMath.sha256(model + "\n" + prompt));
        }).toList();
    }

    private Map<String, String> hashesGuardados() {
        Map<String, String> hashes = new HashMap<>();
        jdbc.query("SELECT entity_type, entity_id, content_hash FROM semantic_embedding WHERE model = ?",
                (RowCallbackHandler) rs -> hashes.put(
                        IndexableItem.key(SearchHitType.valueOf(rs.getString("entity_type")), rs.getLong("entity_id")),
                        rs.getString("content_hash")),
                model);
        return hashes;
    }

    private void borrar(List<String> claves) {
        if (claves.isEmpty()) {
            return;
        }
        jdbc.batchUpdate("DELETE FROM semantic_embedding WHERE model = ? AND entity_type = ? AND entity_id = ?",
                claves, claves.size(), (ps, clave) -> {
                    ps.setString(1, model);
                    ps.setString(2, IndexableItem.typeOf(clave).name());
                    ps.setLong(3, IndexableItem.idOf(clave));
                });
    }

    private void guardar(List<Preparado> lote, List<float[]> embeddings) {
        if (embeddings.size() != lote.size()) {
            throw new IllegalStateException(
                    "El modelo devolvió " + embeddings.size() + " vectores para " + lote.size() + " textos");
        }
        List<Integer> posiciones = IntStream.range(0, lote.size()).boxed().toList();
        jdbc.batchUpdate("""
                INSERT INTO semantic_embedding (entity_type, entity_id, model, dimensions, embedding, content_hash, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, now())
                ON CONFLICT (entity_type, entity_id, model) DO UPDATE
                SET dimensions = EXCLUDED.dimensions,
                    embedding = EXCLUDED.embedding,
                    content_hash = EXCLUDED.content_hash,
                    updated_at = EXCLUDED.updated_at
                """, posiciones, posiciones.size(), (ps, i) -> {
            Preparado preparado = lote.get(i);
            float[] vector = SearchMath.normalize(embeddings.get(i));
            ps.setString(1, preparado.item().type().name());
            ps.setLong(2, preparado.item().id());
            ps.setString(3, model);
            ps.setInt(4, vector.length);
            ps.setArray(5, ps.getConnection().createArrayOf("float4", aObjetos(vector)));
            ps.setString(6, preparado.hash());
        });
    }

    private static Float[] aObjetos(float[] vector) {
        Float[] objetos = new Float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            objetos[i] = vector[i];
        }
        return objetos;
    }

    private static float[] aFloats(Array array) throws SQLException {
        Object[] valores = (Object[]) array.getArray();
        float[] vector = new float[valores.length];
        for (int i = 0; i < valores.length; i++) {
            vector[i] = ((Number) valores[i]).floatValue();
        }
        return vector;
    }

    private record Preparado(IndexableItem item, String prompt, String hash) {
    }

    record IndexReport(int indexed, int removed) {
    }
}
