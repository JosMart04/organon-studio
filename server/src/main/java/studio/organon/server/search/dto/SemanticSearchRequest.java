package studio.organon.server.search.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Por POST y no por GET: la consulta es una frase, a veces larga, y no debe quedar en los logs de acceso. */
public record SemanticSearchRequest(@NotBlank @Size(max = 500) String query, Integer limit) {
}
