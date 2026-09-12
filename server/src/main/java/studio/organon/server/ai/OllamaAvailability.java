package studio.organon.server.ai;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import studio.organon.server.ai.dto.AiStatusDto;

/**
 * Averigua si Ollama esta escuchando y que modelos tiene, sin bloquear nada.
 *
 * <p>Es la pieza que permite que la aplicacion siga siendo util con Ollama
 * apagado: la interfaz pregunta primero y solo entonces ofrece el asistente. La
 * sonda usa su propio cliente HTTP con un tiempo de espera muy corto — dos
 * segundos — porque un puerto cerrado responde al instante y lo unico que se
 * quiere evitar es que la comprobacion cuelgue la pantalla.
 */
@Component
public class OllamaAvailability {

    private static final Logger log = LoggerFactory.getLogger(OllamaAvailability.class);

    /** Ventana de cacheo. Corta a proposito: el lector puede arrancar Ollama a media sesion. */
    private static final Duration CACHE_TTL = Duration.ofSeconds(10);

    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(2);

    private final RestClient probe;
    private final String configuredModel;
    private final String baseUrl;
    private final boolean featureEnabled;

    private volatile Snapshot cached;

    public OllamaAvailability(@Value("${spring.ai.ollama.base-url:http://localhost:11434}") String baseUrl,
                              @Value("${spring.ai.ollama.chat.model:}") String configuredModel,
                              @Value("${organon.ai.enabled:true}") boolean featureEnabled) {
        this.baseUrl = baseUrl;
        this.configuredModel = configuredModel;
        this.featureEnabled = featureEnabled;

        // Cliente propio, deliberadamente ajeno al que usa Spring AI para
        // generar: aquel necesita minutos de espera, este dos segundos.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(PROBE_TIMEOUT);
        factory.setReadTimeout(PROBE_TIMEOUT);
        this.probe = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    /** Estado listo para enviar a la interfaz, con el mensaje ya redactado. */
    public AiStatusDto status() {
        if (!featureEnabled) {
            return new AiStatusDto(false, configuredModel, false, List.of(),
                    "El asistente está desactivado en esta instalación.");
        }

        List<String> models = installedModels();
        if (models == null) {
            return new AiStatusDto(false, configuredModel, false, List.of(),
                    "No encuentro Ollama en " + baseUrl + ". Ábrelo o ejecuta `ollama serve` "
                            + "en una terminal; mientras tanto puedes seguir tomando notas a mano.");
        }

        boolean modelReady = models.stream().anyMatch(m -> matches(m, configuredModel));
        if (!modelReady) {
            String alternative = models.stream().min(Comparator.naturalOrder()).orElse(null);
            String message = "Ollama está funcionando, pero no tienes el modelo `" + configuredModel + "`. "
                    + (alternative == null
                            ? "Descarga uno con `ollama pull " + configuredModel + "`."
                            : "Descárgalo con `ollama pull " + configuredModel + "`, o apunta OLLAMA_MODEL a `"
                                    + alternative + "`, que sí tienes.");
            return new AiStatusDto(false, configuredModel, false, models, message);
        }

        return new AiStatusDto(true, configuredModel, true, models,
                "Asistente listo con `" + configuredModel + "`.");
    }

    /** Atajo para el servicio: evita lanzar una generacion larga contra un puerto cerrado. */
    public void requireAvailable() {
        AiStatusDto status = status();
        if (!status.available()) {
            throw new AiUnavailableException(status.message());
        }
    }

    /** Si Ollama responde. Distingue «apagado» de «encendido pero sin el modelo». */
    public boolean isRunning() {
        return featureEnabled && installedModels() != null;
    }

    /**
     * El modelo de chat y el de embeddings se descargan por separado: puede estar
     * uno y faltar el otro.
     */
    public boolean isModelInstalled(String model) {
        if (!featureEnabled) {
            return false;
        }
        List<String> models = installedModels();
        return models != null && models.stream().anyMatch(m -> matches(m, model));
    }

    /** @return la lista de modelos, o {@code null} si Ollama no responde. */
    private List<String> installedModels() {
        Snapshot snapshot = cached;
        if (snapshot != null && snapshot.isFresh()) {
            return snapshot.models();
        }

        List<String> models;
        try {
            Map<String, Object> body = probe.get().uri("/api/tags").retrieve().body(TAGS_TYPE);
            models = readNames(body);
        } catch (Exception e) {
            // Un puerto cerrado es el caso normal, no una incidencia: se registra
            // en debug para no llenar el log del lector que no usa el asistente.
            log.debug("Ollama no responde en {}: {}", baseUrl, e.getMessage());
            models = null;
        }

        cached = new Snapshot(models, Instant.now());
        return models;
    }

    @SuppressWarnings("unchecked")
    private static List<String> readNames(Map<String, Object> body) {
        if (body == null) {
            return List.of();
        }
        Object models = body.get("models");
        if (!(models instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .map(item -> String.valueOf(item.get("model")))
                .filter(name -> !"null".equals(name))
                .sorted()
                .toList();
    }

    /**
     * Ollama devuelve los nombres con la etiqueta puesta ({@code gemma4:12b}),
     * pero el usuario puede haber configurado el modelo sin ella.
     */
    private static boolean matches(String installed, String configured) {
        if (configured == null || configured.isBlank()) {
            return false;
        }
        return installed.equals(configured)
                || installed.equals(configured + ":latest")
                || installed.startsWith(configured + ":");
    }

    private static final org.springframework.core.ParameterizedTypeReference<Map<String, Object>> TAGS_TYPE =
            new org.springframework.core.ParameterizedTypeReference<>() {
            };

    private record Snapshot(List<String> models, Instant takenAt) {
        boolean isFresh() {
            return Duration.between(takenAt, Instant.now()).compareTo(CACHE_TTL) < 0;
        }
    }
}
