package studio.organon.server.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.organon.server.domain.corpus.Epoch;
import studio.organon.server.domain.corpus.Philosopher;
import studio.organon.server.domain.corpus.Work;
import studio.organon.server.domain.logic.Argument;
import studio.organon.server.domain.logic.FormalScheme;
import studio.organon.server.domain.logic.Objection;
import studio.organon.server.domain.logic.ObjectionType;
import studio.organon.server.domain.logic.Premise;
import studio.organon.server.domain.logic.PremiseType;
import studio.organon.server.domain.logic.SoundStatus;
import studio.organon.server.web.dto.AuditReportDto;
import studio.organon.server.web.dto.AuditSeverity;

/**
 * El auditor es logica pura sobre el grafo de objetos, sin base de datos, asi
 * que se puede fijar con precision. Estas pruebas cubren las reglas que
 * determinan el veredicto, que es la decision que el investigador lee.
 */
class ArgumentAuditorTest {

    private final ArgumentAuditor auditor = new ArgumentAuditor();

    @Test
    @DisplayName("Un argumento completo, ordenado y sin reparos es SOLIDO")
    void solidoCuandoNoHayReparos() {
        Argument argument = argumento(FormalScheme.MODUS_PONENS);
        argument.addPremise(new Premise(0, "Si P entonces Q.", false, PremiseType.AXIOMATICA));
        argument.addPremise(new Premise(1, "P.", false, PremiseType.EMPIRICA));
        argument.addPremise(new Premise(2, "Luego Q.", false, PremiseType.CONCLUSION));

        AuditReportDto report = auditor.audit(argument);

        assertThat(report.soundStatus()).isEqualTo(SoundStatus.SOLIDO);
        assertThat(report.findings()).isEmpty();
        assertThat(report.premiseCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Sin conclusion la reconstruccion no cierra: FALAZ")
    void falazSinConclusion() {
        Argument argument = argumento(FormalScheme.MODUS_PONENS);
        argument.addPremise(new Premise(0, "Si P entonces Q.", false, PremiseType.AXIOMATICA));
        argument.addPremise(new Premise(1, "P.", false, PremiseType.EMPIRICA));

        AuditReportDto report = auditor.audit(argument);

        assertThat(report.soundStatus()).isEqualTo(SoundStatus.FALAZ);
        assertThat(codigos(report)).contains("SIN_CONCLUSION");
    }

    @Test
    @DisplayName("Un argumento sin premisas no se puede auditar todavia")
    void falazSinPremisas() {
        AuditReportDto report = auditor.audit(argumento(FormalScheme.MODUS_PONENS));

        assertThat(report.soundStatus()).isEqualTo(SoundStatus.FALAZ);
        assertThat(codigos(report)).containsExactly("SIN_PREMISAS");
    }

    @Test
    @DisplayName("Una conclusion sin premisas que la sostengan es una asercion, no un argumento")
    void falazConclusionSinApoyo() {
        Argument argument = argumento(FormalScheme.NO_CLASIFICADO);
        argument.addPremise(new Premise(0, "Dios existe.", false, PremiseType.CONCLUSION));

        AuditReportDto report = auditor.audit(argument);

        assertThat(report.soundStatus()).isEqualTo(SoundStatus.FALAZ);
        assertThat(codigos(report)).contains("CONCLUSION_SIN_APOYO");
    }

    @Test
    @DisplayName("Un entimema sin objecion deja el argumento PENDIENTE, no SOLIDO")
    void entimemaSinExaminarBajaAPendiente() {
        Argument argument = argumento(FormalScheme.MODUS_PONENS);
        argument.addPremise(new Premise(0, "Si P entonces Q.", false, PremiseType.AXIOMATICA));
        argument.addPremise(new Premise(1, "P se da siempre.", true, PremiseType.AXIOMATICA));
        argument.addPremise(new Premise(2, "Luego Q.", false, PremiseType.CONCLUSION));

        AuditReportDto report = auditor.audit(argument);

        assertThat(report.soundStatus()).isEqualTo(SoundStatus.PENDIENTE);
        assertThat(report.enthymemeCount()).isEqualTo(1);
        assertThat(codigos(report)).contains("ENTIMEMA_SIN_EXAMINAR");
    }

    @Test
    @DisplayName("Una peticion de principio invalida la inferencia: FALAZ")
    void objecionFatalHaceFalaz() {
        Argument argument = argumento(FormalScheme.MODUS_PONENS);
        argument.addPremise(new Premise(0, "Si P entonces Q.", false, PremiseType.AXIOMATICA));
        Premise atacada = new Premise(1, "P, que es lo que se queria probar.", false, PremiseType.EMPIRICA);
        atacada.addObjection(new Objection(
                ObjectionType.PETICION_DE_PRINCIPIO, "Da por supuesto lo que debia demostrar."));
        argument.addPremise(atacada);
        argument.addPremise(new Premise(2, "Luego Q.", false, PremiseType.CONCLUSION));

        AuditReportDto report = auditor.audit(argument);

        assertThat(report.soundStatus()).isEqualTo(SoundStatus.FALAZ);
        assertThat(report.objectionCount()).isEqualTo(1);
        assertThat(report.findings())
                .anyMatch(f -> f.code().equals("OBJECION_PETICION_DE_PRINCIPIO")
                        && f.severity() == AuditSeverity.BLOQUEANTE);
    }

    @Test
    @DisplayName("Un contraejemplo debilita pero no invalida: PENDIENTE")
    void objecionNoFatalDejaPendiente() {
        Argument argument = argumento(FormalScheme.INDUCTIVE);
        argument.addPremise(new Premise(0, "Todos los cisnes observados son blancos.", false, PremiseType.EMPIRICA));
        Premise atacada = new Premise(1, "La muestra es representativa.", false, PremiseType.EMPIRICA);
        atacada.addObjection(new Objection(ObjectionType.CONTRAEJEMPLO, "Los cisnes negros de Australia."));
        argument.addPremise(atacada);
        argument.addPremise(new Premise(2, "Luego todos los cisnes son blancos.", false, PremiseType.CONCLUSION));

        AuditReportDto report = auditor.audit(argument);

        assertThat(report.soundStatus()).isEqualTo(SoundStatus.PENDIENTE);
        assertThat(report.findings())
                .anyMatch(f -> f.code().equals("OBJECION_CONTRAEJEMPLO")
                        && f.severity() == AuditSeverity.ADVERTENCIA);
    }

    @Test
    @DisplayName("La conclusion fuera de la ultima posicion se senala como advertencia")
    void conclusionFueraDeOrden() {
        Argument argument = argumento(FormalScheme.MODUS_PONENS);
        argument.addPremise(new Premise(0, "Luego Q.", false, PremiseType.CONCLUSION));
        argument.addPremise(new Premise(1, "Si P entonces Q.", false, PremiseType.AXIOMATICA));
        argument.addPremise(new Premise(2, "P.", false, PremiseType.EMPIRICA));

        AuditReportDto report = auditor.audit(argument);

        assertThat(report.soundStatus()).isEqualTo(SoundStatus.PENDIENTE);
        assertThat(codigos(report)).contains("CONCLUSION_FUERA_DE_ORDEN");
    }

    @Test
    @DisplayName("Un esquema deductivo binario con una sola premisa sugiere un entimema por explicitar")
    void premisasInsuficientesParaEsquemaBinario() {
        Argument argument = argumento(FormalScheme.SILOGISMO_CATEGORICO);
        argument.addPremise(new Premise(0, "Todo hombre es mortal.", false, PremiseType.AXIOMATICA));
        argument.addPremise(new Premise(1, "Luego Socrates es mortal.", false, PremiseType.CONCLUSION));

        AuditReportDto report = auditor.audit(argument);

        assertThat(codigos(report)).contains("PREMISAS_INSUFICIENTES");
        assertThat(report.soundStatus()).isEqualTo(SoundStatus.PENDIENTE);
    }

    @Test
    @DisplayName("Dos conclusiones rompen la forma estandar")
    void conclusionMultiple() {
        Argument argument = argumento(FormalScheme.NO_CLASIFICADO);
        argument.addPremise(new Premise(0, "P.", false, PremiseType.EMPIRICA));
        argument.addPremise(new Premise(1, "Luego Q.", false, PremiseType.CONCLUSION));
        argument.addPremise(new Premise(2, "Luego R.", false, PremiseType.CONCLUSION));

        AuditReportDto report = auditor.audit(argument);

        assertThat(report.soundStatus()).isEqualTo(SoundStatus.FALAZ);
        assertThat(codigos(report)).contains("CONCLUSION_MULTIPLE");
    }

    // ----- utilidades -----------------------------------------------------

    private static Argument argumento(FormalScheme scheme) {
        Philosopher philosopher = new Philosopher("Autor de prueba", Epoch.MODERNA, null, null);
        Work work = new Work(philosopher, "Obra de prueba", 1600, null, null);
        return new Argument(work, null, "Argumento de prueba", scheme, null);
    }

    private static java.util.List<String> codigos(AuditReportDto report) {
        return report.findings().stream().map(f -> f.code()).toList();
    }
}
