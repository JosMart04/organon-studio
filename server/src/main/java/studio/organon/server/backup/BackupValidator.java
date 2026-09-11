package studio.organon.server.backup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
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

/**
 * Comprueba una copia entera antes de tocar la base.
 *
 * <p>Todo lo que haria fallar la restauracion a medias —una referencia que no
 * lleva a ningun sitio, un nombre repetido que violaria una restriccion unica—
 * se detecta aqui, y se informa de todos los problemas a la vez en lugar de
 * del primero. Es logica pura: no necesita base de datos para probarse.
 */
public final class BackupValidator {

    /** Con diez problemas ya se ve que pasa; mas alla el informe deja de ayudar. */
    private static final int MAX_PROBLEMS = 10;

    private BackupValidator() {
    }

    public static void validate(BackupDocument document) {
        if (document == null) {
            throw new InvalidBackupException("El fichero está vacío.");
        }
        if (!BackupDocument.FORMAT.equals(document.format())) {
            throw new InvalidBackupException("Este fichero no es una copia de seguridad de Organon Studio.");
        }
        Integer version = document.formatVersion();
        if (version == null || version < 1) {
            throw new InvalidBackupException("La copia no indica qué versión de formato usa.");
        }
        if (version > BackupDocument.CURRENT_VERSION) {
            throw new InvalidBackupException("Esta copia se hizo con una versión más nueva de la aplicación "
                    + "(formato " + version + "). Actualiza Organon Studio antes de restaurarla.");
        }
        if (document.data() == null) {
            throw new InvalidBackupException("La copia no contiene datos.");
        }

        Data data = document.data();
        List<String> problems = new ArrayList<>();

        Set<Long> philosophers = refs(safe(data.philosophers()), PhilosopherEntry::ref, "pensadores", problems);
        Set<Long> works = refs(safe(data.works()), WorkEntry::ref, "libros", problems);
        Set<Long> passages = refs(safe(data.passages()), PassageEntry::ref, "pasajes", problems);
        Set<Long> concepts = refs(safe(data.concepts()), ConceptEntry::ref, "términos", problems);
        Set<Long> arguments = refs(safe(data.arguments()), ArgumentEntry::ref, "ideas", problems);

        checkPhilosophers(safe(data.philosophers()), problems);
        checkWorks(safe(data.works()), philosophers, works, problems);
        checkPassages(safe(data.passages()), works, problems);
        checkConcepts(safe(data.concepts()), problems);
        checkDefinitions(safe(data.definitions()), concepts, philosophers, works, problems);
        checkArguments(safe(data.arguments()), works, passages, problems);
        checkRelations(safe(data.relations()), arguments, problems);

        if (!problems.isEmpty()) {
            String summary = problems.size() == 1
                    ? "La copia tiene un problema y no se ha restaurado nada."
                    : "La copia tiene " + problems.size() + " problemas y no se ha restaurado nada.";
            throw new InvalidBackupException(summary, problems.stream().limit(MAX_PROBLEMS).toList());
        }
    }

    private static void checkPhilosophers(List<PhilosopherEntry> entries, List<String> problems) {
        Set<String> names = new HashSet<>();
        for (PhilosopherEntry e : entries) {
            if (blank(e.name())) {
                problems.add("Hay un pensador sin nombre.");
                continue;
            }
            if (e.epoch() == null) {
                problems.add("«" + e.name() + "» no tiene época.");
            }
            if (!names.add(key(e.name()))) {
                problems.add("El pensador «" + e.name() + "» aparece dos veces.");
            }
        }
    }

    private static void checkWorks(List<WorkEntry> entries, Set<Long> philosophers, Set<Long> works,
                                   List<String> problems) {
        for (WorkEntry e : entries) {
            String title = blank(e.title()) ? "sin título" : e.title();
            if (blank(e.title())) {
                problems.add("Hay un libro sin título.");
            }
            if (!philosophers.contains(e.philosopherRef())) {
                problems.add("El libro «" + title + "» apunta a un pensador que no está en la copia.");
            }
            if (e.directAdversaryRef() != null) {
                if (e.directAdversaryRef().equals(e.ref())) {
                    problems.add("El libro «" + title + "» figura como adversario de sí mismo.");
                } else if (!works.contains(e.directAdversaryRef())) {
                    problems.add("El libro «" + title + "» cita como adversario un libro que no está en la copia.");
                }
            }
        }
    }

    private static void checkPassages(List<PassageEntry> entries, Set<Long> works, List<String> problems) {
        Set<String> scopes = new HashSet<>();
        for (PassageEntry e : entries) {
            String locator = blank(e.locator()) ? "sin página" : e.locator();
            if (blank(e.locator())) {
                problems.add("Hay un fragmento sin página ni referencia.");
            }
            if (blank(e.textContent())) {
                problems.add("El fragmento «" + locator + "» está vacío.");
            }
            if (!works.contains(e.workRef())) {
                problems.add("El fragmento «" + locator + "» apunta a un libro que no está en la copia.");
            } else if (!blank(e.locator()) && !scopes.add(e.workRef() + "|" + e.locator().trim())) {
                problems.add("El fragmento «" + locator + "» aparece dos veces en el mismo libro.");
            }
        }
    }

    private static void checkConcepts(List<ConceptEntry> entries, List<String> problems) {
        Set<String> terms = new HashSet<>();
        for (ConceptEntry e : entries) {
            if (blank(e.term())) {
                problems.add("Hay una palabra del glosario sin escribir.");
            } else if (!terms.add(key(e.term()))) {
                problems.add("La palabra «" + e.term() + "» aparece dos veces en el glosario.");
            }
        }
    }

    private static void checkDefinitions(List<DefinitionEntry> entries, Set<Long> concepts,
                                         Set<Long> philosophers, Set<Long> works, List<String> problems) {
        Set<String> scopes = new HashSet<>();
        for (DefinitionEntry e : entries) {
            boolean refsOk = true;
            if (!concepts.contains(e.conceptRef())) {
                problems.add("Una definición apunta a una palabra que no está en la copia.");
                refsOk = false;
            }
            if (!philosophers.contains(e.philosopherRef())) {
                problems.add("Una definición apunta a un pensador que no está en la copia.");
                refsOk = false;
            }
            if (e.workRef() != null && !works.contains(e.workRef())) {
                problems.add("Una definición apunta a un libro que no está en la copia.");
                refsOk = false;
            }
            if (blank(e.operationalDefinition())) {
                problems.add("Hay una definición vacía.");
            }
            if (refsOk && !scopes.add(e.conceptRef() + "|" + e.philosopherRef() + "|" + e.workRef())) {
                problems.add("Hay dos definiciones de la misma palabra para el mismo autor y libro.");
            }
        }
    }

    private static void checkArguments(List<ArgumentEntry> entries, Set<Long> works, Set<Long> passages,
                                       List<String> problems) {
        for (ArgumentEntry e : entries) {
            String name = blank(e.name()) ? "sin nombre" : e.name();
            if (blank(e.name())) {
                problems.add("Hay una idea sin nombre.");
            }
            if (!works.contains(e.workRef())) {
                problems.add("La idea «" + name + "» apunta a un libro que no está en la copia.");
            }
            if (e.passageRef() != null && !passages.contains(e.passageRef())) {
                problems.add("La idea «" + name + "» apunta a un fragmento que no está en la copia.");
            }
            for (PremiseEntry premise : safe(e.premises())) {
                if (blank(premise.statement())) {
                    problems.add("La idea «" + name + "» tiene una razón vacía.");
                }
                for (ObjectionEntry objection : safe(premise.objections())) {
                    if (objection.objectionType() == null || blank(objection.explanation())) {
                        problems.add("La idea «" + name + "» tiene una crítica incompleta.");
                    }
                }
            }
        }
    }

    private static void checkRelations(List<RelationEntry> entries, Set<Long> arguments, List<String> problems) {
        Set<String> triples = new HashSet<>();
        for (RelationEntry e : entries) {
            if (!arguments.contains(e.sourceArgumentRef()) || !arguments.contains(e.targetArgumentRef())) {
                problems.add("Una conexión del debate apunta a una idea que no está en la copia.");
                continue;
            }
            if (e.relationType() == null) {
                problems.add("Una conexión del debate no indica de qué tipo es.");
                continue;
            }
            if (e.sourceArgumentRef().equals(e.targetArgumentRef())) {
                problems.add("Una conexión del debate une una idea consigo misma.");
            } else if (!triples.add(e.sourceArgumentRef() + "|" + e.targetArgumentRef() + "|" + e.relationType())) {
                problems.add("Hay una conexión del debate repetida.");
            }
        }
    }

    /** Recoge las {@code ref} de un tipo y denuncia las ausentes o repetidas. */
    private static <T> Set<Long> refs(List<T> entries, Function<T, Long> ref, String what, List<String> problems) {
        Set<Long> seen = new HashSet<>();
        boolean missingReported = false;
        for (T entry : entries) {
            Long value = ref.apply(entry);
            if (value == null) {
                if (!missingReported) {
                    problems.add("Hay " + what + " sin referencia interna («ref»).");
                    missingReported = true;
                }
            } else if (!seen.add(value)) {
                problems.add("La referencia " + value + " se repite entre los " + what + ".");
            }
        }
        return seen;
    }

    static <T> List<T> safe(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String key(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
