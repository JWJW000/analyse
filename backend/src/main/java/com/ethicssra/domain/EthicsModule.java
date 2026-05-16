package com.ethicssra.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "ethics_module")
public class EthicsModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String category;

    @Column(length = 512)
    private String keywords;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "case_text", columnDefinition = "TEXT")
    private String caseText;

    private String reference;

    @Column(name = "applicable_scenario", columnDefinition = "TEXT")
    private String applicableScenario;

    @Column(name = "teaching_objective", columnDefinition = "TEXT")
    private String teachingObjective;

    @Column(name = "value_point", columnDefinition = "TEXT")
    private String valuePoint;

    @Column(name = "discussion_questions", columnDefinition = "TEXT")
    private String discussionQuestions;

    @Column(name = "risk_points", columnDefinition = "TEXT")
    private String riskPoints;

    @Column(name = "integration_suggestion", columnDefinition = "TEXT")
    private String integrationSuggestion;

    @Column(name = "applicable_major")
    private String applicableMajor;

    @Column(name = "difficulty_level")
    private String difficultyLevel;

    @Column(name = "current_version", nullable = false)
    private Integer currentVersion = 1;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public EthicsModule() {
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCaseText() {
        return caseText;
    }

    public void setCaseText(String caseText) {
        this.caseText = caseText;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getApplicableScenario() {
        return applicableScenario;
    }

    public void setApplicableScenario(String applicableScenario) {
        this.applicableScenario = applicableScenario;
    }

    public String getTeachingObjective() {
        return teachingObjective;
    }

    public void setTeachingObjective(String teachingObjective) {
        this.teachingObjective = teachingObjective;
    }

    public String getValuePoint() {
        return valuePoint;
    }

    public void setValuePoint(String valuePoint) {
        this.valuePoint = valuePoint;
    }

    public String getDiscussionQuestions() {
        return discussionQuestions;
    }

    public void setDiscussionQuestions(String discussionQuestions) {
        this.discussionQuestions = discussionQuestions;
    }

    public String getRiskPoints() {
        return riskPoints;
    }

    public void setRiskPoints(String riskPoints) {
        this.riskPoints = riskPoints;
    }

    public String getIntegrationSuggestion() {
        return integrationSuggestion;
    }

    public void setIntegrationSuggestion(String integrationSuggestion) {
        this.integrationSuggestion = integrationSuggestion;
    }

    public String getApplicableMajor() {
        return applicableMajor;
    }

    public void setApplicableMajor(String applicableMajor) {
        this.applicableMajor = applicableMajor;
    }

    public String getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public Integer getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(Integer currentVersion) {
        this.currentVersion = currentVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
