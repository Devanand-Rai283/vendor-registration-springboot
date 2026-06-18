CREATE TABLE ratings (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    vendor_id UUID NOT NULL,
    stars INTEGER NOT NULL,
    review_text TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ratings_order_id FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_ratings_customer_id FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_ratings_vendor_id FOREIGN KEY (vendor_id) REFERENCES vendors(id),
    CONSTRAINT uk_ratings_order_id UNIQUE (order_id),
    CONSTRAINT chk_ratings_stars CHECK (stars BETWEEN 1 AND 5)
);

CREATE INDEX idx_ratings_customer_id ON ratings(customer_id);
CREATE INDEX idx_ratings_vendor_id ON ratings(vendor_id);
