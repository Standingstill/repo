package com.ensureback.stripe;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {

    private final String secretKey;
    private final String webhookSecret;

    public StripeProperties(String secretKey, String webhookSecret) {
        this.secretKey = secretKey;
        this.webhookSecret = webhookSecret;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }
}
