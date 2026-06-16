package com.streetvendor.discovery.dto;

import java.math.BigDecimal;

/**
 * A value object representing a geographic bounding box.
 * <p>
 * Defines a rectangular region on the Earth's surface using minimum and
 * maximum latitude/longitude values. Used as the output of
 * {@link com.streetvendor.discovery.util.BoundingBoxCalculator} and
 * the input to bounding-box repository queries. Values are stored as
 * {@link BigDecimal} to match the precision expected by JPA repository
 * query methods.
 * </p>
 *
 * @param minLatitude  the southern boundary of the bounding box in decimal degrees
 * @param maxLatitude  the northern boundary of the bounding box in decimal degrees
 * @param minLongitude the western boundary of the bounding box in decimal degrees
 * @param maxLongitude the eastern boundary of the bounding box in decimal degrees
 */
public record BoundingBox(
        BigDecimal minLatitude,
        BigDecimal maxLatitude,
        BigDecimal minLongitude,
        BigDecimal maxLongitude
) {
}
