package com.ethicssra.repository;

import com.ethicssra.domain.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {
    List<DocumentVersion> findByProjectIdAndRequirementIdOrderByVersionNumberDesc(Long projectId, Long requirementId);
    
    @Query("SELECT MAX(d.versionNumber) FROM DocumentVersion d WHERE d.projectId = :projectId AND d.requirementId = :requirementId")
    Optional<Integer> findMaxVersionNumber(@Param("projectId") Long projectId, @Param("requirementId") Long requirementId);
    
    Optional<DocumentVersion> findByProjectIdAndRequirementIdAndVersionNumber(Long projectId, Long requirementId, Integer versionNumber);
}