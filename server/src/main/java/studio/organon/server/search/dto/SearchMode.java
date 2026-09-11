package studio.organon.server.search.dto;

public enum SearchMode {
    /** Palabras y significado, fusionados. */
    HIBRIDA,
    /** Solo palabras: Ollama o el modelo de embeddings no estan disponibles. */
    SOLO_TEXTO
}
