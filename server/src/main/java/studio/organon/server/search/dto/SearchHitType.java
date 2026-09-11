package studio.organon.server.search.dto;

/** Que clase de entrada del cuaderno es un resultado. Coincide con semantic_embedding.entity_type. */
public enum SearchHitType {
    PASSAGE,
    ARGUMENT,
    PHILOSOPHER,
    DEFINITION
}
