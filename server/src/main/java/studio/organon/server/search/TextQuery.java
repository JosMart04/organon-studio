package studio.organon.server.search;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * Convierte lo que se escribe en una tsquery de PostgreSQL.
 *
 * <p>Mientras se escribe, la ultima palabra casi nunca esta completa: «razo» no
 * es una palabra, y websearch_to_tsquery no encontraria «razón». Por eso la
 * ultima se busca como prefijo y las anteriores enteras.
 *
 * <p>Solo pasan letras y digitos, de modo que la cadena resultante es segura
 * para to_tsquery: ningun operador escrito por el usuario llega a la consulta.
 */
final class TextQuery {

    private static final Pattern PALABRA = Pattern.compile("[\\p{L}\\p{N}]+");

    static final int MAX_PALABRAS = 12;

    private TextQuery() {
    }

    /** Todas las palabras: para ir acotando mientras se escribe en la pestaña de palabras. */
    static Optional<String> allWords(String escrito) {
        return construir(escrito, " & ");
    }

    /**
     * Cualquiera de las palabras: para la parte de texto de la busqueda por
     * significado. Alli la consulta es una frase («el tiempo que se escapa»), y
     * exigirlas todas no encontraria casi nada, justo cuando sin Ollama el texto
     * es lo unico que queda. PostgreSQL descarta las palabras vacias y el ranking
     * pone arriba lo que coincide en mas.
     */
    static Optional<String> anyWord(String escrito) {
        return construir(escrito, " | ");
    }

    private static Optional<String> construir(String escrito, String union) {
        if (escrito == null) {
            return Optional.empty();
        }
        List<String> palabras = PALABRA.matcher(escrito).results()
                .map(MatchResult::group)
                .map(palabra -> palabra.toLowerCase(Locale.ROOT))
                .limit(MAX_PALABRAS)
                .toList();
        if (palabras.isEmpty()) {
            return Optional.empty();
        }
        String ultima = palabras.getLast() + ":*";
        if (palabras.size() == 1) {
            return Optional.of(ultima);
        }
        return Optional.of(String.join(union, palabras.subList(0, palabras.size() - 1)) + union + ultima);
    }
}
