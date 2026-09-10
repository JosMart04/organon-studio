package studio.organon.server.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studio.organon.server.domain.logic.Argument;
import studio.organon.server.domain.logic.Objection;
import studio.organon.server.domain.logic.Premise;
import studio.organon.server.domain.logic.PremiseType;
import studio.organon.server.error.ConflictException;
import studio.organon.server.error.NotFoundException;
import studio.organon.server.repository.ArgumentRepository;
import studio.organon.server.repository.ObjectionRepository;
import studio.organon.server.repository.PremiseRepository;
import studio.organon.server.web.dto.ArgumentDto;
import studio.organon.server.web.dto.ArgumentRequest;
import studio.organon.server.web.dto.ObjectionDto;
import studio.organon.server.web.dto.ObjectionRequest;
import studio.organon.server.web.dto.PremiseDto;
import studio.organon.server.web.dto.PremiseRequest;

/**
 * Reconstruccion logica: alta de argumentos, reordenamiento de premisas,
 * anclaje de objeciones y auditoria de solidez.
 */
@Service
@Transactional(readOnly = true)
public class ArgumentService {

    /**
     * Desplazamiento temporal para reindexar sin chocar con la unicidad de
     * (argument_id, order_index). El indice definitivo se escribe en una
     * segunda pasada, ya sin colisiones posibles.
     */
    private static final int REORDER_OFFSET = 10_000;

    private final ArgumentRepository argumentRepository;
    private final PremiseRepository premiseRepository;
    private final ObjectionRepository objectionRepository;
    private final CorpusService corpusService;
    private final ArgumentAuditor auditor;
    private final DomainMapper mapper;

    public ArgumentService(ArgumentRepository argumentRepository,
                           PremiseRepository premiseRepository,
                           ObjectionRepository objectionRepository,
                           CorpusService corpusService,
                           ArgumentAuditor auditor,
                           DomainMapper mapper) {
        this.argumentRepository = argumentRepository;
        this.premiseRepository = premiseRepository;
        this.objectionRepository = objectionRepository;
        this.corpusService = corpusService;
        this.auditor = auditor;
        this.mapper = mapper;
    }

    // ----- Argumentos -----------------------------------------------------

    public List<ArgumentDto> listArguments(Long workId, Long passageId) {
        List<Argument> found;
        if (passageId != null) {
            found = argumentRepository.findByPassageIdOrderByNameAsc(passageId);
        } else if (workId != null) {
            found = argumentRepository.findByWorkIdOrderByNameAsc(workId);
        } else {
            found = argumentRepository.findAllByOrderByIdAsc();
        }
        return found.stream().map(mapper::toDto).toList();
    }

    public ArgumentDto getArgument(Long id) {
        return mapper.toDto(requireArgument(id));
    }

    @Transactional
    public ArgumentDto createArgument(ArgumentRequest request) {
        Argument argument = new Argument(
                corpusService.requireWork(request.workId()),
                request.passageId() == null ? null : corpusService.requirePassage(request.passageId()),
                request.name(),
                request.formalScheme(),
                request.latexFormalization());

        List<PremiseRequest> incoming = request.premises() == null ? List.of() : request.premises();
        for (int i = 0; i < incoming.size(); i++) {
            PremiseRequest premiseRequest = incoming.get(i);
            argument.addPremise(new Premise(
                    i, premiseRequest.statement(), premiseRequest.enthymeme(), premiseRequest.premiseType()));
        }
        return mapper.toDto(argumentRepository.save(argument));
    }

    @Transactional
    public ArgumentDto updateArgument(Long id, ArgumentRequest request) {
        Argument argument = requireArgument(id);
        argument.setWork(corpusService.requireWork(request.workId()));
        argument.setPassage(
                request.passageId() == null ? null : corpusService.requirePassage(request.passageId()));
        argument.setName(request.name());
        argument.setFormalScheme(request.formalScheme());
        argument.setLatexFormalization(request.latexFormalization());
        if (request.premises() != null) {
            applyPremises(argument, request.premises());
        }
        return mapper.toDto(argument);
    }

    @Transactional
    public void deleteArgument(Long id) {
        argumentRepository.delete(requireArgument(id));
    }

    // ----- Premisas -------------------------------------------------------

    /**
     * Reescribe la lista completa de premisas en el orden recibido. El cliente
     * envia el resultado del drag-and-drop tal cual: la posicion en la lista es
     * el orden, y las premisas ausentes se eliminan.
     */
    @Transactional
    public List<PremiseDto> reorderPremises(Long argumentId, List<PremiseRequest> incoming) {
        Argument argument = requireArgument(argumentId);
        applyPremises(argument, incoming);
        return argument.getPremises().stream()
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public PremiseDto toggleEnthymeme(Long premiseId) {
        Premise premise = requirePremise(premiseId);
        premise.setEnthymeme(!premise.isEnthymeme());
        return mapper.toDto(premise);
    }

    private void applyPremises(Argument argument, List<PremiseRequest> incoming) {
        Map<Long, Premise> existing = new HashMap<>();
        argument.getPremises().forEach(premise -> existing.put(premise.getId(), premise));

        // Primera pasada: apartar los indices vigentes fuera del rango final
        // para que la reasignacion no choque con uk_premise_order.
        int parking = REORDER_OFFSET;
        for (Premise premise : argument.getPremises()) {
            premise.setOrderIndex(parking++);
        }
        premiseRepository.flush();

        List<Premise> retained = new ArrayList<>();
        for (int index = 0; index < incoming.size(); index++) {
            PremiseRequest request = incoming.get(index);
            Premise premise = request.id() == null ? null : existing.get(request.id());
            if (premise == null) {
                if (request.id() != null) {
                    throw new ConflictException(
                            "La premisa " + request.id() + " no pertenece al argumento " + argument.getId());
                }
                premise = new Premise(index, request.statement(), request.enthymeme(), request.premiseType());
                argument.addPremise(premise);
            } else {
                premise.setStatement(request.statement());
                premise.setEnthymeme(request.enthymeme());
                premise.setPremiseType(request.premiseType());
                premise.setOrderIndex(index);
            }
            retained.add(premise);
        }

        // Lo que el cliente no devolvio, se borra (orphanRemoval en el mapeo).
        argument.getPremises().removeIf(premise -> !retained.contains(premise));
        premiseRepository.flush();
    }

    // ----- Objeciones -----------------------------------------------------

    public List<ObjectionDto> listObjections(Long premiseId) {
        requirePremise(premiseId);
        return objectionRepository.findByPremiseIdOrderByIdAsc(premiseId).stream()
                .map(mapper::toDto)
                .toList();
    }

    public List<ObjectionDto> listObjectionsForArgument(Long argumentId) {
        requireArgument(argumentId);
        return objectionRepository.findByPremiseArgumentIdOrderByIdAsc(argumentId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public ObjectionDto addObjection(Long premiseId, ObjectionRequest request) {
        Premise premise = requirePremise(premiseId);
        Objection objection = new Objection(request.objectionType(), request.explanation());
        premise.addObjection(objection);
        premiseRepository.flush();
        return mapper.toDto(objection);
    }

    @Transactional
    public void deleteObjection(Long objectionId) {
        Objection objection = objectionRepository.findById(objectionId)
                .orElseThrow(() -> new NotFoundException("objecion", objectionId));
        objectionRepository.delete(objection);
    }

    // ----- Auditoria ------------------------------------------------------

    /** Somete el argumento a las pruebas de estres y persiste el veredicto. */
    @Transactional
    public studio.organon.server.web.dto.AuditReportDto audit(Long argumentId) {
        Argument argument = requireArgument(argumentId);
        var report = auditor.audit(argument);
        argument.setSoundStatus(report.soundStatus());
        return report;
    }

    /** Vista previa de la auditoria sin tocar el estado almacenado. */
    public studio.organon.server.web.dto.AuditReportDto dryRunAudit(Long argumentId) {
        return auditor.audit(requireArgument(argumentId));
    }

    // ----- Utilidades -----------------------------------------------------

    Argument requireArgument(Long id) {
        return argumentRepository.findFullById(id)
                .orElseThrow(() -> new NotFoundException("argumento", id));
    }

    private Premise requirePremise(Long id) {
        return premiseRepository.findWithObjectionsById(id)
                .orElseThrow(() -> new NotFoundException("premisa", id));
    }

    /** Comodidad para el auditor y la exportacion: la conclusion declarada. */
    static List<Premise> supportingPremises(Argument argument) {
        return argument.getPremises().stream()
                .filter(premise -> premise.getPremiseType() != PremiseType.CONCLUSION)
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .toList();
    }
}
