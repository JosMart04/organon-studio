package studio.organon.server.web;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import studio.organon.server.service.ArgumentService;
import studio.organon.server.web.dto.ArgumentDto;
import studio.organon.server.web.dto.ArgumentRequest;
import studio.organon.server.web.dto.AuditReportDto;
import studio.organon.server.web.dto.ObjectionDto;
import studio.organon.server.web.dto.ObjectionRequest;
import studio.organon.server.web.dto.PremiseDto;
import studio.organon.server.web.dto.PremiseRequest;

@RestController
@RequestMapping("/api/v1")
public class ArgumentController {

    private final ArgumentService argumentService;

    public ArgumentController(ArgumentService argumentService) {
        this.argumentService = argumentService;
    }

    // ----- Argumentos -----------------------------------------------------

    @GetMapping("/arguments")
    public List<ArgumentDto> listArguments(@RequestParam(required = false) Long workId,
                                           @RequestParam(required = false) Long passageId) {
        return argumentService.listArguments(workId, passageId);
    }

    @GetMapping("/arguments/{id}")
    public ArgumentDto getArgument(@PathVariable Long id) {
        return argumentService.getArgument(id);
    }

    @PostMapping("/arguments")
    public ResponseEntity<ArgumentDto> createArgument(@Valid @RequestBody ArgumentRequest request) {
        ArgumentDto created = argumentService.createArgument(request);
        return ResponseEntity.created(URI.create("/api/v1/arguments/" + created.id())).body(created);
    }

    @PutMapping("/arguments/{id}")
    public ArgumentDto updateArgument(@PathVariable Long id, @Valid @RequestBody ArgumentRequest request) {
        return argumentService.updateArgument(id, request);
    }

    @DeleteMapping("/arguments/{id}")
    public ResponseEntity<Void> deleteArgument(@PathVariable Long id) {
        argumentService.deleteArgument(id);
        return ResponseEntity.noContent().build();
    }

    // ----- Premisas -------------------------------------------------------

    /** Recibe el resultado del drag-and-drop: la lista completa en su orden final. */
    @PutMapping("/arguments/{id}/premises")
    public List<PremiseDto> reorderPremises(@PathVariable Long id,
                                            @Valid @RequestBody List<PremiseRequest> premises) {
        return argumentService.reorderPremises(id, premises);
    }

    @PatchMapping("/premises/{id}/enthymeme")
    public PremiseDto toggleEnthymeme(@PathVariable Long id) {
        return argumentService.toggleEnthymeme(id);
    }

    // ----- Objeciones -----------------------------------------------------

    @GetMapping("/premises/{id}/objections")
    public List<ObjectionDto> listObjections(@PathVariable Long id) {
        return argumentService.listObjections(id);
    }

    @PostMapping("/premises/{id}/objections")
    public ObjectionDto addObjection(@PathVariable Long id, @Valid @RequestBody ObjectionRequest request) {
        return argumentService.addObjection(id, request);
    }

    @GetMapping("/arguments/{id}/objections")
    public List<ObjectionDto> listArgumentObjections(@PathVariable Long id) {
        return argumentService.listObjectionsForArgument(id);
    }

    @DeleteMapping("/objections/{id}")
    public ResponseEntity<Void> deleteObjection(@PathVariable Long id) {
        argumentService.deleteObjection(id);
        return ResponseEntity.noContent().build();
    }

    // ----- Auditoria ------------------------------------------------------

    /** Somete el argumento a las pruebas de estres y persiste el veredicto. */
    @PostMapping("/arguments/{id}/audit")
    public AuditReportDto audit(@PathVariable Long id) {
        return argumentService.audit(id);
    }

    /** Misma auditoria sin escribir el resultado, para previsualizar en el constructor. */
    @GetMapping("/arguments/{id}/audit")
    public AuditReportDto previewAudit(@PathVariable Long id) {
        return argumentService.dryRunAudit(id);
    }
}
