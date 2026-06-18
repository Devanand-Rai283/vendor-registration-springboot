package com.streetvendor.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.payment.service.PaymentWebhookService;
import com.streetvendor.support.AbstractSecurityTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-layer security and integration tests for POST /api/payments/webhook.
 *
 * <p>Uses a real WebhookSignatureVerifier bean and a mocked PaymentWebhookService
 * to isolate HTTP-layer concerns from business logic.
 */
@ActiveProfiles("security-test")
@Transactional
class PaymentWebhookControllerTest extends AbstractSecurityTest {

    /** Keep as real bean — we want to test actual HMAC verification logic. */
    @MockitoBean
    private PaymentWebhookService paymentWebhookService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Shared test secret that must match application-security-test.yml or
     * the TestPropertySource for the security-test profile.
     */
    private static final String TEST_WEBHOOK_SECRET = "test-webhook-secret";

    private static final String VALID_BODY =
            "{\"event\":\"payment.captured\"," +
            "\"payload\":{\"payment\":{\"entity\":{" +
            "\"id\":\"pay_test_001\"," +
            "\"order_id\":\"order_test_001\"," +
            "\"amount\":25000," +
            "\"currency\":\"INR\"," +
            "\"status\":\"captured\"}}}}";

    // ------------------------------------------------------------------ //
    // Signature verification                                              //
    // ------------------------------------------------------------------ //

    @Test
    void shouldReturn200WhenSignatureIsValid() throws Exception {
        String validSignature = computeHmac(VALID_BODY, TEST_WEBHOOK_SECRET);
        doNothing().when(paymentWebhookService).processEvent(anyString(), anyString(), anyString(), anyLong(), anyString());

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", validSignature)
                        .content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn401WhenSignatureIsInvalid() throws Exception {
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", "invalid_signature_value")
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn400WhenSignatureHeaderIsMissing() throws Exception {
        // Spring returns 400 when @RequestHeader is missing (or manually checked by us)
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ //
    // Service delegation                                                  //
    // ------------------------------------------------------------------ //

    @Test
    void shouldDelegateToServiceAfterSuccessfulVerification() throws Exception {
        String validSignature = computeHmac(VALID_BODY, TEST_WEBHOOK_SECRET);
        doNothing().when(paymentWebhookService).processEvent(anyString(), anyString(), anyString(), anyLong(), anyString());

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", validSignature)
                        .content(VALID_BODY))
                .andExpect(status().isOk());

        verify(paymentWebhookService).processEvent(
                "payment.captured",
                "order_test_001",
                "pay_test_001",
                25000L,
                "INR");
    }

    @Test
    void shouldNotDelegateToServiceWhenSignatureIsInvalid() throws Exception {
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", "bad_sig")
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());

        verify(paymentWebhookService, never()).processEvent(any(), any(), any(), anyLong(), any());
    }

    @Test
    void shouldReturn200ForUnknownEventTypeAfterValidSignature() throws Exception {
        String unknownEventBody =
                "{\"event\":\"refund.created\"," +
                "\"payload\":{\"payment\":{\"entity\":{" +
                "\"id\":\"pay_refund_001\"," +
                "\"order_id\":\"order_refund_001\"," +
                "\"amount\":25000," +
                "\"currency\":\"INR\"," +
                "\"status\":\"refunded\"}}}}";

        String validSignature = computeHmac(unknownEventBody, TEST_WEBHOOK_SECRET);
        doNothing().when(paymentWebhookService).processEvent(anyString(), anyString(), anyString(), anyLong(), anyString());

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", validSignature)
                        .content(unknownEventBody))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ //
    // Endpoint does not require JWT authentication                        //
    // ------------------------------------------------------------------ //

    @Test
    void shouldNotRequireJwtBearerToken() throws Exception {
        // A valid HMAC signature is all that is required — no Authorization header
        String validSignature = computeHmac(VALID_BODY, TEST_WEBHOOK_SECRET);
        doNothing().when(paymentWebhookService).processEvent(anyString(), anyString(), anyString(), anyLong(), anyString());

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", validSignature)
                        // No Authorization header
                        .content(VALID_BODY))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ //
    // Database record handling                                            //
    // ------------------------------------------------------------------ //

    @Test
    void shouldReturn404WhenRazorpayOrderIdDoesNotExist() throws Exception {
        String validSignature = computeHmac(VALID_BODY, TEST_WEBHOOK_SECRET);
        doThrow(new ResourceNotFoundException("Payment record not found"))
                .when(paymentWebhookService)
                .processEvent(anyString(), anyString(), anyString(), anyLong(), anyString());

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", validSignature)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ //
    // HMAC helper                                                         //
    // ------------------------------------------------------------------ //

    private static String computeHmac(String body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
