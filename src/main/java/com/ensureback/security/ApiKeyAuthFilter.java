package com.ensureback.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    static final String HEADER_KEY_ID = "X-EB-API-KEY-ID";
    static final String HEADER_KEY = "X-EB-API-KEY";
    static final String HEADER_SIGNATURE = "X-EB-API-SIGNATURE";
    static final String HEADER_TIMESTAMP = "X-EB-API-TIMESTAMP";

    private final ApiKeyAuthService apiKeyAuthService;

    public ApiKeyAuthFilter(ApiKeyAuthService apiKeyAuthService) {
        this.apiKeyAuthService = apiKeyAuthService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!requiresApiKey(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth != null && currentAuth.isAuthenticated() && !(currentAuth instanceof ApiKeyAuthenticationToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        String keyIdHeader = request.getHeader(HEADER_KEY_ID);
        String rawKey = request.getHeader(HEADER_KEY);
        String signature = request.getHeader(HEADER_SIGNATURE);
        String timestampHeader = request.getHeader(HEADER_TIMESTAMP);

        // Only attempt API key auth if all required headers are present.
        // Otherwise, fall through so other auth mechanisms (e.g., JWT cookie/header) can authenticate.
        boolean hasAllHeaders = StringUtils.hasText(keyIdHeader)
                && StringUtils.hasText(rawKey)
                && StringUtils.hasText(signature)
                && StringUtils.hasText(timestampHeader);
        if (!hasAllHeaders) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID apiKeyId;
        Instant timestamp;
        try {
            apiKeyId = UUID.fromString(keyIdHeader);
            timestamp = Instant.parse(timestampHeader);
        } catch (Exception ex) {
            unauthorized(response);
            return;
        }

        String canonicalRequest = timestampHeader + ':' + request.getMethod() + ':' + request.getRequestURI() + ':';

        Optional<ApiKeyAuthenticationToken> authentication = apiKeyAuthService.authenticate(apiKeyId, rawKey, signature, timestamp, canonicalRequest);
        if (authentication.isEmpty()) {
            unauthorized(response);
            return;
        }

        ApiKeyAuthenticationToken authToken = authentication.get();
        authToken.setDetails(authToken.getMerchantId());
        SecurityContextHolder.getContext().setAuthentication(authToken);
        filterChain.doFilter(request, response);
    }

    private boolean requiresApiKey(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return false;
        }
        if (!path.startsWith("/api/developer/")) {
            return false;
        }
        // Apply to developer wizard endpoints
        if (path.startsWith("/api/developer/wizard/")) {
            return true;
        }
        // Backward-compat: specific status endpoint (if any)
        return path.startsWith("/api/developer/status");
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        if (!response.isCommitted()) {
            response.sendError(HttpStatus.UNAUTHORIZED.value());
        }
    }
}
