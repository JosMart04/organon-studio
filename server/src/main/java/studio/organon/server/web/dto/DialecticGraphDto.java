package studio.organon.server.web.dto;

import java.util.List;

public record DialecticGraphDto(
        List<GraphNodeDto> nodes,
        List<GraphEdgeDto> edges) {
}
