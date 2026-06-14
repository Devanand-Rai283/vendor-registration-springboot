package com.streetvendor.menu.dto.response;

import java.time.Instant;
import java.util.UUID;

public class MenuCategoryResponse {

    private UUID id;
    private String name;
    private Integer displayOrder;
    private Instant createdAt;

    public MenuCategoryResponse() {
    }

    public MenuCategoryResponse(UUID id, String name, Integer displayOrder, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
