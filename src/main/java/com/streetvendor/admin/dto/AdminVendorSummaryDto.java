package com.streetvendor.admin.dto;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.vendor.enums.VendorStatus;
import java.util.UUID;

/**
 * DTO summarizing vendor details for administrative views.
 */
public record AdminVendorSummaryDto(
        UUID id,
        String businessName,
        String ownerName,
        VendorStatus status,
        String userEmail,
        AccountStatus userAccountStatus
) {}
