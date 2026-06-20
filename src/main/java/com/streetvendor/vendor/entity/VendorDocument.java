package com.streetvendor.vendor.entity;

import com.streetvendor.common.audit.AuditableEntity;
import com.streetvendor.vendor.enums.DocumentType;
import com.streetvendor.vendor.enums.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vendor_documents")
public class VendorDocument extends AuditableEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    @Column(nullable = false)
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus verificationStatus;

    @Column(nullable = false)
    private Instant uploadedAt;

    @Column
    private Instant verifiedAt;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private com.streetvendor.auth.entity.User verifiedBy;

    protected VendorDocument() {
    }

    public VendorDocument(UUID id, Vendor vendor, DocumentType documentType, String fileUrl, VerificationStatus verificationStatus, Instant uploadedAt) {
        this.id = id;
        this.vendor = vendor;
        this.documentType = documentType;
        this.fileUrl = fileUrl;
        this.verificationStatus = verificationStatus;
        this.uploadedAt = uploadedAt;
    }

    public UUID getId() {
        return id;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public com.streetvendor.auth.entity.User getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(com.streetvendor.auth.entity.User verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
