package studio.organon.server.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TermDefinitionRequest(
        @NotNull Long conceptId,
        @NotNull Long philosopherId,
        Long workId,
        @NotBlank String operationalDefinition,
        String notes) {
}
