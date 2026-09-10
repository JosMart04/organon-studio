package studio.organon.server.domain.semantics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import studio.organon.server.domain.BaseEntity;
import studio.organon.server.domain.corpus.Philosopher;
import studio.organon.server.domain.corpus.Work;

/**
 * Definicion operativa de un termino, delimitada por autor y — opcionalmente —
 * por obra. Es la pieza que realiza la "sobrecarga semantica": un mismo
 * {@link SemanticConcept} admite tantas definiciones incompatibles como autores
 * lo empleen, y el lector resuelve cual aplica segun el contexto de lectura.
 *
 * <p>Con `work` a null la definicion vale para todo el autor; con `work` fijado
 * gana precedencia sobre la general porque el autor precisa o desplaza el
 * sentido en esa obra concreta.
 */
@Entity
@Table(
        name = "term_definition",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_term_definition_scope",
                columnNames = {"concept_id", "philosopher_id", "work_id"})
)
public class TermDefinition extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concept_id", nullable = false)
    private SemanticConcept concept;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "philosopher_id", nullable = false)
    private Philosopher philosopher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_id")
    private Work work;

    @NotBlank
    @Column(name = "operational_definition", nullable = false, columnDefinition = "text")
    private String operationalDefinition;

    @Column(columnDefinition = "text")
    private String notes;

    protected TermDefinition() {
    }

    public TermDefinition(SemanticConcept concept, Philosopher philosopher, Work work,
                          String operationalDefinition, String notes) {
        this.concept = concept;
        this.philosopher = philosopher;
        this.work = work;
        this.operationalDefinition = operationalDefinition;
        this.notes = notes;
    }

    public SemanticConcept getConcept() {
        return concept;
    }

    public void setConcept(SemanticConcept concept) {
        this.concept = concept;
    }

    public Philosopher getPhilosopher() {
        return philosopher;
    }

    public void setPhilosopher(Philosopher philosopher) {
        this.philosopher = philosopher;
    }

    public Work getWork() {
        return work;
    }

    public void setWork(Work work) {
        this.work = work;
    }

    public String getOperationalDefinition() {
        return operationalDefinition;
    }

    public void setOperationalDefinition(String operationalDefinition) {
        this.operationalDefinition = operationalDefinition;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
