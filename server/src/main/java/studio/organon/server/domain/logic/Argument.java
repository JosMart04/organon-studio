package studio.organon.server.domain.logic;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import studio.organon.server.domain.BaseEntity;
import studio.organon.server.domain.corpus.Passage;
import studio.organon.server.domain.corpus.Work;

/**
 * Argumento reconstruido en forma estandar. Es la unidad de analisis del
 * sistema: las premisas le pertenecen (ciclo de vida compartido) y las
 * relaciones dialecticas lo toman como nodo.
 */
@Entity
@Table(name = "argument")
public class Argument extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_id", nullable = false)
    private Work work;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passage_id")
    private Passage passage;

    @NotBlank
    @Size(max = 240)
    @Column(nullable = false, length = 240)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "formal_scheme", nullable = false, length = 40)
    private FormalScheme formalScheme = FormalScheme.NO_CLASIFICADO;

    @Column(name = "latex_formalization", columnDefinition = "text")
    private String latexFormalization;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "sound_status", nullable = false, length = 20)
    private SoundStatus soundStatus = SoundStatus.PENDIENTE;

    @OneToMany(mappedBy = "argument", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<Premise> premises = new ArrayList<>();

    protected Argument() {
    }

    public Argument(Work work, Passage passage, String name, FormalScheme formalScheme,
                    String latexFormalization) {
        this.work = work;
        this.passage = passage;
        this.name = name;
        this.formalScheme = formalScheme;
        this.latexFormalization = latexFormalization;
    }

    public void addPremise(Premise premise) {
        premises.add(premise);
        premise.setArgument(this);
    }

    public void removePremise(Premise premise) {
        premises.remove(premise);
        premise.setArgument(null);
    }

    public Work getWork() {
        return work;
    }

    public void setWork(Work work) {
        this.work = work;
    }

    public Passage getPassage() {
        return passage;
    }

    public void setPassage(Passage passage) {
        this.passage = passage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FormalScheme getFormalScheme() {
        return formalScheme;
    }

    public void setFormalScheme(FormalScheme formalScheme) {
        this.formalScheme = formalScheme;
    }

    public String getLatexFormalization() {
        return latexFormalization;
    }

    public void setLatexFormalization(String latexFormalization) {
        this.latexFormalization = latexFormalization;
    }

    public SoundStatus getSoundStatus() {
        return soundStatus;
    }

    public void setSoundStatus(SoundStatus soundStatus) {
        this.soundStatus = soundStatus;
    }

    public List<Premise> getPremises() {
        return premises;
    }

    public void setPremises(List<Premise> premises) {
        this.premises = premises;
    }
}
