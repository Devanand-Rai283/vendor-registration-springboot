package com.streetvendor.admin.service;

import com.streetvendor.admin.dto.AdminVendorSummaryDto;
import com.streetvendor.vendor.enums.VendorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service managing vendor status and account access.
 */
public interface AdminVendorManagementService {

    /**
     * Retrieves a paginated list of vendor summaries, optionally filtered by status.
     * Results are sorted by createdAt descending.
     */
    Page<AdminVendorSummaryDto> getVendors(VendorStatus status, Pageable pageable);

    /**
     * Suspends a vendor's account, invalidates their refresh tokens and active sessions.
     */
    void suspendVendor(UUID id, UUID adminUserId);

    /**
     * Reactivates a suspended vendor's account.
     */
    void reactivateVendor(UUID id, UUID adminUserId);
}
