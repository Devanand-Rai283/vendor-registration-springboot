CREATE TABLE analytics_snapshots (
    id UUID PRIMARY KEY,
    vendor_id UUID NOT NULL,
    snapshot_date DATE NOT NULL,
    total_orders INTEGER NOT NULL,
    total_revenue DECIMAL(10,2) NOT NULL,
    average_order_value DECIMAL(10,2) NOT NULL,
    top_item_id UUID,
    peak_hour INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_analytics_snapshots_vendor_id FOREIGN KEY (vendor_id) REFERENCES vendors(id) ON DELETE CASCADE,
    CONSTRAINT uk_analytics_snapshots_vendor_date UNIQUE (vendor_id, snapshot_date)
);

CREATE INDEX idx_analytics_snapshots_vendor_id ON analytics_snapshots(vendor_id);
CREATE INDEX idx_analytics_snapshots_date ON analytics_snapshots(snapshot_date);
