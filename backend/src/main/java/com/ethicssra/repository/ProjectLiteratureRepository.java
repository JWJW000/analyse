package com.ethicssra.repository;

import com.ethicssra.domain.ProjectLiterature;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProjectLiteratureRepository extends JpaRepository<ProjectLiterature, Long> {
    void deleteByProjectIdAndLiteratureId(Long projectId, Long literatureId);
    
    Optional<ProjectLiterature> findByProjectIdAndLiteratureId(Long projectId, Long literatureId);
    
    boolean existsByProjectIdAndLiteratureId(Long projectId, Long literatureId);
}