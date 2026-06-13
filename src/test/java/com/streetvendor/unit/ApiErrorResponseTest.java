package com.streetvendor.unit;

import com.streetvendor.common.response.ApiErrorResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ApiErrorResponseTest {

    @Test
    void shouldCreateErrorResponseWithAllFields() {
        int expectedStatus = 404;
        String expectedMessage = "Resource not found.";
        String expectedPath = "/api/vendors/123";

        ApiErrorResponse response = new ApiErrorResponse(expectedStatus, expectedMessage, expectedPath);

        assertEquals(expectedStatus, response.getStatus());
        assertEquals(expectedMessage, response.getMessage());
        assertEquals(expectedPath, response.getPath());
    }

    @Test
    void shouldPopulateTimestampAutomatically() {
        Instant before = Instant.now();

        ApiErrorResponse response = new ApiErrorResponse(400, "Bad request", "/api/test");

        assertNotNull(response.getTimestamp());
        assertTrue(response.getTimestamp().compareTo(before) >= 0);
    }

    @Test
    void shouldPreservePathCorrectly() {
        String path = "/api/vendors/999";
        ApiErrorResponse response = new ApiErrorResponse(500, "Server error", path);

        assertEquals(path, response.getPath());
    }

    @Test
    void shouldHandleDifferentStatusCodes() {
        int[] statusCodes = { 400, 401, 403, 404, 409, 422, 500, 503 };

        for (int status : statusCodes) {
            ApiErrorResponse response = new ApiErrorResponse(status, "Error", "/api/test");
            assertEquals(status, response.getStatus());
        }
    }

    @Test
    void shouldHandleDifferentMessages() {
        ApiErrorResponse response1 = new ApiErrorResponse(404, "Not found", "/api/resource");
        assertEquals("Not found", response1.getMessage());

        ApiErrorResponse response2 = new ApiErrorResponse(400, "Validation failed", "/api/create");
        assertEquals("Validation failed", response2.getMessage());

        ApiErrorResponse response3 = new ApiErrorResponse(500, "Internal error", "/api/process");
        assertEquals("Internal error", response3.getMessage());
    }
}
