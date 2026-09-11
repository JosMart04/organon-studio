package studio.organon.server.search.dto;

import java.util.List;

/**
 * @param pending entradas del cuaderno que aun no tienen vector: pueden faltar en la busqueda por significado
 * @param notice  aviso listo para la interfaz, o null si no hay nada que advertir
 */
public record SearchResponseDto(SearchMode mode, List<SearchResultDto> results, int pending, String notice) {
}
