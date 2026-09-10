package studio.organon.server.web.dto;

import java.util.List;
import studio.organon.server.domain.logic.SoundStatus;

public record AuditReportDto(
        Long argumentId,
        String argumentName,
        SoundStatus soundStatus,
        int premiseCount,
        int enthymemeCount,
        int objectionCount,
        List<AuditFindingDto> findings) {
}
