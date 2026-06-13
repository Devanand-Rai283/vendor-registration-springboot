package com.streetvendor.unit;

import com.streetvendor.common.response.ApiResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void shouldCreateResponseWithData() {
        String expectedMessage = "Vendor created successfully.";
        String expectedData = "vendor-123";

        ApiResponse<String> response = ApiResponse.success(expectedMessage, expectedData);

        assertTrue(response.isSuccess());
        assertEquals(expectedMessage, response.getMessage());
        assertEquals(expectedData, response.getData());
    }

    @Test
    void shouldCreateResponseWithoutData() {
        String expectedMessage = "Operation completed.";

        ApiResponse<String> response = ApiResponse.success(expectedMessage);

        assertTrue(response.isSuccess());
        assertEquals(expectedMessage, response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void shouldPopulateTimestampOnCreation() {
        Instant before = Instant.now();

        ApiResponse<String> response = ApiResponse.success("Done", "data");

        assertNotNull(response.getTimestamp());
        assertTrue(response.getTimestamp().compareTo(before) >= 0);
    }

    @Test
    void shouldSupportGenericType() {
        ApiResponse<Integer> intResponse = ApiResponse.success("Count", 42);
        assertEquals(Integer.valueOf(42), intResponse.getData());

        ApiResponse<Double> doubleResponse = ApiResponse.success("Value", 3.14);
        assertEquals(Double.valueOf(3.14), doubleResponse.getData());

        ApiResponse<String> stringResponse = ApiResponse.success("Name", "Alice");
        assertEquals("Alice", stringResponse.getData());
    }
}
