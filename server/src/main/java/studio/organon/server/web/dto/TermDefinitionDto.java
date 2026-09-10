package studio.organon.server.web.dto;

public record TermDefinitionDto(
        Long id,
        Long conceptId,
        String term,
        Long philosopherId,
        String philosopherName,
        Long workId,
        String workTitle,
        String operationalDefinition,
        String notes) {
}
