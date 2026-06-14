package com.streetvendor.vendor.controller;

import com.streetvendor.common.response.ApiResponse;
import com.streetvendor.vendor.dto.RejectVendorRequest;
import com.streetvendor.vendor.dto.VendorResponse;
import com.streetvendor.vendor.service.VendorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/vendors")
public class AdminVendorController {

    private final VendorService vendorService;

    public AdminVendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<VendorResponse>> approveVendor(@PathVariable UUID id) {
        VendorResponse response = vendorService.approveVendor(id);
        return ResponseEntity.ok(ApiResponse.success(response.message(), response));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<VendorResponse>> rejectVendor(
            @PathVariable UUID id,
            @Valid @RequestBody RejectVendorRequest request) {
        VendorResponse response = vendorService.rejectVendor(id, request.reason());
        return ResponseEntity.ok(ApiResponse.success(response.message(), response));
    }
}
