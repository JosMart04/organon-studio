package studio.organon.server.web.dto;

import java.util.Map;

/**
 * Nodo serializado tal como lo espera React Flow. `type` selecciona el
 * componente personalizado del cliente: philosopher, work o argument.
 */
public record GraphNodeDto(
        String id,
        String type,
        Map<String, Object> data,
        GraphPositionDto position) {
}
