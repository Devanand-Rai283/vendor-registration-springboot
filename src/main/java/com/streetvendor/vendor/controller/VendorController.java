package com.streetvendor.vendor.controller;

import com.streetvendor.common.response.ApiResponse;
import com.streetvendor.discovery.dto.VendorMenuResponseDto;
import com.streetvendor.discovery.service.DiscoveryService;
import com.streetvendor.vendor.dto.CreateVendorRequest;
import com.streetvendor.vendor.dto.VendorResponse;
import com.streetvendor.vendor.dto.VendorStatusResponse;
import com.streetvendor.vendor.service.VendorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/vendors")
public class VendorController {

    private final VendorService vendorService;
    private final DiscoveryService discoveryService;

    public VendorController(
            VendorService vendorService,
            DiscoveryService discoveryService) {
        this.vendorService = vendorService;
        this.discoveryService = discoveryService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VendorResponse>> createVendor(
            @Valid @RequestBody CreateVendorRequest request) {
        VendorResponse response = vendorService.createVendor(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response.message(), response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<VendorStatusResponse>> getMyVendorStatus() {
        VendorStatusResponse response = vendorService.getMyVendorStatus();
        return ResponseEntity.ok(ApiResponse.success("Vendor status retrieved", response));
    }

    @GetMapping("/{vendorId}/menu")
    public ResponseEntity<VendorMenuResponseDto> getVendorMenu(
            @PathVariable UUID vendorId) {

        VendorMenuResponseDto response = discoveryService.getVendorMenu(vendorId);

        return ResponseEntity.ok(response);
    }
}
