package studio.organon.server.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExtractIdeasRequest(
        @NotBlank @Size(max = 8000) String text,
        String author,
        String workTitle) {
}
