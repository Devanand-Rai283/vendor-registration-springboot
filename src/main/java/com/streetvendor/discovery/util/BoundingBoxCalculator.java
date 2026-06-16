package com.streetvendor.discovery.util;

import com.streetvendor.discovery.dto.BoundingBox;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility for computing a bounding box around a geographic center point.
 * <p>
 * Bounding-box filtering is a coarse pre-filter step that reduces the
 * candidate vendor set before the more accurate (and computationally
 * expensive) Haversine distance calculation. It works by computing a
 * rectangular region around the search center using the following
 * approximations:
 * </p>
 * <ul>
 *   <li>One degree of latitude ≈ 111 km (constant)</li>
 *   <li>One degree of longitude ≈ 111 km × cos(latitude) (varies with latitude)</li>
 * </ul>
 * <p>
 * <strong>Why bounding-box first:</strong> The bounding box uses simple
 * numeric comparisons ({@code BETWEEN}) that can leverage database indexes
 * on the {@code latitude} and {@code longitude} columns. This efficiently
 * eliminates vendors clearly outside the search area.
 * </p>
 * <p>
 * <strong>Why Haversine is still required:</strong> The bounding box is a
 * rectangle on a flat map projection, not a true circle on the Earth's
 * curved surface. Vendors near the corners of the box may be farther from
 * the center than the specified radius. The Haversine formula calculates
 * the great-circle distance accurately and is applied afterward to filter
 * out false positives from the bounding box.
 * </p>
 */
public final class BoundingBoxCalculator {

    private static final double KM_PER_DEGREE_LAT = 111.0;

    private BoundingBoxCalculator() {
    }

    /**
     * Computes a bounding box centered at the given geographic coordinates
     * with the specified radius.
     *
     * @param latitude  the latitude of the center point in decimal degrees
     * @param longitude the longitude of the center point in decimal degrees
     * @param radiusKm  the search radius in kilometers
     * @return a {@link BoundingBox} with clamped latitude/longitude values
     */
    public static BoundingBox calculate(double latitude, double longitude, double radiusKm) {
        double effectiveRadius = Math.max(0.0, radiusKm);
        double deltaLat = effectiveRadius / KM_PER_DEGREE_LAT;

        double latRad = Math.toRadians(latitude);
        double deltaLng = effectiveRadius / (KM_PER_DEGREE_LAT * Math.cos(latRad));

        double minLat = clampLatitude(latitude - deltaLat);
        double maxLat = clampLatitude(latitude + deltaLat);
        double minLng = clampLongitude(longitude - deltaLng);
        double maxLng = clampLongitude(longitude + deltaLng);

        return new BoundingBox(
                BigDecimal.valueOf(minLat).setScale(8, RoundingMode.HALF_UP),
                BigDecimal.valueOf(maxLat).setScale(8, RoundingMode.HALF_UP),
                BigDecimal.valueOf(minLng).setScale(8, RoundingMode.HALF_UP),
                BigDecimal.valueOf(maxLng).setScale(8, RoundingMode.HALF_UP)
        );
    }

    private static double clampLatitude(double value) {
        return Math.max(-90.0, Math.min(90.0, value));
    }

    private static double clampLongitude(double value) {
        return Math.max(-180.0, Math.min(180.0, value));
    }
}
