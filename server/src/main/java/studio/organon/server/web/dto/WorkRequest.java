package studio.organon.server.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WorkRequest(
        @NotNull Long philosopherId,
        @NotBlank @Size(max = 240) String title,
        Integer originalYear,
        String philosophicalProblem,
        String coreThesis,
        Long directAdversaryId) {
}
