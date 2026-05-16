package com.ethicssra.repository;

import com.ethicssra.domain.Literature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LiteratureRepository extends JpaRepository<Literature, Long> {

    long countByCreatedBy(Long createdBy);

    @Query("SELECT l FROM Literature l WHERE " +
            "LOWER(COALESCE(l.title,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(l.author,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(l.keywords,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(l.abstractText,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(l.doi,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(l.literatureType,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(l.researchMethod,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(l.applicableTopic,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(l.keyFindings,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(l.evidenceValue,'')) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Literature> search(@Param("q") String q);

    @Query("SELECT COUNT(l) FROM Literature l, ProjectLiterature pl WHERE l.id = pl.literature.id AND pl.project.id = :projectId")
    long countByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT l FROM Literature l, ProjectLiterature pl WHERE l.id = pl.literature.id AND pl.project.id = :projectId")
    List<Literature> findByProjectId(@Param("projectId") Long projectId);
}
