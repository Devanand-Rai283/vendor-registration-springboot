package com.streetvendor.order.validation;

import com.streetvendor.order.enums.OrderStatus;

public final class OrderStatusTransitionValidator {

    private OrderStatusTransitionValidator() {
    }

    public static boolean canTransition(OrderStatus current, OrderStatus target) {
        if (current == null || target == null) {
            return false;
        }

        if (current == OrderStatus.COMPLETED || current == OrderStatus.CANCELLED) {
            return false;
        }

        return switch (current) {
            case PLACED -> (target == OrderStatus.ACCEPTED || target == OrderStatus.CANCELLED);
            case ACCEPTED -> (target == OrderStatus.PREPARING);
            case PREPARING -> (target == OrderStatus.READY);
            case READY -> (target == OrderStatus.COMPLETED);
            default -> false;
        };
    }
}
