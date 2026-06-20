package com.streetvendor.discovery.controller;

import com.streetvendor.discovery.dto.NearbyVendorResponse;
import com.streetvendor.discovery.dto.VendorReviewResponse;
import com.streetvendor.discovery.service.DiscoveryService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import com.streetvendor.common.response.ApiResponse;
import com.streetvendor.discovery.dto.VendorDetailDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.util.UUID;

/**
 * REST controller for nearby vendor discovery.
 * <p>
 * Exposes a publicly accessible endpoint for discovering
 * <strong>APPROVED</strong> vendors near a given geographic location.
 * Only vendors with status {@code APPROVED} are returned — pending or
 * rejected vendors are excluded from discovery results.
 * </p>
 * <p>
 * This controller contains no business logic. It extracts query parameters,
 * delegates to {@link DiscoveryService}, and returns the response.
 * Haversine calculations, bounding-box logic, and DTO mapping are all
 * handled in the service layer.
 * </p>
 *
 * <h3>Endpoint</h3>
 * <pre>{@code GET /api/vendors/nearby?lat=12.97&lng=77.59&radius=5.0&page=0&size=10}</pre>
 *
 * <h3>Response</h3>
 * <pre>{@code
 * {
 *   "vendors": [
 *     {
 *       "id": "550e8400-e29b-41d4-a716-446655440000",
 *       "businessName": "Maria's Tacos",
 *       "foodType": "Mexican",
 *       "address": "123 Main St",
 *       "averageRating": 4.5,
 *       "latitude": 12.9716,
 *       "longitude": 77.5946,
 *       "distanceKm": 0.42
 *     }
 *   ],
 *   "page": 0,
 *   "size": 10,
 *   "totalElements": 1,
 *   "totalPages": 1
 * }
 * }</pre>
 */
@RestController
@RequestMapping("/api/vendors")
@Validated
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    public DiscoveryController(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    /**
     * Retrieves a paginated list of APPROVED vendors within the specified
     * radius from the given geographic coordinates.
     * <p>
     * This endpoint is publicly accessible. Only vendors with an
     * {@code APPROVED} status are eligible for discovery.
     * </p>
     *
     * <h4>Parameter constraints</h4>
     * <ul>
     *   <li>{@code lat} — must be between -90.0 and 90.0 (inclusive)</li>
     *   <li>{@code lng} — must be between -180.0 and 180.0 (inclusive)</li>
     *   <li>{@code radius} — must be positive (default: 5.0)</li>
     *   <li>{@code page} — must be zero or positive (default: 0)</li>
     *   <li>{@code size} — must be positive (default: 10)</li>
     * </ul>
     *
     * @param lat    the latitude of the search center point in decimal degrees (must be between -90 and 90)
     * @param lng    the longitude of the search center point in decimal degrees (must be between -180 and 180)
     * @param radius the search radius in kilometers (must be positive, default 5.0)
     * @param page   the page number, zero-indexed (must be zero or positive, default 0)
     * @param size   the number of results per page (must be positive, default 10)
     * @return a {@link NearbyVendorResponse} containing matching vendors
     *         and pagination metadata, with HTTP 200
     */
    @GetMapping("/nearby")
    public ResponseEntity<NearbyVendorResponse> getNearbyVendors(
            @RequestParam
            @DecimalMin("-90.0")
            @DecimalMax("90.0")
            double lat,
            @RequestParam
            @DecimalMin("-180.0")
            @DecimalMax("180.0")
            double lng,
            @RequestParam(defaultValue = "5.0")
            @Positive
            double radius,
            @RequestParam(defaultValue = "0")
            @PositiveOrZero
            int page,
            @RequestParam(defaultValue = "10")
            @Positive
            int size) {
        NearbyVendorResponse response = discoveryService.findNearbyVendors(lat, lng, radius, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves public details of a single APPROVED vendor.
     *
     * @param id the unique identifier of the vendor
     * @return the vendor details, with HTTP 200
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VendorDetailDto>> getVendor(
            @PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Vendor details retrieved successfully",
                        discoveryService.getVendorDetails(id)
                )
        );
    }

    /**
     * Retrieves a paginated list of reviews for a single APPROVED vendor.
     *
     * @param vendorId the unique identifier of the vendor
     * @param page     the page number, zero-indexed (must be zero or positive, default 0)
     * @param size     the number of results per page (must be positive, default 10)
     * @return paginated vendor reviews, with HTTP 200
     */
    @GetMapping("/{vendorId}/ratings")
    public ResponseEntity<Page<VendorReviewResponse>> getVendorReviews(
            @PathVariable UUID vendorId,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "10") @Positive int size) {
        return ResponseEntity.ok(discoveryService.getVendorReviews(vendorId, PageRequest.of(page, size)));
    }
}
