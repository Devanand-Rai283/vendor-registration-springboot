package com.streetvendor.menu.dto.request;

import jakarta.validation.constraints.NotNull;

public class UpdateMenuItemAvailabilityRequest {

    @NotNull(message = "Availability is required")
    private Boolean available;

    public UpdateMenuItemAvailabilityRequest() {
    }

    public UpdateMenuItemAvailabilityRequest(Boolean available) {
        this.available = available;
    }

    public Boolean getAvailable() {
        return available;
    }
}
