package studio.organon.server.web.dto;

import java.util.List;
import studio.organon.server.domain.logic.FormalScheme;
import studio.organon.server.domain.logic.SoundStatus;

public record ArgumentDto(
        Long id,
        Long workId,
        String workTitle,
        String philosopherName,
        Long passageId,
        String passageLocator,
        String name,
        FormalScheme formalScheme,
        String latexFormalization,
        SoundStatus soundStatus,
        List<PremiseDto> premises) {
}
