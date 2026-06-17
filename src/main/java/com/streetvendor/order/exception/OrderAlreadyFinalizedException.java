package com.streetvendor.order.exception;

public class OrderAlreadyFinalizedException extends RuntimeException {
    public OrderAlreadyFinalizedException(String message) {
        super(message);
    }
}
