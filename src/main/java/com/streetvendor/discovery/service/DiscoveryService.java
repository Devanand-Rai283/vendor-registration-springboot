package com.streetvendor.discovery.service;

import com.streetvendor.discovery.dto.FoodSearchResponseDto;
import com.streetvendor.discovery.dto.NearbyVendorResponse;
import com.streetvendor.discovery.dto.VendorMenuResponseDto;
import org.springframework.data.domain.Page;
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

    /**
     * Searches for food items by keyword across all approved vendors,
     * with optional filters for food type and dietary tag.
     *
     * @param keyword    the search keyword (must not be null or blank)
     * @param foodType   optional food type filter (may be null)
     * @param dietaryTag optional dietary tag filter (may be null)
     * @param page       the page number (zero-indexed, defaults to 0 if null)
     * @param size       the page size (defaults to 20 if null, max 100)
     * @return a paginated {@link FoodSearchResponseDto} containing matching items
     * @throws IllegalArgumentException if keyword is null, blank, or whitespace-only
     */
    Page<FoodSearchResponseDto> searchFoods(
            String keyword,
            String foodType,
            String dietaryTag,
            Integer page,
            Integer size);
}
