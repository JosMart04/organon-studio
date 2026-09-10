package studio.organon.server.domain.corpus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import studio.organon.server.domain.BaseEntity;

/**
 * Fragmento citable de una obra. `locator` guarda la referencia canonica que
 * usa la tradicion academica ("Metafisica 980a", "AT VII 40", "KrV B 4"), no un
 * numero de pagina de una edicion concreta.
 */
@Entity
@Table(name = "passage")
public class Passage extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_id", nullable = false)
    private Work work;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String locator;

    @NotBlank
    @Column(name = "text_content", nullable = false, columnDefinition = "text")
    private String textContent;

    @PositiveOrZero
    @Column(name = "page_number")
    private Integer pageNumber;

    protected Passage() {
    }

    public Passage(Work work, String locator, String textContent, Integer pageNumber) {
        this.work = work;
        this.locator = locator;
        this.textContent = textContent;
        this.pageNumber = pageNumber;
    }

    public Work getWork() {
        return work;
    }

    public void setWork(Work work) {
        this.work = work;
    }

    public String getLocator() {
        return locator;
    }

    public void setLocator(String locator) {
        this.locator = locator;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }
}
