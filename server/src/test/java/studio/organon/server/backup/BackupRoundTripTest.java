package studio.organon.server.backup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import studio.organon.server.backup.BackupDocument.ArgumentEntry;
import studio.organon.server.backup.BackupDocument.ConceptEntry;
import studio.organon.server.backup.BackupDocument.Data;
import studio.organon.server.backup.BackupDocument.PassageEntry;
import studio.organon.server.backup.BackupDocument.PhilosopherEntry;
import studio.organon.server.backup.BackupDocument.WorkEntry;
import studio.organon.server.domain.corpus.Epoch;
import studio.organon.server.domain.corpus.Passage;
import studio.organon.server.domain.corpus.Philosopher;
import studio.organon.server.domain.corpus.Work;
import studio.organon.server.domain.dialectic.DialecticalRelation;
import studio.organon.server.domain.dialectic.RelationType;
import studio.organon.server.domain.logic.Argument;
import studio.organon.server.domain.logic.FormalScheme;
import studio.organon.server.domain.logic.Objection;
import studio.organon.server.domain.logic.ObjectionType;
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
 * Ida y vuelta completa contra PostgreSQL: exportar, sustituir el cuaderno por la
 * copia y volver a exportar debe dar lo mismo.
 *
 * <p><b>Por que es seguro correrlo contra la base de desarrollo.</b> Cada prueba
 * va dentro de una transaccion que se revierte al terminar, y en PostgreSQL tanto
 * {@code TRUNCATE} como su {@code RESTART IDENTITY} son transaccionales: aunque la
 * prueba falle a medias o se mate el proceso, las notas reales vuelven intactas.
 * Eso solo se cumple mientras {@link BackupService#importBackup} participe en la
 * transaccion de la prueba; si algun dia se le pone REQUIRES_NEW, esta prueba
 * dejaria de ser segura.
 */
@Tag("integracion")
@Transactional
@SpringBootTest(properties = "organon.seed.enabled=false")
class BackupRoundTripTest {

    @Autowired private BackupService backupService;
    @Autowired private PhilosopherRepository philosophers;
    @Autowired private WorkRepository works;
    @Autowired private PassageRepository passages;
    @Autowired private SemanticConceptRepository concepts;
    @Autowired private TermDefinitionRepository definitions;
    @Autowired private ArgumentRepository arguments;
    @Autowired private DialecticalRelationRepository relations;

    @Test
    @DisplayName("Exportar, sustituir y volver a exportar da exactamente el mismo cuaderno")
    void sustituirConservaElCuaderno() {
        prepararCuaderno();
        BackupDocument original = backupService.export();

        ImportReport informe = backupService.importBackup(original, ImportMode.REPLACE);
        BackupDocument restaurada = backupService.export();

        assertThat(canonical(restaurada)).isEqualTo(canonical(original));
        assertThat(informe.created()).isEqualTo(original.counts());
        assertThat(informe.skipped().values()).allMatch(n -> n == 0);
    }

    @Test
    @DisplayName("Fusionar una copia del propio cuaderno no duplica nada")
    void fusionarNoDuplica() {
        prepararCuaderno();
        BackupDocument original = backupService.export();

        ImportReport informe = backupService.importBackup(original, ImportMode.MERGE);
        BackupDocument despues = backupService.export();

        assertThat(informe.created().values()).allMatch(n -> n == 0);
        assertThat(informe.skipped()).isEqualTo(original.counts());
        assertThat(canonical(despues)).isEqualTo(canonical(original));
    }

    @Test
    @DisplayName("Fusionar con un cuaderno distinto añade lo nuevo y engancha los hijos a lo existente")
    void fusionarAnadeLoNuevo() {
        prepararCuaderno();
        BackupDocument original = backupService.export();

        // Un tercer libro de Séneca, que ya existe: el libro es nuevo, el autor no.
        Long senecaRef = original.data().philosophers().stream()
                .filter(p -> p.name().equals("Séneca")).findFirst().orElseThrow().ref();
        List<WorkEntry> conNuevoLibro = Stream.concat(original.data().works().stream(),
                Stream.of(new WorkEntry(999L, senecaRef, null, "De la brevedad de la vida", 49, null, null)))
                .toList();
        Data data = original.data();
        BackupDocument ampliada = new BackupDocument(BackupDocument.FORMAT, 1, "2", Instant.now(), Map.of(),
                new Data(data.philosophers(), conNuevoLibro, data.passages(), data.concepts(),
                        data.definitions(), data.arguments(), data.relations()));

        ImportReport informe = backupService.importBackup(ampliada, ImportMode.MERGE);

        assertThat(informe.created().get(BackupDocument.WORKS)).isEqualTo(1);
        assertThat(informe.created().get(BackupDocument.PHILOSOPHERS)).isZero();
        assertThat(works.findAll()).anyMatch(w -> w.getTitle().equals("De la brevedad de la vida")
                && w.getPhilosopher().getName().equals("Séneca"));
    }

    @Test
    @DisplayName("Una copia inválida no toca el cuaderno")
    void copiaInvalidaNoTocaNada() {
        prepararCuaderno();
        long antes = philosophers.count();
        BackupDocument rota = new BackupDocument(BackupDocument.FORMAT, 1, "2", Instant.now(), Map.of(),
                new Data(List.of(), List.of(new WorkEntry(1L, 42L, null, "Huérfano", null, null, null)),
                        null, null, null, null, null));

        assertThatThrownBy(() -> backupService.importBackup(rota, ImportMode.REPLACE))
                .isInstanceOf(InvalidBackupException.class);
        assertThat(philosophers.count()).isEqualTo(antes);
    }

    // ----- cuaderno de prueba ----------------------------------------------

    /**
     * Parte de un cuaderno vacío —dentro de la transacción de la prueba— para no
     * depender de lo que haya en la base de desarrollo, y cubre cada tipo y cada
     * caso raro: adversario, definición general y de libro, entimema, crítica,
     * idea sin fragmento y conexión.
     */
    private void prepararCuaderno() {
        backupService.importBackup(new BackupDocument(BackupDocument.FORMAT, 1, "2", Instant.now(), Map.of(),
                new Data(null, null, null, null, null, null, null)), ImportMode.REPLACE);

        Philosopher seneca = new Philosopher("Séneca", Epoch.ANTIGUA, "Estoicismo", "Escribía cartas.");
        seneca.setAvatarEmoji("🏛️");
        philosophers.save(seneca);
        Philosopher epicuro = philosophers.save(new Philosopher("Epicuro", Epoch.ANTIGUA, null, null));

        Work cartas = works.save(new Work(seneca, "Cartas a Lucilio", 65, "¿Cómo vivir bien?", null));
        Work meneceo = new Work(epicuro, "Carta a Meneceo", -300, null, "Perder el miedo a la muerte");
        meneceo.setDirectAdversary(cartas);
        works.save(meneceo);

        Passage carta1 = new Passage(cartas, "Carta 1", "Solo el tiempo es nuestro.", 1);
        carta1.setPersonalNotes("Lo único que no se puede guardar.");
        passages.save(carta1);

        SemanticConcept tiempo = concepts.save(new SemanticConcept("Tiempo", "Lo que pasa."));
        definitions.save(new TermDefinition(tiempo, seneca, cartas, "Lo único que poseemos.", null));
        definitions.save(new TermDefinition(tiempo, epicuro, null, "Lo que el miedo arruina.", "General."));

        Argument idea = new Argument(cartas, carta1, "Lo único que poseemos es el tiempo",
                FormalScheme.NO_CLASIFICADO, "T \\to P");
        idea.setSoundStatus(SoundStatus.FALAZ);
        Premise supuesto = new Premise(1, "Poseer es que no te lo puedan quitar.", true, PremiseType.AXIOMATICA);
        supuesto.addObjection(new Objection(ObjectionType.REDEFINICION, "Cambia lo que significa poseer."));
        idea.addPremise(new Premise(0, "Las cosas se pierden.", false, PremiseType.EMPIRICA));
        idea.addPremise(supuesto);
        idea.addPremise(new Premise(2, "Luego solo el tiempo es nuestro.", false, PremiseType.CONCLUSION));
        arguments.save(idea);

        Argument rival = new Argument(meneceo, null, "El tiempo no es tuyo si lo pasas temiendo",
                FormalScheme.INDUCTIVE, null);
        rival.addPremise(new Premise(0, "El miedo arruina el tiempo que tienes.", false, PremiseType.EMPIRICA));
        arguments.save(rival);

        relations.save(new DialecticalRelation(rival, idea, RelationType.MATIZA, "Matiza la posesión."));
        arguments.flush();
    }

    /**
     * La copia sin sus {@code ref}: cada referencia se sustituye por la clave
     * natural de lo que apunta. Dos cuadernos iguales dan lo mismo aunque sus ids
     * no coincidan.
     */
    private static Map<String, List<String>> canonical(BackupDocument document) {
        Data d = document.data();
        Map<Long, String> autor = byRef(d.philosophers(), PhilosopherEntry::ref, PhilosopherEntry::name);
        Map<Long, String> libro = byRef(d.works(), WorkEntry::ref, w -> autor.get(w.philosopherRef()) + "/" + w.title());
        Map<Long, String> fragmento = byRef(d.passages(), PassageEntry::ref, p -> libro.get(p.workRef()) + "#" + p.locator());
        Map<Long, String> palabra = byRef(d.concepts(), ConceptEntry::ref, ConceptEntry::term);
        Map<Long, String> ideaPorRef = byRef(d.arguments(), ArgumentEntry::ref, a -> libro.get(a.workRef()) + ":" + a.name());

        Map<String, List<String>> out = new LinkedHashMap<>();
        out.put("philosophers", sorted(d.philosophers().stream().map(p ->
                String.join("|", p.name(), String.valueOf(p.epoch()), p.school(), p.biographicalSummary(), p.avatarEmoji()))));
        out.put("works", sorted(d.works().stream().map(w ->
                String.join("|", libro.get(w.ref()), String.valueOf(w.originalYear()), w.philosophicalProblem(),
                        w.coreThesis(), w.directAdversaryRef() == null ? "-" : libro.get(w.directAdversaryRef())))));
        out.put("passages", sorted(d.passages().stream().map(p ->
                String.join("|", fragmento.get(p.ref()), p.textContent(), String.valueOf(p.pageNumber()), p.personalNotes()))));
        out.put("concepts", sorted(d.concepts().stream().map(c -> String.join("|", c.term(), c.description()))));
        out.put("definitions", sorted(d.definitions().stream().map(def ->
                String.join("|", palabra.get(def.conceptRef()), autor.get(def.philosopherRef()),
                        def.workRef() == null ? "-" : libro.get(def.workRef()), def.operationalDefinition(), def.notes()))));
        out.put("arguments", sorted(d.arguments().stream().map(a ->
                String.join("|", ideaPorRef.get(a.ref()), a.passageRef() == null ? "-" : fragmento.get(a.passageRef()),
                        String.valueOf(a.formalScheme()), a.latexFormalization(), String.valueOf(a.soundStatus()),
                        a.premises().stream()
                                .map(p -> p.orderIndex() + ":" + p.statement() + ":" + p.enthymeme() + ":" + p.premiseType()
                                        + p.objections().stream().map(o -> o.objectionType() + "=" + o.explanation())
                                        .sorted().toList())
                                .collect(Collectors.joining(";"))))));
        out.put("relations", sorted(d.relations().stream().map(r ->
                String.join("|", ideaPorRef.get(r.sourceArgumentRef()), ideaPorRef.get(r.targetArgumentRef()),
                        String.valueOf(r.relationType()), r.description()))));
        return out;
    }

    private static <T> Map<Long, String> byRef(List<T> items, Function<T, Long> ref, Function<T, String> label) {
        return items.stream().collect(Collectors.toMap(ref, label));
    }

    private static List<String> sorted(Stream<String> lines) {
        return lines.sorted().toList();
    }
}
