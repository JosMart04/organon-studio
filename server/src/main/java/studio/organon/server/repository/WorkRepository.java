package studio.organon.server.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import studio.organon.server.domain.corpus.Work;

public interface WorkRepository extends JpaRepository<Work, Long> {

    @EntityGraph(attributePaths = {"philosopher", "directAdversary"})
    List<Work> findAllByOrderByOriginalYearAscTitleAsc();

    @EntityGraph(attributePaths = {"philosopher", "directAdversary"})
    List<Work> findByPhilosopherIdOrderByOriginalYearAsc(Long philosopherId);

    @EntityGraph(attributePaths = {"philosopher", "directAdversary"})
    Optional<Work> findWithPhilosopherById(Long id);

    Optional<Work> findByTitleIgnoreCase(String title);
}
