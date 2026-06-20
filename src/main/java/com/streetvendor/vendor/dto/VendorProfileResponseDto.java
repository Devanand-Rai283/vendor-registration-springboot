package com.streetvendor.vendor.dto;

import com.streetvendor.vendor.enums.VendorStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record VendorProfileResponseDto(
        UUID id,
        String businessName,
        String ownerName,
        String phone,
        String foodType,
        String description,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        VendorStatus status,
        BigDecimal averageRating,
        Integer totalReviews,
        String rejectionReason
) {}
