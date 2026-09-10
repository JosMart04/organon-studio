package studio.organon.server.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import studio.organon.server.domain.logic.FormalScheme;

public record ArgumentRequest(
        @NotNull Long workId,
        Long passageId,
        @NotBlank @Size(max = 240) String name,
        @NotNull FormalScheme formalScheme,
        String latexFormalization,
        @Valid List<PremiseRequest> premises) {
}
