package studio.organon.server.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SemanticConceptRequest(
        @NotBlank @Size(max = 120) String term,
        String description) {
}
