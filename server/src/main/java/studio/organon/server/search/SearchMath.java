package studio.organon.server.search;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Las cuentas de la busqueda, sin dependencias: se prueban sin base de datos ni Ollama. */
final class SearchMath {

    /** Constante de Reciprocal Rank Fusion: 60 es la del articulo original y la habitual. */
    static final int RRF_K = 60;

    private SearchMath() {
    }

    record Scored(String key, double score) {
    }

    /** Vector de norma 1: asi el coseno se reduce a un producto escalar. */
    static float[] normalize(float[] vector) {
        double suma = 0;
        for (float x : vector) {
            suma += (double) x * x;
        }
        float[] normalizado = new float[vector.length];
        if (suma == 0) {
            return normalizado;
        }
        double norma = Math.sqrt(suma);
        for (int i = 0; i < vector.length; i++) {
            normalizado[i] = (float) (vector[i] / norma);
        }
        return normalizado;
    }

    /** Coseno entre dos vectores ya normalizados. Con dimensiones distintas no hay nada que comparar. */
    static double cosine(float[] a, float[] b) {
        if (a.length != b.length) {
            return 0;
        }
        double producto = 0;
        for (int i = 0; i < a.length; i++) {
            producto += (double) a[i] * b[i];
        }
        return producto;
    }

    /**
     * Los vectores mas parecidos a la consulta.
     *
     * <p>Todo se parece un poco a todo, asi que hay dos cortes: uno absoluto, y
     * otro relativo al mejor resultado, que es el que evita que una consulta sin
     * nada relevante devuelva medio cuaderno.
     */
    static List<Scored> nearest(float[] consulta, Map<String, float[]> vectores,
                                double minima, double relativa, int limite) {
        List<Scored> puntuados = vectores.entrySet().stream()
                .map(e -> new Scored(e.getKey(), cosine(consulta, e.getValue())))
                .filter(s -> s.score() >= minima)
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .toList();
        if (puntuados.isEmpty()) {
            return puntuados;
        }
        double corte = puntuados.getFirst().score() * relativa;
        return puntuados.stream().filter(s -> s.score() >= corte).limit(limite).toList();
    }

    /**
     * Fusiona rankings cuyas puntuaciones no estan en la misma escala —ts_rank_cd
     * y el coseno no se pueden sumar— usando solo la posicion: cada aparicion
     * aporta 1/(k + posicion). Lo que sale en las dos listas sube.
     *
     * @return claves y puntuacion de mayor a menor; a igualdad, en el orden en
     *         que aparecieron por primera vez
     */
    static LinkedHashMap<String, Double> reciprocalRankFusion(List<List<String>> rankings) {
        Map<String, Double> puntos = new LinkedHashMap<>();
        for (List<String> ranking : rankings) {
            for (int posicion = 0; posicion < ranking.size(); posicion++) {
                puntos.merge(ranking.get(posicion), 1.0 / (RRF_K + posicion + 1), Double::sum);
            }
        }
        LinkedHashMap<String, Double> ordenados = new LinkedHashMap<>();
        puntos.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEachOrdered(e -> ordenados.put(e.getKey(), e.getValue()));
        return ordenados;
    }

    static String sha256(String texto) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("La JVM no incluye SHA-256", e);
        }
    }
}
