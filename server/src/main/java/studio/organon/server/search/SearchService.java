package studio.organon.server.search;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import studio.organon.server.search.dto.IndexStatusDto;
import studio.organon.server.search.dto.MatchSource;
import studio.organon.server.search.dto.SearchHitType;
import studio.organon.server.search.dto.SearchMode;
import studio.organon.server.search.dto.SearchResponseDto;
import studio.organon.server.search.dto.SearchResultDto;

/**
 * Busqueda en el cuaderno: por palabras, que responde al instante y no necesita
 * a nadie, y por significado, que combina esas mismas palabras con la cercania
 * de los vectores del modelo local.
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    static final int LIMITE = 20;
    static final int LIMITE_MAXIMO = 50;

    /** Candidatos de cada lista antes de fusionarlas. */
    static final int CANDIDATOS = 50;

    /** Marcas de las coincidencias. Improbables en notas de lectura, a diferencia de los [[ ]] de Obsidian. */
    static final String MARCA_INICIO = "⟦";
    static final String MARCA_FIN = "⟧";

    /**
     * Un unico extracto, no fragmentos sueltos: en notas cortas los fragmentos
     * empezaban a media frase, y cuando la coincidencia estaba en otra columna
     * (la palabra de una definicion) se quedaban en seis palabras. Si no hay
     * coincidencia en el texto, PostgreSQL muestra su comienzo. Por eso el
     * extracto nunca repite el titulo: si la palabra estaba solo ahi, se ve el
     * principio del resto.
     */
    private static final String OPCIONES_FRAGMENTO = "MaxWords=35, MinWords=15, ShortWord=3, "
            + "StartSel=" + MARCA_INICIO + ", StopSel=" + MARCA_FIN;

    private static final int LARGO_FRAGMENTO = 220;

    private static final String CONSULTA_TEXTO = """
            WITH q AS (SELECT to_tsquery('es_organon', ?) AS query)
            SELECT hits.*
            FROM (
                SELECT 'PASSAGE' AS type, p.id, p.locator AS title,
                       ts_headline('es_organon', p.text_content || coalesce(' · ' || p.personal_notes, ''), q.query, ?) AS snippet,
                       w.id AS work_id, w.title AS work_title, ph.name AS author, NULL::bigint AS concept_id,
                       ts_rank_cd(p.search_vector, q.query) AS rank
                FROM passage p
                JOIN work w ON w.id = p.work_id
                JOIN philosopher ph ON ph.id = w.philosopher_id
                CROSS JOIN q
                WHERE p.search_vector @@ q.query

                UNION ALL

                SELECT 'ARGUMENT', a.id, a.name,
                       ts_headline('es_organon', coalesce(pr.texto, a.name), q.query, ?),
                       w.id, w.title, ph.name, NULL::bigint,
                       ts_rank_cd(a.search_vector, q.query) + coalesce(pr.rank, 0)
                FROM argument a
                JOIN work w ON w.id = a.work_id
                JOIN philosopher ph ON ph.id = w.philosopher_id
                CROSS JOIN q
                LEFT JOIN LATERAL (
                    SELECT string_agg(statement, ' · ' ORDER BY order_index) AS texto,
                           max(ts_rank_cd(search_vector, q.query)) FILTER (WHERE search_vector @@ q.query) AS rank
                    FROM premise
                    WHERE argument_id = a.id
                ) pr ON true
                WHERE a.search_vector @@ q.query OR pr.rank IS NOT NULL

                UNION ALL

                SELECT 'PHILOSOPHER', ph.id, ph.name,
                       ts_headline('es_organon', concat_ws(' · ', ph.school, ph.biographical_summary), q.query, ?),
                       (SELECT min(w.id) FROM work w WHERE w.philosopher_id = ph.id), NULL, ph.name, NULL::bigint,
                       ts_rank_cd(ph.search_vector, q.query)
                FROM philosopher ph
                CROSS JOIN q
                WHERE ph.search_vector @@ q.query

                UNION ALL

                SELECT 'DEFINITION', d.id, c.term,
                       ts_headline('es_organon', d.operational_definition || coalesce(' · ' || d.notes, ''), q.query, ?),
                       w.id, w.title, ph.name, c.id,
                       ts_rank_cd(d.search_vector || c.search_vector, q.query)
                FROM term_definition d
                JOIN semantic_concept c ON c.id = d.concept_id
                JOIN philosopher ph ON ph.id = d.philosopher_id
                LEFT JOIN work w ON w.id = d.work_id
                CROSS JOIN q
                WHERE d.search_vector @@ q.query OR c.search_vector @@ q.query
            ) hits
            ORDER BY hits.rank DESC, hits.type, hits.id
            LIMIT ?
            """;

    /** Datos de lo que solo encontro el significado: no hay coincidencias que resaltar. */
    private static final String CONSULTA_DETALLE = """
            SELECT 'PASSAGE' AS type, p.id, p.locator AS title,
                   p.text_content || coalesce(' · ' || p.personal_notes, '') AS snippet,
                   w.id AS work_id, w.title AS work_title, ph.name AS author, NULL::bigint AS concept_id, 0::real AS rank
            FROM passage p
            JOIN work w ON w.id = p.work_id
            JOIN philosopher ph ON ph.id = w.philosopher_id
            WHERE p.id = ANY(?)

            UNION ALL

            SELECT 'ARGUMENT', a.id, a.name,
                   coalesce((SELECT string_agg(pr.statement, ' · ' ORDER BY pr.order_index)
                             FROM premise pr WHERE pr.argument_id = a.id), a.name),
                   w.id, w.title, ph.name, NULL::bigint, 0::real
            FROM argument a
            JOIN work w ON w.id = a.work_id
            JOIN philosopher ph ON ph.id = w.philosopher_id
            WHERE a.id = ANY(?)

            UNION ALL

            SELECT 'PHILOSOPHER', ph.id, ph.name, concat_ws(' · ', ph.school, ph.biographical_summary),
                   (SELECT min(w.id) FROM work w WHERE w.philosopher_id = ph.id), NULL, ph.name, NULL::bigint, 0::real
            FROM philosopher ph
            WHERE ph.id = ANY(?)

            UNION ALL

            SELECT 'DEFINITION', d.id, c.term, d.operational_definition,
                   w.id, w.title, ph.name, c.id, 0::real
            FROM term_definition d
            JOIN semantic_concept c ON c.id = d.concept_id
            JOIN philosopher ph ON ph.id = d.philosopher_id
            LEFT JOIN work w ON w.id = d.work_id
            WHERE d.id = ANY(?)
            """;

    private static final List<SearchHitType> ORDEN_DETALLE = List.of(
            SearchHitType.PASSAGE, SearchHitType.ARGUMENT, SearchHitType.PHILOSOPHER, SearchHitType.DEFINITION);

    private static final RowMapper<Hit> FILA = (rs, fila) -> new Hit(
            SearchHitType.valueOf(rs.getString("type")),
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("snippet"),
            rs.getObject("work_id", Long.class),
            rs.getString("work_title"),
            rs.getString("author"),
            rs.getObject("concept_id", Long.class),
            rs.getDouble("rank"));

    private final JdbcTemplate jdbc;
    private final EmbeddingIndexer indexer;
    private final double similitudMinima;
    private final double similitudRelativa;

    public SearchService(JdbcTemplate jdbc,
                         EmbeddingIndexer indexer,
                         @Value("${organon.search.min-similarity:0.2}") double similitudMinima,
                         @Value("${organon.search.relative-similarity:0.5}") double similitudRelativa) {
        this.jdbc = jdbc;
        this.indexer = indexer;
        this.similitudMinima = similitudMinima;
        this.similitudRelativa = similitudRelativa;
    }

    /** Solo palabras: pensada para responder mientras se escribe. */
    public List<SearchResultDto> text(String escrito, Integer limite) {
        List<Hit> hits = TextQuery.allWords(escrito).map(q -> porTexto(q, acotar(limite))).orElse(List.of());
        double mejor = hits.stream().mapToDouble(Hit::rank).max().orElse(0);
        return hits.stream()
                .map(hit -> hit.toDto(mejor > 0 ? hit.rank() / mejor : 1, List.of(MatchSource.TEXTO)))
                .toList();
    }

    /**
     * Palabras y significado, fusionados por posicion. Sin el modelo disponible
     * se queda en palabras y lo dice: la busqueda nunca deja de responder.
     */
    public SearchResponseDto semantic(String consulta, Integer limite) {
        int maximo = acotar(limite);
        List<Hit> porTexto = TextQuery.anyWord(consulta).map(q -> porTexto(q, CANDIDATOS)).orElse(List.of());
        IndexStatusDto estado = indexer.status();

        if (!estado.available()) {
            return soloTexto(porTexto, maximo, estado.pending(), estado.message());
        }

        List<SearchMath.Scored> vecinos;
        try {
            vecinos = SearchMath.nearest(indexer.embedQuery(consulta), indexer.vectors(),
                    similitudMinima, similitudRelativa, CANDIDATOS);
        } catch (RuntimeException e) {
            log.warn("No se ha podido vectorizar la consulta: {}", e.getMessage());
            return soloTexto(porTexto, maximo, estado.pending(),
                    "No he podido consultar el modelo de significado; te enseño lo que coincide por palabras.");
        }

        Map<String, Hit> porClave = new LinkedHashMap<>();
        porTexto.forEach(hit -> porClave.put(hit.key(), hit));
        Set<String> clavesTexto = Set.copyOf(porClave.keySet());
        List<String> rankingSignificado = vecinos.stream().map(SearchMath.Scored::key).toList();
        Set<String> clavesSignificado = Set.copyOf(rankingSignificado);

        LinkedHashMap<String, Double> fusion = SearchMath.reciprocalRankFusion(
                List.of(porTexto.stream().map(Hit::key).toList(), rankingSignificado));
        List<String> elegidas = fusion.keySet().stream().limit(maximo).toList();
        detalle(elegidas.stream().filter(clave -> !porClave.containsKey(clave)).toList())
                .forEach(hit -> porClave.put(hit.key(), hit));

        double mejor = fusion.isEmpty() ? 1 : fusion.firstEntry().getValue();
        List<SearchResultDto> resultados = elegidas.stream()
                // Una entrada borrada entre la ultima indexacion y esta consulta ya no tiene detalle.
                .filter(porClave::containsKey)
                .map(clave -> {
                    List<MatchSource> motivos = new ArrayList<>(2);
                    if (clavesTexto.contains(clave)) {
                        motivos.add(MatchSource.TEXTO);
                    }
                    if (clavesSignificado.contains(clave)) {
                        motivos.add(MatchSource.SIGNIFICADO);
                    }
                    return porClave.get(clave).toDto(fusion.get(clave) / mejor, motivos);
                })
                .toList();

        return new SearchResponseDto(SearchMode.HIBRIDA, resultados, estado.pending(), avisoPendientes(estado.pending()));
    }

    private SearchResponseDto soloTexto(List<Hit> porTexto, int maximo, int pendientes, String aviso) {
        double mejor = porTexto.stream().mapToDouble(Hit::rank).max().orElse(0);
        List<SearchResultDto> resultados = porTexto.stream()
                .limit(maximo)
                .map(hit -> hit.toDto(mejor > 0 ? hit.rank() / mejor : 1, List.of(MatchSource.TEXTO)))
                .toList();
        return new SearchResponseDto(SearchMode.SOLO_TEXTO, resultados, pendientes, aviso);
    }

    private List<Hit> porTexto(String tsquery, int limite) {
        return jdbc.query(CONSULTA_TEXTO, FILA, tsquery,
                OPCIONES_FRAGMENTO, OPCIONES_FRAGMENTO, OPCIONES_FRAGMENTO, OPCIONES_FRAGMENTO, limite);
    }

    private List<Hit> detalle(List<String> claves) {
        if (claves.isEmpty()) {
            return List.of();
        }
        Map<SearchHitType, List<Long>> porTipo = new EnumMap<>(SearchHitType.class);
        for (String clave : claves) {
            porTipo.computeIfAbsent(IndexableItem.typeOf(clave), tipo -> new ArrayList<>()).add(IndexableItem.idOf(clave));
        }
        return jdbc.query(CONSULTA_DETALLE, ps -> {
            int posicion = 1;
            for (SearchHitType tipo : ORDEN_DETALLE) {
                Long[] ids = porTipo.getOrDefault(tipo, List.of()).toArray(Long[]::new);
                ps.setArray(posicion++, ps.getConnection().createArrayOf("bigint", ids));
            }
        }, FILA).stream().map(hit -> hit.conFragmento(recortar(hit.snippet()))).toList();
    }

    static String recortar(String texto) {
        if (texto == null) {
            return "";
        }
        String limpio = texto.replaceAll("\\s+", " ").strip();
        if (limpio.length() <= LARGO_FRAGMENTO) {
            return limpio;
        }
        int corte = limpio.lastIndexOf(' ', LARGO_FRAGMENTO);
        return limpio.substring(0, corte > LARGO_FRAGMENTO / 2 ? corte : LARGO_FRAGMENTO) + "…";
    }

    private static String avisoPendientes(int pendientes) {
        if (pendientes == 0) {
            return null;
        }
        return pendientes == 1
                ? "Queda 1 nota por indexar: puede que aún no salga al buscar por significado."
                : "Quedan " + pendientes + " notas por indexar: puede que aún no salgan al buscar por significado.";
    }

    private static int acotar(Integer limite) {
        return limite == null ? LIMITE : Math.clamp(limite, 1, LIMITE_MAXIMO);
    }

    record Hit(SearchHitType type, long id, String title, String snippet, Long workId, String workTitle,
               String author, Long conceptId, double rank) {

        String key() {
            return IndexableItem.key(type, id);
        }

        Hit conFragmento(String fragmento) {
            return new Hit(type, id, title, fragmento, workId, workTitle, author, conceptId, rank);
        }

        SearchResultDto toDto(double relevancia, List<MatchSource> motivos) {
            return new SearchResultDto(type, id, title, snippet, workId, workTitle, author, conceptId,
                    Math.round(relevancia * 100) / 100.0, List.copyOf(motivos));
        }
    }
}
