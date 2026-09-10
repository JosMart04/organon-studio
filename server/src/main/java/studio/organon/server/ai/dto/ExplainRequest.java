package studio.organon.server.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param text    el pasaje denso tal cual
 * @param author  a quien se esta leyendo, si se sabe: cambia mucho la explicacion
 */
public record ExplainRequest(
        @NotBlank @Size(max = 8000) String text,
        String author,
        String workTitle) {
}
