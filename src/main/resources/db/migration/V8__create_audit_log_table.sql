CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    vendor_id UUID NOT NULL,
    admin_user_id UUID,
    details VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_vendor_id FOREIGN KEY (vendor_id) REFERENCES vendors(id),
    CONSTRAINT fk_audit_logs_admin_user_id FOREIGN KEY (admin_user_id) REFERENCES users(id)
);