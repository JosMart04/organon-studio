package studio.organon.server.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import studio.organon.server.domain.dialectic.DialecticalRelation;

public interface DialecticalRelationRepository extends JpaRepository<DialecticalRelation, Long> {

    @EntityGraph(attributePaths = {"sourceArgument", "targetArgument"})
    List<DialecticalRelation> findAllByOrderByIdAsc();

    List<DialecticalRelation> findBySourceArgumentIdOrTargetArgumentId(Long sourceId, Long targetId);
}
