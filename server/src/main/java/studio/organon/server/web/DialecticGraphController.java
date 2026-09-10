package studio.organon.server.web;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import studio.organon.server.domain.corpus.Epoch;
import studio.organon.server.service.DialecticService;
import studio.organon.server.web.dto.DialecticGraphDto;
import studio.organon.server.web.dto.DialecticalRelationDto;
import studio.organon.server.web.dto.DialecticalRelationRequest;

@RestController
@RequestMapping("/api/v1")
public class DialecticGraphController {

    private final DialecticService dialecticService;

    public DialecticGraphController(DialecticService dialecticService) {
        this.dialecticService = dialecticService;
    }

    /**
     * Nodos y aristas serializados para React Flow. Los filtros acotan el mapa:
     * `epoch` a un periodo historico, `conceptId` a los autores que discuten
     * ese concepto en particular.
     */
    @GetMapping("/dialectic-graph")
    public DialecticGraphDto graph(@RequestParam(required = false) Epoch epoch,
                                   @RequestParam(required = false) Long conceptId) {
        return dialecticService.buildGraph(epoch, conceptId);
    }

    @GetMapping("/dialectic-relations")
    public List<DialecticalRelationDto> listRelations() {
        return dialecticService.listRelations();
    }

    @PostMapping("/dialectic-relations")
    public DialecticalRelationDto createRelation(@Valid @RequestBody DialecticalRelationRequest request) {
        return dialecticService.createRelation(request);
    }

    @DeleteMapping("/dialectic-relations/{id}")
    public ResponseEntity<Void> deleteRelation(@PathVariable Long id) {
        dialecticService.deleteRelation(id);
        return ResponseEntity.noContent().build();
    }
}
