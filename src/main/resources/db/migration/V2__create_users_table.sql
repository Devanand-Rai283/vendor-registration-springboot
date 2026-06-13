CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    account_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX idx_users_email ON users(email);

ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('CUSTOMER', 'VENDOR', 'ADMIN'));
ALTER TABLE users ADD CONSTRAINT chk_users_account_status CHECK (account_status IN ('ACTIVE', 'SUSPENDED', 'DELETED'));
