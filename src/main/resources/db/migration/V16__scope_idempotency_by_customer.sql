ALTER TABLE orders DROP CONSTRAINT uk_orders_idempotency_key;
ALTER TABLE orders ADD CONSTRAINT uk_orders_customer_idempotency_key UNIQUE (customer_id, idempotency_key);
