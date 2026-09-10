package studio.organon.server.domain.dialectic;

/**
 * Modo en que un argumento se relaciona con otro. No son sinonimos: REFUTA
 * niega, PRESUPONE hereda, EXTIENDE amplia el alcance, RADICALIZA lleva la
 * premisa mas lejos que su autor y MATIZA restringe sin negar.
 */
public enum RelationType {
    REFUTA,
    PRESUPONE,
    EXTIENDE,
    RADICALIZA,
    MATIZA
}
