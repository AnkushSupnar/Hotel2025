package com.frontend.service;

import com.frontend.entity.AuditLog;
import com.frontend.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for audit logging.
 * Records all critical operations on important entities for traceability.
 */
@Service
public class AuditLogService {

    private static final Logger LOG = LoggerFactory.getLogger(AuditLogService.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * Log an audit event asynchronously to avoid impacting operation performance.
     */
    @Async
    public void logAsync(String entityType, String entityId, String action, String details, String performedBy) {
        try {
            AuditLog log = new AuditLog(entityType, entityId, action, details, performedBy);
            auditLogRepository.save(log);
            LOG.debug("Audit log: {} {} {} by {}", action, entityType, entityId, performedBy);
        } catch (Exception e) {
            LOG.error("Failed to save audit log: {} {} {} - {}", action, entityType, entityId, e.getMessage());
        }
    }

    /**
     * Log an audit event with old/new values for change tracking.
     */
    @Async
    public void logWithValues(String entityType, String entityId, String action,
                              String details, String oldValues, String newValues, String performedBy) {
        try {
            AuditLog log = new AuditLog(entityType, entityId, action, details, performedBy);
            log.setOldValues(oldValues);
            log.setNewValues(newValues);
            auditLogRepository.save(log);
            LOG.debug("Audit log with values: {} {} {} by {}", action, entityType, entityId, performedBy);
        } catch (Exception e) {
            LOG.error("Failed to save audit log: {} {} {} - {}", action, entityType, entityId, e.getMessage());
        }
    }

    /**
     * Log a synchronous audit event (for critical operations where logging must succeed).
     */
    public void log(String entityType, String entityId, String action, String details, String performedBy) {
        try {
            AuditLog log = new AuditLog(entityType, entityId, action, details, performedBy);
            auditLogRepository.save(log);
        } catch (Exception e) {
            LOG.error("Failed to save audit log: {} {} {} - {}", action, entityType, entityId, e.getMessage());
        }
    }

    public List<AuditLog> getLogsForEntity(String entityType, String entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId);
    }

    public List<AuditLog> getLogsByEntityType(String entityType) {
        return auditLogRepository.findByEntityTypeOrderByTimestampDesc(entityType);
    }

    public List<AuditLog> getLogsByUser(String performedBy) {
        return auditLogRepository.findByPerformedByOrderByTimestampDesc(performedBy);
    }

    public List<AuditLog> getLogsBetween(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(start, end);
    }
}
