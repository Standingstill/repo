package com.ensureback.security;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final UUID apiKeyId;
    private final UUID merchantId;
    private final String principal;

    public ApiKeyAuthenticationToken(UUID apiKeyId, UUID merchantId, String principal) {
        super(authorities());
        this.apiKeyId = apiKeyId;
        this.merchantId = merchantId;
        this.principal = principal;
        setAuthenticated(true);
    }

    private static Collection<? extends GrantedAuthority> authorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_KEY"));
    }

    @Override
    public Object getCredentials() {
        return "API_KEY";
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public UUID getApiKeyId() {
        return apiKeyId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }
}