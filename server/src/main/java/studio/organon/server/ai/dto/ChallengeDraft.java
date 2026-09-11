package studio.organon.server.ai.dto;

/** El desafio tal como lo devuelve el modelo, antes de revisarlo. */
public record ChallengeDraft(String question, String counterexample, String hint) {
}
