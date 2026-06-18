package com.streetvendor.payment.service;

import com.streetvendor.auth.entity.User;
import com.streetvendor.payment.dto.CreatePaymentOrderResponse;
import com.streetvendor.payment.dto.PaymentVerificationResponse;
import java.util.UUID;

public interface PaymentService {
    CreatePaymentOrderResponse createPaymentOrder(UUID orderId, User user);
    PaymentVerificationResponse verifyPaymentStatus(UUID orderId, User user);
}
