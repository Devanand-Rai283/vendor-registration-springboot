CREATE TABLE VENDOR_DOCUMENTS (
    id UUID PRIMARY KEY,
    vendor_id UUID NOT NULL,
    document_type VARCHAR NOT NULL,
    file_url VARCHAR NOT NULL,
    verification_status VARCHAR NOT NULL,
    uploaded_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP NULL,
    verified_by UUID NULL,
    CONSTRAINT fk_vendor_documents_vendor_id FOREIGN KEY (vendor_id) REFERENCES vendors(id),
    CONSTRAINT fk_vendor_documents_verified_by FOREIGN KEY (verified_by) REFERENCES users(id)
);
