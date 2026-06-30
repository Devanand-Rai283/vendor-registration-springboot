package com.streetvendor.common.audit;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuditLogEntityTest {

    @Test
    void shouldCreateAuditLogWithAllFields() {
        UUID id = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        AuditEventType eventType = AuditEventType.VENDOR_APPROVED;

        AuditLog auditLog = new AuditLog(id, eventType, vendorId, adminId, null, null);

        assertEquals(id, auditLog.getId());
        assertEquals(eventType, auditLog.getEventType());
        assertEquals(vendorId, auditLog.getVendorId());
        assertEquals(adminId, auditLog.getAdminUserId());
        assertNull(auditLog.getDetails());
    }

    @Test
    void shouldStoreDetailsWhenProvided() {
        UUID id = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        AuditLog auditLog = new AuditLog(id, AuditEventType.VENDOR_REJECTED, vendorId, adminId, null, "Expired FSSAI certificate");

        assertEquals("Expired FSSAI certificate", auditLog.getDetails());
    }

    @Test
    void shouldAllowNullAdminUserId() {
        UUID id = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();

        AuditLog auditLog = new AuditLog(id, AuditEventType.VENDOR_APPROVED, vendorId, null, null, null);

        assertNull(auditLog.getAdminUserId());
    }
}