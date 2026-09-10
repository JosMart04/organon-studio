package studio.organon.server.domain.logic;

/**
 * Veredicto de la auditoria. PENDIENTE es el estado por defecto: un argumento
 * recien transcrito no ha sido sometido a prueba todavia.
 */
public enum SoundStatus {
    PENDIENTE,
    SOLIDO,
    FALAZ
}
