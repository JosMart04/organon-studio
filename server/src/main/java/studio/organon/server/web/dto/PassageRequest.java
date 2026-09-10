package studio.organon.server.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PassageRequest(
        @NotNull Long workId,
        @NotBlank @Size(max = 120) String locator,
        @NotBlank String textContent,
        @PositiveOrZero Integer pageNumber) {
}
