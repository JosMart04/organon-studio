package studio.organon.server.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import studio.organon.server.ai.dto.ExplainRequest;
import studio.organon.server.ai.dto.ExplainResponse;
import studio.organon.server.ai.dto.ExtractIdeasRequest;
import studio.organon.server.ai.dto.ExtractedIdeas;
import studio.organon.server.ai.dto.FindRivalsRequest;
import studio.organon.server.ai.dto.RivalSuggestion;
import studio.organon.server.ai.dto.RivalSuggestions;
import studio.organon.server.domain.dialectic.RelationType;
import studio.organon.server.repository.PhilosopherRepository;

/**
 * El tutor socrático: un modelo local que explica, desglosa y busca rivales.
 *
 * <p>Tres reglas que gobiernan esta clase:
 *
 * <ol>
 *   <li>Nunca escribe en la base de datos. Todo lo que devuelve es una
 *       propuesta que el lector revisa y aprueba en la interfaz.
 *   <li>Ningún fallo del modelo se propaga como error de servidor. Un puerto
 *       cerrado, un modelo que no está o un JSON malformado acaban en un
 *       mensaje que el lector puede entender y actuar.
 *   <li>Los prompts prohíben la jerga explícitamente. El usuario es un lector
 *       aficionado, no un doctorando en lógica.
 * </ol>
 */
@Service
public class SocraticService {

    private static final Logger log = LoggerFactory.getLogger(SocraticService.class);

    private static final String VOICE = """
            Eres un profesor de filosofía que explica a un estudiante de ingeniería
            aficionado a la filosofía. Escribes en español de España, en lenguaje
            corriente.

            Reglas que no puedes romper:
            - Nada de jerga sin explicar. Si necesitas un término técnico, defínelo
              con tus palabras en la misma frase.
            - Nada de notación lógica, símbolos matemáticos ni latín sin traducir.
            - Nada de fórmulas de cortesía, preámbulos ni "en resumen". Vas al grano.
            - No inventes citas ni referencias de página.
            """;

    private final ChatClient chatClient;
    private final OllamaAvailability availability;
    private final PhilosopherRepository philosopherRepository;

    public SocraticService(ChatClient.Builder chatClientBuilder,
                           OllamaAvailability availability,
                           PhilosopherRepository philosopherRepository) {
        this.chatClient = chatClientBuilder.build();
        this.availability = availability;
        this.philosopherRepository = philosopherRepository;
    }

    // ----- Explícamelo sencillo -------------------------------------------

    public ExplainResponse explainSimple(ExplainRequest request) {
        availability.requireAvailable();

        String user = """
                Explica este pasaje en exactamente dos párrafos.

                El primero: qué está diciendo el autor, en cristiano.
                El segundo: por qué le importaba, y qué cambia si tiene razón.

                %s

                Pasaje:
                ---
                %s
                ---
                """.formatted(context(request.author(), request.workTitle()), request.text());

        String answer = generate(() -> chatClient.prompt().system(VOICE).user(user).call().content(),
                "No he podido generar la explicación");

        if (answer == null || answer.isBlank()) {
            throw new AiUnavailableException(
                    "El modelo devolvió una respuesta vacía. Vuelve a intentarlo.");
        }
        return new ExplainResponse(answer.trim());
    }

    // ----- Desglosar razones ----------------------------------------------

    public ExtractedIdeas extractIdeas(ExtractIdeasRequest request) {
        availability.requireAvailable();

        String user = """
                Desmonta este pasaje en sus piezas.

                - mainClaim: lo que el autor sostiene, en una sola frase llana.
                - reasons: las razones con que lo sostiene, en el orden en que aparecen.
                  Cada una, una frase completa y autónoma. Entre dos y cinco.
                - unstatedAssumptions: lo que el autor da por obvio sin llegar a decirlo,
                  y que su razonamiento necesita para funcionar. Es lo más valioso de
                  todo el análisis. Si de verdad no encuentras ninguno, devuelve la
                  lista vacía en lugar de inventarte uno.

                No copies el texto literal: reformula con tus palabras.

                %s

                Pasaje:
                ---
                %s
                ---
                """.formatted(context(request.author(), request.workTitle()), request.text());

        ExtractedIdeas ideas = generate(
                () -> chatClient.prompt().system(VOICE).user(user).call().entity(ExtractedIdeas.class),
                "No he podido desglosar el pasaje");

        if (ideas == null || ideas.mainClaim() == null || ideas.mainClaim().isBlank()) {
            throw new AiUnavailableException(
                    "El modelo no ha conseguido estructurar el pasaje. Prueba con un fragmento "
                            + "más corto, o desglósalo a mano.");
        }
        return new ExtractedIdeas(
                ideas.mainClaim().trim(),
                clean(ideas.reasons()),
                clean(ideas.unstatedAssumptions()));
    }

    // ----- ¿Quién le lleva la contraria? ----------------------------------

    public RivalSuggestions findRivals(FindRivalsRequest request) {
        availability.requireAvailable();

        // Los pensadores que ya están en el cuaderno se le pasan al modelo para
        // que los prefiera: una sugerencia sobre alguien que ya existe se puede
        // convertir en una conexión del grafo con un clic.
        List<String> known = request.knownThinkers() != null && !request.knownThinkers().isEmpty()
                ? request.knownThinkers()
                : philosopherRepository.findAllByOrderByNameAsc().stream()
                        .map(p -> p.getName())
                        .toList();

        String user = """
                Dime qué pensadores discutirían esta tesis y en qué consiste la disputa.

                Devuelve entre uno y tres. Para cada uno:
                - thinker: el nombre del pensador.
                - work: la obra donde lo sostiene, o cadena vacía si no estás seguro.
                - relation: exactamente una de estas palabras, sin variarla —
                  REFUTA (la niega), PRESUPONE (parte de ella para ir a otro sitio),
                  EXTIENDE (la amplía), RADICALIZA (la lleva más lejos que su autor),
                  MATIZA (la restringe sin negarla).
                - explanation: dos o tres frases sobre en qué consiste el desacuerdo.

                %s
                %s

                Tesis:
                ---
                %s
                ---
                """.formatted(
                request.author() == null || request.author().isBlank()
                        ? ""
                        : "La tesis es de " + request.author() + ", así que no lo propongas como rival de sí mismo.",
                known.isEmpty()
                        ? ""
                        : "Si alguno de estos encaja de verdad, prefiérelo: " + String.join(", ", known) + ".",
                request.claim());

        RivalSuggestions raw = generate(
                () -> chatClient.prompt().system(VOICE).user(user).call().entity(RivalSuggestions.class),
                "No he podido buscar rivales");

        if (raw == null || raw.suggestions() == null || raw.suggestions().isEmpty()) {
            throw new AiUnavailableException(
                    "El modelo no ha propuesto ningún rival. Prueba a reformular la tesis.");
        }
        return new RivalSuggestions(validate(raw.suggestions()));
    }

    // ----- Utilidades ------------------------------------------------------

    /**
     * Traduce cualquier tropiezo del modelo a algo que el lector pueda leer.
     * Sin esto, un JSON malformado o un puerto cerrado salen como un 500.
     */
    private <T> T generate(Supplier<T> call, String whatFailed) {
        try {
            return call.get();
        } catch (AiUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.warn("{}: {}", whatFailed, e.toString());
            throw new AiUnavailableException(
                    whatFailed + ". El modelo local puede tardar o devolver algo inesperado; "
                            + "vuelve a intentarlo o hazlo a mano.", e);
        }
    }

    /** Como {@link java.util.function.Supplier} pero puede lanzar. */
    @FunctionalInterface
    private interface Supplier<T> {
        T get() throws Exception;
    }

    private static String context(String author, String workTitle) {
        if (author == null || author.isBlank()) {
            return "";
        }
        return workTitle == null || workTitle.isBlank()
                ? "El autor es " + author + "."
                : "El autor es " + author + " y la obra, «" + workTitle + "».";
    }

    private static List<String> clean(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .toList();
    }

    /**
     * El modelo puede devolver una relación que no existe en el dominio. En vez
     * de descartar la sugerencia entera, se degrada a MATIZA, que es la lectura
     * más débil y por tanto la más segura de atribuir.
     */
    private List<RivalSuggestion> validate(List<RivalSuggestion> suggestions) {
        Set<String> valid = java.util.Arrays.stream(RelationType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());
        Set<String> inNotebook = philosopherRepository.findAllByOrderByNameAsc().stream()
                .map(p -> p.getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        List<RivalSuggestion> checked = new ArrayList<>(suggestions.size());
        for (RivalSuggestion s : suggestions) {
            if (s == null || s.thinker() == null || s.thinker().isBlank()) {
                continue;
            }
            String relation = s.relation() == null ? "" : s.relation().trim().toUpperCase(Locale.ROOT);
            if (!valid.contains(relation)) {
                log.debug("Relación no reconocida del modelo: {}", s.relation());
                relation = RelationType.MATIZA.name();
            }
            checked.add(new RivalSuggestion(
                    s.thinker().trim(),
                    s.work() == null ? "" : s.work().trim(),
                    relation,
                    s.explanation() == null ? "" : s.explanation().trim(),
                    inNotebook.contains(s.thinker().trim().toLowerCase(Locale.ROOT))));
        }
        return checked;
    }
}
