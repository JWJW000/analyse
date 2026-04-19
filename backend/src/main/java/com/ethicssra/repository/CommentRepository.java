package com.ethicssra.repository;

import com.ethicssra.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByProjectIdAndRequirementIdOrderByCreatedAtDesc(Long projectId, Long requirementId);
    
    List<Comment> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    @Query("SELECT c FROM Comment c WHERE c.projectId = :projectId ORDER BY c.createdAt DESC")
    List<Comment> findByProjectOrderByCreatedAtDesc(@Param("projectId") Long projectId);
    
    @Query("SELECT c FROM Comment c WHERE c.projectId = :projectId AND c.requirementId = :requirementId ORDER BY c.createdAt DESC")
    List<Comment> findByProjectAndRequirement(@Param("projectId") Long projectId, @Param("requirementId") Long requirementId);
}