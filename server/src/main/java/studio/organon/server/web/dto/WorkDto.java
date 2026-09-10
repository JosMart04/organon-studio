package studio.organon.server.web.dto;

public record WorkDto(
        Long id,
        Long philosopherId,
        String philosopherName,
        String title,
        Integer originalYear,
        String philosophicalProblem,
        String coreThesis,
        Long directAdversaryId,
        String directAdversaryTitle) {
}
