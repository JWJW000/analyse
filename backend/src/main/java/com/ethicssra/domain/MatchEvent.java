package com.ethicssra.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "match_event")
public class MatchEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "requirement_id")
    private Long requirementId;

    @Column(name = "top_module_ids_json", columnDefinition = "TEXT")
    private String topModuleIdsJson;

    @Column(name = "scores_json", columnDefinition = "TEXT")
    private String scoresJson;

    @Column(name = "created_at")
    private Instant createdAt;

    public MatchEvent() {
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRequirementId() {
        return requirementId;
    }

    public void setRequirementId(Long requirementId) {
        this.requirementId = requirementId;
    }

    public String getTopModuleIdsJson() {
        return topModuleIdsJson;
    }

    public void setTopModuleIdsJson(String topModuleIdsJson) {
        this.topModuleIdsJson = topModuleIdsJson;
    }

    public String getScoresJson() {
        return scoresJson;
    }

    public void setScoresJson(String scoresJson) {
        this.scoresJson = scoresJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
