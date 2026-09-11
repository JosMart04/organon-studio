package studio.organon.server.domain.review;

import java.text.Normalizer;
import java.util.Locale;

/** Como salio un repaso. Mide la solidez del razonamiento del lector, no si coincide con el autor. */
public enum ReviewRating {
    SOLIDA,
    A_MEDIAS,
    FLOJA;

    /**
     * Lo que devuelve el modelo, normalizado. Un modelo local escribe «Sólida»,
     * «a medias» o se inventa otra palabra; lo que no se reconoce se queda en el
     * termino medio, que es lo menos injusto de atribuir a un lector.
     */
    public static ReviewRating fromModel(String value) {
        if (value == null) {
            return A_MEDIAS;
        }
        String normalizado = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .strip()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[\\s-]+", "_");
        for (ReviewRating rating : values()) {
            if (rating.name().equals(normalizado)) {
                return rating;
            }
        }
        return A_MEDIAS;
    }
}
