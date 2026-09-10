package studio.organon.server.web.dto;

/**
 * Peso de un hallazgo de la auditoria.
 *
 * <p>BLOQUEANTE marca un defecto estructural o una objecion que invalida la
 * inferencia. ADVERTENCIA senala algo que exige vigilancia critica — un
 * entimema sin examinar, por ejemplo — sin condenar el argumento.
 */
public enum AuditSeverity {
    BLOQUEANTE,
    ADVERTENCIA,
    INFORMATIVA
}
