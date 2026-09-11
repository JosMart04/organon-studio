package studio.organon.server.backup;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/backup")
public class BackupController {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm");

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    /** El cuaderno entero en un fichero, con nombre fechado para no pisar copias anteriores. */
    @GetMapping("/export")
    public ResponseEntity<BackupDocument> export() {
        String filename = "organon-copia-" + LocalDateTime.now().format(FILE_STAMP) + ".json";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(backupService.export());
    }

    /** Todo o nada. MERGE por defecto, porque es la opcion que no puede borrar nada. */
    @PostMapping("/import")
    public ImportReport importBackup(@RequestParam(defaultValue = "MERGE") ImportMode mode,
                                     @RequestBody BackupDocument document) {
        return backupService.importBackup(document, mode);
    }
}
