package studio.organon.server.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import studio.organon.server.domain.semantics.SemanticConcept;

public interface SemanticConceptRepository extends JpaRepository<SemanticConcept, Long> {

    Optional<SemanticConcept> findByTermIgnoreCase(String term);

    List<SemanticConcept> findAllByOrderByTermAsc();
}
