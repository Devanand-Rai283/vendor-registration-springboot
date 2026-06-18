package com.streetvendor.payment.webhook;

/**
 * Parsed representation of a Razorpay webhook event.
 *
 * <p>Only fields required by PAYMENT-002 are mapped.
 * Unknown fields are silently ignored.
 */
public class RazorpayWebhookEvent {

    private String event;
    private Payload payload;

    public RazorpayWebhookEvent() {
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    public static class Payload {

        private PaymentEntity payment;

        public PaymentEntity getPayment() {
            return payment;
        }

        public void setPayment(PaymentEntity payment) {
            this.payment = payment;
        }
    }

    public static class PaymentEntity {

        private Entity entity;

        public Entity getEntity() {
            return entity;
        }

        public void setEntity(Entity entity) {
            this.entity = entity;
        }
    }

    public static class Entity {

        private String id;

        private String order_id;  // Razorpay uses snake_case in JSON

        private Long amount;

        private String currency;

        private String status;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getOrder_id() {
            return order_id;
        }

        public void setOrder_id(String order_id) {
            this.order_id = order_id;
        }

        public Long getAmount() {
            return amount;
        }

        public void setAmount(Long amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
