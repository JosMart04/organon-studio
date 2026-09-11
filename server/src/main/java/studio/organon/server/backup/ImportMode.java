package studio.organon.server.backup;

/**
 * Como se mezcla una copia con lo que ya hay en el cuaderno.
 *
 * <p>MERGE es la opcion segura: anade lo que falta y no toca lo que ya existe.
 * REPLACE vacia el cuaderno antes de restaurar, y tras el commit no hay vuelta
 * atras.
 */
public enum ImportMode {
    MERGE,
    REPLACE
}
