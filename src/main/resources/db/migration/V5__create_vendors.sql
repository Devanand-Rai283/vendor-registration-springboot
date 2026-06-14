CREATE TABLE vendors (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    business_name VARCHAR(255) NOT NULL,
    owner_name VARCHAR(255),
    phone VARCHAR(50),
    food_type VARCHAR(100),
    description TEXT,
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    address TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_REVIEW',
    average_rating DECIMAL(3,2) DEFAULT 0,
    total_reviews INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vendors_user_id FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_vendors_location ON vendors(latitude, longitude);