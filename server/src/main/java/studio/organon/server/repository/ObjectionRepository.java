package studio.organon.server.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import studio.organon.server.domain.logic.Objection;

public interface ObjectionRepository extends JpaRepository<Objection, Long> {

    List<Objection> findByPremiseIdOrderByIdAsc(Long premiseId);

    List<Objection> findByPremiseArgumentIdOrderByIdAsc(Long argumentId);
}
