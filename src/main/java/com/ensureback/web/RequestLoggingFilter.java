package com.ensureback.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Instant start = Instant.now();
        int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        try {
            filterChain.doFilter(request, response);
        } finally {
            try {
                status = response.getStatus();
            } catch (Exception ignored) {
            }
            long duration = Duration.between(start, Instant.now()).toMillis();
            String correlationId = RequestContext.getCorrelationId();
            String uri = request.getRequestURI();
            String query = maskPii(request.getQueryString());
            String method = request.getMethod();
            String remoteIp = request.getRemoteAddr();
            log.info("requestId={} method={} uri={} query={} status={} durationMs={} ip={}",
                    correlationId,
                    method,
                    uri,
                    query,
                    status,
                    duration,
                    remoteIp);
        }
    }

    private String maskPii(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.replaceAll("(?i)(email=)([^&]+)", "$1***")
                .replaceAll("(?i)(token=)([^&]+)", "$1***");
    }
}