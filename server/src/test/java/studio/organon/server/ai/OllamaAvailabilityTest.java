package studio.organon.server.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La degradacion elegante es la parte del asistente que mas se usa: el lector
 * medio tendra Ollama apagado la mayor parte del tiempo.
 *
 * <p>Las pruebas apuntan a un puerto cerrado a proposito. No hay red de por
 * medio y corren en CI sin nada instalado.
 */
class OllamaAvailabilityTest {

    /** Puerto reservado por IANA para "descartar": nadie escucha ahi. */
    private static final String PUERTO_CERRADO = "http://127.0.0.1:9";

    @Test
    @DisplayName("Con Ollama apagado informa de que no esta y explica como arrancarlo")
    void ollamaApagado() {
        OllamaAvailability availability = new OllamaAvailability(PUERTO_CERRADO, "gemma4:12b", true);

        var status = availability.status();

        assertThat(status.available()).isFalse();
        assertThat(status.modelReady()).isFalse();
        assertThat(status.installedModels()).isEmpty();
        assertThat(status.message())
                .contains("ollama serve")
                .contains("a mano");
    }

    @Test
    @DisplayName("requireAvailable lanza la excepcion del dominio, no un error de red")
    void requireAvailableLanzaExcepcionDelDominio() {
        OllamaAvailability availability = new OllamaAvailability(PUERTO_CERRADO, "gemma4:12b", true);

        assertThatThrownBy(availability::requireAvailable)
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("No encuentro Ollama");
    }

    @Test
    @DisplayName("Con la funcionalidad desactivada ni siquiera consulta a Ollama")
    void desactivadaPorConfiguracion() {
        // base-url deliberadamente invalida: si intentase conectarse, fallaria
        // de otra forma. El mensaje demuestra que ni lo intenta.
        OllamaAvailability availability = new OllamaAvailability("http://no-existe.invalid", "gemma4:12b", false);

        var status = availability.status();

        assertThat(status.available()).isFalse();
        assertThat(status.message()).contains("desactivado");
    }

    @Test
    @DisplayName("Con Ollama apagado ni esta en marcha ni tiene ningun modelo")
    void apagadoSinModelos() {
        OllamaAvailability availability = new OllamaAvailability(PUERTO_CERRADO, "gemma4:12b", true);

        assertThat(availability.isRunning()).isFalse();
        assertThat(availability.isModelInstalled("embeddinggemma")).isFalse();
    }

    @Test
    @DisplayName("El estado nunca lanza: la interfaz siempre puede preguntar")
    void statusNuncaLanza() {
        OllamaAvailability availability = new OllamaAvailability("no-es-una-url", "gemma4:12b", true);

        assertThat(availability.status()).isNotNull();
        assertThat(availability.status().available()).isFalse();
    }
}
