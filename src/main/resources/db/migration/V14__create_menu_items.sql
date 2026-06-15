CREATE TABLE menu_items (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL,
    vendor_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    dietary_tag VARCHAR(100),
    image_url VARCHAR(500),
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_menu_items_category
        FOREIGN KEY (category_id)
        REFERENCES menu_categories(id),
    CONSTRAINT fk_menu_items_vendor
        FOREIGN KEY (vendor_id)
        REFERENCES vendors(id)
);

CREATE INDEX idx_menu_items_vendor
    ON menu_items(vendor_id);

CREATE INDEX idx_menu_items_category
    ON menu_items(category_id);
