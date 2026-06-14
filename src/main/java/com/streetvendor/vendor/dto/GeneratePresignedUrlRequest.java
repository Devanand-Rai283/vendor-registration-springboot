package com.streetvendor.vendor.dto;

import com.streetvendor.vendor.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GeneratePresignedUrlRequest(
        @NotNull(message = "File type is required")
        DocumentType fileType,
        @NotBlank(message = "MIME type is required")
        String mimeType,
        @NotNull(message = "File size is required")
        @Positive(message = "File size must be positive")
        Long fileSizeBytes
) {
}
