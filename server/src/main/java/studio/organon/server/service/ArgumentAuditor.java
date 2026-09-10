package studio.organon.server.service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import studio.organon.server.domain.logic.Argument;
import studio.organon.server.domain.logic.FormalScheme;
import studio.organon.server.domain.logic.Objection;
import studio.organon.server.domain.logic.ObjectionType;
import studio.organon.server.domain.logic.Premise;
import studio.organon.server.domain.logic.PremiseType;
import studio.organon.server.domain.logic.SoundStatus;
import studio.organon.server.web.dto.AuditFindingDto;
import studio.organon.server.web.dto.AuditReportDto;
import studio.organon.server.web.dto.AuditSeverity;

/**
 * Pruebas de estres sobre un argumento reconstruido.
 *
 * <p>El auditor no pretende decidir la verdad de las premisas: eso es trabajo
 * del investigador. Comprueba lo que si es mecanizable: que la reconstruccion
 * este completa, que el orden sea legible, que los supuestos implicitos hayan
 * sido examinados y que ninguna objecion registrada destruya la inferencia.
 */
@Component
public class ArgumentAuditor {

    /** Objeciones que no debilitan el argumento: lo invalidan. */
    private static final Set<ObjectionType> FATAL_OBJECTIONS = EnumSet.of(
            ObjectionType.FALACIA_FORMAL,
            ObjectionType.PETICION_DE_PRINCIPIO,
            ObjectionType.FALSA_DICOTOMIA);

    /** Esquemas deductivos que necesitan al menos dos premisas para cerrar. */
    private static final Set<FormalScheme> BINARY_SCHEMES = EnumSet.of(
            FormalScheme.MODUS_PONENS,
            FormalScheme.MODUS_TOLLENS,
            FormalScheme.SILOGISMO_CATEGORICO,
            FormalScheme.SILOGISMO_DISYUNTIVO,
            FormalScheme.SILOGISMO_HIPOTETICO);

    public AuditReportDto audit(Argument argument) {
        List<AuditFindingDto> findings = new ArrayList<>();

        List<Premise> ordered = argument.getPremises().stream()
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .toList();
        List<Premise> conclusions = ordered.stream()
                .filter(p -> p.getPremiseType() == PremiseType.CONCLUSION)
                .toList();
        List<Premise> support = ArgumentService.supportingPremises(argument);

        checkStructure(argument, ordered, conclusions, support, findings);
        checkOrdering(ordered, findings);
        checkEnthymemes(ordered, findings);
        int objectionCount = checkObjections(ordered, findings);

        long enthymemeCount = ordered.stream().filter(Premise::isEnthymeme).count();

        return new AuditReportDto(
                argument.getId(),
                argument.getName(),
                verdict(findings),
                ordered.size(),
                (int) enthymemeCount,
                objectionCount,
                findings);
    }

    private void checkStructure(Argument argument, List<Premise> ordered, List<Premise> conclusions,
                                List<Premise> support, List<AuditFindingDto> findings) {
        if (ordered.isEmpty()) {
            findings.add(new AuditFindingDto("SIN_PREMISAS", AuditSeverity.BLOQUEANTE,
                    "El argumento no tiene ninguna premisa: no hay nada que auditar todavia.", null));
            return;
        }
        if (conclusions.isEmpty()) {
            findings.add(new AuditFindingDto("SIN_CONCLUSION", AuditSeverity.BLOQUEANTE,
                    "Ninguna linea esta marcada como CONCLUSION: la reconstruccion no cierra.", null));
        }
        if (conclusions.size() > 1) {
            findings.add(new AuditFindingDto("CONCLUSION_MULTIPLE", AuditSeverity.BLOQUEANTE,
                    "Hay %d conclusiones. Un argumento en forma estandar sostiene una sola; el resto deberian ser inferencias intermedias."
                            .formatted(conclusions.size()),
                    conclusions.get(1).getId()));
        }
        if (support.isEmpty() && !conclusions.isEmpty()) {
            findings.add(new AuditFindingDto("CONCLUSION_SIN_APOYO", AuditSeverity.BLOQUEANTE,
                    "La conclusion no descansa sobre ninguna premisa: es una asercion, no un argumento.",
                    conclusions.get(0).getId()));
        }
        if (BINARY_SCHEMES.contains(argument.getFormalScheme()) && support.size() < 2) {
            findings.add(new AuditFindingDto("PREMISAS_INSUFICIENTES", AuditSeverity.ADVERTENCIA,
                    "El esquema %s exige al menos dos premisas y solo hay %d. Probablemente falte un entimema por explicitar."
                            .formatted(argument.getFormalScheme(), support.size()),
                    null));
        }
        if (argument.getFormalScheme() == FormalScheme.NO_CLASIFICADO) {
            findings.add(new AuditFindingDto("ESQUEMA_SIN_CLASIFICAR", AuditSeverity.INFORMATIVA,
                    "El argumento aun no tiene esquema formal asignado.", null));
        }
    }

    private void checkOrdering(List<Premise> ordered, List<AuditFindingDto> findings) {
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getOrderIndex() != i) {
                findings.add(new AuditFindingDto("ORDEN_NO_CONTIGUO", AuditSeverity.ADVERTENCIA,
                        "Los indices de orden tienen huecos. La numeracion deberia ir de 0 a %d sin saltos."
                                .formatted(ordered.size() - 1),
                        ordered.get(i).getId()));
                break;
            }
        }
        Premise last = ordered.isEmpty() ? null : ordered.get(ordered.size() - 1);
        boolean hasConclusion = ordered.stream()
                .anyMatch(p -> p.getPremiseType() == PremiseType.CONCLUSION);
        if (last != null && hasConclusion && last.getPremiseType() != PremiseType.CONCLUSION) {
            findings.add(new AuditFindingDto("CONCLUSION_FUERA_DE_ORDEN", AuditSeverity.ADVERTENCIA,
                    "La conclusion no ocupa la ultima posicion. En forma estandar cierra el argumento.",
                    last.getId()));
        }
    }

    private void checkEnthymemes(List<Premise> ordered, List<AuditFindingDto> findings) {
        for (Premise premise : ordered) {
            if (!premise.isEnthymeme()) {
                continue;
            }
            if (premise.getObjections().isEmpty()) {
                findings.add(new AuditFindingDto("ENTIMEMA_SIN_EXAMINAR", AuditSeverity.ADVERTENCIA,
                        "Supuesto implicito sin objecion registrada. Es el punto donde el argumento suele ceder: conviene atacarlo antes de darlo por solido.",
                        premise.getId()));
            } else {
                findings.add(new AuditFindingDto("ENTIMEMA_EXAMINADO", AuditSeverity.INFORMATIVA,
                        "Supuesto implicito ya sometido a objecion.", premise.getId()));
            }
        }
    }

    private int checkObjections(List<Premise> ordered, List<AuditFindingDto> findings) {
        int total = 0;
        for (Premise premise : ordered) {
            for (Objection objection : premise.getObjections()) {
                total++;
                boolean fatal = FATAL_OBJECTIONS.contains(objection.getObjectionType());
                String message = fatal
                        ? "Objecion de tipo %s: compromete la validez de la inferencia, no solo la plausibilidad de la premisa."
                                .formatted(objection.getObjectionType())
                        : "Objecion de tipo %s pendiente de respuesta."
                                .formatted(objection.getObjectionType());
                findings.add(new AuditFindingDto(
                        "OBJECION_" + objection.getObjectionType(),
                        fatal ? AuditSeverity.BLOQUEANTE : AuditSeverity.ADVERTENCIA,
                        message,
                        premise.getId()));
            }
        }
        return total;
    }

    /**
     * SOLIDO exige ausencia total de reparos. Basta una advertencia para
     * devolver el argumento a PENDIENTE: la auditoria no certifica verdades,
     * senala cuanto trabajo critico queda por hacer.
     */
    private SoundStatus verdict(List<AuditFindingDto> findings) {
        if (findings.stream().anyMatch(f -> f.severity() == AuditSeverity.BLOQUEANTE)) {
            return SoundStatus.FALAZ;
        }
        if (findings.stream().anyMatch(f -> f.severity() == AuditSeverity.ADVERTENCIA)) {
            return SoundStatus.PENDIENTE;
        }
        return SoundStatus.SOLIDO;
    }
}
