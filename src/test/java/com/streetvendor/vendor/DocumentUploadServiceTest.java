package com.streetvendor.vendor;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.common.exception.UnauthorizedException;
import com.streetvendor.config.R2Properties;
import com.streetvendor.vendor.dto.GeneratePresignedUrlRequest;
import com.streetvendor.vendor.dto.GeneratePresignedUrlResponse;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.entity.VendorDocument;
import com.streetvendor.vendor.enums.DocumentType;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.enums.VerificationStatus;
import com.streetvendor.vendor.repository.VendorDocumentRepository;
import com.streetvendor.vendor.repository.VendorRepository;
import com.streetvendor.vendor.service.DocumentUploadService;
import com.streetvendor.vendor.validation.DocumentUploadValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentUploadServiceTest {

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private VendorDocumentRepository vendorDocumentRepository;

    @Mock
    private DocumentUploadValidator validator;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private R2Properties r2Properties;

    @InjectMocks
    private DocumentUploadService documentUploadService;

    @Captor
    private ArgumentCaptor<VendorDocument> vendorDocumentCaptor;

    private User vendorUser;
    private Vendor vendor;
    private GeneratePresignedUrlRequest validRequest;

    @BeforeEach
    void setUp() {
        vendorUser = new User(UUID.randomUUID(), "vendor@example.com", "hash", Role.VENDOR, AccountStatus.ACTIVE);
        vendor = new Vendor(UUID.randomUUID(), vendorUser, "Test Business");
        vendor.setStatus(VendorStatus.PENDING_REVIEW);
        validRequest = new GeneratePresignedUrlRequest(
                DocumentType.FSSAI_CERTIFICATE,
                "application/pdf",
                1024L
        );
    }

    private void setAuthentication(User user) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void mockBucketName() {
        when(r2Properties.getBucketName()).thenReturn("test-bucket");
    }

    private void mockPresignedUrl() throws Exception {
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(s3Presigner.presignPutObject(any(Consumer.class))).thenReturn(presignedRequest);
        when(presignedRequest.url()).thenReturn(new URI("https://upload.example.com/presigned-url").toURL());
    }

    @Test
    void shouldInvokeValidator() throws Exception {
        setAuthentication(vendorUser);
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendor));
        when(vendorDocumentRepository.findByVendorIdAndDocumentType(vendor.getId(), DocumentType.FSSAI_CERTIFICATE))
                .thenReturn(Optional.empty());
        doNothing().when(validator).validate(any(), any(), any());
        mockBucketName();
        mockPresignedUrl();

        documentUploadService.generatePresignedUrl(validRequest);

        verify(validator).validate(DocumentType.FSSAI_CERTIFICATE, "application/pdf", 1024L);
    }

    @Test
    void shouldValidateBeforeUrlGeneration() {
        setAuthentication(vendorUser);
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendor));
        doThrow(new IllegalArgumentException("Unsupported file type")).when(validator)
                .validate(any(), any(), any());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                documentUploadService.generatePresignedUrl(validRequest));

        assertEquals("Unsupported file type", ex.getMessage());
        verify(vendorDocumentRepository, never()).save(any());
    }

    @Test
    void shouldVerifyVendorOwnership() throws Exception {
        setAuthentication(vendorUser);
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendor));
        when(vendorDocumentRepository.findByVendorIdAndDocumentType(vendor.getId(), DocumentType.FSSAI_CERTIFICATE))
                .thenReturn(Optional.empty());
        doNothing().when(validator).validate(any(), any(), any());
        mockBucketName();
        mockPresignedUrl();

        documentUploadService.generatePresignedUrl(validRequest);

        verify(vendorRepository).findByUserId(vendorUser.getId());
    }

    @Test
    void shouldThrowForbiddenWhenVendorMissing() {
        setAuthentication(vendorUser);
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.empty());

        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                documentUploadService.generatePresignedUrl(validRequest));

        assertEquals("Vendor profile not found", ex.getMessage());
    }

    @Test
    void shouldThrowUnauthorizedWhenAnonymous() {
        SecurityContextHolder.clearContext();

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () ->
                documentUploadService.generatePresignedUrl(validRequest));

        assertEquals("Not authenticated", ex.getMessage());
    }

    @Test
    void shouldGeneratePresignedUrl() throws Exception {
        setAuthentication(vendorUser);
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendor));
        when(vendorDocumentRepository.findByVendorIdAndDocumentType(vendor.getId(), DocumentType.FSSAI_CERTIFICATE))
                .thenReturn(Optional.empty());
        doNothing().when(validator).validate(any(), any(), any());
        mockBucketName();
        mockPresignedUrl();

        GeneratePresignedUrlResponse response = documentUploadService.generatePresignedUrl(validRequest);

        assertNotNull(response.uploadUrl());
        assertFalse(response.uploadUrl().isBlank());
        assertNotNull(response.fileUrl());
        assertFalse(response.fileUrl().isBlank());
    }

    @Test
    void shouldPersistMetadataOnNewUpload() throws Exception {
        setAuthentication(vendorUser);
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendor));
        when(vendorDocumentRepository.findByVendorIdAndDocumentType(vendor.getId(), DocumentType.FSSAI_CERTIFICATE))
                .thenReturn(Optional.empty());
        doNothing().when(validator).validate(any(), any(), any());
        mockBucketName();
        mockPresignedUrl();

        documentUploadService.generatePresignedUrl(validRequest);

        verify(vendorDocumentRepository).save(vendorDocumentCaptor.capture());
        VendorDocument saved = vendorDocumentCaptor.getValue();

        assertNotNull(saved.getId());
        assertEquals(vendor.getId(), saved.getVendor().getId());
        assertEquals(DocumentType.FSSAI_CERTIFICATE, saved.getDocumentType());
        assertEquals(VerificationStatus.PENDING, saved.getVerificationStatus());
        assertNotNull(saved.getUploadedAt());
        assertNull(saved.getVerifiedAt());
        assertNull(saved.getVerifiedBy());
    }

    @Test
    void shouldPopulateResponseDto() throws Exception {
        setAuthentication(vendorUser);
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendor));
        when(vendorDocumentRepository.findByVendorIdAndDocumentType(vendor.getId(), DocumentType.FSSAI_CERTIFICATE))
                .thenReturn(Optional.empty());
        doNothing().when(validator).validate(any(), any(), any());
        mockBucketName();
        mockPresignedUrl();

        GeneratePresignedUrlResponse response = documentUploadService.generatePresignedUrl(validRequest);

        assertNotNull(response.uploadUrl());
        assertNotNull(response.fileUrl());
    }

    @Test
    void shouldInvokeRepositorySave() throws Exception {
        setAuthentication(vendorUser);
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendor));
        when(vendorDocumentRepository.findByVendorIdAndDocumentType(vendor.getId(), DocumentType.FSSAI_CERTIFICATE))
                .thenReturn(Optional.empty());
        doNothing().when(validator).validate(any(), any(), any());
        mockBucketName();
        mockPresignedUrl();

        documentUploadService.generatePresignedUrl(validRequest);

        verify(vendorDocumentRepository).save(any(VendorDocument.class));
    }

    @Test
    void shouldUpdateExistingDocumentOnReUpload() throws Exception {
        setAuthentication(vendorUser);
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendor));

        UUID existingDocId = UUID.randomUUID();
        VendorDocument existingDoc = new VendorDocument(
                existingDocId, vendor, DocumentType.FSSAI_CERTIFICATE,
                "old-file-url", VerificationStatus.VERIFIED, Instant.now().minusSeconds(3600)
        );
        existingDoc.setVerifiedAt(Instant.now().minusSeconds(3600));
        existingDoc.setVerifiedBy(vendorUser);

        when(vendorDocumentRepository.findByVendorIdAndDocumentType(vendor.getId(), DocumentType.FSSAI_CERTIFICATE))
                .thenReturn(Optional.of(existingDoc));
        doNothing().when(validator).validate(any(), any(), any());
        mockBucketName();
        mockPresignedUrl();

        GeneratePresignedUrlResponse response = documentUploadService.generatePresignedUrl(validRequest);

        verify(vendorDocumentRepository).save(vendorDocumentCaptor.capture());
        VendorDocument updated = vendorDocumentCaptor.getValue();

        assertEquals(existingDocId, updated.getId());
        assertEquals(VerificationStatus.PENDING, updated.getVerificationStatus());
        assertEquals(response.fileUrl(), updated.getFileUrl());
        assertNull(updated.getVerifiedAt());
        assertNull(updated.getVerifiedBy());
    }

    @Test
    void shouldResetVerificationFieldsOnReUpload() throws Exception {
        setAuthentication(vendorUser);
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendor));

        VendorDocument existingDoc = new VendorDocument(
                UUID.randomUUID(), vendor, DocumentType.FSSAI_CERTIFICATE,
                "old-url", VerificationStatus.REJECTED, Instant.now().minusSeconds(3600)
        );
        existingDoc.setVerifiedAt(Instant.now().minusSeconds(3600));
        existingDoc.setVerifiedBy(vendorUser);

        when(vendorDocumentRepository.findByVendorIdAndDocumentType(vendor.getId(), DocumentType.FSSAI_CERTIFICATE))
                .thenReturn(Optional.of(existingDoc));
        doNothing().when(validator).validate(any(), any(), any());
        mockBucketName();
        mockPresignedUrl();

        documentUploadService.generatePresignedUrl(validRequest);

        verify(vendorDocumentRepository).save(vendorDocumentCaptor.capture());
        VendorDocument updated = vendorDocumentCaptor.getValue();

        assertEquals(VerificationStatus.PENDING, updated.getVerificationStatus());
        assertNull(updated.getVerifiedAt());
        assertNull(updated.getVerifiedBy());
    }

    @Test
    void shouldPreserveEntityIdentityOnReUpload() throws Exception {
        setAuthentication(vendorUser);
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendor));

        UUID existingDocId = UUID.randomUUID();
        VendorDocument existingDoc = new VendorDocument(
                existingDocId, vendor, DocumentType.FSSAI_CERTIFICATE,
                "old-url", VerificationStatus.VERIFIED, Instant.now().minusSeconds(3600)
        );
        existingDoc.setVerifiedAt(Instant.now());
        existingDoc.setVerifiedBy(vendorUser);

        when(vendorDocumentRepository.findByVendorIdAndDocumentType(vendor.getId(), DocumentType.FSSAI_CERTIFICATE))
                .thenReturn(Optional.of(existingDoc));
        doNothing().when(validator).validate(any(), any(), any());
        mockBucketName();
        mockPresignedUrl();

        documentUploadService.generatePresignedUrl(validRequest);

        verify(vendorDocumentRepository).save(vendorDocumentCaptor.capture());
        assertEquals(existingDocId, vendorDocumentCaptor.getValue().getId());
    }

    @Test
    void shouldCreateNewDocumentOnFirstUpload() throws Exception {
        setAuthentication(vendorUser);
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendor));
        when(vendorDocumentRepository.findByVendorIdAndDocumentType(vendor.getId(), DocumentType.FSSAI_CERTIFICATE))
                .thenReturn(Optional.empty());
        doNothing().when(validator).validate(any(), any(), any());
        mockBucketName();
        mockPresignedUrl();

        documentUploadService.generatePresignedUrl(validRequest);

        verify(vendorDocumentRepository).save(vendorDocumentCaptor.capture());
        assertNotNull(vendorDocumentCaptor.getValue().getId());
        assertEquals(VerificationStatus.PENDING, vendorDocumentCaptor.getValue().getVerificationStatus());
    }
}
