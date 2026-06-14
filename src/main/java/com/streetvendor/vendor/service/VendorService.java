package com.streetvendor.vendor.service;

import com.streetvendor.vendor.dto.CreateVendorRequest;
import com.streetvendor.vendor.dto.VendorResponse;

public interface VendorService {
    VendorResponse createVendor(CreateVendorRequest request);
}