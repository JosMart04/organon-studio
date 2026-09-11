package studio.organon.server.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmbeddingPromptsTest {

    @Test
    @DisplayName("EmbeddingGemma recibe los prefijos de su ficha de modelo, tenga o no etiqueta")
    void embeddingGemma() {
        assertThat(EmbeddingPrompts.query("embeddinggemma:latest", "el tiempo"))
                .isEqualTo("task: search result | query: el tiempo");
        assertThat(EmbeddingPrompts.document("EmbeddingGemma", "Cartas a Lucilio", "Nada es nuestro"))
                .isEqualTo("title: Cartas a Lucilio | text: Nada es nuestro");
    }

    @Test
    @DisplayName("Sin titulo, EmbeddingGemma espera la palabra none")
    void sinTitulo() {
        assertThat(EmbeddingPrompts.document("embeddinggemma", " ", "texto")).isEqualTo("title: none | text: texto");
    }

    @Test
    @DisplayName("Un modelo sin prefijos conocidos recibe el texto tal cual")
    void otroModelo() {
        assertThat(EmbeddingPrompts.query("bge-m3", "el tiempo")).isEqualTo("el tiempo");
        assertThat(EmbeddingPrompts.document("bge-m3", "Cartas", "Nada es nuestro")).isEqualTo("Cartas\nNada es nuestro");
    }
}
