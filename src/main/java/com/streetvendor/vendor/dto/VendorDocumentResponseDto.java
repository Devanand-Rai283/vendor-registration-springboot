package com.streetvendor.vendor.dto;

import com.streetvendor.vendor.enums.DocumentType;
import com.streetvendor.vendor.enums.VerificationStatus;
import java.time.Instant;
import java.util.UUID;

public record VendorDocumentResponseDto(
        UUID documentId,
        DocumentType documentType,
        VerificationStatus verificationStatus,
        Instant uploadedAt,
        String viewUrl
) {}
