package studio.organon.server.review;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import studio.organon.server.review.dto.AnswerRequest;
import studio.organon.server.review.dto.ChallengeRequest;
import studio.organon.server.review.dto.ReviewAttemptDto;
import studio.organon.server.review.dto.ReviewCardDto;
import studio.organon.server.review.dto.ReviewProgressDto;

@RestController
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** La idea que toca repasar, o una concreta con {@code argumentId}. 204 si no queda ninguna. */
    @GetMapping("/review/next")
    public ResponseEntity<ReviewCardDto> next(@RequestParam(required = false) Long argumentId,
                                              @RequestParam(required = false) Long workId,
                                              @RequestParam(required = false) Long after) {
        return reviewService.next(argumentId, workId, after)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/review/history")
    public List<ReviewAttemptDto> history(@RequestParam Long argumentId) {
        return reviewService.history(argumentId);
    }

    @GetMapping("/review/progress")
    public ReviewProgressDto progress() {
        return reviewService.progress();
    }

    /** Genera el desafio y guarda el intento. Con el asistente apagado responde 503 con el motivo. */
    @PostMapping("/ai/challenge")
    public ReviewAttemptDto challenge(@RequestBody ChallengeRequest request) {
        return reviewService.challenge(request.argumentId(), request.workId());
    }

    @PostMapping("/ai/challenge/{attemptId}/answer")
    public ReviewAttemptDto answer(@PathVariable Long attemptId, @RequestBody AnswerRequest request) {
        return reviewService.answer(attemptId, request.answer());
    }
}
