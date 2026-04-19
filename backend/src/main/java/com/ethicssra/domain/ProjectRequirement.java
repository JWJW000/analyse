package com.ethicssra.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "project_requirements")
public class ProjectRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_id", nullable = false)
    private Requirement requirement;

    @Column(name = "added_at")
    private Instant addedAt;

    @PrePersist
    void prePersist() {
        if (addedAt == null) addedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public Requirement getRequirement() { return requirement; }
    public void setRequirement(Requirement requirement) { this.requirement = requirement; }

    public Instant getAddedAt() { return addedAt; }
    public void setAddedAt(Instant addedAt) { this.addedAt = addedAt; }
}