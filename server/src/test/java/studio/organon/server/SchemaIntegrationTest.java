package studio.organon.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Levanta el contexto completo contra el PostgreSQL local. Comprueba de paso lo
 * que ninguna prueba unitaria puede: que Flyway aplique la migracion y que
 * Hibernate valide el mapeo contra el esquema resultante, que es donde se
 * detecta una entidad desalineada con su tabla.
 *
 * <p>Etiquetada como integracion y excluida por defecto, porque exige una base
 * de datos viva. Para ejecutarla:
 * {@code .\mvnw.cmd test -Dexcluded.test.groups=}
 */
@Tag("integracion")
@SpringBootTest
class SchemaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Flyway crea el esquema completo y Hibernate lo valida")
    void esquemaMigradoYValidado() {
        Integer tablas = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public'",
                Integer.class);
        assertThat(tablas).isGreaterThanOrEqualTo(10);

        Integer migraciones = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class);
        assertThat(migraciones).isPositive();
    }

    @Test
    @DisplayName("La busqueda en espanol ignora las tildes")
    void busquedaSinTildes() {
        Boolean coincide = jdbc.queryForObject(
                "SELECT to_tsvector('es_organon', 'La razón pura') @@ to_tsquery('es_organon', 'razon')",
                Boolean.class);
        assertThat(coincide).isTrue();
    }

    @Test
    @DisplayName("Las columnas de busqueda son generadas y existe la tabla de vectores")
    void columnasDeBusqueda() {
        List<String> tablas = jdbc.queryForList("""
                SELECT table_name FROM information_schema.columns
                WHERE table_schema = 'public' AND column_name = 'search_vector' AND is_generated = 'ALWAYS'
                ORDER BY table_name
                """, String.class);
        assertThat(tablas).containsExactly(
                "argument", "passage", "philosopher", "premise", "semantic_concept", "term_definition");

        Integer vectores = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'semantic_embedding'",
                Integer.class);
        assertThat(vectores).isOne();
    }

    @Test
    @DisplayName("La unicidad del ambito de definicion trata los nulos como iguales")
    void definicionesGeneralesUnicasPorAutor() {
        String indexDef = jdbc.queryForObject(
                "SELECT indexdef FROM pg_indexes WHERE indexname = 'uk_term_definition_scope'",
                String.class);
        assertThat(indexDef).contains("NULLS NOT DISTINCT");
    }
}
