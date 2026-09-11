package studio.organon.server.review;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import studio.organon.server.domain.review.ReviewRating;

/**
 * Que idea toca repasar.
 *
 * <p>Sin algoritmos de repeticion espaciada, que para un cuaderno personal serian
 * mas maquinaria que beneficio: primero lo que nunca se ha repasado, despues lo
 * que salio flojo la ultima vez y, por ultimo, lo que hace mas tiempo que no se
 * ve. Es logica pura: se prueba sin base de datos.
 */
final class ReviewScheduler {

    /**
     * @param lastRating     valoracion del ultimo repaso respondido, o null si nunca se repaso
     * @param lastReviewedAt cuando se respondio ese repaso, o null
     */
    record Candidate(long argumentId, ReviewRating lastRating, Instant lastReviewedAt) {
    }

    private static final Comparator<Candidate> PRIORIDAD = Comparator
            .comparingInt(ReviewScheduler::grupo)
            .thenComparing(Candidate::lastReviewedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparingLong(Candidate::argumentId);

    private ReviewScheduler() {
    }

    /**
     * @param recienRepasada la idea que se acaba de repasar: no se vuelve a proponer
     *                       enseguida, salvo que no quede ninguna otra
     */
    static Optional<Long> next(List<Candidate> candidatos, Long recienRepasada) {
        return candidatos.stream()
                .sorted(PRIORIDAD)
                .map(Candidate::argumentId)
                .filter(id -> candidatos.size() == 1 || !id.equals(recienRepasada))
                .findFirst();
    }

    private static int grupo(Candidate candidato) {
        if (candidato.lastReviewedAt() == null) {
            return 0;
        }
        return candidato.lastRating() == ReviewRating.FLOJA ? 1 : 2;
    }
}
