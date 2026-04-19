package com.ethicssra.repository;

import com.ethicssra.domain.MatchEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchEventRepository extends JpaRepository<MatchEvent, Long> {
    List<MatchEvent> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);
}
