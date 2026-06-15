package com.streetvendor.menu.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public class UpdateMenuItemRequest {

    @NotNull(message = "Category id is required")
    private UUID categoryId;

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", message = "Price must be non-negative")
    private BigDecimal price;

    @Size(max = 100, message = "Dietary tag must not exceed 100 characters")
    private String dietaryTag;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    @NotNull(message = "Availability is required")
    private Boolean available;

    public UpdateMenuItemRequest() {
    }

    public UpdateMenuItemRequest(UUID categoryId, String name, String description, BigDecimal price,
                                 String dietaryTag, String imageUrl, Boolean available) {
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.dietaryTag = dietaryTag;
        this.imageUrl = imageUrl;
        this.available = available;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getDietaryTag() {
        return dietaryTag;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Boolean getAvailable() {
        return available;
    }
}
