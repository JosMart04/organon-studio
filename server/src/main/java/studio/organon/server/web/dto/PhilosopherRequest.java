package studio.organon.server.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import studio.organon.server.domain.corpus.Epoch;

public record PhilosopherRequest(
        @NotBlank @Size(max = 160) String name,
        @NotNull Epoch epoch,
        @Size(max = 160) String school,
        String biographicalSummary) {
}
