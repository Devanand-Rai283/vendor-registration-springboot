package com.streetvendor.common.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    @Captor
    private ArgumentCaptor<AuditLog> auditLogCaptor;

    @Test
    void shouldLogVendorApprovedEvent() {
        UUID vendorId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        auditService.logEvent(AuditEventType.VENDOR_APPROVED, vendorId, adminId, null);

        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog savedLog = auditLogCaptor.getValue();

        assertNotNull(savedLog.getId());
        assertEquals(AuditEventType.VENDOR_APPROVED, savedLog.getEventType());
        assertEquals(vendorId, savedLog.getVendorId());
        assertEquals(adminId, savedLog.getAdminUserId());
        assertNull(savedLog.getDetails());
    }

    @Test
    void shouldLogVendorRejectedEventWithDetails() {
        UUID vendorId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        auditService.logEvent(AuditEventType.VENDOR_REJECTED, vendorId, adminId, "Expired FSSAI certificate");

        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog savedLog = auditLogCaptor.getValue();

        assertNotNull(savedLog.getId());
        assertEquals(AuditEventType.VENDOR_REJECTED, savedLog.getEventType());
        assertEquals(vendorId, savedLog.getVendorId());
        assertEquals(adminId, savedLog.getAdminUserId());
        assertEquals("Expired FSSAI certificate", savedLog.getDetails());
    }

    @Test
    void shouldAllowNullAdminId() {
        UUID vendorId = UUID.randomUUID();

        auditService.logEvent(AuditEventType.VENDOR_APPROVED, vendorId, null, null);

        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog savedLog = auditLogCaptor.getValue();

        assertNull(savedLog.getAdminUserId());
    }

    @Test
    void shouldInvokeRepositoryExactlyOnce() {
        UUID vendorId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        auditService.logEvent(AuditEventType.VENDOR_APPROVED, vendorId, adminId, null);

        verify(auditLogRepository).save(any(AuditLog.class));
        verifyNoMoreInteractions(auditLogRepository);
    }
}