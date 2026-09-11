package studio.organon.server.search.dto;

/** Por que ha salido un resultado: por compartir palabras con la consulta, por parecerse en significado, o por las dos. */
public enum MatchSource {
    TEXTO,
    SIGNIFICADO
}
