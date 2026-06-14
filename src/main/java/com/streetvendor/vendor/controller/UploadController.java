package com.streetvendor.vendor.controller;

import com.streetvendor.vendor.dto.GeneratePresignedUrlRequest;
import com.streetvendor.vendor.dto.GeneratePresignedUrlResponse;
import com.streetvendor.vendor.service.DocumentUploadService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final DocumentUploadService documentUploadService;

    public UploadController(DocumentUploadService documentUploadService) {
        this.documentUploadService = documentUploadService;
    }

    @PostMapping("/presigned-url")
    public ResponseEntity<GeneratePresignedUrlResponse> generatePresignedUrl(
            @Valid @RequestBody GeneratePresignedUrlRequest request) {
        GeneratePresignedUrlResponse response = documentUploadService.generatePresignedUrl(request);
        return ResponseEntity.ok(response);
    }
}
