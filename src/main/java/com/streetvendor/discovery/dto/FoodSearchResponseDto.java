package com.streetvendor.discovery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Represents a food menu item matching the search criteria")
public record FoodSearchResponseDto(
        @Schema(description = "Unique identifier of the menu item", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID menuItemId,
        @Schema(description = "Name of the menu item", example = "Paneer Tikka")
        String itemName,
        @Schema(description = "Description of the menu item", example = "Grilled cottage cheese skewers")
        String description,
        @Schema(description = "Price of the menu item", example = "180.00")
        BigDecimal price,
        @Schema(description = "Dietary tag of the menu item", example = "VEGETARIAN")
        String dietaryTag,
        @Schema(description = "Unique identifier of the vendor offering this item", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID vendorId,
        @Schema(description = "Business name of the vendor", example = "Sharma Foods")
        String vendorName,
        @Schema(description = "Food type served by the vendor", example = "VEGETARIAN")
        String foodType,
        @Schema(description = "Average rating of the vendor", example = "4.5")
        BigDecimal averageRating) {
}
