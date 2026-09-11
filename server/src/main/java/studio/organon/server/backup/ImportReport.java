package studio.organon.server.backup;

import java.util.List;
import java.util.Map;

/**
 * Resultado de una restauracion.
 *
 * @param created  por tipo, cuantas entradas se han anadido
 * @param skipped  por tipo, cuantas se han omitido porque ya estaban
 * @param warnings situaciones que no impiden restaurar pero conviene saber
 */
public record ImportReport(
        ImportMode mode,
        Map<String, Integer> created,
        Map<String, Integer> skipped,
        List<String> warnings) {
}
