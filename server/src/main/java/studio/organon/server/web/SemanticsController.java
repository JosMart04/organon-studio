package studio.organon.server.web;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import studio.organon.server.service.SemanticsService;
import studio.organon.server.web.dto.ConceptComparisonDto;
import studio.organon.server.web.dto.ResolvedTermDto;
import studio.organon.server.web.dto.SemanticConceptDto;
import studio.organon.server.web.dto.SemanticConceptRequest;
import studio.organon.server.web.dto.TermDefinitionDto;
import studio.organon.server.web.dto.TermDefinitionRequest;

@RestController
@RequestMapping("/api/v1/semantics")
public class SemanticsController {

    private final SemanticsService semanticsService;

    public SemanticsController(SemanticsService semanticsService) {
        this.semanticsService = semanticsService;
    }

    @GetMapping("/concepts")
    public List<SemanticConceptDto> listConcepts() {
        return semanticsService.listConcepts();
    }

    @PostMapping("/concepts")
    public SemanticConceptDto createConcept(@Valid @RequestBody SemanticConceptRequest request) {
        return semanticsService.createConcept(request);
    }

    /** Todas las lecturas rivales de un concepto, para el comparador lado a lado. */
    @GetMapping("/concepts/{id}/comparison")
    public ConceptComparisonDto compareConcept(@PathVariable Long id) {
        return semanticsService.compareConcept(id);
    }

    @GetMapping("/definitions")
    public List<TermDefinitionDto> listDefinitions(@RequestParam(required = false) Long conceptId,
                                                   @RequestParam(required = false) Long philosopherId) {
        return semanticsService.listDefinitions(conceptId, philosopherId);
    }

    @PostMapping("/definitions")
    public TermDefinitionDto createDefinition(@Valid @RequestBody TermDefinitionRequest request) {
        return semanticsService.createDefinition(request);
    }

    @PutMapping("/definitions/{id}")
    public TermDefinitionDto updateDefinition(@PathVariable Long id,
                                              @Valid @RequestBody TermDefinitionRequest request) {
        return semanticsService.updateDefinition(id, request);
    }

    @DeleteMapping("/definitions/{id}")
    public ResponseEntity<Void> deleteDefinition(@PathVariable Long id) {
        semanticsService.deleteDefinition(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Resuelve un termino en el contexto de lectura actual: que entiende este
     * autor por esta palabra, y que entienden los demas.
     */
    @GetMapping("/resolve")
    public ResolvedTermDto resolve(@RequestParam String term,
                                   @RequestParam Long philosopherId,
                                   @RequestParam(required = false) Long workId) {
        return semanticsService.resolve(term, philosopherId, workId);
    }

    /** Glosario del autor para el panel derecho del lector. */
    @GetMapping("/reading-context")
    public List<TermDefinitionDto> readingContext(@RequestParam Long philosopherId,
                                                  @RequestParam(required = false) Long workId) {
        return semanticsService.readingContext(philosopherId, workId);
    }
}
