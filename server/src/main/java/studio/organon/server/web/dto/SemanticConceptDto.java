package studio.organon.server.web.dto;

public record SemanticConceptDto(
        Long id,
        String term,
        String description,
        int definitionCount) {
}
