package com.streetvendor.order.dto;

public record PlaceOrderResult(
        PlaceOrderResponse response,
        boolean isDuplicate
) {
}
