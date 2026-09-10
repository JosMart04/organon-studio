package studio.organon.server.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import studio.organon.server.domain.semantics.TermDefinition;

public interface TermDefinitionRepository extends JpaRepository<TermDefinition, Long> {

    @EntityGraph(attributePaths = {"concept", "philosopher", "work"})
    List<TermDefinition> findByConceptIdOrderByPhilosopherNameAsc(Long conceptId);

    @EntityGraph(attributePaths = {"concept", "philosopher", "work"})
    List<TermDefinition> findByPhilosopherIdOrderByConceptTermAsc(Long philosopherId);

    /**
     * Candidatas para resolver un termino en un contexto de lectura. Devuelve
     * tanto la definicion especifica de la obra como la general del autor; la
     * eleccion entre ambas es del servicio, que prefiere siempre la mas
     * concreta.
     */
    @EntityGraph(attributePaths = {"concept", "philosopher", "work"})
    @Query("""
            SELECT d FROM TermDefinition d
            WHERE LOWER(d.concept.term) = LOWER(:term)
              AND d.philosopher.id = :philosopherId
            """)
    List<TermDefinition> findCandidates(@Param("term") String term,
                                        @Param("philosopherId") Long philosopherId);

    /** Definiciones que un autor fija dentro de una obra concreta, para el panel del lector. */
    @EntityGraph(attributePaths = {"concept", "philosopher", "work"})
    @Query("""
            SELECT d FROM TermDefinition d
            WHERE d.philosopher.id = :philosopherId
              AND (d.work.id = :workId OR d.work IS NULL)
            ORDER BY d.concept.term ASC
            """)
    List<TermDefinition> findForReadingContext(@Param("philosopherId") Long philosopherId,
                                               @Param("workId") Long workId);

    @EntityGraph(attributePaths = {"concept", "philosopher", "work"})
    List<TermDefinition> findAllByOrderByConceptTermAscPhilosopherNameAsc();
}
