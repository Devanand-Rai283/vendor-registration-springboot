package com.streetvendor.vendor;

import com.streetvendor.vendor.enums.DocumentType;
import com.streetvendor.vendor.validation.DocumentUploadValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentUploadValidatorTest {

    private DocumentUploadValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DocumentUploadValidator();
    }

    @Test
    void shouldAcceptValidPdf() {
        assertDoesNotThrow(() ->
                validator.validate(DocumentType.FSSAI_CERTIFICATE, "application/pdf", 1024L));
    }

    @Test
    void shouldAcceptValidJpg() {
        assertDoesNotThrow(() ->
                validator.validate(DocumentType.IDENTITY_PROOF, "image/jpg", 1024L));
    }

    @Test
    void shouldAcceptValidJpeg() {
        assertDoesNotThrow(() ->
                validator.validate(DocumentType.FOOD_IMAGE, "image/jpeg", 1024L));
    }

    @Test
    void shouldAcceptValidPng() {
        assertDoesNotThrow(() ->
                validator.validate(DocumentType.FOOD_IMAGE, "image/png", 1024L));
    }

    @Test
    void shouldRejectZip() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validate(DocumentType.FSSAI_CERTIFICATE, "application/zip", 1024L));

        assertEquals("Unsupported file type. Allowed types: PDF, JPG, JPEG, PNG.", ex.getMessage());
    }

    @Test
    void shouldRejectExe() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validate(DocumentType.FSSAI_CERTIFICATE, "application/x-msdownload", 1024L));

        assertEquals("Unsupported file type. Allowed types: PDF, JPG, JPEG, PNG.", ex.getMessage());
    }

    @Test
    void shouldRejectHtml() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validate(DocumentType.FSSAI_CERTIFICATE, "text/html", 1024L));

        assertEquals("Unsupported file type. Allowed types: PDF, JPG, JPEG, PNG.", ex.getMessage());
    }

    @Test
    void shouldRejectJavascript() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validate(DocumentType.FSSAI_CERTIFICATE, "application/javascript", 1024L));

        assertEquals("Unsupported file type. Allowed types: PDF, JPG, JPEG, PNG.", ex.getMessage());
    }

    @Test
    void shouldRejectZeroByteFile() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validate(DocumentType.FSSAI_CERTIFICATE, "application/pdf", 0L));

        assertEquals("File cannot be empty.", ex.getMessage());
    }

    @Test
    void shouldRejectNegativeByteFile() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validate(DocumentType.FSSAI_CERTIFICATE, "application/pdf", -1L));

        assertEquals("File cannot be empty.", ex.getMessage());
    }

    @Test
    void shouldRejectOversizedPdf() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validate(DocumentType.FSSAI_CERTIFICATE, "application/pdf", 6L * 1024 * 1024));

        assertEquals("PDF documents cannot exceed 5 MB.", ex.getMessage());
    }

    @Test
    void shouldAcceptMaxSizePdf() {
        assertDoesNotThrow(() ->
                validator.validate(DocumentType.FSSAI_CERTIFICATE, "application/pdf", 5L * 1024 * 1024));
    }

    @Test
    void shouldRejectOversizedImage() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validate(DocumentType.FOOD_IMAGE, "image/png", 3L * 1024 * 1024));

        assertEquals("Images cannot exceed 2 MB.", ex.getMessage());
    }

    @Test
    void shouldAcceptMaxSizeImage() {
        assertDoesNotThrow(() ->
                validator.validate(DocumentType.FOOD_IMAGE, "image/jpeg", 2L * 1024 * 1024));
    }

    @Test
    void shouldRejectNullFileType() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validate(null, "application/pdf", 1024L));

        assertEquals("File type is required", ex.getMessage());
    }

    @Test
    void shouldRejectBlankMimeType() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validate(DocumentType.FSSAI_CERTIFICATE, "  ", 1024L));

        assertEquals("MIME type is required", ex.getMessage());
    }

    @Test
    void shouldRejectNullMimeType() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validate(DocumentType.FSSAI_CERTIFICATE, null, 1024L));

        assertEquals("MIME type is required", ex.getMessage());
    }

    @Test
    void shouldRejectNullFileSizeBytes() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validate(DocumentType.FSSAI_CERTIFICATE, "application/pdf", null));

        assertEquals("File size is required", ex.getMessage());
    }
}
