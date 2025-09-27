package com.ensureback.web;

import com.ensureback.developer.ApiAuditLog;
import com.ensureback.developer.ApiAuditLogRepository;
import com.ensureback.developer.ApiKeyRepository;
import com.ensureback.security.ApiKeyAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class ApiAuditFilter extends OncePerRequestFilter {

    private final ApiAuditLogRepository apiAuditLogRepository;
    private final ApiKeyRepository apiKeyRepository;

    public ApiAuditFilter(ApiAuditLogRepository apiAuditLogRepository,
                          ApiKeyRepository apiKeyRepository) {
        this.apiAuditLogRepository = apiAuditLogRepository;
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        StatusCaptureResponseWrapper responseWrapper = new StatusCaptureResponseWrapper(response);
        try {
            filterChain.doFilter(request, responseWrapper);
        } finally {
            tryPersistAudit(request, responseWrapper);
        }
    }

    private void tryPersistAudit(HttpServletRequest request, StatusCaptureResponseWrapper response) {
        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/api")) {
            return;
        }
        ApiAuditLog audit = new ApiAuditLog();
        audit.setId(UUID.randomUUID());
        audit.setMethod(request.getMethod());
        audit.setPath(path);
        audit.setStatus(response.getStatus());
        audit.setIp(request.getRemoteAddr());
        audit.setCreatedAt(OffsetDateTime.now());

        if (request.getUserPrincipal() instanceof ApiKeyAuthenticationToken apiKeyAuthenticationToken) {
            apiKeyRepository.findById(apiKeyAuthenticationToken.getApiKeyId())
                    .ifPresent(audit::setApiKey);
        }

        apiAuditLogRepository.save(audit);
    }
}