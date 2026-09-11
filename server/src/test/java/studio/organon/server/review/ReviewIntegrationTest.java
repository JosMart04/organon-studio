package studio.organon.server.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import studio.organon.server.domain.corpus.Epoch;
import studio.organon.server.domain.logic.FormalScheme;
import studio.organon.server.domain.logic.PremiseType;
import studio.organon.server.domain.review.ChallengeKind;
import studio.organon.server.domain.review.ReviewAttempt;
import studio.organon.server.domain.review.ReviewRating;
import studio.organon.server.error.ConflictException;
import studio.organon.server.repository.ArgumentRepository;
import studio.organon.server.repository.ReviewAttemptRepository;
import studio.organon.server.review.dto.ReviewAttemptDto;
import studio.organon.server.review.dto.ReviewProgressDto;
import studio.organon.server.service.ArgumentService;
import studio.organon.server.service.CorpusService;
import studio.organon.server.web.dto.ArgumentRequest;
import studio.organon.server.web.dto.PhilosopherRequest;
import studio.organon.server.web.dto.PremiseRequest;
import studio.organon.server.web.dto.WorkRequest;

/**
 * El repaso contra el PostgreSQL real, sin tocar el modelo: que idea toca, el
 * progreso, el historial y las comprobaciones que ocurren antes de llamarlo.
 *
 * <p>Las ideas se crean en un libro propio y la eleccion se filtra por el, asi
 * que lo que haya en la base de desarrollo no influye. Todo se deshace al acabar.
 */
@Tag("integracion")
@Transactional
@SpringBootTest(properties = {"organon.seed.enabled=false", "organon.search.reindex-on-startup=false"})
class ReviewIntegrationTest {

    @Autowired private CorpusService corpus;
    @Autowired private ArgumentService arguments;
    @Autowired private ArgumentRepository argumentRepository;
    @Autowired private ReviewAttemptRepository attempts;
    @Autowired private ReviewService review;

    private long libro;

    @BeforeEach
    void cuaderno() {
        long autora = corpus.createPhilosopher(
                new PhilosopherRequest("Pensadora de prueba de repaso", Epoch.MODERNA, null, null, null)).id();
        libro = corpus.createWork(new WorkRequest(autora, "Tratado de prueba de repaso", null, null, null, null)).id();
    }

    @Test
    @DisplayName("Toca primero lo nunca repasado, después lo flojo, y nunca una idea sin razones")
    void ordenDeRepaso() {
        long floja = idea("Idea que salió floja", true);
        long nueva = idea("Idea nunca repasada", true);
        idea("Idea sin razones", false);
        intento(floja, ReviewRating.FLOJA);

        assertThat(review.next(null, libro, null).orElseThrow().argumentId()).isEqualTo(nueva);
        assertThat(review.next(null, libro, nueva).orElseThrow().argumentId()).isEqualTo(floja);
    }

    @Test
    @DisplayName("La tarjeta separa las razones de la conclusión y cuenta los intentos")
    void tarjeta() {
        long id = idea("Idea con tarjeta", true);
        intento(id, ReviewRating.A_MEDIAS);

        var tarjeta = review.next(id, null, null).orElseThrow();

        assertThat(tarjeta.reasons()).containsExactly("Una razón para idea con tarjeta");
        assertThat(tarjeta.conclusion()).isEqualTo("Luego idea con tarjeta");
        assertThat(tarjeta.attempts()).isOne();
        assertThat(tarjeta.lastRating()).isEqualTo(ReviewRating.A_MEDIAS);
        assertThat(tarjeta.author()).isEqualTo("Pensadora de prueba de repaso");
    }

    @Test
    @DisplayName("El progreso cuenta lo repasado esta semana y lo que nunca se ha repasado")
    void progreso() {
        ReviewProgressDto antes = review.progress();
        long repasada = idea("Idea repasada", true);
        idea("Idea pendiente", true);
        idea("Idea sin razones", false);
        intento(repasada, ReviewRating.SOLIDA);

        ReviewProgressDto despues = review.progress();

        assertThat(despues.reviewable() - antes.reviewable()).isEqualTo(2);
        assertThat(despues.neverReviewed() - antes.neverReviewed()).isEqualTo(1);
        assertThat(despues.reviewedThisWeek() - antes.reviewedThisWeek()).isEqualTo(1);
    }

    @Test
    @DisplayName("El historial devuelve los intentos, el más reciente primero")
    void historial() {
        long id = idea("Idea con historial", true);
        long primero = intento(id, ReviewRating.FLOJA).getId();
        long segundo = intento(id, null).getId();

        assertThat(review.history(id)).extracting(ReviewAttemptDto::id).containsExactly(segundo, primero);
    }

    @Test
    @DisplayName("Una respuesta vacía o a un desafío ya respondido se rechaza antes de llamar al modelo")
    void comprobacionesAntesDelModelo() {
        long id = idea("Idea ya respondida", true);
        long respondido = intento(id, ReviewRating.SOLIDA).getId();

        assertThatThrownBy(() -> review.answer(respondido, "   ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> review.answer(respondido, "Otra respuesta")).isInstanceOf(ConflictException.class);
    }

    // ----- utilidades -----------------------------------------------------

    private long idea(String nombre, boolean conRazones) {
        List<PremiseRequest> razones = conRazones
                ? List.of(new PremiseRequest(null, "Una razón para " + nombre.toLowerCase(), false, PremiseType.EMPIRICA),
                        new PremiseRequest(null, "Luego " + nombre.toLowerCase(), false, PremiseType.CONCLUSION))
                : List.of();
        return arguments.createArgument(new ArgumentRequest(libro, null, nombre, FormalScheme.NO_CLASIFICADO, null, razones))
                .id();
    }

    private ReviewAttempt intento(long ideaId, ReviewRating valoracion) {
        ReviewAttempt intento = new ReviewAttempt(argumentRepository.getReferenceById(ideaId),
                ChallengeKind.CONTRAEJEMPLO, "¿Cómo lo defenderías?", null, null);
        if (valoracion != null) {
            intento.recordAnswer("Así lo defendería.", valoracion, "Bien planteado.", "Falta un ejemplo.", null,
                    Instant.now());
        }
        return attempts.save(intento);
    }
}
