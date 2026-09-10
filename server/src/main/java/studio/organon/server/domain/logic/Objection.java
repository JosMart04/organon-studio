package studio.organon.server.domain.logic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import studio.organon.server.domain.BaseEntity;

/**
 * Objecion anclada a una premisa concreta, no al argumento entero: la critica
 * util senala el eslabon exacto que cede.
 */
@Entity
@Table(name = "objection")
public class Objection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "premise_id", nullable = false)
    private Premise premise;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "objection_type", nullable = false, length = 40)
    private ObjectionType objectionType;

    @NotBlank
    @Column(nullable = false, columnDefinition = "text")
    private String explanation;

    protected Objection() {
    }

    public Objection(ObjectionType objectionType, String explanation) {
        this.objectionType = objectionType;
        this.explanation = explanation;
    }

    public Premise getPremise() {
        return premise;
    }

    public void setPremise(Premise premise) {
        this.premise = premise;
    }

    public ObjectionType getObjectionType() {
        return objectionType;
    }

    public void setObjectionType(ObjectionType objectionType) {
        this.objectionType = objectionType;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
