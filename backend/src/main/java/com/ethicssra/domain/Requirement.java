package com.ethicssra.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "requirement")
public class Requirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private String title;

    @Column(name = "text_content", columnDefinition = "LONGTEXT")
    private String textContent;

    @Column(name = "embedded_modules", columnDefinition = "TEXT")
    private String embeddedModules;

    @Column(name = "matching_score")
    private Double matchingScore;

    @Column(name = "diagram_json", columnDefinition = "LONGTEXT")
    private String diagramJson;

    @Column(name = "spec_wizard_json", columnDefinition = "LONGTEXT")
    private String specWizardJson;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "assignment_id")
    private Long assignmentId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 32)
    private RequirementStatus status = RequirementStatus.DRAFT;

    @Column(name = "teacher_comment", columnDefinition = "TEXT")
    private String teacherComment;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Requirement() {
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public String getEmbeddedModules() {
        return embeddedModules;
    }

    public void setEmbeddedModules(String embeddedModules) {
        this.embeddedModules = embeddedModules;
    }

    public Double getMatchingScore() {
        return matchingScore;
    }

    public void setMatchingScore(Double matchingScore) {
        this.matchingScore = matchingScore;
    }

    public String getDiagramJson() {
        return diagramJson;
    }

    public void setDiagramJson(String diagramJson) {
        this.diagramJson = diagramJson;
    }

    public String getSpecWizardJson() {
        return specWizardJson;
    }

    public void setSpecWizardJson(String specWizardJson) {
        this.specWizardJson = specWizardJson;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public RequirementStatus getStatus() {
        return status;
    }

    public void setStatus(RequirementStatus status) {
        this.status = status;
    }

    public String getTeacherComment() {
        return teacherComment;
    }

    public void setTeacherComment(String teacherComment) {
        this.teacherComment = teacherComment;
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
