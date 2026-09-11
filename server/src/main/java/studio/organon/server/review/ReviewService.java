package studio.organon.server.review;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import studio.organon.server.ai.SocraticService;
import studio.organon.server.ai.dto.Challenge;
import studio.organon.server.ai.dto.Evaluation;
import studio.organon.server.ai.dto.ReviewSubject;
import studio.organon.server.domain.logic.Argument;
import studio.organon.server.domain.logic.Premise;
import studio.organon.server.domain.logic.PremiseType;
import studio.organon.server.domain.review.ReviewAttempt;
import studio.organon.server.domain.review.ReviewRating;
import studio.organon.server.error.ConflictException;
import studio.organon.server.error.NotFoundException;
import studio.organon.server.repository.ArgumentRepository;
import studio.organon.server.repository.ReviewAttemptRepository;
import studio.organon.server.review.dto.ReviewAttemptDto;
import studio.organon.server.review.dto.ReviewCardDto;
import studio.organon.server.review.dto.ReviewProgressDto;

/**
 * El repaso socratico: elegir que idea toca, pedir un desafio al asistente y
 * guardar lo que el lector responde.
 *
 * <p>Las transacciones van alrededor de la llamada al modelo, nunca a traves de
 * ella: un modelo local en CPU puede tardar minutos, y retener una conexion de la
 * base mientras tanto agotaria el pool con dos pestanas abiertas.
 */
@Service
public class ReviewService {

    static final int MAX_RESPUESTA = 4000;

    /** Ideas con al menos una razon, con la valoracion y la fecha de su ultimo repaso respondido. */
    private static final String CANDIDATOS = """
            SELECT a.id, ultimo.rating, ultimo.answered_at
            FROM argument a
            LEFT JOIN LATERAL (
                SELECT ra.rating, ra.answered_at
                FROM review_attempt ra
                WHERE ra.argument_id = a.id AND ra.answered_at IS NOT NULL
                ORDER BY ra.answered_at DESC, ra.id DESC
                LIMIT 1
            ) ultimo ON true
            WHERE EXISTS (SELECT 1 FROM premise p WHERE p.argument_id = a.id)
              AND (CAST(? AS bigint) IS NULL OR a.work_id = ?)
            """;

    private final ArgumentRepository argumentRepository;
    private final ReviewAttemptRepository attemptRepository;
    private final SocraticService socratic;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate lectura;
    private final TransactionTemplate escritura;

    public ReviewService(ArgumentRepository argumentRepository,
                         ReviewAttemptRepository attemptRepository,
                         SocraticService socratic,
                         JdbcTemplate jdbc,
                         PlatformTransactionManager transactionManager) {
        this.argumentRepository = argumentRepository;
        this.attemptRepository = attemptRepository;
        this.socratic = socratic;
        this.jdbc = jdbc;
        this.escritura = new TransactionTemplate(transactionManager);
        this.lectura = new TransactionTemplate(transactionManager);
        this.lectura.setReadOnly(true);
    }

    /**
     * La tarjeta de una idea concreta o, sin ella, la que toca repasar.
     *
     * @param after la idea que se acaba de repasar, para no proponerla otra vez enseguida
     */
    public Optional<ReviewCardDto> next(Long argumentId, Long workId, Long after) {
        Optional<Long> elegida = argumentId != null ? Optional.of(argumentId) : elegir(workId, after);
        return elegida.map(id -> lectura.execute(estado -> tarjeta(requireArgument(id))));
    }

    public ReviewAttemptDto challenge(Long argumentId, Long workId) {
        long elegida = argumentId != null
                ? argumentId
                : elegir(workId, null).orElseThrow(() ->
                        new IllegalArgumentException("Todavía no hay ideas con razones que repasar."));

        ReviewSubject sujeto = lectura.execute(estado ->
                sujeto(requireArgument(elegida), attemptRepository.countByArgumentId(elegida)));

        Challenge desafio = socratic.challenge(sujeto);

        return escritura.execute(estado -> dto(attemptRepository.save(new ReviewAttempt(
                argumentRepository.getReferenceById(elegida),
                desafio.kind(), desafio.question(), desafio.counterexample(), desafio.hint()))));
    }

    public ReviewAttemptDto answer(Long attemptId, String respuesta) {
        String limpia = validarRespuesta(respuesta);

        Preparada preparada = lectura.execute(estado -> {
            ReviewAttempt intento = requireUnanswered(attemptId);
            return new Preparada(sujeto(requireArgument(intento.getArgument().getId()), 0),
                    intento.getQuestion(), intento.getCounterexample());
        });

        Evaluation valoracion = socratic.evaluate(
                preparada.sujeto(), preparada.pregunta(), preparada.contraejemplo(), limpia);

        return escritura.execute(estado -> {
            // Se vuelve a comprobar: dos envios seguidos no deben valorar la misma respuesta dos veces.
            ReviewAttempt intento = requireUnanswered(attemptId);
            intento.recordAnswer(limpia, valoracion.rating(), valoracion.whatWorked(),
                    valoracion.whatToImprove(), valoracion.followUpQuestion(), Instant.now());
            return dto(intento);
        });
    }

    public List<ReviewAttemptDto> history(Long argumentId) {
        return lectura.execute(estado -> attemptRepository.findByArgumentIdOrderByIdDesc(argumentId).stream()
                .map(ReviewService::dto)
                .toList());
    }

    public ReviewProgressDto progress() {
        return jdbc.queryForObject("""
                SELECT
                    (SELECT count(DISTINCT argument_id) FROM review_attempt
                     WHERE answered_at >= now() - interval '7 days') AS esta_semana,
                    (SELECT count(*) FROM argument a
                     WHERE EXISTS (SELECT 1 FROM premise p WHERE p.argument_id = a.id)
                       AND NOT EXISTS (SELECT 1 FROM review_attempt ra
                                       WHERE ra.argument_id = a.id AND ra.answered_at IS NOT NULL)) AS nunca,
                    (SELECT count(*) FROM argument a
                     WHERE EXISTS (SELECT 1 FROM premise p WHERE p.argument_id = a.id)) AS repasables
                """, (rs, fila) -> new ReviewProgressDto(
                rs.getInt("esta_semana"), rs.getInt("nunca"), rs.getInt("repasables")));
    }

    /** Mensajes pensados para el lector, que es quien los vera en la interfaz. */
    static String validarRespuesta(String respuesta) {
        if (respuesta == null || respuesta.isBlank()) {
            throw new IllegalArgumentException("Escribe tu respuesta antes de enviarla.");
        }
        String limpia = respuesta.strip();
        if (limpia.length() > MAX_RESPUESTA) {
            throw new IllegalArgumentException(
                    "La respuesta es demasiado larga: como mucho " + MAX_RESPUESTA + " caracteres.");
        }
        return limpia;
    }

    // ----- Utilidades -------------------------------------------------------

    private Optional<Long> elegir(Long workId, Long after) {
        List<ReviewScheduler.Candidate> candidatos = jdbc.query(CANDIDATOS, (rs, fila) -> {
            String rating = rs.getString("rating");
            Timestamp respondido = rs.getTimestamp("answered_at");
            return new ReviewScheduler.Candidate(
                    rs.getLong("id"),
                    rating == null ? null : ReviewRating.valueOf(rating),
                    respondido == null ? null : respondido.toInstant());
        }, workId, workId);
        return ReviewScheduler.next(candidatos, after);
    }

    private ReviewCardDto tarjeta(Argument idea) {
        List<ReviewAttempt> intentos = attemptRepository.findByArgumentIdOrderByIdDesc(idea.getId());
        ReviewAttempt ultimo = intentos.stream()
                .filter(ReviewAttempt::isAnswered)
                .max(Comparator.comparing(ReviewAttempt::getAnsweredAt))
                .orElse(null);
        List<Premise> razones = ordenadas(idea).stream()
                .filter(p -> p.getPremiseType() != PremiseType.CONCLUSION)
                .toList();
        return new ReviewCardDto(
                idea.getId(),
                idea.getName(),
                idea.getWork().getId(),
                idea.getWork().getTitle(),
                idea.getWork().getPhilosopher().getName(),
                idea.getWork().getPhilosopher().getAvatarEmoji(),
                conclusion(idea),
                razones.stream().map(Premise::getStatement).toList(),
                (int) razones.stream().filter(Premise::isEnthymeme).count(),
                intentos.size(),
                ultimo == null ? null : ultimo.getRating(),
                ultimo == null ? null : ultimo.getAnsweredAt());
    }

    private static ReviewSubject sujeto(Argument idea, long intentosPrevios) {
        List<Premise> ordenadas = ordenadas(idea);
        List<String> razones = ordenadas.stream()
                .filter(p -> p.getPremiseType() != PremiseType.CONCLUSION)
                .map(p -> p.isEnthymeme() ? p.getStatement() + " (supuesto implícito: el autor no lo dice)" : p.getStatement())
                .toList();
        List<String> supuestos = ordenadas.stream().filter(Premise::isEnthymeme).map(Premise::getStatement).toList();
        // Se rota entre los supuestos: repasar dos veces la misma idea no repite la pregunta.
        String foco = supuestos.isEmpty() ? null : supuestos.get((int) (intentosPrevios % supuestos.size()));
        return new ReviewSubject(idea.getName(), idea.getWork().getPhilosopher().getName(),
                idea.getWork().getTitle(), razones, conclusion(idea), foco);
    }

    private static List<Premise> ordenadas(Argument idea) {
        return idea.getPremises().stream().sorted(Comparator.comparingInt(Premise::getOrderIndex)).toList();
    }

    /** La conclusion declarada; si no se marco ninguna, el nombre de la idea hace sus veces. */
    private static String conclusion(Argument idea) {
        return ordenadas(idea).stream()
                .filter(p -> p.getPremiseType() == PremiseType.CONCLUSION)
                .map(Premise::getStatement)
                .reduce((primera, ultima) -> ultima)
                .orElse(idea.getName());
    }

    private Argument requireArgument(long id) {
        return argumentRepository.findFullById(id).orElseThrow(() -> new NotFoundException("argumento", id));
    }

    private ReviewAttempt requireUnanswered(Long attemptId) {
        ReviewAttempt intento = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new NotFoundException("repaso", attemptId));
        if (intento.isAnswered()) {
            throw new ConflictException("Este desafío ya tiene respuesta. Pide otro para seguir repasando.");
        }
        return intento;
    }

    static ReviewAttemptDto dto(ReviewAttempt intento) {
        return new ReviewAttemptDto(intento.getId(), intento.getArgument().getId(), intento.getChallengeKind(),
                intento.getQuestion(), intento.getCounterexample(), intento.getHint(), intento.getAnswer(),
                intento.getRating(), intento.getWhatWorked(), intento.getWhatToImprove(),
                intento.getFollowUpQuestion(), intento.getCreatedAt(), intento.getAnsweredAt());
    }

    private record Preparada(ReviewSubject sujeto, String pregunta, String contraejemplo) {
    }
}
