package studio.organon.server.ai.dto;

import java.util.List;

/**
 * Propuesta de desglose. Es exactamente eso: una propuesta. Nada se guarda
 * hasta que el lector la revisa y la aprueba en el constructor.
 *
 * @param mainClaim          lo que el autor sostiene, en una frase
 * @param reasons            las razones con que lo sostiene, en el orden del texto
 * @param unstatedAssumptions lo que da por obvio sin decirlo, que suele ser el punto flojo
 */
public record ExtractedIdeas(
        String mainClaim,
        List<String> reasons,
        List<String> unstatedAssumptions) {
}
