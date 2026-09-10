package studio.organon.server.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import studio.organon.server.domain.corpus.Passage;

public interface PassageRepository extends JpaRepository<Passage, Long> {

    List<Passage> findByWorkIdOrderByLocatorAsc(Long workId);

    Optional<Passage> findByWorkIdAndLocator(Long workId, String locator);
}
