package com.streetvendor.vendor.dto;

import com.streetvendor.vendor.enums.VendorStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record VendorStatusResponse(
        UUID id,
        String businessName,
        VendorStatus status,
        BigDecimal averageRating
) {
}
