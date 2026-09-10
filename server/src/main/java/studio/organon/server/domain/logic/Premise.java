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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.ArrayList;
import java.util.List;
import studio.organon.server.domain.BaseEntity;

/**
 * Enunciado individual de la reconstruccion.
 *
 * <p>El indicador de entimema marca el supuesto implicito: la premisa que el
 * autor no escribio pero que su inferencia necesita para cerrar. Explicitarla
 * es el gesto critico central de la plataforma, porque casi siempre es ahi
 * donde el argumento resulta atacable.
 */
@Entity
@Table(
        name = "premise",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_premise_order",
                columnNames = {"argument_id", "order_index"})
)
public class Premise extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "argument_id", nullable = false)
    private Argument argument;

    @PositiveOrZero
    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @NotBlank
    @Column(nullable = false, columnDefinition = "text")
    private String statement;

    @Column(name = "is_enthymeme", nullable = false)
    private boolean enthymeme = false;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "premise_type", nullable = false, length = 30)
    private PremiseType premiseType = PremiseType.EMPIRICA;

    @OneToMany(mappedBy = "premise", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Objection> objections = new ArrayList<>();

    protected Premise() {
    }

    public Premise(int orderIndex, String statement, boolean enthymeme, PremiseType premiseType) {
        this.orderIndex = orderIndex;
        this.statement = statement;
        this.enthymeme = enthymeme;
        this.premiseType = premiseType;
    }

    public void addObjection(Objection objection) {
        objections.add(objection);
        objection.setPremise(this);
    }

    public Argument getArgument() {
        return argument;
    }

    public void setArgument(Argument argument) {
        this.argument = argument;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public boolean isEnthymeme() {
        return enthymeme;
    }

    public void setEnthymeme(boolean enthymeme) {
        this.enthymeme = enthymeme;
    }

    public PremiseType getPremiseType() {
        return premiseType;
    }

    public void setPremiseType(PremiseType premiseType) {
        this.premiseType = premiseType;
    }

    public List<Objection> getObjections() {
        return objections;
    }

    public void setObjections(List<Objection> objections) {
        this.objections = objections;
    }
}
