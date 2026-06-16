package com.streetvendor.unit;

import com.streetvendor.discovery.dto.BoundingBox;
import com.streetvendor.discovery.util.BoundingBoxCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BoundingBoxCalculatorTest {

    private static final double DELTA = 1e-8;

    @Test
    void shouldCalculateBoundingBoxAtEquator() {
        BoundingBox box = BoundingBoxCalculator.calculate(0.0, 0.0, 5.0);

        assertNotNull(box);
        assertEquals(-0.04504505, box.minLatitude().doubleValue(), DELTA);
        assertEquals(0.04504505, box.maxLatitude().doubleValue(), DELTA);
        assertEquals(-0.04504505, box.minLongitude().doubleValue(), DELTA);
        assertEquals(0.04504505, box.maxLongitude().doubleValue(), DELTA);
    }

    @Test
    void shouldCalculateBoundingBoxAtMidLatitude() {
        BoundingBox box = BoundingBoxCalculator.calculate(45.0, -73.0, 10.0);

        assertEquals(44.90990991, box.minLatitude().doubleValue(), DELTA);
        assertEquals(45.09009009, box.maxLatitude().doubleValue(), DELTA);

        double expectedDeltaLng = 10.0 / (111.0 * Math.cos(Math.toRadians(45.0)));
        assertEquals(-73.0 - expectedDeltaLng, box.minLongitude().doubleValue(), DELTA);
        assertEquals(-73.0 + expectedDeltaLng, box.maxLongitude().doubleValue(), DELTA);
    }

    @Test
    void shouldReturnCenterForZeroRadius() {
        BoundingBox box = BoundingBoxCalculator.calculate(12.9716, 77.5946, 0.0);

        assertEquals(12.9716, box.minLatitude().doubleValue(), DELTA);
        assertEquals(12.9716, box.maxLatitude().doubleValue(), DELTA);
        assertEquals(77.5946, box.minLongitude().doubleValue(), DELTA);
        assertEquals(77.5946, box.maxLongitude().doubleValue(), DELTA);
    }

    @Test
    void shouldClampLatitudeAtNorthPole() {
        BoundingBox box = BoundingBoxCalculator.calculate(90.0, 0.0, 100.0);

        assertEquals(89.09909910, box.minLatitude().doubleValue(), DELTA);
        assertEquals(90.0, box.maxLatitude().doubleValue(), DELTA);
    }

    @Test
    void shouldClampLatitudeAtSouthPole() {
        BoundingBox box = BoundingBoxCalculator.calculate(-90.0, 0.0, 100.0);

        assertEquals(-90.0, box.minLatitude().doubleValue(), DELTA);
        assertEquals(-89.09909910, box.maxLatitude().doubleValue(), DELTA);
    }

    @Test
    void shouldClampLatitudeNearNorthPole() {
        BoundingBox box = BoundingBoxCalculator.calculate(89.0, 0.0, 200.0);

        assertTrue(box.minLatitude().doubleValue() >= -90.0);
        assertTrue(box.maxLatitude().doubleValue() <= 90.0);
        assertEquals(90.0, box.maxLatitude().doubleValue(), DELTA);
    }

    @Test
    void shouldClampLongitudeAtDateLine() {
        BoundingBox box = BoundingBoxCalculator.calculate(0.0, 180.0, 50.0);

        assertTrue(box.minLongitude().doubleValue() >= -180.0);
        assertEquals(180.0, box.maxLongitude().doubleValue(), DELTA);
    }

    @Test
    void shouldClampLongitudeAtNegativeDateLine() {
        BoundingBox box = BoundingBoxCalculator.calculate(0.0, -180.0, 50.0);

        assertEquals(-180.0, box.minLongitude().doubleValue(), DELTA);
        assertTrue(box.maxLongitude().doubleValue() <= 180.0);
    }

    @Test
    void shouldReturnBigDecimalWithScaleEight() {
        BoundingBox box = BoundingBoxCalculator.calculate(12.9716, 77.5946, 5.0);

        assertEquals(8, box.minLatitude().scale());
        assertEquals(8, box.maxLatitude().scale());
        assertEquals(8, box.minLongitude().scale());
        assertEquals(8, box.maxLongitude().scale());
    }

    @Test
    void shouldHandleNegativeRadiusLikeZero() {
        BoundingBox box = BoundingBoxCalculator.calculate(10.0, 20.0, -5.0);

        assertEquals(10.0, box.minLatitude().doubleValue(), DELTA);
        assertEquals(10.0, box.maxLatitude().doubleValue(), DELTA);
    }

    @Test
    void shouldHandleLargeRadiusAcrossWholeGlobe() {
        BoundingBox box = BoundingBoxCalculator.calculate(0.0, 0.0, 20000.0);

        assertEquals(-90.0, box.minLatitude().doubleValue(), DELTA);
        assertEquals(90.0, box.maxLatitude().doubleValue(), DELTA);
        assertEquals(-180.0, box.minLongitude().doubleValue(), DELTA);
        assertEquals(180.0, box.maxLongitude().doubleValue(), DELTA);
    }

    @Test
    void shouldProduceMinLessThanMax() {
        BoundingBox box = BoundingBoxCalculator.calculate(-33.8688, 151.2093, 10.0);

        assertTrue(box.minLatitude().compareTo(box.maxLatitude()) < 0);
        assertTrue(box.minLongitude().compareTo(box.maxLongitude()) < 0);
    }
}
