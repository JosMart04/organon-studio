package studio.organon.server.search;

import java.util.Locale;

/**
 * El texto exacto que se envia al modelo de embeddings.
 *
 * <p>Algunos modelos se entrenan con prefijos que distinguen consultas de
 * documentos. EmbeddingGemma es uno (su ficha de modelo los documenta), y con
 * notas en espanol separan mucho mejor lo relevante de lo que no: buscando «lo
 * efimero de la existencia», la carta de Epicuro sobre la muerte frente a un
 * texto de Kant pasa de 0,58 contra 0,29 sin prefijos a 0,45 contra 0,15 con
 * ellos.
 *
 * <p>Un modelo sin prefijos conocidos recibe el texto tal cual: cambiar de
 * modelo es tocar esta clase, no el indexador.
 */
public final class EmbeddingPrompts {

    private EmbeddingPrompts() {
    }

    public static String query(String model, String text) {
        return isEmbeddingGemma(model) ? "task: search result | query: " + text : text;
    }

    public static String document(String model, String title, String text) {
        boolean sinTitulo = title == null || title.isBlank();
        if (isEmbeddingGemma(model)) {
            return "title: " + (sinTitulo ? "none" : title) + " | text: " + text;
        }
        return sinTitulo ? text : title + "\n" + text;
    }

    static boolean isEmbeddingGemma(String model) {
        return model != null && model.toLowerCase(Locale.ROOT).startsWith("embeddinggemma");
    }
}
