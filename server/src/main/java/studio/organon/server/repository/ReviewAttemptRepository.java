package studio.organon.server.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import studio.organon.server.domain.review.ReviewAttempt;

public interface ReviewAttemptRepository extends JpaRepository<ReviewAttempt, Long> {

    /** El mas reciente primero. */
    List<ReviewAttempt> findByArgumentIdOrderByIdDesc(Long argumentId);

    long countByArgumentId(Long argumentId);
}
