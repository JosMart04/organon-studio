package studio.organon.server.review.dto;

/** Una idea concreta, o la que toque repasar dentro de un libro (o de todo el cuaderno si no se indica). */
public record ChallengeRequest(Long argumentId, Long workId) {
}
