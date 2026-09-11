package studio.organon.server.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.organon.server.domain.review.ReviewRating;
import studio.organon.server.review.ReviewScheduler.Candidate;

class ReviewSchedulerTest {

    private static final Instant HACE_UN_MES = Instant.parse("2026-08-10T09:00:00Z");
    private static final Instant AYER = Instant.parse("2026-09-10T09:00:00Z");

    @Test
    @DisplayName("Lo que nunca se ha repasado va primero")
    void nuncaRepasadoPrimero() {
        List<Candidate> candidatos = List.of(
                new Candidate(1, ReviewRating.FLOJA, AYER),
                new Candidate(2, null, null),
                new Candidate(3, ReviewRating.SOLIDA, HACE_UN_MES));

        assertThat(ReviewScheduler.next(candidatos, null)).contains(2L);
    }

    @Test
    @DisplayName("Después va lo que salió flojo, aunque sea reciente")
    void flojoAntesQueLoAntiguo() {
        List<Candidate> candidatos = List.of(
                new Candidate(3, ReviewRating.SOLIDA, HACE_UN_MES),
                new Candidate(1, ReviewRating.FLOJA, AYER));

        assertThat(ReviewScheduler.next(candidatos, null)).contains(1L);
    }

    @Test
    @DisplayName("Entre lo demás, va lo que hace más tiempo que no se ve")
    void loMasAntiguo() {
        List<Candidate> candidatos = List.of(
                new Candidate(1, ReviewRating.SOLIDA, AYER),
                new Candidate(3, ReviewRating.A_MEDIAS, HACE_UN_MES));

        assertThat(ReviewScheduler.next(candidatos, null)).contains(3L);
    }

    @Test
    @DisplayName("No vuelve a proponer enseguida la idea recién repasada si hay otra")
    void noRepiteLaRecienRepasada() {
        List<Candidate> candidatos = List.of(new Candidate(1, ReviewRating.FLOJA, AYER), new Candidate(2, ReviewRating.SOLIDA, HACE_UN_MES));

        assertThat(ReviewScheduler.next(candidatos, 1L)).contains(2L);
    }

    @Test
    @DisplayName("Si solo queda la recién repasada, se propone igualmente")
    void unicaIdea() {
        assertThat(ReviewScheduler.next(List.of(new Candidate(1, ReviewRating.FLOJA, AYER)), 1L)).contains(1L);
    }

    @Test
    @DisplayName("Sin ideas no hay nada que repasar")
    void sinIdeas() {
        assertThat(ReviewScheduler.next(List.of(), null)).isEmpty();
    }
}
