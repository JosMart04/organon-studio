package studio.organon.server.domain.dialectic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import studio.organon.server.domain.BaseEntity;
import studio.organon.server.domain.logic.Argument;

/**
 * Arista dirigida del grafo dialectico: el argumento fuente hace algo al
 * argumento destino. La direccion importa — que Hume refute a Descartes no
 * implica lo reciproco.
 */
@Entity
@Table(
        name = "dialectical_relation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_dialectical_relation_triple",
                columnNames = {"source_argument_id", "target_argument_id", "relation_type"})
)
public class DialecticalRelation extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_argument_id", nullable = false)
    private Argument sourceArgument;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_argument_id", nullable = false)
    private Argument targetArgument;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 20)
    private RelationType relationType;

    @Column(columnDefinition = "text")
    private String description;

    protected DialecticalRelation() {
    }

    public DialecticalRelation(Argument sourceArgument, Argument targetArgument,
                               RelationType relationType, String description) {
        this.sourceArgument = sourceArgument;
        this.targetArgument = targetArgument;
        this.relationType = relationType;
        this.description = description;
    }

    public Argument getSourceArgument() {
        return sourceArgument;
    }

    public void setSourceArgument(Argument sourceArgument) {
        this.sourceArgument = sourceArgument;
    }

    public Argument getTargetArgument() {
        return targetArgument;
    }

    public void setTargetArgument(Argument targetArgument) {
        this.targetArgument = targetArgument;
    }

    public RelationType getRelationType() {
        return relationType;
    }

    public void setRelationType(RelationType relationType) {
        this.relationType = relationType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
