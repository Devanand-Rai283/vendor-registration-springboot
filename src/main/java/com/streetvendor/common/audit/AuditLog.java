package com.streetvendor.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog extends AuditableEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private AuditEventType eventType;

    @Column(name = "vendor_id", columnDefinition = "uuid")
    private UUID vendorId;

    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "admin_user_id", columnDefinition = "uuid")
    private UUID adminUserId;

    @Column(length = 500)
    private String details;

    protected AuditLog() {
    }

    public AuditLog(UUID id, AuditEventType eventType, UUID vendorId, UUID adminUserId, UUID userId, String details) {
        this.id = id;
        this.eventType = eventType;
        this.vendorId = vendorId;
        this.adminUserId = adminUserId;
        this.userId = userId;
        this.details = details;
    }

    public UUID getId() {
        return id;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public UUID getVendorId() {
        return vendorId;
    }

    public UUID getAdminUserId() {
        return adminUserId;
    }

    public String getDetails() {
        return details;
    }

    public UUID getUserId() {
        return userId;
    }
}