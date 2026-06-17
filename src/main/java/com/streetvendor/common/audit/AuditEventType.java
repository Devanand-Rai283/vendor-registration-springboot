package com.streetvendor.common.audit;

public enum AuditEventType {
    VENDOR_APPROVED,
    VENDOR_REJECTED,
    ORDER_ACCEPTED,
    ORDER_CANCELLED,
    ORDER_PREPARING,
    ORDER_READY,
    ORDER_COMPLETED,
    ORDER_CANCELLED_BY_CUSTOMER
}