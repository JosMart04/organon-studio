package studio.organon.server.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import studio.organon.server.domain.logic.Argument;

public interface ArgumentRepository extends JpaRepository<Argument, Long> {

    @EntityGraph(attributePaths = {"work", "work.philosopher", "passage", "premises", "premises.objections"})
    Optional<Argument> findFullById(Long id);

    @EntityGraph(attributePaths = {"work", "work.philosopher", "passage", "premises"})
    List<Argument> findByWorkIdOrderByNameAsc(Long workId);

    @EntityGraph(attributePaths = {"work", "work.philosopher", "passage", "premises"})
    List<Argument> findByPassageIdOrderByNameAsc(Long passageId);

    @EntityGraph(attributePaths = {"work", "work.philosopher", "passage", "premises"})
    List<Argument> findAllByOrderByIdAsc();
}
