package studio.organon.server.backup;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import studio.organon.server.domain.corpus.Epoch;
import studio.organon.server.domain.dialectic.RelationType;
import studio.organon.server.domain.logic.FormalScheme;
import studio.organon.server.domain.logic.ObjectionType;
import studio.organon.server.domain.logic.PremiseType;
import studio.organon.server.domain.logic.SoundStatus;

/**
 * Copia de seguridad portable del cuaderno completo.
 *
 * <p>Las entradas se enlazan entre si con {@code ref}, no con los ids de la base:
 * esos ids cambian de una instalacion a otra, y una copia hecha en un equipo
 * tiene que poder restaurarse en otro. El importador traduce cada {@code ref} al
 * id que le toque en destino.
 *
 * <p>Los embeddings de busqueda no viajan en la copia: son derivados y se
 * regeneran solos a partir del texto.
 */
public record BackupDocument(
        String format,
        Integer formatVersion,
        String schemaVersion,
        Instant exportedAt,
        Map<String, Integer> counts,
        Data data) {

    public static final String FORMAT = "organon-backup";
    public static final int CURRENT_VERSION = 1;

    public static final String PHILOSOPHERS = "philosophers";
    public static final String WORKS = "works";
    public static final String PASSAGES = "passages";
    public static final String CONCEPTS = "concepts";
    public static final String DEFINITIONS = "definitions";
    public static final String ARGUMENTS = "arguments";
    public static final String RELATIONS = "relations";

    /** Orden de restauracion: cada tipo solo apunta a tipos anteriores. */
    public static final List<String> TYPES =
            List.of(PHILOSOPHERS, WORKS, PASSAGES, CONCEPTS, DEFINITIONS, ARGUMENTS, RELATIONS);

    public record Data(
            List<PhilosopherEntry> philosophers,
            List<WorkEntry> works,
            List<PassageEntry> passages,
            List<ConceptEntry> concepts,
            List<DefinitionEntry> definitions,
            List<ArgumentEntry> arguments,
            List<RelationEntry> relations) {
    }

    public record PhilosopherEntry(
            Long ref,
            String name,
            Epoch epoch,
            String school,
            String biographicalSummary,
            String avatarEmoji) {
    }

    public record WorkEntry(
            Long ref,
            Long philosopherRef,
            Long directAdversaryRef,
            String title,
            Integer originalYear,
            String philosophicalProblem,
            String coreThesis) {
    }

    public record PassageEntry(
            Long ref,
            Long workRef,
            String locator,
            String textContent,
            Integer pageNumber,
            String personalNotes) {
    }

    public record ConceptEntry(
            Long ref,
            String term,
            String description) {
    }

    /** {@code workRef} nulo: la definicion vale para todo el autor. */
    public record DefinitionEntry(
            Long ref,
            Long conceptRef,
            Long philosopherRef,
            Long workRef,
            String operationalDefinition,
            String notes) {
    }

    public record ArgumentEntry(
            Long ref,
            Long workRef,
            Long passageRef,
            String name,
            FormalScheme formalScheme,
            String latexFormalization,
            SoundStatus soundStatus,
            List<PremiseEntry> premises) {
    }

    /** Las razones no llevan {@code ref}: nada fuera de su idea apunta a ellas. */
    public record PremiseEntry(
            int orderIndex,
            String statement,
            boolean enthymeme,
            PremiseType premiseType,
            List<ObjectionEntry> objections) {
    }

    public record ObjectionEntry(
            ObjectionType objectionType,
            String explanation) {
    }

    public record RelationEntry(
            Long sourceArgumentRef,
            Long targetArgumentRef,
            RelationType relationType,
            String description) {
    }
}
