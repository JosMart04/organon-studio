package studio.organon.server.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import studio.organon.server.domain.logic.ObjectionType;

public record ObjectionRequest(
        @NotNull ObjectionType objectionType,
        @NotBlank String explanation) {
}
