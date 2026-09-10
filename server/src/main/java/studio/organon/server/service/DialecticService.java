package studio.organon.server.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studio.organon.server.domain.corpus.Epoch;
import studio.organon.server.domain.corpus.Philosopher;
import studio.organon.server.domain.corpus.Work;
import studio.organon.server.domain.dialectic.DialecticalRelation;
import studio.organon.server.domain.dialectic.RelationType;
import studio.organon.server.domain.logic.Argument;
import studio.organon.server.domain.logic.Premise;
import studio.organon.server.error.ConflictException;
import studio.organon.server.error.NotFoundException;
import studio.organon.server.repository.ArgumentRepository;
import studio.organon.server.repository.DialecticalRelationRepository;
import studio.organon.server.repository.PhilosopherRepository;
import studio.organon.server.repository.TermDefinitionRepository;
import studio.organon.server.repository.WorkRepository;
import studio.organon.server.web.dto.DialecticGraphDto;
import studio.organon.server.web.dto.DialecticalRelationDto;
import studio.organon.server.web.dto.DialecticalRelationRequest;
import studio.organon.server.web.dto.GraphEdgeDto;
import studio.organon.server.web.dto.GraphNodeDto;
import studio.organon.server.web.dto.GraphPositionDto;

/**
 * Red dialectica: alta de relaciones y proyeccion del grafo completo.
 *
 * <p>El grafo mezcla dos familias de aristas. Las estructurales (quien escribe
 * que, que obra sostiene que argumento) son andamiaje. Las dialecticas son el
 * contenido: quien refuta, presupone, extiende o radicaliza a quien.
 */
@Service
@Transactional(readOnly = true)
public class DialecticService {

    private static final double COLUMN_WIDTH = 420;
    private static final double PHILOSOPHER_ROW_Y = 0;
    private static final double WORK_ROW_Y = 200;
    private static final double ARGUMENT_ROW_Y = 420;
    private static final double ARGUMENT_ROW_STEP = 150;

    private final PhilosopherRepository philosopherRepository;
    private final WorkRepository workRepository;
    private final ArgumentRepository argumentRepository;
    private final DialecticalRelationRepository relationRepository;
    private final TermDefinitionRepository definitionRepository;
    private final ArgumentService argumentService;
    private final DomainMapper mapper;

    public DialecticService(PhilosopherRepository philosopherRepository,
                            WorkRepository workRepository,
                            ArgumentRepository argumentRepository,
                            DialecticalRelationRepository relationRepository,
                            TermDefinitionRepository definitionRepository,
                            ArgumentService argumentService,
                            DomainMapper mapper) {
        this.philosopherRepository = philosopherRepository;
        this.workRepository = workRepository;
        this.argumentRepository = argumentRepository;
        this.relationRepository = relationRepository;
        this.definitionRepository = definitionRepository;
        this.argumentService = argumentService;
        this.mapper = mapper;
    }

    // ----- Relaciones -----------------------------------------------------

    public List<DialecticalRelationDto> listRelations() {
        return relationRepository.findAllByOrderByIdAsc().stream().map(mapper::toDto).toList();
    }

    @Transactional
    public DialecticalRelationDto createRelation(DialecticalRelationRequest request) {
        if (request.sourceArgumentId().equals(request.targetArgumentId())) {
            throw new ConflictException(
                    "Un argumento no se relaciona consigo mismo: eso seria contradiccion interna, no dialectica.");
        }
        Argument source = argumentService.requireArgument(request.sourceArgumentId());
        Argument target = argumentService.requireArgument(request.targetArgumentId());
        DialecticalRelation relation = new DialecticalRelation(
                source, target, request.relationType(), request.description());
        return mapper.toDto(relationRepository.save(relation));
    }

    @Transactional
    public void deleteRelation(Long id) {
        DialecticalRelation relation = relationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("relacion dialectica", id));
        relationRepository.delete(relation);
    }

    // ----- Proyeccion del grafo -------------------------------------------

    /**
     * Serializa el grafo completo en la forma que consume React Flow.
     *
     * @param epoch     limita el mapa a una epoca historica
     * @param conceptId limita el mapa a los autores que definen ese concepto,
     *                  que es como se aisla un debate concreto del corpus entero
     */
    public DialecticGraphDto buildGraph(Epoch epoch, Long conceptId) {
        List<Philosopher> philosophers = (epoch == null
                ? philosopherRepository.findAllByOrderByNameAsc()
                : philosopherRepository.findByEpochOrderByNameAsc(epoch));

        if (conceptId != null) {
            Set<Long> involved = definitionRepository
                    .findByConceptIdOrderByPhilosopherNameAsc(conceptId).stream()
                    .map(definition -> definition.getPhilosopher().getId())
                    .collect(Collectors.toSet());
            philosophers = philosophers.stream()
                    .filter(philosopher -> involved.contains(philosopher.getId()))
                    .toList();
        }

        Set<Long> philosopherIds = philosophers.stream().map(Philosopher::getId).collect(Collectors.toSet());

        List<Work> works = workRepository.findAllByOrderByOriginalYearAscTitleAsc().stream()
                .filter(work -> philosopherIds.contains(work.getPhilosopher().getId()))
                .toList();
        Set<Long> workIds = works.stream().map(Work::getId).collect(Collectors.toSet());

        List<Argument> arguments = argumentRepository.findAllByOrderByIdAsc().stream()
                .filter(argument -> workIds.contains(argument.getWork().getId()))
                .toList();
        Set<Long> argumentIds = arguments.stream().map(Argument::getId).collect(Collectors.toSet());

        List<GraphNodeDto> nodes = new ArrayList<>();
        List<GraphEdgeDto> edges = new ArrayList<>();

        Map<Long, Integer> columnByPhilosopher = new LinkedHashMap<>();
        for (int i = 0; i < philosophers.size(); i++) {
            Philosopher philosopher = philosophers.get(i);
            columnByPhilosopher.put(philosopher.getId(), i);
            nodes.add(new GraphNodeDto(
                    nodeId("philosopher", philosopher.getId()),
                    "philosopher",
                    Map.of(
                            "label", philosopher.getName(),
                            "epoch", philosopher.getEpoch().name(),
                            "school", philosopher.getSchool() == null ? "" : philosopher.getSchool(),
                            "avatarEmoji",
                            philosopher.getAvatarEmoji() == null ? "" : philosopher.getAvatarEmoji()),
                    new GraphPositionDto(i * COLUMN_WIDTH, PHILOSOPHER_ROW_Y)));
        }

        Map<Long, Integer> argumentSlot = new LinkedHashMap<>();
        for (Work work : works) {
            int column = columnByPhilosopher.getOrDefault(work.getPhilosopher().getId(), 0);
            nodes.add(new GraphNodeDto(
                    nodeId("work", work.getId()),
                    "work",
                    Map.of(
                            "label", work.getTitle(),
                            "originalYear", work.getOriginalYear() == null ? "" : work.getOriginalYear(),
                            "philosopherName", work.getPhilosopher().getName(),
                            "coreThesis", work.getCoreThesis() == null ? "" : work.getCoreThesis()),
                    new GraphPositionDto(column * COLUMN_WIDTH, WORK_ROW_Y)));

            edges.add(structuralEdge(
                    "wrote-" + work.getPhilosopher().getId() + "-" + work.getId(),
                    nodeId("philosopher", work.getPhilosopher().getId()),
                    nodeId("work", work.getId()),
                    "escribe"));

            Work adversary = work.getDirectAdversary();
            if (adversary != null && workIds.contains(adversary.getId())) {
                edges.add(new GraphEdgeDto(
                        "adversary-" + work.getId() + "-" + adversary.getId(),
                        nodeId("work", work.getId()),
                        nodeId("work", adversary.getId()),
                        "adversary",
                        "escribe contra",
                        false,
                        Map.of("kind", "ADVERSARIO")));
            }
        }

        for (Argument argument : arguments) {
            int column = columnByPhilosopher.getOrDefault(argument.getWork().getPhilosopher().getId(), 0);
            int slot = argumentSlot.merge(argument.getWork().getPhilosopher().getId(), 0, (a, b) -> a + 1);
            long enthymemes = argument.getPremises().stream().filter(Premise::isEnthymeme).count();

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("label", argument.getName());
            data.put("formalScheme", argument.getFormalScheme().name());
            data.put("soundStatus", argument.getSoundStatus().name());
            data.put("latexFormalization",
                    argument.getLatexFormalization() == null ? "" : argument.getLatexFormalization());
            data.put("workTitle", argument.getWork().getTitle());
            data.put("philosopherName", argument.getWork().getPhilosopher().getName());
            data.put("premiseCount", argument.getPremises().size());
            data.put("enthymemeCount", enthymemes);

            nodes.add(new GraphNodeDto(
                    nodeId("argument", argument.getId()),
                    "argument",
                    data,
                    new GraphPositionDto(column * COLUMN_WIDTH, ARGUMENT_ROW_Y + slot * ARGUMENT_ROW_STEP)));

            edges.add(structuralEdge(
                    "contains-" + argument.getWork().getId() + "-" + argument.getId(),
                    nodeId("work", argument.getWork().getId()),
                    nodeId("argument", argument.getId()),
                    "argumenta"));
        }

        for (DialecticalRelation relation : relationRepository.findAllByOrderByIdAsc()) {
            Long sourceId = relation.getSourceArgument().getId();
            Long targetId = relation.getTargetArgument().getId();
            if (!argumentIds.contains(sourceId) || !argumentIds.contains(targetId)) {
                continue;
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("kind", "DIALECTICA");
            data.put("relationType", relation.getRelationType().name());
            data.put("description", relation.getDescription() == null ? "" : relation.getDescription());

            edges.add(new GraphEdgeDto(
                    "relation-" + relation.getId(),
                    nodeId("argument", sourceId),
                    nodeId("argument", targetId),
                    "dialectical",
                    relation.getRelationType().name(),
                    // La refutacion se anima: es la arista que el lector busca primero.
                    relation.getRelationType() == RelationType.REFUTA,
                    data));
        }

        return new DialecticGraphDto(nodes, edges);
    }

    private static GraphEdgeDto structuralEdge(String id, String source, String target, String label) {
        return new GraphEdgeDto(id, source, target, "structural", label, false,
                Map.of("kind", "ESTRUCTURAL"));
    }

    private static String nodeId(String prefix, Long id) {
        return prefix + "-" + id;
    }
}
