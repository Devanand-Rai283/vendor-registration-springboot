package com.streetvendor.vendor.controller;

import com.streetvendor.auth.entity.User;
import com.streetvendor.common.exception.UnauthorizedException;
import com.streetvendor.common.response.ApiResponse;
import com.streetvendor.vendor.dto.VendorDocumentResponseDto;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.service.DocumentUploadService;
import com.streetvendor.vendor.service.VendorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vendors/documents")
public class VendorDocumentController {

    private final DocumentUploadService documentUploadService;
    private final VendorService vendorService;

    public VendorDocumentController(DocumentUploadService documentUploadService, VendorService vendorService) {
        this.documentUploadService = documentUploadService;
        this.vendorService = vendorService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VendorDocumentResponseDto>>> getDocuments(
            @AuthenticationPrincipal User user) {
        
        if (user == null) {
            throw new UnauthorizedException("Not authenticated");
        }

        Vendor vendor = vendorService.getVendorByUserId(user.getId());
        List<VendorDocumentResponseDto> documents = documentUploadService.getDocuments(vendor.getId());

        return ResponseEntity.ok(ApiResponse.success("Documents retrieved successfully", documents));
    }
}
