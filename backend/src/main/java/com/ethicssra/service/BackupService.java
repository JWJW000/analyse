package com.ethicssra.service;

import com.ethicssra.domain.BackupRecord;
import com.ethicssra.repository.BackupRecordRepository;
import com.ethicssra.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BackupService {

    private final BackupRecordRepository backupRecordRepository;
    private final AuditService auditService;

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${app.backup-path}")
    private String backupPath;

    public BackupService(BackupRecordRepository backupRecordRepository, AuditService auditService) {
        this.backupRecordRepository = backupRecordRepository;
        this.auditService = auditService;
    }

    public List<BackupRecord> list() {
        return backupRecordRepository.findAllByOrderByCreatedAtDesc();
    }

    public BackupRecord runMysqlDump() throws Exception {
        Files.createDirectories(Path.of(backupPath));
        String ts = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault()).format(Instant.now());
        String fileName = "ethics_sra_" + ts + ".sql";
        Path outFile = Path.of(backupPath, fileName);

        String host = "localhost";
        String port = "3306";
        String db = "ethics_sra";
        if (jdbcUrl.contains("//")) {
            String rest = jdbcUrl.substring(jdbcUrl.indexOf("//") + 2);
            int slash = rest.indexOf('/');
            if (slash > 0) {
                String hp = rest.substring(0, slash);
                String[] hpa = hp.split(":");
                host = hpa[0];
                if (hpa.length > 1) {
                    port = hpa[1];
                }
                String q = rest.substring(slash + 1);
                int qm = q.indexOf('?');
                db = qm > 0 ? q.substring(0, qm) : q;
            }
        }

        ProcessBuilder pb = new ProcessBuilder(
                "mysqldump",
                "-h", host,
                "-P", port,
                "-u", dbUser,
                "-p" + dbPassword,
                "--single-transaction",
                db
        );
        pb.redirectOutput(outFile.toFile());
        pb.redirectError(ProcessBuilder.Redirect.PIPE);
        Process p = pb.start();
        int code = p.waitFor();
        BackupRecord rec = new BackupRecord();
        rec.setFilePath(outFile.toAbsolutePath().toString());
        rec.setSizeBytes(Files.exists(outFile) ? Files.size(outFile) : 0L);
        rec.setStatus(code == 0 ? "COMPLETED" : "FAILED");
        rec.setCreatedBy(SecurityUtils.currentUserId());
        rec = backupRecordRepository.save(rec);
        auditService.log(SecurityUtils.currentUserId(), "BACKUP_RUN", "BackupRecord", rec.getId(), Map.of("code", code));
        return rec;
    }

    public List<String> tailLog(String logFile, int lines) throws Exception {
        Path p = Path.of(logFile);
        if (!Files.isRegularFile(p)) {
            return List.of("(日志文件不存在: " + logFile + ")");
        }
        List<String> all = Files.readAllLines(p);
        int from = Math.max(0, all.size() - lines);
        return new ArrayList<>(all.subList(from, all.size()));
    }
}
