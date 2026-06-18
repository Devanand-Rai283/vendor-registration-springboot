package com.streetvendor.payment.dto;

import java.util.UUID;

public record CreatePaymentOrderResponse(
        UUID paymentId,
        String razorpayOrderId,
        Integer amount,
        String currency,
        String status
) {
}
