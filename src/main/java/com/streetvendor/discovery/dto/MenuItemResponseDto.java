package com.streetvendor.discovery.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * API response DTO representing a single menu item in a vendor menu category.
 * <p>
 * Contains only boundary fields required by the vendor menu viewing API.
 * </p>
 *
 * @param id          the menu item identifier
 * @param name        the menu item name
 * @param description the menu item description
 * @param price       the menu item price
 * @param dietaryTag  the dietary tag (e.g., veg/non-veg) or similar
 *                    classification
 * @param imageUrl    the URL of the menu item's image
 * @param available   whether the item is currently available (is_available =
 *                    true)
 */
public record MenuItemResponseDto(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        String dietaryTag,
        String imageUrl,
        Boolean available) {
}
