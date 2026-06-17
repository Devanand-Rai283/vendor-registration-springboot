package com.streetvendor.vendor;

import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.common.audit.AuditEventType;
import com.streetvendor.common.audit.AuditService;
import com.streetvendor.common.exception.ConflictException;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.vendor.dto.CreateVendorRequest;
import com.streetvendor.vendor.dto.VendorResponse;
import com.streetvendor.vendor.dto.VendorStatusResponse;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.repository.VendorRepository;
import com.streetvendor.vendor.service.VendorServiceImpl;
import com.streetvendor.discovery.cache.DiscoveryCacheService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorServiceImplTest {

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private DiscoveryCacheService discoveryCacheService;

    @InjectMocks
    private VendorServiceImpl vendorService;

    private User vendorUser;
    private User customerUser;
    private CreateVendorRequest validRequest;
    private Vendor vendor;

    @BeforeEach
    void setUp() {
        vendorUser = new User(UUID.randomUUID(), "vendor@example.com", "hash", Role.VENDOR, com.streetvendor.auth.entity.AccountStatus.ACTIVE);
        customerUser = new User(UUID.randomUUID(), "customer@example.com", "hash", Role.CUSTOMER, com.streetvendor.auth.entity.AccountStatus.ACTIVE);
        vendor = new Vendor(vendorUser.getId(), vendorUser, "Test Business");
        vendor.setStatus(VendorStatus.PENDING_REVIEW);

        validRequest = new CreateVendorRequest(
                "Test Business",
                "Owner",
                "1234567890",
                "Indian",
                "Delicious food",
                new BigDecimal("12.9716"),
                new BigDecimal("77.5946"),
                "123 Main St"
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(User user) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void shouldCreateVendorSuccessfully() {
        setAuthentication(vendorUser);
        when(vendorRepository.existsByUserId(vendorUser.getId())).thenReturn(false);
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> {
            Vendor vendor = invocation.getArgument(0);
            return vendor;
        });

        VendorResponse response = vendorService.createVendor(validRequest);

        assertNotNull(response);
        assertEquals(VendorStatus.PENDING_REVIEW, response.status());
        assertEquals("Vendor profile created successfully.", response.message());
        verify(vendorRepository).save(any(Vendor.class));
    }

    @Test
    void shouldRejectDuplicateVendor() {
        setAuthentication(vendorUser);
        when(vendorRepository.existsByUserId(vendorUser.getId())).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () -> {
            vendorService.createVendor(validRequest);
        });

        assertEquals("Vendor profile already exists", exception.getMessage());
        verify(vendorRepository, never()).save(any(Vendor.class));
    }

    @Test
    void shouldRejectNonVendorUser() {
        setAuthentication(customerUser);

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> {
            vendorService.createVendor(validRequest);
        });

        assertEquals("Only vendors can create vendor profiles", exception.getMessage());
        verify(vendorRepository, never()).save(any(Vendor.class));
    }

    @Test
    void shouldLinkVendorToAuthenticatedUser() {
        setAuthentication(vendorUser);
        when(vendorRepository.existsByUserId(vendorUser.getId())).thenReturn(false);
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> {
            Vendor vendor = invocation.getArgument(0);
            assertEquals(vendorUser.getId(), vendor.getUser().getId());
            return vendor;
        });

        vendorService.createVendor(validRequest);

        verify(vendorRepository).save(any(Vendor.class));
    }

    @Test
    void shouldForceStatusToPendingReview() {
        setAuthentication(vendorUser);
        when(vendorRepository.existsByUserId(vendorUser.getId())).thenReturn(false);
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> {
            Vendor vendor = invocation.getArgument(0);
            assertEquals(VendorStatus.PENDING_REVIEW, vendor.getStatus());
            return vendor;
        });

        vendorService.createVendor(validRequest);

        verify(vendorRepository).save(any(Vendor.class));
    }

    @Test
    void shouldReturnVendorStatusForAuthenticatedVendor() {
        setAuthentication(vendorUser);
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(java.util.Optional.of(vendor));

        VendorStatusResponse response = vendorService.getMyVendorStatus();

        assertNotNull(response);
        assertEquals(vendor.getId(), response.id());
        assertEquals(vendor.getBusinessName(), response.businessName());
        assertEquals(vendor.getStatus(), response.status());
        assertEquals(vendor.getAverageRating(), response.averageRating());
    }

    @Test
    void shouldThrowResourceNotFoundWhenVendorProfileMissing() {
        setAuthentication(vendorUser);
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(java.util.Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            vendorService.getMyVendorStatus();
        });

        assertEquals("Vendor profile not found", exception.getMessage());
    }

    @Test
    void shouldThrowForbiddenForNonVendorUser() {
        setAuthentication(customerUser);

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> {
            vendorService.getMyVendorStatus();
        });

        assertEquals("Only vendors can access their vendor profile", exception.getMessage());
    }

    @Test
    void shouldApprovePendingVendor() {
        UUID vendorId = UUID.randomUUID();
        Vendor pendingVendor = new Vendor(vendorId, vendorUser, "Test Business");
        pendingVendor.setStatus(VendorStatus.PENDING_REVIEW);

        when(vendorRepository.findById(vendorId)).thenReturn(java.util.Optional.of(pendingVendor));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VendorResponse response = vendorService.approveVendor(vendorId);

        assertNotNull(response);
        assertEquals(vendorId, response.vendorId());
        assertEquals(VendorStatus.APPROVED, response.status());
        assertEquals("Vendor approved successfully.", response.message());
        verify(vendorRepository).save(pendingVendor);
        assertEquals(VendorStatus.APPROVED, pendingVendor.getStatus());
        verify(discoveryCacheService).evictPattern("search:vendors:*");
        verify(auditService).logEvent(AuditEventType.VENDOR_APPROVED, vendorId, null, null);
    }

    @Test
    void shouldThrowConflictWhenApprovingAlreadyApprovedVendor() {
        UUID vendorId = UUID.randomUUID();
        Vendor approvedVendor = new Vendor(vendorId, vendorUser, "Test Business");
        approvedVendor.setStatus(VendorStatus.APPROVED);

        when(vendorRepository.findById(vendorId)).thenReturn(java.util.Optional.of(approvedVendor));

        ConflictException exception = assertThrows(ConflictException.class, () -> {
            vendorService.approveVendor(vendorId);
        });

        assertEquals("Cannot transition vendor from APPROVED to APPROVED", exception.getMessage());
        verify(vendorRepository, never()).save(any(Vendor.class));
        verify(auditService, never()).logEvent(any(), any(), any(), any());
    }

    @Test
    void shouldThrowConflictWhenApprovingRejectedVendor() {
        UUID vendorId = UUID.randomUUID();
        Vendor rejectedVendor = new Vendor(vendorId, vendorUser, "Test Business");
        rejectedVendor.setStatus(VendorStatus.REJECTED);

        when(vendorRepository.findById(vendorId)).thenReturn(java.util.Optional.of(rejectedVendor));

        ConflictException exception = assertThrows(ConflictException.class, () -> {
            vendorService.approveVendor(vendorId);
        });

        assertEquals("Cannot transition vendor from REJECTED to APPROVED", exception.getMessage());
        verify(vendorRepository, never()).save(any(Vendor.class));
        verify(auditService, never()).logEvent(any(), any(), any(), any());
    }

    @Test
    void shouldThrowResourceNotFoundWhenApprovingNonExistentVendor() {
        UUID vendorId = UUID.randomUUID();
        when(vendorRepository.findById(vendorId)).thenReturn(java.util.Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            vendorService.approveVendor(vendorId);
        });

        assertEquals("Vendor not found", exception.getMessage());
        verify(vendorRepository, never()).save(any(Vendor.class));
        verify(auditService, never()).logEvent(any(), any(), any(), any());
    }

    @Test
    void shouldRejectPendingVendor() {
        UUID vendorId = UUID.randomUUID();
        Vendor pendingVendor = new Vendor(vendorId, vendorUser, "Test Business");
        pendingVendor.setStatus(VendorStatus.PENDING_REVIEW);

        when(vendorRepository.findById(vendorId)).thenReturn(java.util.Optional.of(pendingVendor));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VendorResponse response = vendorService.rejectVendor(vendorId, "Expired FSSAI certificate");

        assertNotNull(response);
        assertEquals(vendorId, response.vendorId());
        assertEquals(VendorStatus.REJECTED, response.status());
        assertEquals("Vendor rejected successfully.", response.message());
        assertEquals("Expired FSSAI certificate", response.rejectionReason());
        verify(vendorRepository).save(pendingVendor);
        assertEquals(VendorStatus.REJECTED, pendingVendor.getStatus());
        assertEquals("Expired FSSAI certificate", pendingVendor.getRejectionReason());
        verify(discoveryCacheService).evictPattern("search:vendors:*");
        verify(auditService).logEvent(AuditEventType.VENDOR_REJECTED, vendorId, null, "Expired FSSAI certificate");
    }

    @Test
    void shouldThrowConflictWhenRejectingAlreadyApprovedVendor() {
        UUID vendorId = UUID.randomUUID();
        Vendor approvedVendor = new Vendor(vendorId, vendorUser, "Test Business");
        approvedVendor.setStatus(VendorStatus.APPROVED);

        when(vendorRepository.findById(vendorId)).thenReturn(java.util.Optional.of(approvedVendor));

        ConflictException exception = assertThrows(ConflictException.class, () -> {
            vendorService.rejectVendor(vendorId, "Some reason");
        });

        assertEquals("Cannot transition vendor from APPROVED to REJECTED", exception.getMessage());
        verify(vendorRepository, never()).save(any(Vendor.class));
        verify(auditService, never()).logEvent(any(), any(), any(), any());
    }

    @Test
    void shouldThrowConflictWhenRejectingAlreadyRejectedVendor() {
        UUID vendorId = UUID.randomUUID();
        Vendor rejectedVendor = new Vendor(vendorId, vendorUser, "Test Business");
        rejectedVendor.setStatus(VendorStatus.REJECTED);

        when(vendorRepository.findById(vendorId)).thenReturn(java.util.Optional.of(rejectedVendor));

        ConflictException exception = assertThrows(ConflictException.class, () -> {
            vendorService.rejectVendor(vendorId, "Some reason");
        });

        assertEquals("Cannot transition vendor from REJECTED to REJECTED", exception.getMessage());
        verify(vendorRepository, never()).save(any(Vendor.class));
        verify(auditService, never()).logEvent(any(), any(), any(), any());
    }

    @Test
    void shouldThrowResourceNotFoundWhenRejectingNonExistentVendor() {
        UUID vendorId = UUID.randomUUID();
        when(vendorRepository.findById(vendorId)).thenReturn(java.util.Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            vendorService.rejectVendor(vendorId, "Some reason");
        });

        assertEquals("Vendor not found", exception.getMessage());
        verify(vendorRepository, never()).save(any(Vendor.class));
        verify(auditService, never()).logEvent(any(), any(), any(), any());
    }

    @Test
    void shouldAcceptPendingToApprovedTransition() {
        assertDoesNotThrow(() ->
                vendorService.validateTransition(VendorStatus.PENDING_REVIEW, VendorStatus.APPROVED));
    }

    @Test
    void shouldAcceptPendingToRejectedTransition() {
        assertDoesNotThrow(() ->
                vendorService.validateTransition(VendorStatus.PENDING_REVIEW, VendorStatus.REJECTED));
    }

    @Test
    void shouldRejectApprovedToApprovedTransition() {
        ConflictException exception = assertThrows(ConflictException.class, () ->
                vendorService.validateTransition(VendorStatus.APPROVED, VendorStatus.APPROVED));
        assertEquals("Cannot transition vendor from APPROVED to APPROVED", exception.getMessage());
    }

    @Test
    void shouldRejectApprovedToRejectedTransition() {
        ConflictException exception = assertThrows(ConflictException.class, () ->
                vendorService.validateTransition(VendorStatus.APPROVED, VendorStatus.REJECTED));
        assertEquals("Cannot transition vendor from APPROVED to REJECTED", exception.getMessage());
    }

    @Test
    void shouldRejectRejectedToApprovedTransition() {
        ConflictException exception = assertThrows(ConflictException.class, () ->
                vendorService.validateTransition(VendorStatus.REJECTED, VendorStatus.APPROVED));
        assertEquals("Cannot transition vendor from REJECTED to APPROVED", exception.getMessage());
    }

    @Test
    void shouldRejectRejectedToRejectedTransition() {
        ConflictException exception = assertThrows(ConflictException.class, () ->
                vendorService.validateTransition(VendorStatus.REJECTED, VendorStatus.REJECTED));
        assertEquals("Cannot transition vendor from REJECTED to REJECTED", exception.getMessage());
    }

    @Test
    void shouldRejectPendingToPendingTransition() {
        ConflictException exception = assertThrows(ConflictException.class, () ->
                vendorService.validateTransition(VendorStatus.PENDING_REVIEW, VendorStatus.PENDING_REVIEW));
        assertEquals("Cannot transition vendor from PENDING_REVIEW to PENDING_REVIEW", exception.getMessage());
    }
}