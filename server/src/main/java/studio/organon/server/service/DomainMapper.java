package studio.organon.server.service;

import java.util.List;
import org.springframework.stereotype.Component;
import studio.organon.server.domain.corpus.Passage;
import studio.organon.server.domain.corpus.Philosopher;
import studio.organon.server.domain.corpus.Work;
import studio.organon.server.domain.dialectic.DialecticalRelation;
import studio.organon.server.domain.logic.Argument;
import studio.organon.server.domain.logic.Objection;
import studio.organon.server.domain.logic.Premise;
import studio.organon.server.domain.semantics.SemanticConcept;
import studio.organon.server.domain.semantics.TermDefinition;
import studio.organon.server.web.dto.ArgumentDto;
import studio.organon.server.web.dto.DialecticalRelationDto;
import studio.organon.server.web.dto.ObjectionDto;
import studio.organon.server.web.dto.PassageDto;
import studio.organon.server.web.dto.PhilosopherDto;
import studio.organon.server.web.dto.PremiseDto;
import studio.organon.server.web.dto.SemanticConceptDto;
import studio.organon.server.web.dto.TermDefinitionDto;
import studio.organon.server.web.dto.WorkDto;

/** Proyeccion de entidades a DTO. Sin logica de negocio: solo forma. */
@Component
public class DomainMapper {

    public PhilosopherDto toDto(Philosopher philosopher) {
        return new PhilosopherDto(
                philosopher.getId(),
                philosopher.getName(),
                philosopher.getEpoch(),
                philosopher.getSchool(),
                philosopher.getBiographicalSummary());
    }

    public WorkDto toDto(Work work) {
        Work adversary = work.getDirectAdversary();
        return new WorkDto(
                work.getId(),
                work.getPhilosopher().getId(),
                work.getPhilosopher().getName(),
                work.getTitle(),
                work.getOriginalYear(),
                work.getPhilosophicalProblem(),
                work.getCoreThesis(),
                adversary == null ? null : adversary.getId(),
                adversary == null ? null : adversary.getTitle());
    }

    public PassageDto toDto(Passage passage) {
        return new PassageDto(
                passage.getId(),
                passage.getWork().getId(),
                passage.getWork().getTitle(),
                passage.getLocator(),
                passage.getTextContent(),
                passage.getPageNumber());
    }

    public SemanticConceptDto toDto(SemanticConcept concept, int definitionCount) {
        return new SemanticConceptDto(
                concept.getId(),
                concept.getTerm(),
                concept.getDescription(),
                definitionCount);
    }

    public TermDefinitionDto toDto(TermDefinition definition) {
        Work work = definition.getWork();
        return new TermDefinitionDto(
                definition.getId(),
                definition.getConcept().getId(),
                definition.getConcept().getTerm(),
                definition.getPhilosopher().getId(),
                definition.getPhilosopher().getName(),
                work == null ? null : work.getId(),
                work == null ? null : work.getTitle(),
                definition.getOperationalDefinition(),
                definition.getNotes());
    }

    public ObjectionDto toDto(Objection objection) {
        return new ObjectionDto(
                objection.getId(),
                objection.getPremise().getId(),
                objection.getObjectionType(),
                objection.getExplanation());
    }

    public PremiseDto toDto(Premise premise) {
        List<ObjectionDto> objections = premise.getObjections().stream().map(this::toDto).toList();
        return new PremiseDto(
                premise.getId(),
                premise.getOrderIndex(),
                premise.getStatement(),
                premise.isEnthymeme(),
                premise.getPremiseType(),
                objections);
    }

    public ArgumentDto toDto(Argument argument) {
        Passage passage = argument.getPassage();
        List<PremiseDto> premises = argument.getPremises().stream()
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .map(this::toDto)
                .toList();
        return new ArgumentDto(
                argument.getId(),
                argument.getWork().getId(),
                argument.getWork().getTitle(),
                argument.getWork().getPhilosopher().getName(),
                passage == null ? null : passage.getId(),
                passage == null ? null : passage.getLocator(),
                argument.getName(),
                argument.getFormalScheme(),
                argument.getLatexFormalization(),
                argument.getSoundStatus(),
                premises);
    }

    public DialecticalRelationDto toDto(DialecticalRelation relation) {
        return new DialecticalRelationDto(
                relation.getId(),
                relation.getSourceArgument().getId(),
                relation.getSourceArgument().getName(),
                relation.getTargetArgument().getId(),
                relation.getTargetArgument().getName(),
                relation.getRelationType(),
                relation.getDescription());
    }
}
