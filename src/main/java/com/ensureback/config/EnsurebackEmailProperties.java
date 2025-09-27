package com.ensureback.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ensureback.email")
public class EnsurebackEmailProperties {

    private final boolean enabled;
    private final String from;

    public EnsurebackEmailProperties(boolean enabled, String from) {
        this.enabled = enabled;
        this.from = from;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getFrom() {
        return from;
    }
}