package com.streetvendor.admin.service;

import com.streetvendor.admin.dto.AdminVendorDetailResponseDto;
import com.streetvendor.admin.dto.AdminVendorDocumentResponseDto;
import com.streetvendor.admin.dto.AdminVendorSummaryDto;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.RefreshToken;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.RefreshTokenRepository;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.common.audit.AuditEventType;
import com.streetvendor.common.audit.AuditService;
import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.repository.VendorDocumentRepository;
import com.streetvendor.vendor.repository.VendorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminVendorManagementServiceImpl implements AdminVendorManagementService {

    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AuditService auditService;
    private final VendorDocumentRepository vendorDocumentRepository;

    public AdminVendorManagementServiceImpl(
            VendorRepository vendorRepository,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            RedisTemplate<String, Object> redisTemplate,
            AuditService auditService,
            VendorDocumentRepository vendorDocumentRepository) {
        this.vendorRepository = vendorRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.redisTemplate = redisTemplate;
        this.auditService = auditService;
        this.vendorDocumentRepository = vendorDocumentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminVendorSummaryDto> getVendors(VendorStatus status, Pageable pageable) {
        // Enforce deterministic default sort order by createdAt descending
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Vendor> vendorsPage;
        if (status != null) {
            vendorsPage = vendorRepository.findByStatus(status, sortedPageable);
        } else {
            vendorsPage = vendorRepository.findAll(sortedPageable);
        }

        return vendorsPage.map(vendor -> new AdminVendorSummaryDto(
                vendor.getId(),
                vendor.getBusinessName(),
                vendor.getOwnerName(),
                vendor.getStatus(),
                vendor.getUser() != null ? vendor.getUser().getEmail() : null,
                vendor.getUser() != null ? vendor.getUser().getAccountStatus() : null
        ));
    }

    @Override
    @Transactional
    public void suspendVendor(UUID id, UUID adminUserId) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + id));

        User user = vendor.getUser();
        if (user == null) {
            throw new ResourceNotFoundException("No user associated with vendor id: " + id);
        }

        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            // Already suspended - ensure Redis key exists for idempotency, then return
            redisTemplate.opsForValue().set("suspended_users:" + user.getId(), "true", Duration.ofMinutes(15));
            return;
        }

        // 1. Update user account status to SUSPENDED
        user.setAccountStatus(AccountStatus.SUSPENDED);
        userRepository.save(user);

        // 2. Revoke all active refresh tokens (set revokedAt = Instant.now())
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId());
        if (activeTokens != null && !activeTokens.isEmpty()) {
            Instant now = Instant.now();
            for (RefreshToken token : activeTokens) {
                token.setRevokedAt(now);
            }
            refreshTokenRepository.saveAll(activeTokens);
        }

        // 3. Add user_id to Redis key with 15-minute TTL
        redisTemplate.opsForValue().set("suspended_users:" + user.getId(), "true", Duration.ofMinutes(15));

        // 4. Write ACCOUNT_SUSPENDED audit event
        auditService.logEvent(AuditEventType.ACCOUNT_SUSPENDED, vendor.getId(), adminUserId, "Vendor account suspended by admin.");
    }

    @Override
    @Transactional
    public void reactivateVendor(UUID id, UUID adminUserId) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + id));

        User user = vendor.getUser();
        if (user == null) {
            throw new ResourceNotFoundException("No user associated with vendor id: " + id);
        }

        if (user.getAccountStatus() == AccountStatus.ACTIVE) {
            // Already active - ensure Redis key is removed for idempotency, then return
            redisTemplate.delete("suspended_users:" + user.getId());
            return;
        }

        // 1. Update user account status to ACTIVE
        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        // 2. Remove Redis key
        redisTemplate.delete("suspended_users:" + user.getId());

        // 3. Write ACCOUNT_REACTIVATED audit event
        auditService.logEvent(AuditEventType.ACCOUNT_REACTIVATED, vendor.getId(), adminUserId, "Vendor account reactivated by admin.");
    }

    @Override
    @Transactional(readOnly = true)
    public AdminVendorDetailResponseDto getVendorDetails(UUID vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + vendorId));

        User user = vendor.getUser();
        if (user == null) {
            throw new ResourceNotFoundException("No user associated with vendor id: " + vendorId);
        }

        List<AdminVendorDocumentResponseDto> documents = vendorDocumentRepository.findByVendorId(vendorId).stream()
                .map(doc -> new AdminVendorDocumentResponseDto(
                        doc.getId(),
                        doc.getDocumentType(),
                        doc.getVerificationStatus(),
                        doc.getFileUrl(),
                        doc.getUploadedAt(),
                        doc.getRejectionReason()
                ))
                .collect(Collectors.toList());

        return new AdminVendorDetailResponseDto(
                vendor.getId(),
                vendor.getBusinessName(),
                vendor.getOwnerName(),
                user.getEmail(),
                vendor.getPhone(),
                vendor.getDescription(),
                vendor.getFoodType(),
                vendor.getStatus(),
                user.getAccountStatus(),
                vendor.getAddress(),
                vendor.getLatitude(),
                vendor.getLongitude(),
                vendor.getAverageRating(),
                vendor.getTotalReviews(),
                vendor.getCreatedAt(),
                vendor.getUpdatedAt(),
                vendor.getRejectionReason(),
                documents
        );
    }
}
