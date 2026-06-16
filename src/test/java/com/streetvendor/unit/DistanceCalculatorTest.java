package com.streetvendor.unit;

import com.streetvendor.discovery.util.DistanceCalculator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistanceCalculatorTest {

    private static final double DELTA = 1.0;

    @Test
    void shouldReturnZeroForSamePoint() {
        double distance = DistanceCalculator.calculateDistanceKm(48.8566, 2.3522, 48.8566, 2.3522);
        assertEquals(0.0, distance, 1e-10);
    }

    @Test
    void shouldCalculateDistanceOnEquator() {
        double distance = DistanceCalculator.calculateDistanceKm(0.0, 0.0, 0.0, 1.0);
        assertEquals(111.19, distance, 0.5);
    }

    @Test
    void shouldCalculateDistanceBetweenPoleAndEquator() {
        double distance = DistanceCalculator.calculateDistanceKm(90.0, 0.0, 0.0, 0.0);
        assertEquals(10007.0, distance, 10.0);
    }

    @Test
    void shouldCalculateKnownCityDistance() {
        double distance = DistanceCalculator.calculateDistanceKm(48.8566, 2.3522, 51.5074, -0.1278);
        assertEquals(344.0, distance, 10.0);
    }

    @Test
    void shouldReturnSmallNonZeroDistance() {
        double distance = DistanceCalculator.calculateDistanceKm(48.8566, 2.3522, 48.8567, 2.3522);
        assertTrue(distance > 0.0);
        assertTrue(distance < 0.1);
    }

    @Test
    void shouldHandleNegativeLatitude() {
        double distance = DistanceCalculator.calculateDistanceKm(-33.8688, 151.2093, -33.8698, 151.2103);
        assertTrue(distance > 0.0);
        assertTrue(distance < 0.2);
    }

    @Test
    void shouldHandleNegativeLongitude() {
        double distance = DistanceCalculator.calculateDistanceKm(40.7128, -74.0060, 40.7138, -74.0070);
        assertTrue(distance > 0.0);
        assertTrue(distance < 0.2);
    }

    @Test
    void shouldBeSymmetric() {
        double d1 = DistanceCalculator.calculateDistanceKm(48.8566, 2.3522, 51.5074, -0.1278);
        double d2 = DistanceCalculator.calculateDistanceKm(51.5074, -0.1278, 48.8566, 2.3522);
        assertEquals(d1, d2, 1e-10);
    }

    @Test
    void shouldHandleAntipodalPoints() {
        double distance = DistanceCalculator.calculateDistanceKm(0.0, 0.0, 0.0, 180.0);
        assertEquals(20015.0, distance, 15.0);
    }

    @Test
    void shouldHandlePointsOnSameLongitude() {
        double distance = DistanceCalculator.calculateDistanceKm(0.0, 10.0, 1.0, 10.0);
        assertEquals(111.19, distance, 0.5);
    }

    @Test
    void shouldNotBeNegative() {
        double distance = DistanceCalculator.calculateDistanceKm(-90.0, -180.0, 90.0, 180.0);
        assertTrue(distance >= 0.0);
    }

    @Test
    void shouldHandlePointsAtNorthPole() {
        double distance = DistanceCalculator.calculateDistanceKm(90.0, 0.0, 90.0, 45.0);
        assertEquals(0.0, distance, 1e-10);
    }

    @Test
    void shouldCalculateCrossEquatorDistance() {
        double distance = DistanceCalculator.calculateDistanceKm(-30.0, 0.0, 30.0, 0.0);
        assertEquals(6672.0, distance, 10.0);
    }
}
