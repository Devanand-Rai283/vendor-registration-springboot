package com.streetvendor.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.auth.entity.User;
import com.streetvendor.payment.config.WebhookConfig;
import com.streetvendor.payment.dto.CreatePaymentOrderRequest;
import com.streetvendor.payment.dto.CreatePaymentOrderResponse;
import com.streetvendor.payment.service.PaymentService;
import com.streetvendor.payment.service.PaymentWebhookService;
import com.streetvendor.payment.webhook.RazorpayWebhookEvent;
import com.streetvendor.payment.webhook.WebhookSignatureVerifier;
import com.streetvendor.common.audit.AuditService;
import com.streetvendor.common.exception.UnauthorizedException;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.streetvendor.payment.dto.PaymentVerificationResponse;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentWebhookService paymentWebhookService;
    private final WebhookSignatureVerifier signatureVerifier;
    private final WebhookConfig webhookConfig;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public PaymentController(PaymentService paymentService,
                             PaymentWebhookService paymentWebhookService,
                             WebhookSignatureVerifier signatureVerifier,
                             WebhookConfig webhookConfig,
                             ObjectMapper objectMapper,
                             AuditService auditService) {
        this.paymentService = paymentService;
        this.paymentWebhookService = paymentWebhookService;
        this.signatureVerifier = signatureVerifier;
        this.webhookConfig = webhookConfig;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------------ //
    // POST /api/payments/create-order                                     //
    // ------------------------------------------------------------------ //

    @PostMapping("/create-order")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CreatePaymentOrderResponse> createPaymentOrder(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreatePaymentOrderRequest request) {
        CreatePaymentOrderResponse response = paymentService.createPaymentOrder(request.orderId(), user);
        return ResponseEntity.ok(response);
    }

    // ------------------------------------------------------------------ //
    // GET /api/payments/orders/{orderId}/verify                            //
    // ------------------------------------------------------------------ //

    @GetMapping("/orders/{orderId}/verify")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentVerificationResponse> verifyPayment(
            @AuthenticationPrincipal User user,
            @PathVariable UUID orderId) {
        PaymentVerificationResponse response = paymentService.verifyPaymentStatus(orderId, user);
        return ResponseEntity.ok(response);
    }

    // ------------------------------------------------------------------ //
    // POST /api/payments/webhook                                          //
    // Publicly accessible endpoint — protected by HMAC, not JWT.         //
    // Signature verification MUST happen before any business logic.      //
    // ------------------------------------------------------------------ //

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            HttpServletRequest request,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) throws Exception {

        if (signature == null || signature.isBlank()) {
            throw new IllegalArgumentException("Missing X-Razorpay-Signature header");
        }

        // 1. Capture raw body before any deserialization (HMAC requires exact bytes)
        byte[] rawBody = request.getInputStream().readAllBytes();

        // 2. Verify HMAC-SHA256 signature — throws UnauthorizedException (HTTP 401) on failure
        try {
            signatureVerifier.verify(rawBody, signature, webhookConfig.getWebhookSecret());
        } catch (UnauthorizedException ex) {
            auditService.logEvent(
                    com.streetvendor.common.audit.AuditEventType.PAYMENT_VERIFICATION_FAILED,
                    null,
                    null,
                    "Webhook signature verification failed"
            );
            throw ex;
        }

        // 3. Parse event after signature is confirmed valid
        RazorpayWebhookEvent event = objectMapper.readValue(rawBody, RazorpayWebhookEvent.class);

        String eventType = event.getEvent();
        if (eventType == null || event.getPayload() == null || event.getPayload().getPayment() == null
                || event.getPayload().getPayment().getEntity() == null) {
            // Unrecognised structure — acknowledge without processing
            return ResponseEntity.ok().build();
        }

        RazorpayWebhookEvent.Entity entity = event.getPayload().getPayment().getEntity();
        String razorpayOrderId   = entity.getOrder_id();
        String razorpayPaymentId = entity.getId();
        Long   amountInPaise     = entity.getAmount();
        String currency          = entity.getCurrency();

        // 4. Delegate to service — always return 200 so Razorpay does not retry
        paymentWebhookService.processEvent(
                eventType,
                razorpayOrderId,
                razorpayPaymentId,
                amountInPaise != null ? amountInPaise : 0L,
                currency);

        return ResponseEntity.ok().build();
    }
}
