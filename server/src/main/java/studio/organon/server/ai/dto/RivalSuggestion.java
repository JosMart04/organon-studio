package studio.organon.server.ai.dto;

/**
 * Un pensador que discute la tesis.
 *
 * @param thinker      quien
 * @param work         donde lo dice, si se sabe
 * @param relation     REFUTA, PRESUPONE, EXTIENDE, RADICALIZA o MATIZA; se valida
 *                     contra el enum del dominio antes de ofrecer crear la conexion
 * @param explanation  en que consiste la disputa, en lenguaje llano
 * @param inNotebook   si ese pensador ya existe en el cuaderno del lector
 */
public record RivalSuggestion(
        String thinker,
        String work,
        String relation,
        String explanation,
        boolean inNotebook) {
}
