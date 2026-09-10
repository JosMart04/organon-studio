package studio.organon.server.ai.dto;

import java.util.List;

/**
 * Lo que el panel del asistente necesita saber antes de ofrecer nada.
 *
 * @param available        si Ollama responde ahora mismo
 * @param configuredModel  el modelo que usaria esta instalacion
 * @param modelReady       si ese modelo concreto esta descargado
 * @param installedModels  lo que hay en la maquina, para poder sugerir alternativas
 * @param message          explicacion en lenguaje llano, lista para mostrar
 */
public record AiStatusDto(
        boolean available,
        String configuredModel,
        boolean modelReady,
        List<String> installedModels,
        String message) {
}
