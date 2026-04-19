package com.ethicssra.repository;

import com.ethicssra.domain.Requirement;
import com.ethicssra.domain.RequirementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RequirementRepository extends JpaRepository<Requirement, Long> {
    List<Requirement> findByUserIdOrderByUpdatedAtDesc(Long userId);
    
    List<Requirement> findByUserId(Long userId);

    List<Requirement> findByCourseId(Long courseId);

    List<Requirement> findByAssignmentId(Long assignmentId);
    List<Requirement> findByAssignmentIdAndUserIdOrderByUpdatedAtDesc(Long assignmentId, Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, RequirementStatus status);

    @Query("SELECT r FROM Requirement r WHERE " +
            "LOWER(COALESCE(r.title,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(r.textContent,'')) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Requirement> searchByText(@Param("q") String q);
}
