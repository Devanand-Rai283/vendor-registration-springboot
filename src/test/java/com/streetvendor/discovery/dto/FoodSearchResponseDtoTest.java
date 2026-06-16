package com.streetvendor.discovery.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FoodSearchResponseDtoTest {

    @Test
    void shouldCreateDtoWithAllFields() {
        UUID menuItemId = UUID.randomUUID();
        String itemName = "Chicken Tacos";
        String description = "Spicy chicken tacos with salsa";
        BigDecimal price = new BigDecimal("12.99");
        String dietaryTag = "non-veg";
        UUID vendorId = UUID.randomUUID();
        String vendorName = "Maria's Tacos";
        String foodType = "Mexican";
        BigDecimal averageRating = new BigDecimal("4.5");

        var dto = new FoodSearchResponseDto(
                menuItemId, itemName, description, price, dietaryTag,
                vendorId, vendorName, foodType, averageRating
        );

        assertThat(dto.menuItemId()).isEqualTo(menuItemId);
        assertThat(dto.itemName()).isEqualTo(itemName);
        assertThat(dto.description()).isEqualTo(description);
        assertThat(dto.price()).isEqualTo(price);
        assertThat(dto.dietaryTag()).isEqualTo(dietaryTag);
        assertThat(dto.vendorId()).isEqualTo(vendorId);
        assertThat(dto.vendorName()).isEqualTo(vendorName);
        assertThat(dto.foodType()).isEqualTo(foodType);
        assertThat(dto.averageRating()).isEqualTo(averageRating);
    }

    @Test
    void shouldBeEqualWhenSameValues() {
        UUID menuItemId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        var price = new BigDecimal("12.99");
        var rating = new BigDecimal("4.5");

        var dto1 = new FoodSearchResponseDto(
                menuItemId, "Taco", "Spicy", price, "non-veg",
                vendorId, "Maria's", "Mexican", rating);
        var dto2 = new FoodSearchResponseDto(
                menuItemId, "Taco", "Spicy", price, "non-veg",
                vendorId, "Maria's", "Mexican", rating);

        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenDifferentValues() {
        var price = new BigDecimal("12.99");
        var rating = new BigDecimal("4.5");

        var dto1 = new FoodSearchResponseDto(
                UUID.randomUUID(), "Taco", "Spicy", price, "non-veg",
                UUID.randomUUID(), "Maria's", "Mexican", rating);
        var dto2 = new FoodSearchResponseDto(
                UUID.randomUUID(), "Burger", "Grilled", price, "non-veg",
                UUID.randomUUID(), "Bob's", "American", rating);

        assertThat(dto1).isNotEqualTo(dto2);
    }

    @Test
    void shouldReturnMeaningfulToString() {
        var dto = new FoodSearchResponseDto(
                UUID.randomUUID(), "Taco", "Spicy taco", new BigDecimal("12.99"), "non-veg",
                UUID.randomUUID(), "Maria's", "Mexican", new BigDecimal("4.5"));

        String str = dto.toString();

        assertThat(str).contains("Taco");
        assertThat(str).contains("Spicy taco");
        assertThat(str).contains("Maria's");
        assertThat(str).contains("Mexican");
        assertThat(str).contains("menuItemId");
        assertThat(str).contains("itemName");
        assertThat(str).contains("vendorName");
    }
}
