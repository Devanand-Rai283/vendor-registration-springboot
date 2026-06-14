package com.streetvendor.vendor.service;

import com.streetvendor.vendor.dto.CreateVendorRequest;
import com.streetvendor.vendor.dto.VendorResponse;
import com.streetvendor.vendor.dto.VendorStatusResponse;

import java.util.UUID;

public interface VendorService {
    VendorResponse createVendor(CreateVendorRequest request);

    VendorStatusResponse getMyVendorStatus();

    VendorResponse approveVendor(UUID vendorId);

    VendorResponse rejectVendor(UUID vendorId, String reason);
}