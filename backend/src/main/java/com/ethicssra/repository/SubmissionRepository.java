package com.ethicssra.repository;

import com.ethicssra.domain.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByAssignmentId(Long assignmentId);

    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    List<Submission> findByStudentId(Long studentId);

    Optional<Submission> findFirstByRequirementId(Long requirementId);
}
