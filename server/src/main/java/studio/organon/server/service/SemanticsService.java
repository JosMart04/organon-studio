package studio.organon.server.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studio.organon.server.domain.semantics.SemanticConcept;
import studio.organon.server.domain.semantics.TermDefinition;
import studio.organon.server.error.ConflictException;
import studio.organon.server.error.NotFoundException;
import studio.organon.server.repository.SemanticConceptRepository;
import studio.organon.server.repository.TermDefinitionRepository;
import studio.organon.server.web.dto.ConceptComparisonDto;
import studio.organon.server.web.dto.DefinitionScope;
import studio.organon.server.web.dto.ResolvedTermDto;
import studio.organon.server.web.dto.SemanticConceptDto;
import studio.organon.server.web.dto.SemanticConceptRequest;
import studio.organon.server.web.dto.TermDefinitionDto;
import studio.organon.server.web.dto.TermDefinitionRequest;

/**
 * Glosario ontologico. Su operacion caracteristica no es buscar una definicion,
 * sino elegir cual de varias incompatibles aplica en el contexto de lectura.
 */
@Service
@Transactional(readOnly = true)
public class SemanticsService {

    private final SemanticConceptRepository conceptRepository;
    private final TermDefinitionRepository definitionRepository;
    private final CorpusService corpusService;
    private final DomainMapper mapper;

    public SemanticsService(SemanticConceptRepository conceptRepository,
                            TermDefinitionRepository definitionRepository,
                            CorpusService corpusService,
                            DomainMapper mapper) {
        this.conceptRepository = conceptRepository;
        this.definitionRepository = definitionRepository;
        this.corpusService = corpusService;
        this.mapper = mapper;
    }

    // ----- Conceptos ------------------------------------------------------

    public List<SemanticConceptDto> listConcepts() {
        return conceptRepository.findAllByOrderByTermAsc().stream()
                .map(concept -> mapper.toDto(
                        concept,
                        definitionRepository.findByConceptIdOrderByPhilosopherNameAsc(concept.getId()).size()))
                .toList();
    }

    @Transactional
    public SemanticConceptDto createConcept(SemanticConceptRequest request) {
        conceptRepository.findByTermIgnoreCase(request.term()).ifPresent(existing -> {
            throw new ConflictException("El termino " + existing.getTerm() + " ya esta en el glosario");
        });
        SemanticConcept concept = new SemanticConcept(request.term(), request.description());
        return mapper.toDto(conceptRepository.save(concept), 0);
    }

    /**
     * Un concepto con todas sus lecturas rivales. Es lo que alimenta el
     * comparador lado a lado: ver "Sustancia" en Descartes junto a "Sustancia"
     * en Spinoza es constatar que no hablan de lo mismo.
     */
    public ConceptComparisonDto compareConcept(Long conceptId) {
        SemanticConcept concept = requireConcept(conceptId);
        List<TermDefinitionDto> readings =
                definitionRepository.findByConceptIdOrderByPhilosopherNameAsc(conceptId).stream()
                        .map(mapper::toDto)
                        .toList();
        return new ConceptComparisonDto(
                concept.getId(), concept.getTerm(), concept.getDescription(), readings);
    }

    // ----- Definiciones ---------------------------------------------------

    public List<TermDefinitionDto> listDefinitions(Long conceptId, Long philosopherId) {
        List<TermDefinition> found;
        if (conceptId != null) {
            found = definitionRepository.findByConceptIdOrderByPhilosopherNameAsc(conceptId);
        } else if (philosopherId != null) {
            found = definitionRepository.findByPhilosopherIdOrderByConceptTermAsc(philosopherId);
        } else {
            found = definitionRepository.findAllByOrderByConceptTermAscPhilosopherNameAsc();
        }
        return found.stream().map(mapper::toDto).toList();
    }

    @Transactional
    public TermDefinitionDto createDefinition(TermDefinitionRequest request) {
        TermDefinition definition = new TermDefinition(
                requireConcept(request.conceptId()),
                corpusService.requirePhilosopher(request.philosopherId()),
                request.workId() == null ? null : corpusService.requireWork(request.workId()),
                request.operationalDefinition(),
                request.notes());
        return mapper.toDto(definitionRepository.save(definition));
    }

    @Transactional
    public TermDefinitionDto updateDefinition(Long id, TermDefinitionRequest request) {
        TermDefinition definition = requireDefinition(id);
        definition.setConcept(requireConcept(request.conceptId()));
        definition.setPhilosopher(corpusService.requirePhilosopher(request.philosopherId()));
        definition.setWork(request.workId() == null ? null : corpusService.requireWork(request.workId()));
        definition.setOperationalDefinition(request.operationalDefinition());
        definition.setNotes(request.notes());
        return mapper.toDto(definition);
    }

    @Transactional
    public void deleteDefinition(Long id) {
        definitionRepository.delete(requireDefinition(id));
    }

    // ----- Resolucion contextual -----------------------------------------

    /**
     * Devuelve la acepcion que gobierna el termino mientras se lee a un autor
     * concreto. La definicion fijada en la obra desplaza a la general del
     * autor; si no hay ninguna, se informa igualmente de como lo entienden los
     * demas, que suele ser justo el dato que el lector necesita.
     */
    public ResolvedTermDto resolve(String term, Long philosopherId, Long workId) {
        corpusService.requirePhilosopher(philosopherId);

        List<TermDefinition> candidates = definitionRepository.findCandidates(term, philosopherId);

        Optional<TermDefinition> workScoped = workId == null
                ? Optional.empty()
                : candidates.stream()
                        .filter(d -> d.getWork() != null && workId.equals(d.getWork().getId()))
                        .findFirst();

        Optional<TermDefinition> authorScoped = candidates.stream()
                .filter(d -> d.getWork() == null)
                .findFirst();

        TermDefinition winner = workScoped.or(() -> authorScoped).orElse(null);
        DefinitionScope scope;
        if (workScoped.isPresent()) {
            scope = DefinitionScope.OBRA;
        } else if (authorScoped.isPresent()) {
            scope = DefinitionScope.AUTOR;
        } else {
            scope = DefinitionScope.SIN_DEFINICION;
        }

        List<TermDefinitionDto> rivals = conceptRepository.findByTermIgnoreCase(term)
                .map(concept -> definitionRepository
                        .findByConceptIdOrderByPhilosopherNameAsc(concept.getId()).stream()
                        .filter(d -> !d.getPhilosopher().getId().equals(philosopherId))
                        .sorted(Comparator.comparing(d -> d.getPhilosopher().getName()))
                        .map(mapper::toDto)
                        .toList())
                .orElseGet(List::of);

        return new ResolvedTermDto(
                term, winner == null ? null : mapper.toDto(winner), scope, rivals);
    }

    /** Glosario del autor tal como se muestra en el panel derecho del lector. */
    public List<TermDefinitionDto> readingContext(Long philosopherId, Long workId) {
        corpusService.requirePhilosopher(philosopherId);
        return definitionRepository.findForReadingContext(philosopherId, workId).stream()
                .map(mapper::toDto)
                .toList();
    }

    SemanticConcept requireConcept(Long id) {
        return conceptRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("concepto", id));
    }

    private TermDefinition requireDefinition(Long id) {
        return definitionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("definicion", id));
    }
}
