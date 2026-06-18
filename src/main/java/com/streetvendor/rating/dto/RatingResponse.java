package com.streetvendor.rating.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Response payload representing a rating details")
public record RatingResponse(
        @Schema(description = "Unique identifier of the rating", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Unique identifier of the rated order", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID orderId,

        @Schema(description = "Unique identifier of the customer who made the rating", example = "d3b07384-d113-4c9e-a140-5b1234567890")
        UUID customerId,

        @Schema(description = "Unique identifier of the vendor who received the rating", example = "efb3b1cd-40a2-4aef-84ab-c3d38e2ea5df")
        UUID vendorId,

        @Schema(description = "Stars rating value (1 to 5)", example = "5")
        Integer stars,

        @Schema(description = "Text review comments", example = "Excellent food and service!")
        String reviewText,

        @Schema(description = "Timestamp when the rating was created", example = "2026-06-19T02:40:00Z")
        Instant createdAt
) {}
