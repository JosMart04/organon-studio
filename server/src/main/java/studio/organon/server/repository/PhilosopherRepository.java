package studio.organon.server.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import studio.organon.server.domain.corpus.Epoch;
import studio.organon.server.domain.corpus.Philosopher;

public interface PhilosopherRepository extends JpaRepository<Philosopher, Long> {

    Optional<Philosopher> findByNameIgnoreCase(String name);

    List<Philosopher> findByEpochOrderByNameAsc(Epoch epoch);

    List<Philosopher> findAllByOrderByNameAsc();
}
