package com.streetvendor.order.validation;

import com.streetvendor.order.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTransitionValidatorTest {

    @Test
    void shouldAllowValidTransitions() {
        assertTrue(OrderStatusTransitionValidator.canTransition(OrderStatus.PLACED, OrderStatus.ACCEPTED));
        assertTrue(OrderStatusTransitionValidator.canTransition(OrderStatus.PLACED, OrderStatus.CANCELLED));
        assertTrue(OrderStatusTransitionValidator.canTransition(OrderStatus.ACCEPTED, OrderStatus.PREPARING));
        assertTrue(OrderStatusTransitionValidator.canTransition(OrderStatus.PREPARING, OrderStatus.READY));
        assertTrue(OrderStatusTransitionValidator.canTransition(OrderStatus.READY, OrderStatus.COMPLETED));
    }

    @Test
    void shouldDenyInvalidTransitions() {
        // Skipping states
        assertFalse(OrderStatusTransitionValidator.canTransition(OrderStatus.PLACED, OrderStatus.PREPARING));
        assertFalse(OrderStatusTransitionValidator.canTransition(OrderStatus.PLACED, OrderStatus.READY));
        assertFalse(OrderStatusTransitionValidator.canTransition(OrderStatus.PLACED, OrderStatus.COMPLETED));
        assertFalse(OrderStatusTransitionValidator.canTransition(OrderStatus.ACCEPTED, OrderStatus.READY));
        assertFalse(OrderStatusTransitionValidator.canTransition(OrderStatus.ACCEPTED, OrderStatus.COMPLETED));
        assertFalse(OrderStatusTransitionValidator.canTransition(OrderStatus.PREPARING, OrderStatus.COMPLETED));

        // Going backwards
        assertFalse(OrderStatusTransitionValidator.canTransition(OrderStatus.ACCEPTED, OrderStatus.PLACED));
        assertFalse(OrderStatusTransitionValidator.canTransition(OrderStatus.PREPARING, OrderStatus.ACCEPTED));
        assertFalse(OrderStatusTransitionValidator.canTransition(OrderStatus.READY, OrderStatus.PREPARING));
    }

    @Test
    void shouldDenyTransitionsFromFinalStates() {
        for (OrderStatus status : OrderStatus.values()) {
            assertFalse(OrderStatusTransitionValidator.canTransition(OrderStatus.COMPLETED, status));
            assertFalse(OrderStatusTransitionValidator.canTransition(OrderStatus.CANCELLED, status));
        }
    }

    @Test
    void shouldHandleNullInputsGracefully() {
        assertFalse(OrderStatusTransitionValidator.canTransition(null, OrderStatus.PLACED));
        assertFalse(OrderStatusTransitionValidator.canTransition(OrderStatus.PLACED, null));
        assertFalse(OrderStatusTransitionValidator.canTransition(null, null));
    }
}
