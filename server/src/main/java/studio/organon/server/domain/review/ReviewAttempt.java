package studio.organon.server.domain.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import studio.organon.server.domain.BaseEntity;
import studio.organon.server.domain.logic.Argument;

/**
 * Un intento de repaso: el desafio que el asistente planteo sobre una idea y,
 * si el lector llego a responder, su respuesta y la valoracion.
 */
@Entity
@Table(name = "review_attempt")
public class ReviewAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "argument_id", nullable = false)
    private Argument argument;

    @Enumerated(EnumType.STRING)
    @Column(name = "challenge_kind", nullable = false, length = 20)
    private ChallengeKind challengeKind;

    @Column(nullable = false, columnDefinition = "text")
    private String question;

    @Column(columnDefinition = "text")
    private String counterexample;

    @Column(columnDefinition = "text")
    private String hint;

    @Column(columnDefinition = "text")
    private String answer;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReviewRating rating;

    @Column(name = "what_worked", columnDefinition = "text")
    private String whatWorked;

    @Column(name = "what_to_improve", columnDefinition = "text")
    private String whatToImprove;

    @Column(name = "follow_up_question", columnDefinition = "text")
    private String followUpQuestion;

    @Column(name = "answered_at")
    private Instant answeredAt;

    protected ReviewAttempt() {
    }

    public ReviewAttempt(Argument argument, ChallengeKind challengeKind, String question,
                         String counterexample, String hint) {
        this.argument = argument;
        this.challengeKind = challengeKind;
        this.question = question;
        this.counterexample = counterexample;
        this.hint = hint;
    }

    public void recordAnswer(String answer, ReviewRating rating, String whatWorked, String whatToImprove,
                             String followUpQuestion, Instant answeredAt) {
        this.answer = answer;
        this.rating = rating;
        this.whatWorked = whatWorked;
        this.whatToImprove = whatToImprove;
        this.followUpQuestion = followUpQuestion;
        this.answeredAt = answeredAt;
    }

    /** Al restaurar una copia el intento conserva su fecha: de ella dependen el historial y el progreso. */
    public void restoreCreatedAt(Instant createdAt) {
        super.restoreCreatedAt(createdAt);
    }

    public boolean isAnswered() {
        return answeredAt != null;
    }

    public Argument getArgument() {
        return argument;
    }

    public ChallengeKind getChallengeKind() {
        return challengeKind;
    }

    public String getQuestion() {
        return question;
    }

    public String getCounterexample() {
        return counterexample;
    }

    public String getHint() {
        return hint;
    }

    public String getAnswer() {
        return answer;
    }

    public ReviewRating getRating() {
        return rating;
    }

    public String getWhatWorked() {
        return whatWorked;
    }

    public String getWhatToImprove() {
        return whatToImprove;
    }

    public String getFollowUpQuestion() {
        return followUpQuestion;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }
}
