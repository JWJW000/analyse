package com.ethicssra.repository;

import com.ethicssra.domain.BackupRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BackupRecordRepository extends JpaRepository<BackupRecord, Long> {
    List<BackupRecord> findAllByOrderByCreatedAtDesc();
}
