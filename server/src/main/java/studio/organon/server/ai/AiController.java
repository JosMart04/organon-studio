package studio.organon.server.ai;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import studio.organon.server.ai.dto.AiStatusDto;
import studio.organon.server.ai.dto.ExplainRequest;
import studio.organon.server.ai.dto.ExplainResponse;
import studio.organon.server.ai.dto.ExtractIdeasRequest;
import studio.organon.server.ai.dto.ExtractedIdeas;
import studio.organon.server.ai.dto.FindRivalsRequest;
import studio.organon.server.ai.dto.RivalSuggestions;

/**
 * Asistente socrático sobre un modelo local. Ninguno de estos endpoints escribe
 * nada: todos devuelven propuestas que el lector aprueba después.
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final SocraticService socraticService;
    private final OllamaAvailability availability;

    public AiController(SocraticService socraticService, OllamaAvailability availability) {
        this.socraticService = socraticService;
        this.availability = availability;
    }

    /**
     * Lo primero que consulta el panel al abrirse. Nunca falla: si Ollama no
     * está, responde 200 con {@code available: false} y el motivo redactado.
     */
    @GetMapping("/status")
    public AiStatusDto status() {
        return availability.status();
    }

    /** «Explícamelo como a un estudiante». */
    @PostMapping("/explain-simple")
    public ExplainResponse explainSimple(@Valid @RequestBody ExplainRequest request) {
        return socraticService.explainSimple(request);
    }

    /** «Desglosar razones»: tesis, razones de apoyo y supuestos implícitos. */
    @PostMapping("/extract-ideas")
    public ExtractedIdeas extractIdeas(@Valid @RequestBody ExtractIdeasRequest request) {
        return socraticService.extractIdeas(request);
    }

    /** «¿Quién le lleva la contraria?». */
    @PostMapping("/find-rivals")
    public RivalSuggestions findRivals(@Valid @RequestBody FindRivalsRequest request) {
        return socraticService.findRivals(request);
    }
}
