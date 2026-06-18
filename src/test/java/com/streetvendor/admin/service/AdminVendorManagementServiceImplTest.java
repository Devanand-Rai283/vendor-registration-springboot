package com.streetvendor.admin.service;

import com.streetvendor.admin.dto.AdminVendorSummaryDto;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.RefreshToken;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.RefreshTokenRepository;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.common.audit.AuditEventType;
import com.streetvendor.common.audit.AuditService;
import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminVendorManagementServiceImplTest {

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AdminVendorManagementServiceImpl service;

    private UUID vendorId;
    private UUID userId;
    private UUID adminUserId;
    private User testUser;
    private Vendor testVendor;

    @BeforeEach
    void setUp() {
        vendorId = UUID.randomUUID();
        userId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();
        testUser = new User(userId, "vendor@example.com", "hash", Role.VENDOR, AccountStatus.ACTIVE);
        testVendor = new Vendor(vendorId, testUser, "Vendor Business Name");
        testVendor.setStatus(VendorStatus.APPROVED);
        testVendor.setOwnerName("John Doe");
    }

    @Test
    void getVendors_withoutStatus_returnsAllVendorsSortedByCreatedAtDesc() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Vendor> mockPage = new PageImpl<>(List.of(testVendor));

        // Expect findByStatus not to be called, findAll to be called with modified Pageable
        when(vendorRepository.findAll(any(Pageable.class))).thenReturn(mockPage);

        Page<AdminVendorSummaryDto> result = service.getVendors(null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Vendor Business Name", result.getContent().get(0).businessName());
        assertEquals("vendor@example.com", result.getContent().get(0).userEmail());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(vendorRepository).findAll(pageableCaptor.capture());
        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(0, capturedPageable.getPageNumber());
        assertEquals(10, capturedPageable.getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt"), capturedPageable.getSort());
    }

    @Test
    void getVendors_withStatus_returnsFilteredVendorsSortedByCreatedAtDesc() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Vendor> mockPage = new PageImpl<>(List.of(testVendor));

        when(vendorRepository.findByStatus(eq(VendorStatus.APPROVED), any(Pageable.class))).thenReturn(mockPage);

        Page<AdminVendorSummaryDto> result = service.getVendors(VendorStatus.APPROVED, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(VendorStatus.APPROVED, result.getContent().get(0).status());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(vendorRepository).findByStatus(eq(VendorStatus.APPROVED), pageableCaptor.capture());
        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt"), capturedPageable.getSort());
    }

    @Test
    void suspendVendor_success_updatesStatusRevokesTokensAndBlacklistsUser() {
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(testVendor));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        RefreshToken activeToken = new RefreshToken(UUID.randomUUID(), userId, "tokenhash", Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId)).thenReturn(List.of(activeToken));

        service.suspendVendor(vendorId, adminUserId);

        assertEquals(AccountStatus.SUSPENDED, testUser.getAccountStatus());
        verify(userRepository).save(testUser);
        assertTrue(activeToken.isRevoked());
        verify(refreshTokenRepository).saveAll(anyList());
        verify(valueOperations).set(eq("suspended_users:" + userId), eq("true"), eq(Duration.ofMinutes(15)));
        verify(auditService).logEvent(eq(AuditEventType.ACCOUNT_SUSPENDED), eq(vendorId), eq(adminUserId), anyString());
    }

    @Test
    void suspendVendor_idempotency_doesNotSaveOrLogIfAlreadySuspended() {
        testUser.setAccountStatus(AccountStatus.SUSPENDED);
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(testVendor));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service.suspendVendor(vendorId, adminUserId);

        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).saveAll(anyList());
        // Should still ensure Redis key is present
        verify(valueOperations).set(eq("suspended_users:" + userId), eq("true"), eq(Duration.ofMinutes(15)));
        verify(auditService, never()).logEvent(any(), any(), any(), any());
    }

    @Test
    void suspendVendor_vendorNotFound_throwsResourceNotFoundException() {
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.suspendVendor(vendorId, adminUserId));
    }

    @Test
    void reactivateVendor_success_updatesStatusAndRemovesRedisKey() {
        testUser.setAccountStatus(AccountStatus.SUSPENDED);
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(testVendor));

        service.reactivateVendor(vendorId, adminUserId);

        assertEquals(AccountStatus.ACTIVE, testUser.getAccountStatus());
        verify(userRepository).save(testUser);
        verify(redisTemplate).delete(eq("suspended_users:" + userId));
        verify(auditService).logEvent(eq(AuditEventType.ACCOUNT_REACTIVATED), eq(vendorId), eq(adminUserId), anyString());
    }

    @Test
    void reactivateVendor_idempotency_doesNotSaveOrLogIfAlreadyActive() {
        testUser.setAccountStatus(AccountStatus.ACTIVE);
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(testVendor));

        service.reactivateVendor(vendorId, adminUserId);

        verify(userRepository, never()).save(any());
        // Should still ensure Redis key is deleted
        verify(redisTemplate).delete(eq("suspended_users:" + userId));
        verify(auditService, never()).logEvent(any(), any(), any(), any());
    }
}
