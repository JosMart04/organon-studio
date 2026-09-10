package studio.organon.server.web.dto;

/**
 * Precision con la que se resolvio un termino. OBRA gana siempre a AUTOR:
 * cuando un filosofo redefine un termino dentro de una obra, esa acepcion
 * desplaza a la que emplea en el resto de su corpus.
 */
public enum DefinitionScope {
    OBRA,
    AUTOR,
    SIN_DEFINICION
}
