package com.streetvendor.vendor.repository;

import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.enums.VendorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    boolean existsByUserId(UUID userId);

    Optional<Vendor> findByUserId(UUID userId);

    /**
     * Retrieves a page of APPROVED vendors whose geographic coordinates fall
     * within the specified bounding box.
     * <p>
     * Bounding-box filtering is performed <strong>before</strong> any Haversine
     * or great-circle distance calculation because it uses simple numeric
     * comparisons ({@code BETWEEN}) that can leverage database indexes on the
     * {@code latitude} and {@code longitude} columns. This efficiently reduces
     * the candidate set to a rectangular region around the search center,
     * allowing the more expensive Haversine calculation (which occurs later
     * in {@link com.streetvendor.discovery.service.DiscoveryService}) to
     * operate on a much smaller result set.
     * </p>
     *
     * @param status       the vendor status to filter by (expected to be {@link VendorStatus#APPROVED})
     * @param minLatitude  the minimum latitude of the bounding box
     * @param maxLatitude  the maximum latitude of the bounding box
     * @param minLongitude the minimum longitude of the bounding box
     * @param maxLongitude the maximum longitude of the bounding box
     * @param pageable     pagination information
     * @return a page of vendors whose coordinates are within the bounding box and status is APPROVED
     */
    Page<Vendor> findByStatusAndLatitudeBetweenAndLongitudeBetween(
            VendorStatus status,
            BigDecimal minLatitude,
            BigDecimal maxLatitude,
            BigDecimal minLongitude,
            BigDecimal maxLongitude,
            Pageable pageable
    );
}