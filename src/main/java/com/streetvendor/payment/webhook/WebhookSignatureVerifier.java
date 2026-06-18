package com.streetvendor.payment.webhook;

import com.streetvendor.common.exception.UnauthorizedException;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Verifies Razorpay webhook HMAC-SHA256 signatures.
 *
 * <p>Razorpay signs every webhook delivery with:
 * <pre>HMAC-SHA256(rawBody, webhookSecret)</pre>
 * The resulting hex digest is sent in the {@code X-Razorpay-Signature} header.
 *
 * <p>Verification MUST be performed on the raw byte body before any JSON
 * deserialization occurs, because whitespace changes invalidate the digest.
 */
@Component
public class WebhookSignatureVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * Verifies the Razorpay webhook signature.
     *
     * @param rawBody           Raw request body bytes as received from Razorpay
     * @param receivedSignature Value of the {@code X-Razorpay-Signature} header
     * @param webhookSecret     {@code RAZORPAY_WEBHOOK_SECRET} from environment
     * @throws UnauthorizedException if the signature does not match (HTTP 401)
     */
    public void verify(byte[] rawBody, String receivedSignature, String webhookSecret) {
        String expectedSignature = computeHmac(rawBody, webhookSecret);
        if (!constantTimeEquals(expectedSignature, receivedSignature)) {
            throw new UnauthorizedException("Webhook signature verification failed");
        }
    }

    private String computeHmac(byte[] data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] digest = mac.doFinal(data);
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 computation failed", e);
        }
    }

    /**
     * Constant-time hex string comparison to prevent timing attacks.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
