package com.streetvendor.vendor.service;

import com.streetvendor.vendor.dto.CreateVendorRequest;
import com.streetvendor.vendor.dto.UpdateVendorProfileRequest;
import com.streetvendor.vendor.dto.VendorProfileResponseDto;
import com.streetvendor.vendor.dto.VendorResponse;
import com.streetvendor.vendor.dto.VendorStatusResponse;

import java.util.UUID;

public interface VendorService {
    VendorResponse createVendor(CreateVendorRequest request);

    VendorStatusResponse getMyVendorStatus();

    VendorProfileResponseDto getMyVendorProfile();

    VendorProfileResponseDto updateMyVendorProfile(UpdateVendorProfileRequest request);

    VendorResponse approveVendor(UUID vendorId);

    VendorResponse rejectVendor(UUID vendorId, String reason);

    com.streetvendor.vendor.entity.Vendor getVendorByUserId(UUID userId);
}