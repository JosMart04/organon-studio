-- ===========================================================================
-- Busqueda en el cuaderno
--
-- Dos mitades. Texto completo en espanol, que funciona siempre y sin Ollama; y
-- una tabla de vectores de significado que rellena el modelo de embeddings
-- local cuando esta disponible.
--
-- pgvector no esta instalado en esta maquina (en Windows hay que compilarlo con
-- Visual Studio), asi que los vectores se guardan como REAL[] y el coseno se
-- calcula en Java. Para un cuaderno personal son milisegundos.
-- ===========================================================================


-- ---------------------------------------------------------------------------
-- Texto completo
-- ---------------------------------------------------------------------------

-- Extension "trusted" desde PostgreSQL 13: basta con ser dueno de la base.
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Espanol sin tildes: «razon» encuentra «razón». unaccent va antes del stemmer
-- para que las dos formas lleguen iguales a la raiz.
CREATE TEXT SEARCH CONFIGURATION es_organon (COPY = spanish);
ALTER TEXT SEARCH CONFIGURATION es_organon
    ALTER MAPPING FOR hword, hword_part, word WITH unaccent, spanish_stem;

-- Columnas generadas: PostgreSQL las mantiene al dia en cada escritura. No se
-- mapean en JPA, de modo que Hibernate nunca intenta escribir en ellas y
-- ddl-auto=validate las ignora.

ALTER TABLE philosopher ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
    setweight(to_tsvector('es_organon', coalesce(name, '')), 'A') ||
    setweight(to_tsvector('es_organon', coalesce(school, '')), 'B') ||
    setweight(to_tsvector('es_organon', coalesce(biographical_summary, '')), 'C')
) STORED;

ALTER TABLE passage ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
    setweight(to_tsvector('es_organon', coalesce(text_content, '')), 'A') ||
    setweight(to_tsvector('es_organon', coalesce(personal_notes, '')), 'B')
) STORED;

ALTER TABLE argument ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
    setweight(to_tsvector('es_organon', coalesce(name, '')), 'A')
) STORED;

ALTER TABLE premise ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
    setweight(to_tsvector('es_organon', coalesce(statement, '')), 'B')
) STORED;

ALTER TABLE semantic_concept ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
    setweight(to_tsvector('es_organon', coalesce(term, '')), 'A') ||
    setweight(to_tsvector('es_organon', coalesce(description, '')), 'C')
) STORED;

ALTER TABLE term_definition ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
    setweight(to_tsvector('es_organon', coalesce(operational_definition, '')), 'A') ||
    setweight(to_tsvector('es_organon', coalesce(notes, '')), 'B')
) STORED;

CREATE INDEX idx_philosopher_search      ON philosopher      USING GIN (search_vector);
CREATE INDEX idx_passage_search          ON passage          USING GIN (search_vector);
CREATE INDEX idx_argument_search         ON argument         USING GIN (search_vector);
CREATE INDEX idx_premise_search          ON premise          USING GIN (search_vector);
CREATE INDEX idx_semantic_concept_search ON semantic_concept USING GIN (search_vector);
CREATE INDEX idx_term_definition_search  ON term_definition  USING GIN (search_vector);


-- ---------------------------------------------------------------------------
-- Vectores de significado
-- ---------------------------------------------------------------------------

CREATE TABLE semantic_embedding (
    entity_type   VARCHAR(20) NOT NULL,
    entity_id     BIGINT      NOT NULL,
    model         VARCHAR(80) NOT NULL,
    dimensions    INTEGER     NOT NULL,
    embedding     REAL[]      NOT NULL,
    content_hash  CHAR(64)    NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_semantic_embedding PRIMARY KEY (entity_type, entity_id, model),
    CONSTRAINT ck_semantic_embedding_type CHECK (
        entity_type IN ('PASSAGE', 'ARGUMENT', 'PHILOSOPHER', 'DEFINITION')
    ),
    CONSTRAINT ck_semantic_embedding_dimensions CHECK (dimensions = cardinality(embedding))
);

COMMENT ON TABLE semantic_embedding IS
    'Derivada: se regenera sola desde el cuaderno y no entra en las copias de seguridad. Sin claves foraneas porque apunta a cuatro tablas; el indexador retira lo que ya no existe.';

COMMENT ON COLUMN semantic_embedding.content_hash IS
    'SHA-256 del texto exacto enviado al modelo, prefijos y nombre del modelo incluidos: solo se recalcula lo que cambia.';

COMMENT ON COLUMN semantic_embedding.embedding IS
    'Vector normalizado a norma 1, de modo que el coseno es un producto escalar.';
