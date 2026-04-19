package com.ethicssra.repository;

import com.ethicssra.domain.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByCourseIdOrderByCreatedAtDesc(Long courseId);
}
