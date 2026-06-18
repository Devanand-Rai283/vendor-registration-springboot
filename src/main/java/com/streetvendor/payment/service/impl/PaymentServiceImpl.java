package com.streetvendor.payment.service.impl;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.streetvendor.auth.entity.User;
import com.streetvendor.common.exception.ConflictException;
import com.streetvendor.common.exception.ExternalServiceException;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.customer.entity.Customer;
import com.streetvendor.customer.repository.CustomerRepository;
import com.streetvendor.order.entity.Order;
import com.streetvendor.order.enums.OrderStatus;
import com.streetvendor.order.repository.OrderRepository;
import com.streetvendor.payment.dto.CreatePaymentOrderResponse;
import com.streetvendor.payment.dto.PaymentVerificationResponse;
import com.streetvendor.payment.entity.Payment;
import com.streetvendor.payment.enums.PaymentStatus;
import com.streetvendor.payment.repository.PaymentRepository;
import com.streetvendor.payment.service.PaymentService;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final RazorpayClient razorpayClient;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              OrderRepository orderRepository,
                              CustomerRepository customerRepository,
                              RazorpayClient razorpayClient) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.razorpayClient = razorpayClient;
    }

    @Override
    @Transactional
    public CreatePaymentOrderResponse createPaymentOrder(UUID orderId, User user) {
        // 1. Resolve customer profile
        Customer customer = customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        // 2. Resolve order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // 3. Ownership check: Customer entity ID check, not User ID
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new ForbiddenException("You do not own this order");
        }

        // 4. Validate order status is not CANCELLED (using OrderStatus enum)
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("Cannot create payment for a cancelled order");
        }

        // 5. Validate payment not already PAID, and return existing CREATED payment for idempotency
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment != null) {
            if (payment.getStatus() == PaymentStatus.PAID) {
                throw new ConflictException("Payment is already completed");
            }
            if (payment.getStatus() == PaymentStatus.CREATED && payment.getRazorpayOrderId() != null) {
                return new CreatePaymentOrderResponse(
                        payment.getId(),
                        payment.getRazorpayOrderId(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getStatus().name()
                );
            }
        }

        // 6. Sourced from database total_amount, convert to paise (rupees -> paise) with overflow checks
        long amountInPaiseLong = order.getTotalAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();
        int amountInPaise = Math.toIntExact(amountInPaiseLong);

        // 7. Create Razorpay Order
        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", orderId.toString());

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            // 8. Create or update payment record
            if (payment == null) {
                payment = new Payment(UUID.randomUUID(), order, razorpayOrderId, amountInPaise);
            } else {
                payment.setRazorpayOrderId(razorpayOrderId);
                payment.setStatus(PaymentStatus.CREATED);
            }
            Payment savedPayment = paymentRepository.save(payment);

            return new CreatePaymentOrderResponse(
                    savedPayment.getId(),
                    savedPayment.getRazorpayOrderId(),
                    savedPayment.getAmount(),
                    savedPayment.getCurrency(),
                    savedPayment.getStatus().name()
            );
        } catch (RazorpayException e) {
            throw new ExternalServiceException("Failed to create Razorpay order: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentVerificationResponse verifyPaymentStatus(UUID orderId, User user) {
        // 1. Resolve customer profile
        Customer customer = customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        // 2. Resolve order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // 3. Ownership check: Must match customer ID (throws 404 to prevent UUID enumeration)
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new ResourceNotFoundException("Order not found");
        }

        // 4. Resolve payment
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found for order: " + orderId));

        return new PaymentVerificationResponse(
                payment.getId(),
                order.getId(),
                payment.getStatus().name(),
                order.getPaymentStatus().name(),
                order.getStatus().name(),
                payment.getRazorpayOrderId(),
                payment.getRazorpayPaymentId()
        );
    }
}
