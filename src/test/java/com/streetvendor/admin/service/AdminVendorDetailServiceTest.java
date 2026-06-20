package com.streetvendor.admin.service;

import com.streetvendor.admin.dto.AdminVendorDetailResponseDto;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.entity.VendorDocument;
import com.streetvendor.vendor.enums.DocumentType;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.enums.VerificationStatus;
import com.streetvendor.vendor.repository.VendorDocumentRepository;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminVendorDetailService Unit Tests")
class AdminVendorDetailServiceTest {

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private VendorDocumentRepository vendorDocumentRepository;

    @InjectMocks
    private AdminVendorManagementServiceImpl adminVendorManagementService;

    private UUID vendorId;
    private User testUser;
    private Vendor testVendor;

    @BeforeEach
    void setUp() {
        vendorId = UUID.randomUUID();
        testUser = new User(UUID.randomUUID(), "test@vendor.com", "password", Role.VENDOR, AccountStatus.ACTIVE);
        testVendor = new Vendor(vendorId, testUser, "Test Business");
        testVendor.setOwnerName("Test Owner");
        testVendor.setPhone("1234567890");
        testVendor.setDescription("A test description");
        testVendor.setFoodType("Mexican");
        testVendor.setStatus(VendorStatus.PENDING_REVIEW);
        testVendor.setAddress("123 Test St");
        testVendor.setLatitude(BigDecimal.valueOf(12.34));
        testVendor.setLongitude(BigDecimal.valueOf(56.78));
        testVendor.setAverageRating(BigDecimal.valueOf(4.5));
        testVendor.setTotalReviews(10);
        testVendor.setRejectionReason("Needs better photos");
    }

    @Test
    @DisplayName("getVendorDetails maps vendor and documents correctly")
    void getVendorDetails_mapsDataCorrectly() {
        VendorDocument doc1 = new VendorDocument(UUID.randomUUID(), testVendor, DocumentType.FSSAI_CERTIFICATE,
                "url1", VerificationStatus.VERIFIED, Instant.now());
        
        VendorDocument doc2 = new VendorDocument(UUID.randomUUID(), testVendor, DocumentType.IDENTITY_PROOF,
                "url2", VerificationStatus.REJECTED, Instant.now());
        doc2.setRejectionReason("Blurry ID");

        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(testVendor));
        when(vendorDocumentRepository.findByVendorId(vendorId)).thenReturn(List.of(doc1, doc2));

        AdminVendorDetailResponseDto result = adminVendorManagementService.getVendorDetails(vendorId);

        assertNotNull(result);
        assertEquals(vendorId, result.id());
        assertEquals("Test Business", result.businessName());
        assertEquals("test@vendor.com", result.email());
        assertEquals("1234567890", result.phoneNumber());
        assertEquals(AccountStatus.ACTIVE, result.accountStatus());
        assertEquals("Needs better photos", result.rejectionReason());

        assertNotNull(result.documents());
        assertEquals(2, result.documents().size());
        
        assertEquals(DocumentType.FSSAI_CERTIFICATE, result.documents().get(0).documentType());
        assertEquals(VerificationStatus.VERIFIED, result.documents().get(0).status());
        
        assertEquals(DocumentType.IDENTITY_PROOF, result.documents().get(1).documentType());
        assertEquals(VerificationStatus.REJECTED, result.documents().get(1).status());
        assertEquals("Blurry ID", result.documents().get(1).rejectionReason());
    }

    @Test
    @DisplayName("shouldReturnVendorDetailsWhenNoDocumentsExist")
    void shouldReturnVendorDetailsWhenNoDocumentsExist() {
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(testVendor));
        when(vendorDocumentRepository.findByVendorId(vendorId)).thenReturn(List.of());

        AdminVendorDetailResponseDto result = adminVendorManagementService.getVendorDetails(vendorId);

        assertNotNull(result);
        assertEquals(vendorId, result.id());
        assertNotNull(result.documents());
        assertTrue(result.documents().isEmpty());
    }
}
