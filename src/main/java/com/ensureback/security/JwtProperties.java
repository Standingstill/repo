package com.ensureback.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private final String secret;
    private final Duration expiration;

    public JwtProperties(String secret, Duration expiration) {
        this.secret = secret;
        this.expiration = expiration;
    }

    public String getSecret() {
        return secret;
    }

    public Duration getExpiration() {
        return expiration;
    }
}
