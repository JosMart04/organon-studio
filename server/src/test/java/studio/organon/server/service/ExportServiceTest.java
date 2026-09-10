package studio.organon.server.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El escapado de LaTeX decide si el bloque exportado compila o rompe el
 * manuscrito del investigador, y los textos filosoficos traen justo los
 * caracteres peligrosos: porcentajes, guiones bajos, llaves.
 */
class ExportServiceTest {

    @Test
    @DisplayName("Los caracteres de sintaxis de LaTeX se neutralizan")
    void escapaCaracteresDeSintaxis() {
        assertThat(ExportService.latexEscape("100% de los casos")).isEqualTo("100\\% de los casos");
        assertThat(ExportService.latexEscape("a_b")).isEqualTo("a\\_b");
        assertThat(ExportService.latexEscape("{conjunto}")).isEqualTo("\\{conjunto\\}");
        assertThat(ExportService.latexEscape("Fulano & Mengano")).isEqualTo("Fulano \\& Mengano");
        assertThat(ExportService.latexEscape("coste $5")).isEqualTo("coste \\$5");
        assertThat(ExportService.latexEscape("nota#3")).isEqualTo("nota\\#3");
    }

    @Test
    @DisplayName("Los caracteres sin escape simple usan su macro de texto")
    void escapaCaracteresSinBarra() {
        assertThat(ExportService.latexEscape("a~b")).isEqualTo("a\\textasciitilde{}b");
        assertThat(ExportService.latexEscape("2^10")).isEqualTo("2\\textasciicircum{}10");
        assertThat(ExportService.latexEscape("C:\\ruta")).isEqualTo("C:\\textbackslash{}ruta");
    }

    @Test
    @DisplayName("El texto filosofico normal pasa intacto, acentos incluidos")
    void dejaPasarElTextoNormal() {
        String original = "La conexión necesaria no está en los objetos: es un hábito del observador.";
        assertThat(ExportService.latexEscape(original)).isEqualTo(original);
    }

    @Test
    @DisplayName("Un valor nulo no revienta la exportacion")
    void toleraNulo() {
        assertThat(ExportService.latexEscape(null)).isEmpty();
    }
}
