package studio.organon.server.ai.dto;

import java.util.List;

/**
 * Lo que el asistente necesita saber de una idea para desafiar al lector. Solo
 * texto, sin entidades: la llamada al modelo ocurre fuera de cualquier
 * transaccion.
 *
 * @param reasons         las razones en orden, con los supuestos implicitos senalados
 * @param focusAssumption el supuesto sobre el que preguntar, o null si la idea no tiene
 */
public record ReviewSubject(
        String name,
        String author,
        String workTitle,
        List<String> reasons,
        String conclusion,
        String focusAssumption) {
}
