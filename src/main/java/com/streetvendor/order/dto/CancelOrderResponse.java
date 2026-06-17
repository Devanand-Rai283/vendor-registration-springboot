package com.streetvendor.order.dto;

import com.streetvendor.order.enums.OrderStatus;
import com.streetvendor.order.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CancelOrderResponse(
        UUID orderId,
        OrderStatus status,
        PaymentStatus paymentStatus,
        BigDecimal totalAmount,
        Instant cancelledAt
) {
}
