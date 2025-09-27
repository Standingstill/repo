package com.ensureback.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ensureback")
public class EnsurebackProperties {

    private final int disputeWindowDefaultHours;
    private final int feeBps;
    private final int fixedFeeCents;
    private final String appBaseUrl;

    public EnsurebackProperties(int disputeWindowDefaultHours,
                                int feeBps,
                                int fixedFeeCents,
                                String appBaseUrl) {
        this.disputeWindowDefaultHours = disputeWindowDefaultHours;
        this.feeBps = feeBps;
        this.fixedFeeCents = fixedFeeCents;
        this.appBaseUrl = appBaseUrl;
    }

    public int getDisputeWindowDefaultHours() {
        return disputeWindowDefaultHours;
    }

    public int getFeeBps() {
        return feeBps;
    }

    public int getFixedFeeCents() {
        return fixedFeeCents;
    }

    public String getAppBaseUrl() {
        return appBaseUrl;
    }
}
