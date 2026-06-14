package com.streetvendor.common.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void logEvent(AuditEventType eventType, UUID vendorId, UUID adminUserId, String details) {
        AuditLog auditLog = new AuditLog(
                UUID.randomUUID(),
                eventType,
                vendorId,
                adminUserId,
                details
        );
        auditLogRepository.save(auditLog);
    }
}