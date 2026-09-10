package studio.organon.server.domain.logic;

/** Esquema inferencial al que se reduce el argumento una vez reconstruido. */
public enum FormalScheme {
    MODUS_PONENS,
    MODUS_TOLLENS,
    SILOGISMO_CATEGORICO,
    SILOGISMO_DISYUNTIVO,
    SILOGISMO_HIPOTETICO,
    REDUCTIO_AD_ABSURDUM,
    INDUCTIVE,
    ABDUCTIVE,
    TRASCENDENTAL,
    ANALOGICO,
    NO_CLASIFICADO
}
