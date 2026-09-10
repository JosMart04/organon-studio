package studio.organon.server.web.dto;

import java.util.List;

/** Un concepto con todas sus lecturas rivales, listo para el comparador lado a lado. */
public record ConceptComparisonDto(
        Long conceptId,
        String term,
        String description,
        List<TermDefinitionDto> readings) {
}
