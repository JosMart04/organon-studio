package studio.organon.server.domain.corpus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import studio.organon.server.domain.BaseEntity;

/**
 * Obra de un filosofo. `directAdversary` apunta a otra obra del corpus: es la
 * arista editorial que declara contra quien se escribe, previa a las relaciones
 * dialecticas finas entre argumentos concretos.
 */
@Entity
@Table(name = "work")
public class Work extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "philosopher_id", nullable = false)
    private Philosopher philosopher;

    @NotBlank
    @Size(max = 240)
    @Column(nullable = false, length = 240)
    private String title;

    @Column(name = "original_year")
    private Integer originalYear;

    @Column(name = "philosophical_problem", columnDefinition = "text")
    private String philosophicalProblem;

    @Column(name = "core_thesis", columnDefinition = "text")
    private String coreThesis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "direct_adversary_id")
    private Work directAdversary;

    protected Work() {
    }

    public Work(Philosopher philosopher, String title, Integer originalYear,
                String philosophicalProblem, String coreThesis) {
        this.philosopher = philosopher;
        this.title = title;
        this.originalYear = originalYear;
        this.philosophicalProblem = philosophicalProblem;
        this.coreThesis = coreThesis;
    }

    public Philosopher getPhilosopher() {
        return philosopher;
    }

    public void setPhilosopher(Philosopher philosopher) {
        this.philosopher = philosopher;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getOriginalYear() {
        return originalYear;
    }

    public void setOriginalYear(Integer originalYear) {
        this.originalYear = originalYear;
    }

    public String getPhilosophicalProblem() {
        return philosophicalProblem;
    }

    public void setPhilosophicalProblem(String philosophicalProblem) {
        this.philosophicalProblem = philosophicalProblem;
    }

    public String getCoreThesis() {
        return coreThesis;
    }

    public void setCoreThesis(String coreThesis) {
        this.coreThesis = coreThesis;
    }

    public Work getDirectAdversary() {
        return directAdversary;
    }

    public void setDirectAdversary(Work directAdversary) {
        this.directAdversary = directAdversary;
    }
}
