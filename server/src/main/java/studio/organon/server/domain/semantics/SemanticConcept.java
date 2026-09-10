package studio.organon.server.domain.semantics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import studio.organon.server.domain.BaseEntity;

/**
 * Termino filosofico como etiqueta neutra ("Sustancia", "Idea", "Causa").
 * Deliberadamente no lleva definicion: el significado solo existe delimitado
 * por autor y obra, y vive en {@link TermDefinition}.
 */
@Entity
@Table(name = "semantic_concept")
public class SemanticConcept extends BaseEntity {

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120, unique = true)
    private String term;

    @Column(columnDefinition = "text")
    private String description;

    protected SemanticConcept() {
    }

    public SemanticConcept(String term, String description) {
        this.term = term;
        this.description = description;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
