package studio.organon.server.domain.corpus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import studio.organon.server.domain.BaseEntity;

@Entity
@Table(name = "philosopher")
public class Philosopher extends BaseEntity {

    @NotBlank
    @Size(max = 160)
    @Column(nullable = false, length = 160, unique = true)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Epoch epoch;

    @Size(max = 160)
    @Column(length = 160)
    private String school;

    @Column(name = "biographical_summary", columnDefinition = "text")
    private String biographicalSummary;

    protected Philosopher() {
    }

    public Philosopher(String name, Epoch epoch, String school, String biographicalSummary) {
        this.name = name;
        this.epoch = epoch;
        this.school = school;
        this.biographicalSummary = biographicalSummary;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Epoch getEpoch() {
        return epoch;
    }

    public void setEpoch(Epoch epoch) {
        this.epoch = epoch;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public String getBiographicalSummary() {
        return biographicalSummary;
    }

    public void setBiographicalSummary(String biographicalSummary) {
        this.biographicalSummary = biographicalSummary;
    }
}
