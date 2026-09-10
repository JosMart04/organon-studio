package studio.organon.server.web.dto;

import java.util.Map;

/**
 * Arista serializada para React Flow. Distingue dos familias: las
 * estructurales (autoria, pertenencia), que solo dan armazon al grafo, y las
 * dialecticas, que son el contenido filosofico real del mapa.
 */
public record GraphEdgeDto(
        String id,
        String source,
        String target,
        String type,
        String label,
        boolean animated,
        Map<String, Object> data) {
}
