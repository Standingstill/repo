package com.ensureback.stripe;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {

    private final String secretKey;
    private final String webhookSecret;
    private final String connectClientId;
    private final String connectRedirectUri;

    public StripeProperties(String secretKey,
                            String webhookSecret,
                            String connectClientId,
                            String connectRedirectUri) {
        this.secretKey = secretKey;
        this.webhookSecret = webhookSecret;
        this.connectClientId = connectClientId;
        this.connectRedirectUri = connectRedirectUri;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public String getConnectClientId() {
        return connectClientId;
    }

    public String getConnectRedirectUri() {
        return connectRedirectUri;
    }
}
