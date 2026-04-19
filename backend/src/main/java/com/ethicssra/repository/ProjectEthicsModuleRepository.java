package com.ethicssra.repository;

import com.ethicssra.domain.ProjectEthicsModule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProjectEthicsModuleRepository extends JpaRepository<ProjectEthicsModule, Long> {
    void deleteByProjectIdAndEthicsModuleId(Long projectId, Long ethicsModuleId);
    
    Optional<ProjectEthicsModule> findByProjectIdAndEthicsModuleId(Long projectId, Long ethicsModuleId);
    
    boolean existsByProjectIdAndEthicsModuleId(Long projectId, Long ethicsModuleId);
}