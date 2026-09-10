package studio.organon.server.web;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import studio.organon.server.domain.corpus.Epoch;
import studio.organon.server.service.CorpusService;
import studio.organon.server.web.dto.PassageDto;
import studio.organon.server.web.dto.PassageRequest;
import studio.organon.server.web.dto.PhilosopherDto;
import studio.organon.server.web.dto.PhilosopherRequest;
import studio.organon.server.web.dto.WorkDto;
import studio.organon.server.web.dto.WorkRequest;

@RestController
@RequestMapping("/api/v1/corpus")
public class CorpusController {

    private final CorpusService corpusService;

    public CorpusController(CorpusService corpusService) {
        this.corpusService = corpusService;
    }

    // ----- Filosofos ------------------------------------------------------

    @GetMapping("/philosophers")
    public List<PhilosopherDto> listPhilosophers(@RequestParam(required = false) Epoch epoch) {
        return corpusService.listPhilosophers(epoch);
    }

    @GetMapping("/philosophers/{id}")
    public PhilosopherDto getPhilosopher(@PathVariable Long id) {
        return corpusService.getPhilosopher(id);
    }

    @PostMapping("/philosophers")
    public ResponseEntity<PhilosopherDto> createPhilosopher(@Valid @RequestBody PhilosopherRequest request) {
        PhilosopherDto created = corpusService.createPhilosopher(request);
        return ResponseEntity.created(URI.create("/api/v1/corpus/philosophers/" + created.id())).body(created);
    }

    @PutMapping("/philosophers/{id}")
    public PhilosopherDto updatePhilosopher(@PathVariable Long id,
                                            @Valid @RequestBody PhilosopherRequest request) {
        return corpusService.updatePhilosopher(id, request);
    }

    @DeleteMapping("/philosophers/{id}")
    public ResponseEntity<Void> deletePhilosopher(@PathVariable Long id) {
        corpusService.deletePhilosopher(id);
        return ResponseEntity.noContent().build();
    }

    // ----- Obras ----------------------------------------------------------

    @GetMapping("/works")
    public List<WorkDto> listWorks(@RequestParam(required = false) Long philosopherId) {
        return corpusService.listWorks(philosopherId);
    }

    @GetMapping("/works/{id}")
    public WorkDto getWork(@PathVariable Long id) {
        return corpusService.getWork(id);
    }

    @GetMapping("/works/{id}/passages")
    public List<PassageDto> listWorkPassages(@PathVariable Long id) {
        return corpusService.listPassages(id);
    }

    @PostMapping("/works")
    public ResponseEntity<WorkDto> createWork(@Valid @RequestBody WorkRequest request) {
        WorkDto created = corpusService.createWork(request);
        return ResponseEntity.created(URI.create("/api/v1/corpus/works/" + created.id())).body(created);
    }

    @PutMapping("/works/{id}")
    public WorkDto updateWork(@PathVariable Long id, @Valid @RequestBody WorkRequest request) {
        return corpusService.updateWork(id, request);
    }

    @DeleteMapping("/works/{id}")
    public ResponseEntity<Void> deleteWork(@PathVariable Long id) {
        corpusService.deleteWork(id);
        return ResponseEntity.noContent().build();
    }

    // ----- Pasajes --------------------------------------------------------

    @GetMapping("/passages/{id}")
    public PassageDto getPassage(@PathVariable Long id) {
        return corpusService.getPassage(id);
    }

    @PostMapping("/passages")
    public ResponseEntity<PassageDto> createPassage(@Valid @RequestBody PassageRequest request) {
        PassageDto created = corpusService.createPassage(request);
        return ResponseEntity.created(URI.create("/api/v1/corpus/passages/" + created.id())).body(created);
    }

    @PutMapping("/passages/{id}")
    public PassageDto updatePassage(@PathVariable Long id, @Valid @RequestBody PassageRequest request) {
        return corpusService.updatePassage(id, request);
    }

    @DeleteMapping("/passages/{id}")
    public ResponseEntity<Void> deletePassage(@PathVariable Long id) {
        corpusService.deletePassage(id);
        return ResponseEntity.noContent().build();
    }
}
