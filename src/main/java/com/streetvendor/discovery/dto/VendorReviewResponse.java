package com.streetvendor.discovery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Public response payload representing a customer review for a vendor")
public record VendorReviewResponse(
        @Schema(description = "Unique identifier of the rating", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Stars rating value (1 to 5)", example = "5")
        Integer stars,

        @Schema(description = "Text review comments", example = "Excellent food and service!")
        String reviewText,

        @Schema(description = "Display name of the customer who left the review", example = "John Doe")
        String customerDisplayName,

        @Schema(description = "Timestamp when the rating was created", example = "2026-06-19T02:40:00Z")
        Instant createdAt
) {}
