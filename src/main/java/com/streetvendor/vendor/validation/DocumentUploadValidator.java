package com.streetvendor.vendor.validation;

import com.streetvendor.vendor.enums.DocumentType;
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

    private static final long MAX_PDF_SIZE_BYTES = 5 * 1024 * 1024;
    private static final long MAX_IMAGE_SIZE_BYTES = 2 * 1024 * 1024;

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

        if (PDF_MIME_TYPE.equals(mimeType) && fileSizeBytes > MAX_PDF_SIZE_BYTES) {
            throw new IllegalArgumentException("PDF documents cannot exceed 5 MB.");
        }

        if (!PDF_MIME_TYPE.equals(mimeType) && fileSizeBytes > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("Images cannot exceed 2 MB.");
        }
    }
}
