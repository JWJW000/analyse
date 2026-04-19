package com.ethicssra.repository;

import com.ethicssra.domain.EthicsModuleRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EthicsModuleRevisionRepository extends JpaRepository<EthicsModuleRevision, Long> {
    List<EthicsModuleRevision> findByModuleIdOrderByVersionDesc(Long moduleId);
}
