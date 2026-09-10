package studio.organon.server.web.dto;

import studio.organon.server.domain.corpus.Epoch;

public record PhilosopherDto(
        Long id,
        String name,
        Epoch epoch,
        String school,
        String biographicalSummary) {
}
