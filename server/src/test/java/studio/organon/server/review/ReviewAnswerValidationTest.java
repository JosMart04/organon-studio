package studio.organon.server.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** La respuesta se valida antes de molestar al modelo, con mensajes pensados para el lector. */
class ReviewAnswerValidationTest {

    @Test
    @DisplayName("Una respuesta vacía no se envía")
    void vacia() {
        assertThatThrownBy(() -> ReviewService.validarRespuesta("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Escribe tu respuesta");
        assertThatThrownBy(() -> ReviewService.validarRespuesta(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Una respuesta de más de 4000 caracteres se rechaza, y la de 4000 justos no")
    void demasiadoLarga() {
        assertThatThrownBy(() -> ReviewService.validarRespuesta("x".repeat(ReviewService.MAX_RESPUESTA + 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("4000");
        assertThat(ReviewService.validarRespuesta("x".repeat(ReviewService.MAX_RESPUESTA))).hasSize(4000);
    }

    @Test
    @DisplayName("Se guarda sin espacios sobrantes")
    void sinEspaciosSobrantes() {
        assertThat(ReviewService.validarRespuesta("  Porque confunde poseer con controlar.\n"))
                .isEqualTo("Porque confunde poseer con controlar.");
    }
}
