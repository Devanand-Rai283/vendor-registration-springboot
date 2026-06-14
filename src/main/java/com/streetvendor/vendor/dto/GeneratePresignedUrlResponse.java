package com.streetvendor.vendor.dto;

import jakarta.validation.constraints.NotBlank;

public record GeneratePresignedUrlResponse(
        @NotBlank(message = "Upload URL is required")
        String uploadUrl,
        @NotBlank(message = "File URL is required")
        String fileUrl
) {
}
