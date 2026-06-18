package com.streetvendor.payment.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreatePaymentOrderRequest(
        @NotNull(message = "Order ID is required")
        UUID orderId
) {
}
