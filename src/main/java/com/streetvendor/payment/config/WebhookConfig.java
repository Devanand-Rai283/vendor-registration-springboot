package com.streetvendor.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class WebhookConfig {

    private final String webhookSecret;

    public WebhookConfig(@Value("${razorpay.webhook-secret}") String webhookSecret) {
        if (!StringUtils.hasText(webhookSecret)) {
            throw new IllegalArgumentException(
                    "RAZORPAY_WEBHOOK_SECRET must be configured. Set razorpay.webhook-secret.");
        }
        this.webhookSecret = webhookSecret;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }
}
