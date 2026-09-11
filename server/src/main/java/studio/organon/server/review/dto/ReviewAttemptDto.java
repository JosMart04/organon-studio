package studio.organon.server.review.dto;

import java.time.Instant;
import studio.organon.server.domain.review.ChallengeKind;
import studio.organon.server.domain.review.ReviewRating;

/** Un desafio y, si ya se respondio, la respuesta y su valoracion. */
public record ReviewAttemptDto(
        long id,
        long argumentId,
        ChallengeKind kind,
        String question,
        String counterexample,
        String hint,
        String answer,
        ReviewRating rating,
        String whatWorked,
        String whatToImprove,
        String followUpQuestion,
        Instant createdAt,
        Instant answeredAt) {
}
