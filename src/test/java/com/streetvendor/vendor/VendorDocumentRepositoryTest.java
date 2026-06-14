package com.streetvendor.vendor;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.entity.VendorDocument;
import com.streetvendor.vendor.enums.DocumentType;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.enums.VerificationStatus;
import com.streetvendor.vendor.repository.VendorDocumentRepository;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("vendor-test")
@Transactional
class VendorDocumentRepositoryTest {

    @Autowired
    private VendorDocumentRepository vendorDocumentRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private UserRepository userRepository;

    private Vendor testVendor;

    @BeforeEach
    void setUp() {
        User user = new User(UUID.randomUUID(), "vendor@example.com", "hashedPassword", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(user);

        testVendor = new Vendor(UUID.randomUUID(), user, "Test Business");
        testVendor.setStatus(VendorStatus.PENDING_REVIEW);
        vendorRepository.save(testVendor);
    }

    @Test
    void shouldSaveAndFindByVendorId() {
        VendorDocument doc = new VendorDocument(
                UUID.randomUUID(),
                testVendor,
                DocumentType.FSSAI_CERTIFICATE,
                "vendors/" + testVendor.getId() + "/documents/fssai_certificate/uuid-123",
                VerificationStatus.PENDING,
                Instant.now()
        );
        vendorDocumentRepository.save(doc);

        List<VendorDocument> found = vendorDocumentRepository.findByVendorId(testVendor.getId());

        assertFalse(found.isEmpty());
        assertEquals(1, found.size());
        assertEquals(DocumentType.FSSAI_CERTIFICATE, found.get(0).getDocumentType());
        assertEquals(VerificationStatus.PENDING, found.get(0).getVerificationStatus());
        assertNotNull(found.get(0).getUploadedAt());
    }

    @Test
    void shouldReturnMultipleDocumentsForVendor() {
        VendorDocument doc1 = new VendorDocument(
                UUID.randomUUID(), testVendor, DocumentType.FSSAI_CERTIFICATE,
                "key1", VerificationStatus.PENDING, Instant.now()
        );
        VendorDocument doc2 = new VendorDocument(
                UUID.randomUUID(), testVendor, DocumentType.IDENTITY_PROOF,
                "key2", VerificationStatus.PENDING, Instant.now()
        );
        vendorDocumentRepository.save(doc1);
        vendorDocumentRepository.save(doc2);

        List<VendorDocument> found = vendorDocumentRepository.findByVendorId(testVendor.getId());

        assertEquals(2, found.size());
    }

    @Test
    void shouldReturnEmptyListWhenNoDocuments() {
        List<VendorDocument> found = vendorDocumentRepository.findByVendorId(testVendor.getId());

        assertTrue(found.isEmpty());
    }

    @Test
    void shouldFindByVendorIdAndDocumentType() {
        VendorDocument doc = new VendorDocument(
                UUID.randomUUID(), testVendor, DocumentType.FSSAI_CERTIFICATE,
                "key", VerificationStatus.PENDING, Instant.now()
        );
        vendorDocumentRepository.save(doc);

        Optional<VendorDocument> found = vendorDocumentRepository.findByVendorIdAndDocumentType(
                testVendor.getId(), DocumentType.FSSAI_CERTIFICATE);

        assertTrue(found.isPresent());
        assertEquals(DocumentType.FSSAI_CERTIFICATE, found.get().getDocumentType());
    }

    @Test
    void shouldReturnEmptyWhenDocumentTypeNotFound() {
        VendorDocument doc = new VendorDocument(
                UUID.randomUUID(), testVendor, DocumentType.FSSAI_CERTIFICATE,
                "key", VerificationStatus.PENDING, Instant.now()
        );
        vendorDocumentRepository.save(doc);

        Optional<VendorDocument> found = vendorDocumentRepository.findByVendorIdAndDocumentType(
                testVendor.getId(), DocumentType.IDENTITY_PROOF);

        assertFalse(found.isPresent());
    }

    @Test
    void shouldReturnEmptyWhenVendorIdNotFound() {
        Optional<VendorDocument> found = vendorDocumentRepository.findByVendorIdAndDocumentType(
                UUID.randomUUID(), DocumentType.FSSAI_CERTIFICATE);

        assertFalse(found.isPresent());
    }

    @Test
    void shouldRetrieveExistingDocumentById() {
        UUID docId = UUID.randomUUID();
        VendorDocument doc = new VendorDocument(
                docId, testVendor, DocumentType.FOOD_IMAGE,
                "key", VerificationStatus.VERIFIED, Instant.now()
        );
        vendorDocumentRepository.save(doc);
        vendorDocumentRepository.flush();

        Optional<VendorDocument> found = vendorDocumentRepository.findById(docId);

        assertTrue(found.isPresent());
        assertEquals(docId, found.get().getId());
        assertEquals(DocumentType.FOOD_IMAGE, found.get().getDocumentType());
        assertEquals(VerificationStatus.VERIFIED, found.get().getVerificationStatus());
    }
}
