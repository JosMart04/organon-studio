package studio.organon.server.search.dto;

/**
 * Estado del indice de significado, para el pie de la paleta de busqueda.
 *
 * @param available si ahora mismo se puede buscar por significado
 * @param message   explicacion lista para la interfaz; null cuando todo esta en orden
 */
public record IndexStatusDto(int indexed, int pending, String model, boolean available, String message) {
}
