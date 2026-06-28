package com.streetvendor.vendor.validation;

import com.streetvendor.vendor.enums.DocumentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DocumentUploadValidator {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png"
    );

    private static final String PDF_MIME_TYPE = "application/pdf";

    private final long maxPdfSizeBytes;
    private final long maxImageSizeBytes;

    public DocumentUploadValidator() {
        this(5L, 2L);
    }

    public DocumentUploadValidator(
            @Value("${security.max-file-size-pdf-mb:5}") long maxPdfSizeMb,
            @Value("${security.max-file-size-image-mb:2}") long maxImageSizeMb) {
        this.maxPdfSizeBytes = maxPdfSizeMb * 1024 * 1024;
        this.maxImageSizeBytes = maxImageSizeMb * 1024 * 1024;
    }

    public void validate(DocumentType fileType, String mimeType, Long fileSizeBytes) {
        if (fileType == null) {
            throw new IllegalArgumentException("File type is required");
        }

        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("MIME type is required");
        }

        if (fileSizeBytes == null) {
            throw new IllegalArgumentException("File size is required");
        }

        if (fileSizeBytes <= 0) {
            throw new IllegalArgumentException("File cannot be empty.");
        }

        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException("Unsupported file type. Allowed types: PDF, JPG, JPEG, PNG.");
        }

        if (PDF_MIME_TYPE.equals(mimeType) && fileSizeBytes > maxPdfSizeBytes) {
            throw new IllegalArgumentException("PDF documents cannot exceed " + (maxPdfSizeBytes / (1024 * 1024)) + " MB.");
        }

        if (!PDF_MIME_TYPE.equals(mimeType) && fileSizeBytes > maxImageSizeBytes) {
            throw new IllegalArgumentException("Images cannot exceed " + (maxImageSizeBytes / (1024 * 1024)) + " MB.");
        }
    }
}
