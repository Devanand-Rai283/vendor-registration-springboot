package com.streetvendor.rating.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Request body to rate and review an order")
public record CreateRatingRequest(
        @Schema(description = "Unique identifier of the order being reviewed", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "Order ID is required")
        UUID orderId,

        @Schema(description = "Stars rating for the order (1 to 5)", example = "5")
        @NotNull(message = "Stars rating is required")
        @Min(value = 1, message = "Rating must be at least 1 star")
        @Max(value = 5, message = "Rating cannot be more than 5 stars")
        Integer stars,

        @Schema(description = "Optional text review comment", example = "Excellent food and service!")
        String reviewText
) {}
