package studio.organon.server.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param claim          la tesis que se quiere contrastar
 * @param author         quien la sostiene, para que el modelo no lo proponga como su propio rival
 * @param knownThinkers  pensadores que ya estan en el cuaderno; se le pide al modelo que
 *                       los prefiera, para que la conexion sugerida se pueda crear de verdad
 */
public record FindRivalsRequest(
        @NotBlank @Size(max = 4000) String claim,
        String author,
        java.util.List<String> knownThinkers) {
}
