package com.streetvendor.payment.entity;

import com.streetvendor.order.entity.Order;
import com.streetvendor.payment.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "payments",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_payments_order_id", columnNames = "order_id"),
        @UniqueConstraint(name = "uk_payments_razorpay_payment_id", columnNames = "razorpay_payment_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @NotBlank
    @Column(name = "razorpay_order_id", nullable = false)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", unique = true)
    private String razorpayPaymentId;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentStatus status;

    @Column(name = "paid_at")
    private Instant paidAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Payment() {
    }

    public Payment(UUID id, Order order, String razorpayOrderId, Integer amount) {
        this.id = id;
        this.order = order;
        this.razorpayOrderId = razorpayOrderId;
        this.amount = amount;
        this.currency = "INR";
        this.status = PaymentStatus.CREATED;
    }

    public UUID getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public Integer getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
