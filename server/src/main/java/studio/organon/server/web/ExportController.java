package studio.organon.server.web;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import studio.organon.server.service.ExportService;

/**
 * Salidas del pipeline de exportacion. Ambas devuelven texto plano con
 * Content-Disposition para que el navegador ofrezca guardar el fichero.
 */
@RestController
@RequestMapping("/api/v1/export")
public class ExportController {

    private static final MediaType MARKDOWN = MediaType.parseMediaType("text/markdown; charset=UTF-8");
    private static final MediaType LATEX = MediaType.parseMediaType("text/x-tex; charset=UTF-8");

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    /** Obra convertida en notas atomicas Zettelkasten con wikilinks. */
    @GetMapping("/markdown/{workId}")
    public ResponseEntity<String> markdown(@PathVariable Long workId) {
        String body = exportService.toMarkdown(workId);
        return ResponseEntity.ok()
                .contentType(MARKDOWN)
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment("organon-obra-" + workId + ".md"))
                .body(body);
    }

    /** Argumento como bloque LaTeX listo para pegar en un articulo. */
    @GetMapping("/latex/{argumentId}")
    public ResponseEntity<String> latex(@PathVariable Long argumentId) {
        String body = exportService.toLatex(argumentId);
        return ResponseEntity.ok()
                .contentType(LATEX)
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment("organon-argumento-" + argumentId + ".tex"))
                .body(body);
    }

    private static String attachment(String filename) {
        return ContentDisposition.attachment().filename(filename).build().toString();
    }
}
