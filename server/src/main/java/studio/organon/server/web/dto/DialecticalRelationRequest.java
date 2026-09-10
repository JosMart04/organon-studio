package studio.organon.server.web.dto;

import jakarta.validation.constraints.NotNull;
import studio.organon.server.domain.dialectic.RelationType;

public record DialecticalRelationRequest(
        @NotNull Long sourceArgumentId,
        @NotNull Long targetArgumentId,
        @NotNull RelationType relationType,
        String description) {
}
