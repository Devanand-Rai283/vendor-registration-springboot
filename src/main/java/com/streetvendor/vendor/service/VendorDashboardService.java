package com.streetvendor.vendor.service;

import com.streetvendor.vendor.dto.VendorDashboardMetricsResponseDto;

import java.util.UUID;

public interface VendorDashboardService {
    VendorDashboardMetricsResponseDto getDashboardMetrics(UUID vendorId);
}
