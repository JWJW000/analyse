package com.ethicssra.repository;

import com.ethicssra.domain.ProjectRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProjectRequirementRepository extends JpaRepository<ProjectRequirement, Long> {
    void deleteByProjectIdAndRequirementId(Long projectId, Long requirementId);

    Optional<ProjectRequirement> findByProjectIdAndRequirementId(Long projectId, Long requirementId);

    boolean existsByProjectIdAndRequirementId(Long projectId, Long requirementId);

    @Query("SELECT COUNT(pr) FROM ProjectRequirement pr WHERE pr.project.id = :projectId")
    long countByProjectId(@Param("projectId") Long projectId);
}