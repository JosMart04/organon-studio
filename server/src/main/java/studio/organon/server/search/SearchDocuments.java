package studio.organon.server.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import studio.organon.server.search.dto.SearchHitType;

/**
 * Lee del cuaderno lo que se indexa por significado.
 *
 * <p>Con SQL y no con JPA: son lecturas en bloque de unas pocas columnas, y
 * recorrer las entidades cargaria relaciones que aqui no hacen falta.
 */
@Component
class SearchDocuments {

    private final JdbcTemplate jdbc;

    SearchDocuments(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<IndexableItem> all() {
        List<IndexableItem> items = new ArrayList<>();

        // Un fragmento: la cita y, aparte, lo que el lector penso de ella.
        items.addAll(jdbc.query("""
                SELECT p.id, p.text_content, p.personal_notes, w.title AS work_title, ph.name AS author
                FROM passage p
                JOIN work w ON w.id = p.work_id
                JOIN philosopher ph ON ph.id = w.philosopher_id
                ORDER BY p.id
                """, (rs, fila) -> new IndexableItem(
                SearchHitType.PASSAGE,
                rs.getLong("id"),
                rs.getString("work_title") + ", de " + rs.getString("author"),
                unir(rs.getString("text_content"), conPrefijo("Notas del lector: ", rs.getString("personal_notes"))))));

        // Una idea con sus razones en orden: el resultado es la idea entera, no una razon suelta.
        items.addAll(jdbc.query("""
                SELECT a.id, a.name, w.title AS work_title, ph.name AS author,
                       string_agg(pr.statement, E'\\n' ORDER BY pr.order_index) AS premises
                FROM argument a
                JOIN work w ON w.id = a.work_id
                JOIN philosopher ph ON ph.id = w.philosopher_id
                LEFT JOIN premise pr ON pr.argument_id = a.id
                GROUP BY a.id, a.name, w.title, ph.name
                ORDER BY a.id
                """, (rs, fila) -> new IndexableItem(
                SearchHitType.ARGUMENT,
                rs.getLong("id"),
                rs.getString("name") + " (" + rs.getString("author") + ", " + rs.getString("work_title") + ")",
                unir(rs.getString("premises"), rs.getString("premises") == null ? rs.getString("name") : null))));

        items.addAll(jdbc.query("""
                SELECT id, name, epoch, school, biographical_summary
                FROM philosopher
                ORDER BY id
                """, (rs, fila) -> new IndexableItem(
                SearchHitType.PHILOSOPHER,
                rs.getLong("id"),
                rs.getString("name"),
                unir("Época " + rs.getString("epoch").toLowerCase(Locale.ROOT),
                        rs.getString("school"),
                        rs.getString("biographical_summary")))));

        items.addAll(jdbc.query("""
                SELECT d.id, c.term, ph.name AS author, w.title AS work_title, d.operational_definition, d.notes
                FROM term_definition d
                JOIN semantic_concept c ON c.id = d.concept_id
                JOIN philosopher ph ON ph.id = d.philosopher_id
                LEFT JOIN work w ON w.id = d.work_id
                ORDER BY d.id
                """, (rs, fila) -> new IndexableItem(
                SearchHitType.DEFINITION,
                rs.getLong("id"),
                rs.getString("term") + " según " + rs.getString("author")
                        + (rs.getString("work_title") == null ? "" : ", en " + rs.getString("work_title")),
                unir(rs.getString("operational_definition"), conPrefijo("Notas: ", rs.getString("notes"))))));

        return items;
    }

    static String unir(String... partes) {
        return Arrays.stream(partes)
                .filter(parte -> parte != null && !parte.isBlank())
                .map(String::strip)
                .collect(Collectors.joining("\n"));
    }

    private static String conPrefijo(String prefijo, String valor) {
        return valor == null || valor.isBlank() ? null : prefijo + valor;
    }
}
