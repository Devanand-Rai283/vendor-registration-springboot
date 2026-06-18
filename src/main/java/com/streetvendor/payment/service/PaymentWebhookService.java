package com.streetvendor.payment.service;

/**
 * Processes verified Razorpay webhook events.
 *
 * <p>Implementations assume the HMAC signature has already been validated
 * by the caller before this method is invoked.
 */
public interface PaymentWebhookService {

    /**
     * Processes a Razorpay webhook event.
     *
     * @param eventType         The event type string (e.g. "payment.captured")
     * @param razorpayOrderId   The Razorpay order ID from the event payload
     * @param razorpayPaymentId The Razorpay payment ID from the event payload
     * @param amountInPaise     The amount from the event payload, in paise
     */
    void processEvent(String eventType,
                      String razorpayOrderId,
                      String razorpayPaymentId,
                      long amountInPaise,
                      String currency);
}
