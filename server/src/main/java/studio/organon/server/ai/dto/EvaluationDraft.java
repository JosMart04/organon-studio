package studio.organon.server.ai.dto;

/** La valoracion tal como la devuelve el modelo: la valoracion llega como texto libre y se normaliza despues. */
public record EvaluationDraft(String rating, String whatWorked, String whatToImprove, String followUpQuestion) {
}
