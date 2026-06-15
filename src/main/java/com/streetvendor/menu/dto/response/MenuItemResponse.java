package com.streetvendor.menu.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class MenuItemResponse {

    private final UUID id;
    private final UUID categoryId;
    private final UUID vendorId;
    private final String name;
    private final String description;
    private final BigDecimal price;
    private final String dietaryTag;
    private final String imageUrl;
    private final boolean available;
    private final Instant createdAt;
    private final Instant updatedAt;

    public MenuItemResponse(UUID id, UUID categoryId, UUID vendorId, String name, String description,
                            BigDecimal price, String dietaryTag, String imageUrl, boolean available,
                            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.categoryId = categoryId;
        this.vendorId = vendorId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.dietaryTag = dietaryTag;
        this.imageUrl = imageUrl;
        this.available = available;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getCategoryId() { return categoryId; }
    public UUID getVendorId() { return vendorId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public String getDietaryTag() { return dietaryTag; }
    public String getImageUrl() { return imageUrl; }
    public boolean isAvailable() { return available; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
