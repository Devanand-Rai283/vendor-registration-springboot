package com.streetvendor.vendor.service;

import com.streetvendor.auth.entity.User;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.common.exception.UnauthorizedException;
import com.streetvendor.config.R2Properties;
import com.streetvendor.vendor.dto.GeneratePresignedUrlRequest;
import com.streetvendor.vendor.dto.GeneratePresignedUrlResponse;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.entity.VendorDocument;
import com.streetvendor.vendor.enums.VerificationStatus;
import com.streetvendor.vendor.repository.VendorDocumentRepository;
import com.streetvendor.vendor.repository.VendorRepository;
import com.streetvendor.vendor.validation.DocumentUploadValidator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class DocumentUploadService {

    private final VendorRepository vendorRepository;
    private final VendorDocumentRepository vendorDocumentRepository;
    private final DocumentUploadValidator validator;
    private final S3Presigner s3Presigner;
    private final R2Properties r2Properties;

    public DocumentUploadService(VendorRepository vendorRepository,
                                 VendorDocumentRepository vendorDocumentRepository,
                                 DocumentUploadValidator validator,
                                 S3Presigner s3Presigner,
                                 R2Properties r2Properties) {
        this.vendorRepository = vendorRepository;
        this.vendorDocumentRepository = vendorDocumentRepository;
        this.validator = validator;
        this.s3Presigner = s3Presigner;
        this.r2Properties = r2Properties;
    }

    @Transactional
    public GeneratePresignedUrlResponse generatePresignedUrl(GeneratePresignedUrlRequest request) {
        User user = getAuthenticatedUser();

        Vendor vendor = vendorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ForbiddenException("Vendor profile not found"));

        validator.validate(request.fileType(), request.mimeType(), request.fileSizeBytes());

        String objectKey = String.format("vendors/%s/documents/%s/%s",
                vendor.getId(), request.fileType().name().toLowerCase(), UUID.randomUUID());

        String uploadUrl = generatePresignedPutUrl(objectKey, request.mimeType());

        String fileUrl = objectKey;

        VendorDocument vendorDocument = vendorDocumentRepository
                .findByVendorIdAndDocumentType(vendor.getId(), request.fileType())
                .map(existing -> {
                    existing.setFileUrl(fileUrl);
                    existing.setVerificationStatus(VerificationStatus.PENDING);
                    existing.setVerifiedAt(null);
                    existing.setVerifiedBy(null);
                    return existing;
                })
                .orElseGet(() -> new VendorDocument(
                        UUID.randomUUID(),
                        vendor,
                        request.fileType(),
                        fileUrl,
                        VerificationStatus.PENDING,
                        Instant.now()
                ));

        vendorDocumentRepository.save(vendorDocument);

        return new GeneratePresignedUrlResponse(uploadUrl, fileUrl);
    }

    private String generatePresignedPutUrl(String objectKey, String contentType) {
        if (s3Presigner == null) {
            throw new RuntimeException("Document upload presigner is not configured");
        }
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(r2Properties.getBucketName())
                .key(objectKey)
                .contentType(contentType)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(p -> p
                .putObjectRequest(putObjectRequest)
                .signatureDuration(Duration.ofMinutes(15)));

        return presignedRequest.url().toString();
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            throw new UnauthorizedException("Invalid authentication");
        }

        return (User) principal;
    }
}
