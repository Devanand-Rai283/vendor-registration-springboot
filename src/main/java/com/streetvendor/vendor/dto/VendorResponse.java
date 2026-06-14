package com.streetvendor.vendor.dto;

import com.streetvendor.vendor.enums.VendorStatus;

import java.util.UUID;

public record VendorResponse(
        UUID vendorId,
        VendorStatus status,
        String message
) {
}