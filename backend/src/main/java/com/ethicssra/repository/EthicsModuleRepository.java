package com.ethicssra.repository;

import com.ethicssra.domain.EthicsModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EthicsModuleRepository extends JpaRepository<EthicsModule, Long> {

    @Query("SELECT e FROM EthicsModule e WHERE " +
            "LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(e.category,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(e.keywords,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(e.description,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(e.caseText,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(e.reference,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(e.applicableScenario,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(e.teachingObjective,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(e.valuePoint,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(e.discussionQuestions,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(e.riskPoints,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(e.integrationSuggestion,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(e.applicableMajor,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(e.difficultyLevel,'')) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<EthicsModule> search(@Param("q") String q);

    @Query("SELECT COUNT(e) FROM EthicsModule e, ProjectEthicsModule pem WHERE e.id = pem.ethicsModule.id AND pem.project.id = :projectId")
    long countByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT e FROM EthicsModule e, ProjectEthicsModule pem WHERE e.id = pem.ethicsModule.id AND pem.project.id = :projectId")
    List<EthicsModule> findByProjectId(@Param("projectId") Long projectId);
}
