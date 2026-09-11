package studio.organon.server.backup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.organon.server.backup.BackupDocument.ArgumentEntry;
import studio.organon.server.backup.BackupDocument.Data;
import studio.organon.server.backup.BackupDocument.PhilosopherEntry;
import studio.organon.server.backup.BackupDocument.PremiseEntry;
import studio.organon.server.backup.BackupDocument.RelationEntry;
import studio.organon.server.backup.BackupDocument.WorkEntry;
import studio.organon.server.domain.corpus.Epoch;
import studio.organon.server.domain.dialectic.RelationType;
import studio.organon.server.domain.logic.FormalScheme;
import studio.organon.server.domain.logic.PremiseType;
import studio.organon.server.domain.logic.SoundStatus;

/**
 * Lo que la validacion deja pasar es lo que el importador intentara escribir.
 * Todo lo que rompiera la restauracion a medias tiene que pararse aqui.
 */
class BackupValidatorTest {

    private static final PhilosopherEntry SENECA =
            new PhilosopherEntry(1L, "Séneca", Epoch.ANTIGUA, null, null, "🏛️");
    private static final WorkEntry CARTAS =
            new WorkEntry(10L, 1L, null, "Cartas a Lucilio", 65, null, null);

    @Test
    @DisplayName("Una copia vacía es válida: el cuaderno recién estrenado también se puede salvar")
    void aceptaCopiaVacia() {
        BackupDocument vacia = document(new Data(null, null, null, null, null, null, null));

        assertThatCode(() -> BackupValidator.validate(vacia)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Una copia coherente con referencias cruzadas pasa")
    void aceptaCopiaCoherente() {
        ArgumentEntry idea = idea(100L, 10L, "Solo el tiempo es nuestro");
        ArgumentEntry rival = idea(101L, 10L, "El tiempo no es tuyo si temes");
        BackupDocument copia = document(new Data(List.of(SENECA), List.of(CARTAS), List.of(), List.of(),
                List.of(), List.of(idea, rival),
                List.of(new RelationEntry(101L, 100L, RelationType.MATIZA, null))));

        assertThatCode(() -> BackupValidator.validate(copia)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rechaza un JSON que no es una copia de Organon")
    void rechazaFormatoAjeno() {
        BackupDocument ajeno = new BackupDocument("otra-cosa", 1, "2", Instant.now(), Map.of(),
                new Data(null, null, null, null, null, null, null));

        assertThatThrownBy(() -> BackupValidator.validate(ajeno))
                .isInstanceOf(InvalidBackupException.class)
                .hasMessageContaining("no es una copia de seguridad");
    }

    @Test
    @DisplayName("Una copia de una versión más nueva pide actualizar en vez de fallar a medias")
    void rechazaVersionMasNueva() {
        BackupDocument futura = new BackupDocument(BackupDocument.FORMAT, 99, "9", Instant.now(), Map.of(),
                new Data(null, null, null, null, null, null, null));

        assertThatThrownBy(() -> BackupValidator.validate(futura))
                .isInstanceOf(InvalidBackupException.class)
                .hasMessageContaining("Actualiza");
    }

    @Test
    @DisplayName("Un libro que apunta a un pensador ausente se detecta antes de escribir nada")
    void detectaReferenciaColgante() {
        WorkEntry huerfano = new WorkEntry(10L, 7L, null, "Cartas a Lucilio", 65, null, null);
        BackupDocument copia = document(new Data(List.of(SENECA), List.of(huerfano),
                null, null, null, null, null));

        assertThatThrownBy(() -> BackupValidator.validate(copia))
                .isInstanceOf(InvalidBackupException.class)
                .satisfies(e -> assertThat(((InvalidBackupException) e).getProblems())
                        .anyMatch(p -> p.contains("apunta a un pensador que no está")));
    }

    @Test
    @DisplayName("Dos pensadores con el mismo nombre, con distinta mayúscula, violarían la unicidad")
    void detectaNombresRepetidos() {
        PhilosopherEntry duplicado = new PhilosopherEntry(2L, "séneca", Epoch.ANTIGUA, null, null, null);
        BackupDocument copia = document(new Data(List.of(SENECA, duplicado), null, null, null, null, null, null));

        assertThatThrownBy(() -> BackupValidator.validate(copia))
                .isInstanceOf(InvalidBackupException.class)
                .satisfies(e -> assertThat(((InvalidBackupException) e).getProblems())
                        .anyMatch(p -> p.contains("aparece dos veces")));
    }

    @Test
    @DisplayName("Una conexión de una idea consigo misma se rechaza")
    void detectaRelacionConsigoMisma() {
        ArgumentEntry idea = idea(100L, 10L, "Solo el tiempo es nuestro");
        BackupDocument copia = document(new Data(List.of(SENECA), List.of(CARTAS), null, null, null,
                List.of(idea), List.of(new RelationEntry(100L, 100L, RelationType.REFUTA, null))));

        assertThatThrownBy(() -> BackupValidator.validate(copia))
                .isInstanceOf(InvalidBackupException.class)
                .satisfies(e -> assertThat(((InvalidBackupException) e).getProblems())
                        .anyMatch(p -> p.contains("consigo misma")));
    }

    @Test
    @DisplayName("Informa de todos los problemas a la vez, no solo del primero")
    void informaDeTodosLosProblemas() {
        WorkEntry sinTitulo = new WorkEntry(10L, 7L, null, " ", null, null, null);
        PhilosopherEntry sinEpoca = new PhilosopherEntry(1L, "Séneca", null, null, null, null);
        BackupDocument copia = document(new Data(List.of(sinEpoca), List.of(sinTitulo),
                null, null, null, null, null));

        assertThatThrownBy(() -> BackupValidator.validate(copia))
                .isInstanceOf(InvalidBackupException.class)
                .satisfies(e -> assertThat(((InvalidBackupException) e).getProblems()).hasSizeGreaterThanOrEqualTo(3));
    }

    // ----- utilidades -----------------------------------------------------

    private static BackupDocument document(Data data) {
        return new BackupDocument(BackupDocument.FORMAT, BackupDocument.CURRENT_VERSION, "2",
                Instant.now(), Map.of(), data);
    }

    private static ArgumentEntry idea(Long ref, Long workRef, String name) {
        return new ArgumentEntry(ref, workRef, null, name, FormalScheme.NO_CLASIFICADO, null,
                SoundStatus.PENDIENTE,
                List.of(new PremiseEntry(0, "Luego " + name.toLowerCase(), false, PremiseType.CONCLUSION, List.of())));
    }
}
