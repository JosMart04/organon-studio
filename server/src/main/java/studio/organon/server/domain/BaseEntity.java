package studio.organon.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import java.util.Objects;

/**
 * Identidad y sellos temporales compartidos por todas las entidades del corpus.
 * La igualdad se define por identificador persistido: dos instancias sin id
 * (aun no guardadas) nunca son iguales entre si.
 */
@MappedSuperclass
@EntityListeners(NotebookChangeListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant ahora = Instant.now();
        // Lo que llega de una copia restaurada trae su fecha original; lo demas nace ahora.
        if (this.createdAt == null) {
            this.createdAt = ahora;
        }
        this.updatedAt = ahora;
    }

    /** Solo para restaurar copias, antes de guardar: conserva cuando se creo de verdad. */
    protected void restoreCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BaseEntity that) || !getClass().equals(other.getClass())) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass().getSimpleName());
    }
}
