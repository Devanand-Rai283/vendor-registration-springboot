package com.streetvendor.admin.dto;

import com.streetvendor.vendor.enums.DocumentType;
import com.streetvendor.vendor.enums.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing an uploaded vendor document for administrative views.
 */
@Schema(description = "Details of a vendor's uploaded verification document")
public record AdminVendorDocumentResponseDto(

        @Schema(description = "Unique ID of the document record", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Type of document uploaded", example = "FSSAI_CERTIFICATE")
        DocumentType documentType,

        @Schema(description = "Current verification status of this specific document", example = "PENDING")
        VerificationStatus status,

        @Schema(description = "Secure URL to view the document file", example = "https://bucket.s3.region.amazonaws.com/...")
        String fileUrl,

        @Schema(description = "Timestamp when the document was uploaded")
        Instant uploadedAt,

        @Schema(description = "Reason for rejection if the document was rejected", example = "Image is too blurry to read")
        String rejectionReason
) {
}
