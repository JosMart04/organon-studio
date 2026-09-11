package studio.organon.server.ai.dto;

import studio.organon.server.domain.review.ChallengeKind;

public record Challenge(ChallengeKind kind, String question, String counterexample, String hint) {
}
