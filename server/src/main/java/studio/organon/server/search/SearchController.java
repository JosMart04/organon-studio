package studio.organon.server.search;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import studio.organon.server.search.dto.IndexStatusDto;
import studio.organon.server.search.dto.SearchResponseDto;
import studio.organon.server.search.dto.SearchResultDto;
import studio.organon.server.search.dto.SemanticSearchRequest;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;
    private final EmbeddingIndexer indexer;

    public SearchController(SearchService searchService, EmbeddingIndexer indexer) {
        this.searchService = searchService;
        this.indexer = indexer;
    }

    /** Por palabras. Una consulta vacia o sin letras devuelve una lista vacia, no un error: llega mientras se escribe. */
    @GetMapping("/text")
    public List<SearchResultDto> text(@RequestParam(defaultValue = "") String q,
                                      @RequestParam(required = false) Integer limit) {
        return searchService.text(q, limit);
    }

    @PostMapping("/semantic")
    public SearchResponseDto semantic(@Valid @RequestBody SemanticSearchRequest request) {
        return searchService.semantic(request.query(), request.limit());
    }

    @GetMapping("/status")
    public IndexStatusDto status() {
        return indexer.status();
    }

    /** Pide una pasada ahora. Responde enseguida: la indexacion sigue en segundo plano. */
    @PostMapping("/reindex")
    public ResponseEntity<IndexStatusDto> reindex() {
        indexer.requestReindex();
        return ResponseEntity.accepted().body(indexer.status());
    }
}
