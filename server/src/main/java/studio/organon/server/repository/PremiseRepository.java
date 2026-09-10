package studio.organon.server.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import studio.organon.server.domain.logic.Premise;

public interface PremiseRepository extends JpaRepository<Premise, Long> {

    List<Premise> findByArgumentIdOrderByOrderIndexAsc(Long argumentId);

    @EntityGraph(attributePaths = {"argument", "objections"})
    Optional<Premise> findWithObjectionsById(Long id);
}
