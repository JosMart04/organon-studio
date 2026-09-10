package studio.organon.server.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import studio.organon.server.domain.logic.PremiseType;

/**
 * Premisa entrante. `id` viaja solo en reordenamientos y ediciones: si llega
 * null la premisa es nueva. El orden lo fija la posicion en la lista, no un
 * campo, para que el drag-and-drop del cliente sea la unica fuente de verdad.
 */
public record PremiseRequest(
        Long id,
        @NotBlank String statement,
        boolean enthymeme,
        @NotNull PremiseType premiseType) {
}
