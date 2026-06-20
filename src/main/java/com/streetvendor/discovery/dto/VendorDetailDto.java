package com.streetvendor.discovery.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * API response DTO representing full details for a single vendor.
 * <p>
 * Exposes only the fields required for public vendor page customer discovery.
 * Designed as an immutable record.
 * </p>
 */
public record VendorDetailDto(
        UUID id,
        String businessName,
        String description,
        String foodType,
        BigDecimal averageRating,
        String address,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
