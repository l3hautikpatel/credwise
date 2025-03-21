package com.team1_5.credwise.service;

import com.team1_5.credwise.config.ApplicationConfig;
import com.team1_5.credwise.model.AuditLog;
import com.team1_5.credwise.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing audit logs across the application
 *
 * Provides comprehensive logging of critical events and actions
 *
 * @author Credwise Development Team
 * @version 1.0.0
 */
@Service
public class AuditLogService {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;
    private final ApplicationConfig applicationConfig;
    private final ObjectMapper objectMapper;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            ApplicationConfig applicationConfig,
            ObjectMapper objectMapper
    ) {
        this.auditLogRepository = auditLogRepository;
        this.applicationConfig = applicationConfig;
        this.objectMapper = objectMapper;
    }

    /**
     * Enumerate audit log event types
     */
    public enum AuditEventType {
        USER_REGISTRATION,
        LOGIN,
        LOGOUT,
        LOAN_APPLICATION_SUBMITTED,
        LOAN_APPLICATION_UPDATED,
        LOAN_APPLICATION_APPROVED,
        LOAN_APPLICATION_REJECTED,
        CREDIT_SCORE_CHECK,
        ML_MODEL_PREDICTION,
        SYSTEM_ERROR
    }

    /**
     * Create an audit log entry
     *
     * @param eventType Type of event
     * @param description Event description
     * @param details Additional event details
     */
    @Async
    @Transactional
    public void logEvent(
            AuditEventType eventType,
            String description,
            Map<String, Object> details
    ) {
        try {
            // Skip logging if audit is disabled
            if (!applicationConfig.isAuditLogEnabled()) {
                return;
            }

            // Create audit log entry
            AuditLog auditLog = new AuditLog();
            auditLog.setEventType(eventType.name());
            auditLog.setDescription(description);
            auditLog.setTimestamp(LocalDateTime.now());

            // Set user context
            setUserContext(auditLog);

            // Set event details
            setEventDetails(auditLog, details);

            // Save audit log
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            logger.error("Error creating audit log", e);
        }
    }

    /**
     * Set user context for audit log
     *
     * @param auditLog Audit log entry to update
     */
    private void setUserContext(AuditLog auditLog) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null) {
                auditLog.setUsername(authentication.getName());

                // Additional user details can be extracted here
                // For example, user ID, roles, etc.
            }
        } catch (Exception e) {
            logger.warn("Could not set user context for audit log", e);
            auditLog.setUsername("SYSTEM");
        }
    }

    /**
     * Set event details for audit log
     *
     * @param auditLog Audit log entry to update
     * @param details Event details
     */
    private void setEventDetails(AuditLog auditLog, Map<String, Object> details) {
        try {
            if (details != null && !details.isEmpty()) {
                // Convert details to JSON string
                auditLog.setDetails(objectMapper.writeValueAsString(details));
            }
        } catch (Exception e) {
            logger.error("Error converting audit log details to JSON", e);
        }
    }

    /**
     * Convenience method for logging loan application events
     *
     * @param applicationId Loan application identifier
     * @param eventType Event type
     * @param description Event description
     */
    public void logLoanApplicationEvent(
            String applicationId,
            AuditEventType eventType,
            String description
    ) {
        Map<String, Object> details = new HashMap<>();
        details.put("applicationId", applicationId);

        logEvent(eventType, description, details);
    }

    /**
     * Retrieve audit log by ID
     *
     * @param id Audit log identifier
     * @return Optional containing audit log
     */
    public Optional<AuditLog> getAuditLogById(Long id) {
        return auditLogRepository.findById(id);
    }

    /**
     * Clean up old audit logs
     * Scheduled task to remove audit logs older than configured retention period
     */
    @Scheduled(cron = "0 0 1 * * ?") // Run daily at 1 AM
    @Transactional
    public void cleanupOldAuditLogs() {
        if (!applicationConfig.isAuditLogEnabled()) {
            return;
        }

        try {
            LocalDateTime retentionDate = LocalDateTime.now()
                    .minusDays(applicationConfig.getAuditLogRetentionDays());

            int deletedLogs = auditLogRepository.deleteByTimestampBefore(retentionDate);

            logger.info("Cleaned up {} old audit logs", deletedLogs);
        } catch (Exception e) {
            logger.error("Error cleaning up audit logs", e);
        }
    }

    /**
     * Log system error
     *
     * @param errorMessage Error message
     * @param exception Exception details
     */
    public void logSystemError(String errorMessage, Exception exception) {
        Map<String, Object> details = new HashMap<>();
        details.put("errorMessage", errorMessage);
        details.put("exceptionClass", exception.getClass().getName());
        details.put("exceptionMessage", exception.getMessage());

        logEvent(
                AuditEventType.SYSTEM_ERROR,
                "System error occurred",
                details
        );
    }
}