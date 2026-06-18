package com.streetvendor.payment.service.impl;

import com.streetvendor.common.audit.AuditEventType;
import com.streetvendor.common.audit.AuditService;
import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.order.entity.Order;
import com.streetvendor.order.repository.OrderRepository;
import com.streetvendor.payment.entity.Payment;
import com.streetvendor.payment.enums.PaymentStatus;
import com.streetvendor.payment.repository.PaymentRepository;
import com.streetvendor.payment.service.PaymentWebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PaymentWebhookServiceImpl implements PaymentWebhookService {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookServiceImpl.class);

    private static final String EVENT_PAYMENT_CAPTURED = "payment.captured";
    private static final String EVENT_PAYMENT_FAILED   = "payment.failed";

    private final PaymentRepository paymentRepository;
    private final OrderRepository   orderRepository;
    private final AuditService      auditService;

    public PaymentWebhookServiceImpl(PaymentRepository paymentRepository,
                                     OrderRepository orderRepository,
                                     AuditService auditService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository   = orderRepository;
        this.auditService      = auditService;
    }

    @Override
    @Transactional
    public void processEvent(String eventType,
                             String razorpayOrderId,
                             String razorpayPaymentId,
                             long amountInPaise,
                             String currency) {

        switch (eventType) {
            case EVENT_PAYMENT_CAPTURED -> handleCaptured(razorpayOrderId, razorpayPaymentId, amountInPaise, currency);
            case EVENT_PAYMENT_FAILED   -> handleFailed(razorpayOrderId);
            default -> log.debug("Ignoring unhandled Razorpay event type: {}", eventType);
        }
    }

    // ------------------------------------------------------------------ //
    // payment.captured                                                    //
    // ------------------------------------------------------------------ //

    private void handleCaptured(String razorpayOrderId,
                                String razorpayPaymentId,
                                long amountInPaise,
                                String currency) {

        // Currency verification — reject non-INR currencies
        if (!"INR".equals(currency)) {
            log.warn("Webhook payment.captured: currency mismatch for razorpayOrderId={} (expected INR, received={})",
                    razorpayOrderId, currency);
            throw new IllegalArgumentException("Only INR currency is supported");
        }

        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> {
                    log.warn("Webhook payment.captured: no payment record for razorpayOrderId={}", razorpayOrderId);
                    return new ResourceNotFoundException("Payment record not found for Razorpay order: " + razorpayOrderId);
                });

        // Idempotency: already in a terminal state → return silently
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.debug("Webhook payment.captured: already PAID for razorpayOrderId={} — ignoring duplicate", razorpayOrderId);
            return;
        }
        if (payment.getStatus() == PaymentStatus.FAILED) {
            log.warn("Webhook payment.captured: received CAPTURED for a FAILED payment razorpayOrderId={} — ignoring", razorpayOrderId);
            return;
        }

        // Amount verification — reject mismatches (security skill mandate)
        if (payment.getAmount() != null && payment.getAmount().longValue() != amountInPaise) {
            log.warn("Webhook payment.captured: amount mismatch for razorpayOrderId={} " +
                            "(expected={}, received={}). Recording audit event.",
                    razorpayOrderId, payment.getAmount(), amountInPaise);
            auditService.logEvent(
                    AuditEventType.PAYMENT_VERIFICATION_FAILED,
                    null,
                    null,
                    "Amount mismatch for razorpayOrderId=" + razorpayOrderId);
            return;
        }

        try {
            // Transition payment to PAID
            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(Instant.now());
            paymentRepository.save(payment);

            // Synchronise order.paymentStatus in the same transaction
            Order order = payment.getOrder();
            order.setPaymentStatus(com.streetvendor.order.enums.PaymentStatus.PAID);
            orderRepository.save(order);

            log.info("Webhook payment.captured: payment PAID razorpayOrderId={} orderId={}",
                    razorpayOrderId, order.getId());

        } catch (DataIntegrityViolationException e) {
            // UNIQUE constraint on razorpay_payment_id fired — duplicate concurrent delivery
            log.warn("Webhook payment.captured: duplicate delivery detected for razorpayPaymentId={} — ignoring",
                    razorpayPaymentId);
        }
    }

    // ------------------------------------------------------------------ //
    // payment.failed                                                      //
    // ------------------------------------------------------------------ //

    private void handleFailed(String razorpayOrderId) {

        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> {
                    log.warn("Webhook payment.failed: no payment record for razorpayOrderId={}", razorpayOrderId);
                    return new ResourceNotFoundException("Payment record not found for Razorpay order: " + razorpayOrderId);
                });

        // Idempotency: already terminal → return silently
        if (payment.getStatus() == PaymentStatus.PAID || payment.getStatus() == PaymentStatus.FAILED) {
            log.debug("Webhook payment.failed: already terminal ({}) for razorpayOrderId={} — ignoring duplicate",
                    payment.getStatus(), razorpayOrderId);
            return;
        }

        // Transition payment to FAILED
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        // Synchronise order.paymentStatus — Order.status remains UNCHANGED (Q3)
        Order order = payment.getOrder();
        order.setPaymentStatus(com.streetvendor.order.enums.PaymentStatus.FAILED);
        orderRepository.save(order);

        log.info("Webhook payment.failed: payment FAILED razorpayOrderId={} orderId={}",
                razorpayOrderId, order.getId());
    }
}
