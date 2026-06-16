package com.streetvendor.discovery.dto;

import java.util.List;

/**
 * API response DTO for the paginated nearby vendor search endpoint.
 * <p>
 * Wraps a list of {@link VendorSummaryResponse} results with pagination metadata.
 * Used at API boundaries — never exposes JPA entities directly. Designed as an
 * immutable record to align with the project's DTO conventions.
 * </p>
 *
 * @param vendors       the list of nearby vendor summaries on the current page
 * @param page          the current page number (zero-indexed)
 * @param size          the number of items per page
 * @param totalElements the total number of matching vendors across all pages
 * @param totalPages    the total number of pages
 */
public record NearbyVendorResponse(
        List<VendorSummaryResponse> vendors,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
