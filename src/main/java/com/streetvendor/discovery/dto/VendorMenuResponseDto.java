package com.streetvendor.discovery.dto;

import java.util.List;
import java.util.UUID;

/**
 * API response DTO representing the complete menu for a single vendor.
 * <p>
 * Returned by the vendor menu viewing API boundary. This DTO hierarchy maps
 * directly to the JSON response structure:
 * VendorMenuResponseDto -> categories[] -> items[].
 * </p>
 *
 * @param vendorId   the vendor identifier
 * @param vendorName the vendor display name
 * @param categories the list of menu categories for the vendor
 */
public record VendorMenuResponseDto(
        UUID vendorId,
        String vendorName,
        List<MenuCategoryResponseDto> categories) {
}
