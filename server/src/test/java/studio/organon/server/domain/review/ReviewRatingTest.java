package studio.organon.server.domain.review;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewRatingTest {

    @Test
    @DisplayName("Reconoce la valoración aunque el modelo la escriba con tilde, en minúsculas o con espacios")
    void normalizaLoQueEscribeElModelo() {
        assertThat(ReviewRating.fromModel("Sólida")).isEqualTo(ReviewRating.SOLIDA);
        assertThat(ReviewRating.fromModel("a medias")).isEqualTo(ReviewRating.A_MEDIAS);
        assertThat(ReviewRating.fromModel("A-MEDIAS")).isEqualTo(ReviewRating.A_MEDIAS);
        assertThat(ReviewRating.fromModel(" floja ")).isEqualTo(ReviewRating.FLOJA);
    }

    @Test
    @DisplayName("Lo que no reconoce se queda en el término medio")
    void loDesconocidoQuedaAMedias() {
        assertThat(ReviewRating.fromModel("excelente")).isEqualTo(ReviewRating.A_MEDIAS);
        assertThat(ReviewRating.fromModel("")).isEqualTo(ReviewRating.A_MEDIAS);
        assertThat(ReviewRating.fromModel(null)).isEqualTo(ReviewRating.A_MEDIAS);
    }
}
