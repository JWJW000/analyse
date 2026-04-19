package com.ethicssra.dto;

import com.ethicssra.domain.*;
import java.time.Instant;
import java.util.List;

public record ProjectDto(
    Long id,
    String name,
    String description,
    Long courseId,
    Long ownerId,
    String ownerName,
    ProjectStatus status,
    ProjectPhase currentPhase,
    Instant createdAt,
    Instant updatedAt,
    List<ProjectMemberDto> members,
    Integer literatureCount,
    Integer requirementCount,
    Integer ethicsModuleCount,
    Double progress
) {
    public static ProjectDto from(Project p, String ownerName) {
        double progress = calculateProgress(p);
        return new ProjectDto(
            p.getId(),
            p.getName(),
            p.getDescription(),
            p.getCourseId(),
            p.getOwnerId(),
            ownerName,
            p.getStatus(),
            p.getCurrentPhase(),
            p.getCreatedAt(),
            p.getUpdatedAt(),
            p.getMembers().stream().map(ProjectMemberDto::from).toList(),
            p.getLiteratures().size(),
            p.getRequirements().size(),
            p.getEthicsModules().size(),
            progress
        );
    }

    private static double calculateProgress(Project p) {
        if (p.getCurrentPhase() == null) return 0.0;
        int currentIndex = p.getCurrentPhase().ordinal();
        int totalPhases = ProjectPhase.values().length;
        return (double) currentIndex / totalPhases * 100;
    }
}