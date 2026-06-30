ALTER TABLE audit_logs ALTER COLUMN vendor_id DROP NOT NULL;
ALTER TABLE audit_logs ADD COLUMN user_id UUID;
ALTER TABLE audit_logs ADD CONSTRAINT fk_audit_logs_user_id FOREIGN KEY (user_id) REFERENCES users(id);
