package studio.organon.server.domain.logic;

/** Estatuto epistemico de cada enunciado dentro de la reconstruccion. */
public enum PremiseType {
    AXIOMATICA,
    EMPIRICA,
    DEFINICION,
    INFERENCIA_INTERMEDIA,
    CONCLUSION
}
