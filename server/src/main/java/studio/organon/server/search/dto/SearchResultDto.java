package studio.organon.server.search.dto;

import java.util.List;

/**
 * Un resultado de busqueda. El enlace lo compone la interfaz con el tipo y los
 * identificadores, que es donde se sabe a que pantalla lleva cada cosa.
 *
 * @param snippet   fragmento del texto; si salio por palabras, las coincidencias van entre ⟦ y ⟧
 * @param workId    libro al que pertenece; en un pensador, el primero de sus libros
 * @param conceptId solo en las definiciones: la palabra del glosario
 * @param relevance de 0 a 1, relativa al mejor resultado de esta misma busqueda
 */
public record SearchResultDto(
        SearchHitType type,
        long id,
        String title,
        String snippet,
        Long workId,
        String workTitle,
        String author,
        Long conceptId,
        double relevance,
        List<MatchSource> matchedBy) {
}
