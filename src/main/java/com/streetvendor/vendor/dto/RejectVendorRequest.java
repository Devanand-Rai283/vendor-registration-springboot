package com.streetvendor.vendor.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectVendorRequest(
        @NotBlank(message = "Reason is required")
        String reason
) {
}
