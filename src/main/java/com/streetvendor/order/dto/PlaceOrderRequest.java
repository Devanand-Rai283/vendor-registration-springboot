package com.streetvendor.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PlaceOrderRequest(
        @NotEmpty(message = "Order items list cannot be empty")
        @Valid
        List<OrderItemRequest> items,

        String notes
) {
}
