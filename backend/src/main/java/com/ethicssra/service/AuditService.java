package com.ethicssra.service;

import com.ethicssra.domain.AuditLog;
import com.ethicssra.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public void log(Long userId, String action, String entityType, Long entityId, Object detail) {
        String json = null;
        if (detail != null) {
            try {
                json = objectMapper.writeValueAsString(detail);
            } catch (JsonProcessingException ignored) {
                json = detail.toString();
            }
        }
        AuditLog a = new AuditLog();
        a.setUserId(userId);
        a.setAction(action);
        a.setEntityType(entityType);
        a.setEntityId(entityId);
        a.setDetailJson(json);
        auditLogRepository.save(a);
    }
}
