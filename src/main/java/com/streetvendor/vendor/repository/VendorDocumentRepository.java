package com.streetvendor.vendor.repository;

import com.streetvendor.vendor.entity.VendorDocument;
import com.streetvendor.vendor.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorDocumentRepository extends JpaRepository<VendorDocument, UUID> {

    List<VendorDocument> findByVendorId(UUID vendorId);

    Optional<VendorDocument> findByVendorIdAndDocumentType(UUID vendorId, DocumentType documentType);
}
