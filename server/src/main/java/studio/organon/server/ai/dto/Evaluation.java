package studio.organon.server.ai.dto;

import studio.organon.server.domain.review.ReviewRating;

public record Evaluation(ReviewRating rating, String whatWorked, String whatToImprove, String followUpQuestion) {
}
