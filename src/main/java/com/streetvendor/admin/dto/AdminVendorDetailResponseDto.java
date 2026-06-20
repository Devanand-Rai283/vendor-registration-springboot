package com.streetvendor.admin.dto;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.vendor.enums.VendorStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO containing full administrative details for a single vendor, including linked user info and documents.
 */
@Schema(description = "Detailed administrative view of a vendor profile, including user status and verification documents")
public record AdminVendorDetailResponseDto(

        @Schema(description = "Unique ID of the vendor profile")
        UUID id,

        @Schema(description = "Name of the business", example = "Maria's Tacos")
        String businessName,

        @Schema(description = "Full name of the vendor owner", example = "Maria Garcia")
        String ownerName,

        @Schema(description = "Email address of the associated user account", example = "maria@example.com")
        String email,

        @Schema(description = "Contact phone number", example = "+1234567890")
        String phoneNumber,

        @Schema(description = "Business description")
        String description,

        @Schema(description = "Type of food served", example = "Mexican")
        String foodType,

        @Schema(description = "Current application status of the vendor profile", example = "PENDING_REVIEW")
        VendorStatus status,

        @Schema(description = "Current system account status of the linked user", example = "ACTIVE")
        AccountStatus accountStatus,

        @Schema(description = "Physical address of the vendor")
        String address,

        @Schema(description = "Geographic latitude", example = "12.9716")
        BigDecimal latitude,

        @Schema(description = "Geographic longitude", example = "77.5946")
        BigDecimal longitude,

        @Schema(description = "Average customer rating", example = "4.5")
        BigDecimal averageRating,

        @Schema(description = "Total number of completed reviews", example = "42")
        Integer totalReviews,

        @Schema(description = "Timestamp when the vendor profile was created")
        Instant createdAt,

        @Schema(description = "Timestamp when the vendor profile was last updated")
        Instant updatedAt,

        @Schema(description = "Reason for application rejection if status is REJECTED")
        String rejectionReason,

        @Schema(description = "List of uploaded verification documents")
        List<AdminVendorDocumentResponseDto> documents
) {
}
