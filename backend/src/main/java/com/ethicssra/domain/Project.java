package com.ethicssra.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_phase", length = 20)
    private ProjectPhase currentPhase = ProjectPhase.LITERATURE;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectLiterature> literatures = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectRequirement> requirements = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectEthicsModule> ethicsModules = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }

    public ProjectPhase getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(ProjectPhase currentPhase) { this.currentPhase = currentPhase; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<ProjectMember> getMembers() { return members; }
    public void setMembers(List<ProjectMember> members) { this.members = members; }

    public List<ProjectLiterature> getLiteratures() { return literatures; }
    public void setLiteratures(List<ProjectLiterature> literatures) { this.literatures = literatures; }

    public List<ProjectRequirement> getRequirements() { return requirements; }
    public void setRequirements(List<ProjectRequirement> requirements) { this.requirements = requirements; }

    public List<ProjectEthicsModule> getEthicsModules() { return ethicsModules; }
    public void setEthicsModules(List<ProjectEthicsModule> ethicsModules) { this.ethicsModules = ethicsModules; }

    public void addMember(ProjectMember member) {
        members.add(member);
        member.setProject(this);
    }

    public void addLiterature(ProjectLiterature literature) {
        literatures.add(literature);
        literature.setProject(this);
    }

    public void addRequirement(ProjectRequirement requirement) {
        requirements.add(requirement);
        requirement.setProject(this);
    }

    public void addEthicsModule(ProjectEthicsModule ethicsModule) {
        ethicsModules.add(ethicsModule);
        ethicsModule.setProject(this);
    }
}