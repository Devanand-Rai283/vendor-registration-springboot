package com.streetvendor.discovery.service;

import com.streetvendor.discovery.dto.NearbyVendorResponse;
import com.streetvendor.discovery.dto.VendorMenuResponseDto;
import java.util.UUID;

/**
 * Service interface for vendor discovery use cases.
 * <p>
 * Defines the contract for searching and discovering nearby vendors
 * based on geographic location. Implementations handle use-case
 * orchestration only, delegating data access and domain logic to
 * repositories and utility classes.
 * </p>
 */
public interface DiscoveryService {

    /**
     * Finds vendors located within the specified radius from the given
     * geographic coordinates, with pagination.
     *
     * @param latitude  the latitude of the search center point in decimal degrees
     * @param longitude the longitude of the search center point in decimal degrees
     * @param radiusKm  the search radius in kilometers
     * @param page      the page number (zero-indexed)
     * @param size      the number of results per page
     * @return a {@link NearbyVendorResponse} containing the matching vendors and pagination metadata
     */
    NearbyVendorResponse findNearbyVendors(double latitude, double longitude, double radiusKm, int page, int size);

    VendorMenuResponseDto getVendorMenu(UUID vendorId);
}
