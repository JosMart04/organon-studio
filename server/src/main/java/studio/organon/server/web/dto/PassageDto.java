package studio.organon.server.web.dto;

public record PassageDto(
        Long id,
        Long workId,
        String workTitle,
        String locator,
        String textContent,
        Integer pageNumber,
        String personalNotes) {
}
