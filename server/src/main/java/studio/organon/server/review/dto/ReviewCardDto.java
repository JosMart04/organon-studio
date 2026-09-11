package studio.organon.server.review.dto;

import java.time.Instant;
import java.util.List;
import studio.organon.server.domain.review.ReviewRating;

/**
 * La cara de la tarjeta de repaso: la idea, de quien es y en que se apoya.
 *
 * @param reasons     las razones en orden, sin la conclusion
 * @param assumptions cuantas de esas razones son supuestos implicitos
 * @param attempts    cuantas veces se ha pedido un desafio sobre esta idea
 */
public record ReviewCardDto(
        long argumentId,
        String name,
        long workId,
        String workTitle,
        String author,
        String avatarEmoji,
        String conclusion,
        List<String> reasons,
        int assumptions,
        int attempts,
        ReviewRating lastRating,
        Instant lastReviewedAt) {
}
