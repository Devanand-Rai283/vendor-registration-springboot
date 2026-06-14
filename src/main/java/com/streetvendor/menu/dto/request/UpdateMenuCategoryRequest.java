package com.streetvendor.menu.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class UpdateMenuCategoryRequest {

    @NotBlank(message = "Category name is required")
    private String name;

    @NotNull(message = "Display order is required")
    @PositiveOrZero(message = "Display order must be zero or greater")
    private Integer displayOrder;

    public UpdateMenuCategoryRequest() {
    }

    public UpdateMenuCategoryRequest(String name, Integer displayOrder) {
        this.name = name;
        this.displayOrder = displayOrder;
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
}
