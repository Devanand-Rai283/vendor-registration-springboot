package com.streetvendor.payment.dto;

import java.util.UUID;

public record PaymentVerificationResponse(
        UUID paymentId,
        UUID orderId,
        String paymentStatus,
        String orderPaymentStatus,
        String orderStatus,
        String razorpayOrderId,
        String razorpayPaymentId
) {
}
