package com.streetvendor.discovery.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * API response DTO representing a single vendor result in discovery search results.
 * <p>
 * Contains only the fields relevant for displaying a vendor in nearby search results
 * or on a map. Never exposes the full JPA entity. Designed as an immutable record
 * to align with the project's DTO conventions.
 * </p>
 *
 * @param id            the unique identifier of the vendor
 * @param businessName  the display name of the vendor's business
 * @param foodType      the type of food the vendor serves
 * @param address       the street address or location description of the vendor
 * @param averageRating the vendor's average customer rating
 * @param latitude      the geographic latitude of the vendor's location
 * @param longitude     the geographic longitude of the vendor's location
 * @param distanceKm    the distance from the search center point in kilometers
 */
public record VendorSummaryResponse(
        UUID id,
        String businessName,
        String foodType,
        String address,
        BigDecimal averageRating,
        Double latitude,
        Double longitude,
        Double distanceKm
) {
}
