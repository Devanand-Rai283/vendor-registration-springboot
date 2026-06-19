package com.streetvendor.payment.repository;

import com.streetvendor.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    Optional<Payment> findByOrderId(UUID orderId);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Payment p JOIN FETCH p.order o JOIN FETCH o.vendor v " +
            "WHERE p.status = com.streetvendor.payment.enums.PaymentStatus.PAID " +
            "AND o.status = com.streetvendor.order.enums.OrderStatus.COMPLETED " +
            "AND o.createdAt >= :startDate AND o.createdAt < :endDate")
    List<Payment> findPaidCompletedPaymentsForOrdersCreatedBetween(
            @org.springframework.data.repository.query.Param("startDate") java.time.Instant startDate,
            @org.springframework.data.repository.query.Param("endDate") java.time.Instant endDate
    );
}
