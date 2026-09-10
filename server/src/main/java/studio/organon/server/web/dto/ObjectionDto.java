package studio.organon.server.web.dto;

import studio.organon.server.domain.logic.ObjectionType;

public record ObjectionDto(
        Long id,
        Long premiseId,
        ObjectionType objectionType,
        String explanation) {
}
