package studio.organon.server.web.dto;

import studio.organon.server.domain.dialectic.RelationType;

public record DialecticalRelationDto(
        Long id,
        Long sourceArgumentId,
        String sourceArgumentName,
        Long targetArgumentId,
        String targetArgumentName,
        RelationType relationType,
        String description) {
}
