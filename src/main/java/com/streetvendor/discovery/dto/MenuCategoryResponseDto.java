package com.streetvendor.discovery.dto;

import java.util.List;
import java.util.UUID;

/**
 * API response DTO representing a menu category for a vendor.
 * <p>
 * Contains only boundary fields required by the vendor menu viewing API.
 * </p>
 *
 * @param id           the menu category identifier
 * @param name         the menu category name
 * @param displayOrder the ordering position of the category in the menu
 * @param items        the list of menu items available in this category
 */
public record MenuCategoryResponseDto(
        UUID id,
        String name,
        Integer displayOrder,
        List<MenuItemResponseDto> items) {
}
