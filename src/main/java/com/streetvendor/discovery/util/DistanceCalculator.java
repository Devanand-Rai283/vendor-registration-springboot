package com.streetvendor.discovery.util;

/**
 * Utility for calculating great-circle distances between geographic coordinates.
 * <p>
 * Uses the Haversine formula to compute the distance between two points on
 * the Earth's surface. The Earth is approximated as a sphere with radius
 * 6371 km, which provides sufficient accuracy for the discovery module's
 * nearby vendor search use case. The Haversine formula was chosen over the
 * Vincenty formula because it is computationally simpler, numerically stable
 * for all distances, and does not require iterative refinement — making it
 * well-suited for filtering a large result set in the service layer.
 * </p>
 * <h3>Why Haversine is necessary after bounding-box filtering</h3>
 * <p>
 * The {@link BoundingBoxCalculator bounding-box pre-filter} efficiently
 * narrows the candidate set using SQL {@code BETWEEN} comparisons, but it
 * produces a rectangular region on a flat map projection. This introduces
 * false positives near the corners of the box, where points are farther
 * from the search center than the specified radius. The Haversine formula
 * accurately measures the great-circle distance and is applied to each
 * bounding-box result to eliminate those false positives.
 * </p>
 * <p>
 * <strong>Limitation:</strong> This implementation assumes a spherical Earth.
 * The actual Earth is an oblate spheroid, so distances at extreme latitudes
 * or over very long baselines may differ by up to ~0.5 % from the true
 * ellipsoidal distance. This error is acceptable for the nearby vendor
 * discovery use case (typical radius ≤ 50 km).
 * </p>
 */
public final class DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private DistanceCalculator() {
    }

    /**
     * Calculates the great-circle distance in kilometers between two
     * geographic points using the Haversine formula.
     *
     * @param lat1 the latitude of the first point in decimal degrees
     * @param lon1 the longitude of the first point in decimal degrees
     * @param lat2 the latitude of the second point in decimal degrees
     * @param lon2 the longitude of the second point in decimal degrees
     * @return the distance between the points in kilometers
     */
    public static double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double sinHalfDeltaLat = Math.sin(deltaLat / 2.0);
        double sinHalfDeltaLon = Math.sin(deltaLon / 2.0);

        double a = sinHalfDeltaLat * sinHalfDeltaLat
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * sinHalfDeltaLon * sinHalfDeltaLon;

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        return EARTH_RADIUS_KM * c;
    }
}
