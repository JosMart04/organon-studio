package studio.organon.server.web.dto;

import java.util.List;
import studio.organon.server.domain.logic.PremiseType;

public record PremiseDto(
        Long id,
        int orderIndex,
        String statement,
        boolean enthymeme,
        PremiseType premiseType,
        List<ObjectionDto> objections) {
}
