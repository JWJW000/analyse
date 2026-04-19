package com.ethicssra.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "project_ethics_modules")
public class ProjectEthicsModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ethics_module_id", nullable = false)
    private EthicsModule ethicsModule;

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

    public EthicsModule getEthicsModule() { return ethicsModule; }
    public void setEthicsModule(EthicsModule ethicsModule) { this.ethicsModule = ethicsModule; }

    public Instant getAddedAt() { return addedAt; }
    public void setAddedAt(Instant addedAt) { this.addedAt = addedAt; }
}