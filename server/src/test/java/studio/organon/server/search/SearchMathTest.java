package studio.organon.server.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SearchMathTest {

    @Test
    @DisplayName("Normalizar deja el vector con norma 1, y su coseno consigo mismo vale 1")
    void normalizar() {
        float[] vector = SearchMath.normalize(new float[] {3, 4});

        assertThat(vector[0]).isCloseTo(0.6f, within(1e-6f));
        assertThat(vector[1]).isCloseTo(0.8f, within(1e-6f));
        assertThat(SearchMath.cosine(vector, vector)).isCloseTo(1.0, within(1e-6));
    }

    @Test
    @DisplayName("Un vector nulo no provoca una division por cero")
    void vectorNulo() {
        assertThat(SearchMath.normalize(new float[] {0, 0})).containsExactly(0f, 0f);
    }

    @Test
    @DisplayName("Vectores ortogonales no se parecen, y dimensiones distintas no se comparan")
    void coseno() {
        assertThat(SearchMath.cosine(new float[] {1, 0}, new float[] {0, 1})).isZero();
        assertThat(SearchMath.cosine(new float[] {1, 0}, new float[] {1, 0, 0})).isZero();
    }

    @Test
    @DisplayName("En la fusion sube lo que aparece en las dos listas, y los empates respetan el orden de llegada")
    void fusionPremiaLoComun() {
        var fusion = SearchMath.reciprocalRankFusion(List.of(
                List.of("PASSAGE:1", "PASSAGE:2", "ARGUMENT:7"),
                List.of("ARGUMENT:7", "PASSAGE:9")));

        // ARGUMENT:7 = 1/63 + 1/61; PASSAGE:1 = 1/61; PASSAGE:2 y PASSAGE:9 = 1/62.
        assertThat(fusion.keySet()).containsExactly("ARGUMENT:7", "PASSAGE:1", "PASSAGE:2", "PASSAGE:9");
    }

    @Test
    @DisplayName("Los vecinos pasan el corte absoluto y el relativo al mejor")
    void vecinosConDosCortes() {
        Map<String, float[]> vectores = new LinkedHashMap<>();
        vectores.put("PASSAGE:1", SearchMath.normalize(new float[] {0.9f, 0.436f}));
        vectores.put("PASSAGE:2", SearchMath.normalize(new float[] {0.5f, 0.866f}));
        vectores.put("PASSAGE:3", SearchMath.normalize(new float[] {0.3f, 0.954f}));
        vectores.put("PASSAGE:4", SearchMath.normalize(new float[] {0.1f, 0.995f}));

        var vecinos = SearchMath.nearest(new float[] {1, 0}, vectores, 0.2, 0.5, 10);

        // 0,9 y 0,5 pasan; 0,3 no llega a la mitad del mejor; 0,1 no llega al minimo.
        assertThat(vecinos).extracting(SearchMath.Scored::key).containsExactly("PASSAGE:1", "PASSAGE:2");
    }

    @Test
    @DisplayName("Si nada llega al minimo no hay vecinos")
    void sinVecinos() {
        Map<String, float[]> vectores = Map.of("PASSAGE:1", new float[] {0, 1});

        assertThat(SearchMath.nearest(new float[] {1, 0}, vectores, 0.2, 0.5, 10)).isEmpty();
    }

    @Test
    @DisplayName("El hash es SHA-256 en hexadecimal")
    void hash() {
        assertThat(SearchMath.sha256("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
