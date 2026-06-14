CREATE TABLE menu_categories (
    id UUID PRIMARY KEY,
    vendor_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_menu_categories_vendor
        FOREIGN KEY (vendor_id)
        REFERENCES vendors(id)
);
