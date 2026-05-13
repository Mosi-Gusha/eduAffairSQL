package com.student.management.service;

import java.util.List;
import java.util.Locale;

import com.student.management.common.TransactionAuditContext;
import com.student.management.mapper.TransactionAuditMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TransactionAuditService {
    private static final Logger log = LoggerFactory.getLogger(TransactionAuditService.class);
    private static final int COMPENSATION_MINUTES = 10;
    private static final int COMPENSATION_BATCH_SIZE = 100;

    private final TransactionAuditMapper mapper;
    private final PlatformTransactionManager transactionManager;

    public TransactionAuditService(TransactionAuditMapper mapper, PlatformTransactionManager transactionManager) {
        this.mapper = mapper;
        this.transactionManager = transactionManager;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void start(String transactionId, String businessType, Long actorUserId, String message) {
        mapper.insertTransaction(transactionId, businessType, actorUserId);
        mapper.insertEntry(transactionId, "START", null, null, "started", message);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void logStep(String operation, String tableName, Long recordId, String status, String message) {
        String transactionId = TransactionAuditContext.transactionId().orElse(null);
        if (transactionId == null) {
            return;
        }
        mapper.insertEntry(transactionId, normalizeOperation(operation), tableName, recordId, normalizeStatus(status), message);
        TransactionAuditContext.markEntryWritten();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void finishCommitted(String transactionId) {
        mapper.finishTransaction(transactionId, "committed");
        mapper.insertEntry(transactionId, "COMMIT", null, null, "success", "committed");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rollback(String transactionId, String tableName, Long recordId, String message) {
        mapper.finishTransaction(transactionId, "rolled_back");
        mapper.insertEntry(transactionId, "ROLLBACK", tableName, recordId, "rolled_back", message);
    }

    @Scheduled(initialDelayString = "${app.audit.compensation-initial-delay-ms:60000}",
            fixedDelayString = "${app.audit.compensation-delay-ms:300000}")
    public void compensateStaleStartedTransactions() {
        try {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            template.executeWithoutResult(status -> {
                List<String> transactionIds = mapper.staleStartedTransactionIds(COMPENSATION_MINUTES, COMPENSATION_BATCH_SIZE);
                for (String transactionId : transactionIds) {
                    int updated = mapper.finishStartedTransaction(transactionId, "failed");
                    if (updated > 0) {
                        mapper.insertEntry(transactionId, "ROLLBACK", null, null, "failed",
                                "compensated stale started transaction after " + COMPENSATION_MINUTES + " minutes");
                    }
                }
            });
        } catch (RuntimeException ex) {
            log.warn("Failed to compensate stale transaction audit records", ex);
        }
    }

    private String normalizeOperation(String operation) {
        String normalized = operation == null ? "" : operation.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "START", "INSERT", "UPDATE", "DELETE", "UPSERT", "COMMIT", "ROLLBACK" -> normalized;
            default -> "UPSERT";
        };
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "" : status.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "started", "success", "failed", "rolled_back" -> normalized;
            default -> "success";
        };
    }
}
