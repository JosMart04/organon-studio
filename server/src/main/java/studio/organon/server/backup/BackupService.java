package studio.organon.server.backup;

import static studio.organon.server.backup.BackupValidator.safe;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studio.organon.server.backup.BackupDocument.ArgumentEntry;
import studio.organon.server.backup.BackupDocument.ConceptEntry;
import studio.organon.server.backup.BackupDocument.Data;
import studio.organon.server.backup.BackupDocument.DefinitionEntry;
import studio.organon.server.backup.BackupDocument.ObjectionEntry;
import studio.organon.server.backup.BackupDocument.PassageEntry;
import studio.organon.server.backup.BackupDocument.PhilosopherEntry;
import studio.organon.server.backup.BackupDocument.PremiseEntry;
import studio.organon.server.backup.BackupDocument.RelationEntry;
import studio.organon.server.backup.BackupDocument.WorkEntry;
import studio.organon.server.domain.corpus.Passage;
import studio.organon.server.domain.corpus.Philosopher;
import studio.organon.server.domain.corpus.Work;
import studio.organon.server.domain.dialectic.DialecticalRelation;
import studio.organon.server.domain.logic.Argument;
import studio.organon.server.domain.logic.FormalScheme;
import studio.organon.server.domain.logic.Objection;
import studio.organon.server.domain.logic.Premise;
import studio.organon.server.domain.logic.PremiseType;
import studio.organon.server.domain.logic.SoundStatus;
import studio.organon.server.domain.semantics.SemanticConcept;
import studio.organon.server.domain.semantics.TermDefinition;
import studio.organon.server.repository.ArgumentRepository;
import studio.organon.server.repository.DialecticalRelationRepository;
import studio.organon.server.repository.PassageRepository;
import studio.organon.server.repository.PhilosopherRepository;
import studio.organon.server.repository.SemanticConceptRepository;
import studio.organon.server.repository.TermDefinitionRepository;
import studio.organon.server.repository.WorkRepository;

/**
 * Saca el cuaderno entero a un fichero y lo vuelve a meter.
 *
 * <p>La importacion es todo o nada: valida la copia completa antes de escribir,
 * trabaja en una sola transaccion y fuerza el flush antes de salir, de modo que
 * cualquier fallo deja la base exactamente como estaba.
 */
@Service
public class BackupService {

    private static final Sort BY_ID = Sort.by("id");

    /**
     * Transaccional en PostgreSQL, igual que el reinicio de las identidades: si
     * algo falla despues, el rollback devuelve los datos.
     *
     * <p>Los vectores de busqueda van tambien: con las identidades reiniciadas,
     * un vector antiguo apuntaria a otra entrada con el mismo id.
     */
    private static final String TRUNCATE_ALL = """
            TRUNCATE TABLE dialectical_relation, objection, premise, argument,
                           term_definition, semantic_concept, passage, work, philosopher,
                           semantic_embedding
            RESTART IDENTITY CASCADE""";

    private final PhilosopherRepository philosopherRepository;
    private final WorkRepository workRepository;
    private final PassageRepository passageRepository;
    private final SemanticConceptRepository conceptRepository;
    private final TermDefinitionRepository definitionRepository;
    private final ArgumentRepository argumentRepository;
    private final DialecticalRelationRepository relationRepository;
    private final JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager entityManager;

    public BackupService(PhilosopherRepository philosopherRepository,
                         WorkRepository workRepository,
                         PassageRepository passageRepository,
                         SemanticConceptRepository conceptRepository,
                         TermDefinitionRepository definitionRepository,
                         ArgumentRepository argumentRepository,
                         DialecticalRelationRepository relationRepository,
                         JdbcTemplate jdbc) {
        this.philosopherRepository = philosopherRepository;
        this.workRepository = workRepository;
        this.passageRepository = passageRepository;
        this.conceptRepository = conceptRepository;
        this.definitionRepository = definitionRepository;
        this.argumentRepository = argumentRepository;
        this.relationRepository = relationRepository;
        this.jdbc = jdbc;
    }

    // ----- Exportar ---------------------------------------------------------

    /** Orden por id en todos los tipos: dos exportaciones del mismo cuaderno dan el mismo fichero. */
    @Transactional(readOnly = true)
    public BackupDocument export() {
        List<PhilosopherEntry> philosophers = philosopherRepository.findAll(BY_ID).stream()
                .map(p -> new PhilosopherEntry(p.getId(), p.getName(), p.getEpoch(), p.getSchool(),
                        p.getBiographicalSummary(), p.getAvatarEmoji()))
                .toList();

        List<WorkEntry> works = workRepository.findAll(BY_ID).stream()
                .map(w -> new WorkEntry(w.getId(), w.getPhilosopher().getId(),
                        w.getDirectAdversary() == null ? null : w.getDirectAdversary().getId(),
                        w.getTitle(), w.getOriginalYear(), w.getPhilosophicalProblem(), w.getCoreThesis()))
                .toList();

        List<PassageEntry> passages = passageRepository.findAll(BY_ID).stream()
                .map(p -> new PassageEntry(p.getId(), p.getWork().getId(), p.getLocator(), p.getTextContent(),
                        p.getPageNumber(), p.getPersonalNotes()))
                .toList();

        List<ConceptEntry> concepts = conceptRepository.findAll(BY_ID).stream()
                .map(c -> new ConceptEntry(c.getId(), c.getTerm(), c.getDescription()))
                .toList();

        List<DefinitionEntry> definitions = definitionRepository.findAll(BY_ID).stream()
                .map(d -> new DefinitionEntry(d.getId(), d.getConcept().getId(), d.getPhilosopher().getId(),
                        d.getWork() == null ? null : d.getWork().getId(),
                        d.getOperationalDefinition(), d.getNotes()))
                .toList();

        List<ArgumentEntry> arguments = argumentRepository.findAllForBackup().stream()
                .map(BackupService::toEntry)
                .toList();

        List<RelationEntry> relations = relationRepository.findAll(BY_ID).stream()
                .map(r -> new RelationEntry(r.getSourceArgument().getId(), r.getTargetArgument().getId(),
                        r.getRelationType(), r.getDescription()))
                .toList();

        Data data = new Data(philosophers, works, passages, concepts, definitions, arguments, relations);
        return new BackupDocument(BackupDocument.FORMAT, BackupDocument.CURRENT_VERSION, schemaVersion(),
                Instant.now(), countsOf(data), data);
    }

    private static ArgumentEntry toEntry(Argument argument) {
        List<PremiseEntry> premises = argument.getPremises().stream()
                .sorted(Comparator.comparingInt(Premise::getOrderIndex))
                .map(p -> new PremiseEntry(p.getOrderIndex(), p.getStatement(), p.isEnthymeme(), p.getPremiseType(),
                        p.getObjections().stream()
                                .sorted(Comparator.comparing(Objection::getId))
                                .map(o -> new ObjectionEntry(o.getObjectionType(), o.getExplanation()))
                                .toList()))
                .toList();
        return new ArgumentEntry(argument.getId(), argument.getWork().getId(),
                argument.getPassage() == null ? null : argument.getPassage().getId(),
                argument.getName(), argument.getFormalScheme(), argument.getLatexFormalization(),
                argument.getSoundStatus(), premises);
    }

    // ----- Importar ---------------------------------------------------------

    @Transactional
    public ImportReport importBackup(BackupDocument document, ImportMode mode) {
        BackupValidator.validate(document);

        Tally tally = new Tally();
        List<String> warnings = new ArrayList<>();

        if (mode == ImportMode.REPLACE) {
            jdbc.execute(TRUNCATE_ALL);
            // Tras el TRUNCATE las identidades vuelven a empezar: cualquier entidad
            // que siguiera en el contexto de persistencia chocaria con las nuevas.
            entityManager.clear();
        }

        Data data = document.data();
        Map<Long, Philosopher> philosophers = importPhilosophers(safe(data.philosophers()), tally);
        Map<Long, Work> works = importWorks(safe(data.works()), philosophers, tally);
        Map<Long, Passage> passages = importPassages(safe(data.passages()), works, tally);
        Map<Long, SemanticConcept> concepts = importConcepts(safe(data.concepts()), tally);
        importDefinitions(safe(data.definitions()), concepts, philosophers, works, tally);
        Map<Long, Argument> arguments = importArguments(safe(data.arguments()), works, passages, tally);
        importRelations(safe(data.relations()), arguments, tally, warnings);

        // Flush dentro de la transaccion y a traves de un repositorio: una violacion
        // de restriccion sale aqui traducida a 409, no al hacer commit como un 500.
        argumentRepository.flush();

        return new ImportReport(mode, tally.created, tally.skipped, warnings);
    }

    /*
     * Criterio comun a todos los tipos en modo MERGE: lo que ya existia por clave
     * natural se omite y su id entra en el mapa de referencias, para que los hijos
     * de la copia se enganchen a el. Nunca se modifica lo existente.
     *
     * Los tipos con restriccion unica en la base (pensador, fragmento, palabra,
     * definicion, conexion) anaden tambien lo recien creado al indice, porque dos
     * entradas que acaben apuntando al mismo destino violarian la restriccion. Los
     * que no la tienen (libro, idea) solo se casan con lo que habia antes: dos
     * libros homonimos del mismo autor en la copia se respetan como distintos.
     */

    private Map<Long, Philosopher> importPhilosophers(List<PhilosopherEntry> entries, Tally tally) {
        Map<String, Philosopher> existing = index(philosopherRepository.findAll(), p -> key(p.getName()));
        Map<Long, Philosopher> byRef = new HashMap<>();
        for (PhilosopherEntry e : entries) {
            Philosopher found = existing.get(key(e.name()));
            if (found != null) {
                byRef.put(e.ref(), found);
                tally.skip(BackupDocument.PHILOSOPHERS);
                continue;
            }
            Philosopher philosopher = new Philosopher(e.name().trim(), e.epoch(), e.school(), e.biographicalSummary());
            philosopher.setAvatarEmoji(e.avatarEmoji());
            Philosopher saved = philosopherRepository.save(philosopher);
            existing.put(key(saved.getName()), saved);
            byRef.put(e.ref(), saved);
            tally.create(BackupDocument.PHILOSOPHERS);
        }
        return byRef;
    }

    private Map<Long, Work> importWorks(List<WorkEntry> entries, Map<Long, Philosopher> philosophers, Tally tally) {
        Map<String, Work> existing = index(workRepository.findAll(),
                w -> w.getPhilosopher().getId() + "|" + key(w.getTitle()));
        Map<Long, Work> byRef = new HashMap<>();
        List<WorkEntry> created = new ArrayList<>();
        for (WorkEntry e : entries) {
            Philosopher author = philosophers.get(e.philosopherRef());
            Work found = existing.get(author.getId() + "|" + key(e.title()));
            if (found != null) {
                byRef.put(e.ref(), found);
                tally.skip(BackupDocument.WORKS);
                continue;
            }
            Work saved = workRepository.save(new Work(author, e.title().trim(), e.originalYear(),
                    e.philosophicalProblem(), e.coreThesis()));
            byRef.put(e.ref(), saved);
            created.add(e);
            tally.create(BackupDocument.WORKS);
        }
        // Segunda pasada: el adversario puede aparecer en la copia despues del libro que lo cita.
        for (WorkEntry e : created) {
            if (e.directAdversaryRef() != null) {
                byRef.get(e.ref()).setDirectAdversary(byRef.get(e.directAdversaryRef()));
            }
        }
        return byRef;
    }

    private Map<Long, Passage> importPassages(List<PassageEntry> entries, Map<Long, Work> works, Tally tally) {
        Map<String, Passage> existing = index(passageRepository.findAll(),
                p -> p.getWork().getId() + "|" + p.getLocator().trim());
        Map<Long, Passage> byRef = new HashMap<>();
        for (PassageEntry e : entries) {
            Work work = works.get(e.workRef());
            String scope = work.getId() + "|" + e.locator().trim();
            Passage found = existing.get(scope);
            if (found != null) {
                byRef.put(e.ref(), found);
                tally.skip(BackupDocument.PASSAGES);
                continue;
            }
            Passage passage = new Passage(work, e.locator().trim(), e.textContent(), e.pageNumber());
            passage.setPersonalNotes(e.personalNotes());
            Passage saved = passageRepository.save(passage);
            existing.put(scope, saved);
            byRef.put(e.ref(), saved);
            tally.create(BackupDocument.PASSAGES);
        }
        return byRef;
    }

    private Map<Long, SemanticConcept> importConcepts(List<ConceptEntry> entries, Tally tally) {
        Map<String, SemanticConcept> existing = index(conceptRepository.findAll(), c -> key(c.getTerm()));
        Map<Long, SemanticConcept> byRef = new HashMap<>();
        for (ConceptEntry e : entries) {
            SemanticConcept found = existing.get(key(e.term()));
            if (found != null) {
                byRef.put(e.ref(), found);
                tally.skip(BackupDocument.CONCEPTS);
                continue;
            }
            SemanticConcept saved = conceptRepository.save(new SemanticConcept(e.term().trim(), e.description()));
            existing.put(key(saved.getTerm()), saved);
            byRef.put(e.ref(), saved);
            tally.create(BackupDocument.CONCEPTS);
        }
        return byRef;
    }

    private void importDefinitions(List<DefinitionEntry> entries, Map<Long, SemanticConcept> concepts,
                                   Map<Long, Philosopher> philosophers, Map<Long, Work> works, Tally tally) {
        Map<String, TermDefinition> existing = index(definitionRepository.findAll(),
                d -> scope(d.getConcept().getId(), d.getPhilosopher().getId(),
                        d.getWork() == null ? null : d.getWork().getId()));
        for (DefinitionEntry e : entries) {
            SemanticConcept concept = concepts.get(e.conceptRef());
            Philosopher philosopher = philosophers.get(e.philosopherRef());
            Work work = e.workRef() == null ? null : works.get(e.workRef());
            String scope = scope(concept.getId(), philosopher.getId(), work == null ? null : work.getId());
            if (existing.containsKey(scope)) {
                tally.skip(BackupDocument.DEFINITIONS);
                continue;
            }
            TermDefinition saved = definitionRepository.save(
                    new TermDefinition(concept, philosopher, work, e.operationalDefinition(), e.notes()));
            existing.put(scope, saved);
            tally.create(BackupDocument.DEFINITIONS);
        }
    }

    private Map<Long, Argument> importArguments(List<ArgumentEntry> entries, Map<Long, Work> works,
                                                Map<Long, Passage> passages, Tally tally) {
        Map<String, Argument> existing = index(argumentRepository.findAll(),
                a -> a.getWork().getId() + "|" + key(a.getName()));
        Map<Long, Argument> byRef = new HashMap<>();
        for (ArgumentEntry e : entries) {
            Work work = works.get(e.workRef());
            Argument found = existing.get(work.getId() + "|" + key(e.name()));
            if (found != null) {
                // Se omite la idea entera: mezclar sus razones con las de la copia
                // produciria un argumento que nadie escribio.
                byRef.put(e.ref(), found);
                tally.skip(BackupDocument.ARGUMENTS);
                continue;
            }
            Passage passage = e.passageRef() == null ? null : passages.get(e.passageRef());
            Argument argument = new Argument(work, passage, e.name().trim(),
                    e.formalScheme() == null ? FormalScheme.NO_CLASIFICADO : e.formalScheme(),
                    e.latexFormalization());
            argument.setSoundStatus(e.soundStatus() == null ? SoundStatus.PENDIENTE : e.soundStatus());

            // Se renumeran 0..n-1 en el orden de la copia: un hueco o un indice
            // repetido en el fichero no debe chocar con la unicidad del orden.
            List<PremiseEntry> ordered = safe(e.premises()).stream()
                    .sorted(Comparator.comparingInt(PremiseEntry::orderIndex))
                    .toList();
            for (int i = 0; i < ordered.size(); i++) {
                PremiseEntry pe = ordered.get(i);
                Premise premise = new Premise(i, pe.statement().trim(), pe.enthymeme(),
                        pe.premiseType() == null ? PremiseType.EMPIRICA : pe.premiseType());
                for (ObjectionEntry oe : safe(pe.objections())) {
                    premise.addObjection(new Objection(oe.objectionType(), oe.explanation().trim()));
                }
                argument.addPremise(premise);
            }
            byRef.put(e.ref(), argumentRepository.save(argument));
            tally.create(BackupDocument.ARGUMENTS);
        }
        return byRef;
    }

    private void importRelations(List<RelationEntry> entries, Map<Long, Argument> arguments, Tally tally,
                                 List<String> warnings) {
        Map<String, DialecticalRelation> existing = index(relationRepository.findAll(),
                r -> r.getSourceArgument().getId() + "|" + r.getTargetArgument().getId() + "|" + r.getRelationType());
        for (RelationEntry e : entries) {
            Argument source = arguments.get(e.sourceArgumentRef());
            Argument target = arguments.get(e.targetArgumentRef());
            if (source.getId().equals(target.getId())) {
                // Solo puede pasar al fusionar: dos ideas distintas de la copia que
                // resultan ser la misma del cuaderno.
                warnings.add("Se ha omitido una conexión de «" + source.getName()
                        + "» consigo misma: al fusionar, sus dos extremos resultaron ser la misma idea.");
                tally.skip(BackupDocument.RELATIONS);
                continue;
            }
            String triple = source.getId() + "|" + target.getId() + "|" + e.relationType();
            if (existing.containsKey(triple)) {
                tally.skip(BackupDocument.RELATIONS);
                continue;
            }
            DialecticalRelation saved = relationRepository.save(
                    new DialecticalRelation(source, target, e.relationType(), e.description()));
            existing.put(triple, saved);
            tally.create(BackupDocument.RELATIONS);
        }
    }

    // ----- Utilidades -------------------------------------------------------

    static Map<String, Integer> countsOf(Data data) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put(BackupDocument.PHILOSOPHERS, safe(data.philosophers()).size());
        counts.put(BackupDocument.WORKS, safe(data.works()).size());
        counts.put(BackupDocument.PASSAGES, safe(data.passages()).size());
        counts.put(BackupDocument.CONCEPTS, safe(data.concepts()).size());
        counts.put(BackupDocument.DEFINITIONS, safe(data.definitions()).size());
        counts.put(BackupDocument.ARGUMENTS, safe(data.arguments()).size());
        counts.put(BackupDocument.RELATIONS, safe(data.relations()).size());
        return counts;
    }

    private String schemaVersion() {
        try {
            return jdbc.queryForObject("""
                    SELECT version FROM flyway_schema_history
                    WHERE success AND version IS NOT NULL
                    ORDER BY installed_rank DESC LIMIT 1""", String.class);
        } catch (DataAccessException e) {
            return "desconocida";
        }
    }

    private static <T> Map<String, T> index(List<T> items, Function<T, String> key) {
        Map<String, T> index = new HashMap<>();
        for (T item : items) {
            index.putIfAbsent(key.apply(item), item);
        }
        return index;
    }

    private static String scope(Long conceptId, Long philosopherId, Long workId) {
        return conceptId + "|" + philosopherId + "|" + (workId == null ? "-" : workId);
    }

    private static String key(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    /** Recuentos por tipo, con todos los tipos presentes aunque valgan cero. */
    private static final class Tally {
        private final Map<String, Integer> created = zeroes();
        private final Map<String, Integer> skipped = zeroes();

        void create(String type) {
            created.merge(type, 1, Integer::sum);
        }

        void skip(String type) {
            skipped.merge(type, 1, Integer::sum);
        }

        private static Map<String, Integer> zeroes() {
            Map<String, Integer> map = new LinkedHashMap<>();
            BackupDocument.TYPES.forEach(type -> map.put(type, 0));
            return map;
        }
    }
}
