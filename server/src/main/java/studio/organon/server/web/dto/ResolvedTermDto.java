package studio.organon.server.web.dto;

import java.util.List;

/**
 * Respuesta del resolutor contextual del glosario.
 *
 * @param definition   acepcion que aplica en el contexto de lectura, o null si el autor no fija el termino
 * @param scope        precision con la que se resolvio
 * @param rivalReadings mismas palabras en boca de otros autores; es lo que hace visible la sobrecarga
 */
public record ResolvedTermDto(
        String term,
        TermDefinitionDto definition,
        DefinitionScope scope,
        List<TermDefinitionDto> rivalReadings) {
}
