package studio.organon.server.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TextQueryTest {

    @Test
    @DisplayName("La ultima palabra se busca como prefijo y las anteriores enteras")
    void prefijoEnLaUltima() {
        assertThat(TextQuery.allWords("Razón pur")).contains("razón & pur:*");
    }

    @Test
    @DisplayName("Una sola palabra, a medio escribir, es un prefijo")
    void unaPalabra() {
        assertThat(TextQuery.allWords("razo")).contains("razo:*");
        assertThat(TextQuery.anyWord("razo")).contains("razo:*");
    }

    @Test
    @DisplayName("Para una frase basta con cualquiera de sus palabras")
    void cualquieraDeLasPalabras() {
        assertThat(TextQuery.anyWord("el tiempo que se escapa")).contains("el | tiempo | que | se | escapa:*");
    }

    @Test
    @DisplayName("Los operadores que escribe el usuario no llegan a la consulta")
    void sinOperadoresDelUsuario() {
        assertThat(TextQuery.allWords("a' | !b & (c)")).contains("a & b & c:*");
        assertThat(TextQuery.anyWord("a' & !b")).contains("a | b:*");
    }

    @Test
    @DisplayName("Sin letras ni digitos no hay consulta")
    void vacia() {
        assertThat(TextQuery.allWords("  ¿? … ")).isEmpty();
        assertThat(TextQuery.anyWord(null)).isEmpty();
    }

    @Test
    @DisplayName("Una consulta muy larga se recorta")
    void larga() {
        String consulta = TextQuery.allWords("uno ".repeat(20)).orElseThrow();

        assertThat(consulta.split(" & ")).hasSize(TextQuery.MAX_PALABRAS);
    }
}
