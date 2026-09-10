package studio.organon.server.web.dto;

/**
 * Hallazgo individual. `premiseId` apunta al eslabon exacto cuando el defecto
 * es localizable; queda null cuando afecta a la estructura del argumento entero.
 */
public record AuditFindingDto(
        String code,
        AuditSeverity severity,
        String message,
        Long premiseId) {
}
