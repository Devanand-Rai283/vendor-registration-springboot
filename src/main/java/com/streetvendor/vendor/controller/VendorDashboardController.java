package com.streetvendor.vendor.controller;

import com.streetvendor.auth.entity.User;
import com.streetvendor.common.exception.UnauthorizedException;
import com.streetvendor.common.response.ApiResponse;
import com.streetvendor.vendor.dto.VendorDashboardMetricsResponseDto;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.service.VendorDashboardService;
import com.streetvendor.vendor.service.VendorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vendors/dashboard")
public class VendorDashboardController {

    private final VendorDashboardService vendorDashboardService;
    private final VendorService vendorService;

    public VendorDashboardController(VendorDashboardService vendorDashboardService, VendorService vendorService) {
        this.vendorDashboardService = vendorDashboardService;
        this.vendorService = vendorService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<VendorDashboardMetricsResponseDto>> getDashboardMetrics(
            @AuthenticationPrincipal User user) {
        
        if (user == null) {
            throw new UnauthorizedException("Not authenticated");
        }

        Vendor vendor = vendorService.getVendorByUserId(user.getId());
        VendorDashboardMetricsResponseDto metrics = vendorDashboardService.getDashboardMetrics(vendor.getId());

        return ResponseEntity.ok(ApiResponse.success("Dashboard metrics retrieved successfully", metrics));
    }
}
