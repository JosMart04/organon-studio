package studio.organon.server.review.dto;

/**
 * @param reviewedThisWeek ideas distintas repasadas en los ultimos siete dias
 * @param neverReviewed    ideas con razones que nunca se han repasado
 * @param reviewable       ideas con al menos una razon, las unicas que se pueden repasar
 */
public record ReviewProgressDto(int reviewedThisWeek, int neverReviewed, int reviewable) {
}
