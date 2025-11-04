package com.ensureback.stripe;

import com.ensureback.auth.StripeConnectService;
import com.ensureback.config.EnsurebackProperties;
import com.ensureback.developer.IntegrationWizardService;
import com.ensureback.developer.dto.StripeCallbackResponse;
import com.ensureback.security.EnsurebackUserDetails;
import com.stripe.exception.StripeException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/stripe")
public class StripeConnectController {

    private final StripeConnectService stripeConnectService;
    private final IntegrationWizardService integrationWizardService;
    private final EnsurebackProperties ensurebackProperties;

    public StripeConnectController(StripeConnectService stripeConnectService,
                                   IntegrationWizardService integrationWizardService,
                                   EnsurebackProperties ensurebackProperties) {
        this.stripeConnectService = stripeConnectService;
        this.integrationWizardService = integrationWizardService;
        this.ensurebackProperties = ensurebackProperties;
    }

    @GetMapping("/onboard")
    public ResponseEntity<?> onboard(@AuthenticationPrincipal EnsurebackUserDetails principal,
                                     @RequestParam(value = "returnPath", required = false) String returnPath,
                                     @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                                     @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String acceptHeader) {

        StripeConnectService.OnboardingRedirect redirect =
                stripeConnectService.initiateOnboarding(principal != null ? principal.getUserId() : null, returnPath);

        boolean wantsJson = "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (StringUtils.hasText(acceptHeader) && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE));

        if (wantsJson) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("redirectUrl", redirect.redirectUri().toString());
            payload.put("state", redirect.state() != null ? redirect.state().toString() : null);
            payload.put("alreadyConnected", redirect.alreadyConnected());
            return ResponseEntity.ok(payload);
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(redirect.redirectUri())
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(@RequestParam(value = "state", required = false) String state,
                                      @RequestParam(value = "code", required = false) String code,
                                      @RequestParam(value = "error", required = false) String error,
                                      @RequestParam(value = "error_description", required = false) String errorDescription,
                                      @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                                      @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String acceptHeader)
            throws StripeException {
        // If state is missing but cookie flow likely succeeded, redirect to app base (dev UX).
        if (!StringUtils.hasText(state)) {
            boolean wantsJson = "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                    || (StringUtils.hasText(acceptHeader) && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE));
            if (wantsJson) {
                return ResponseEntity.ok(Map.of(
                        "connected", true,
                        "message", "Session established"
                ));
            }
            String base = ensurebackProperties.getAppBaseUrl();
            if (!StringUtils.hasText(base)) {
                base = "/";
            }
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(base)).build();
        }

        StripeCallbackResponse response = integrationWizardService.handleStripeCallback(state, code, error, errorDescription);

        boolean wantsJson = "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (StringUtils.hasText(acceptHeader) && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE));

        if (wantsJson) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("redirectUrl", response.redirectUrl());
            payload.put("connected", response.connected());
            payload.put("stripeAccountId", response.stripeAccountId());
            payload.put("checklistUpdated", response.checklistUpdated());
            payload.put("integrated", response.integrated());
            payload.put("nextStep", response.nextStep());
            payload.put("message", response.message());
            // Also set HttpOnly cookie if a token is present in the response (not exposed in payload)
            if (response.token() != null && !response.token().isBlank()) {
                boolean isLocal = true; // dev default; cookie 'Secure' toggled below
                try {
                    java.net.URI uri = response.redirectUrl() != null ? java.net.URI.create(response.redirectUrl()) : null;
                    if (uri != null) {
                        String host = uri.getHost();
                        isLocal = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
                    }
                } catch (Exception ignore) {}
                java.time.Duration maxAge = java.time.Duration.ofHours(2);
                org.springframework.http.ResponseCookie auth = org.springframework.http.ResponseCookie.from("EB_AUTH", response.token())
                        .httpOnly(true)
                        .secure(!isLocal)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(maxAge)
                        .build();
                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, auth.toString())
                        .body(payload);
            }

            return ResponseEntity.ok(payload);
        }

        URI redirectUri = response.redirectUrl() != null ? URI.create(response.redirectUrl()) : null;
        if (redirectUri == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stripe callback missing redirect URL");
        }
        // If token is present, set an HttpOnly cookie and strip it from the redirect URL
        String token = response.token();
        try {
            boolean isLocal = false;
            try {
                String host = redirectUri.getHost();
                isLocal = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
            } catch (Exception ignore2) {}
            // In local development, keep token in URL so SPA can persist it (proxy may drop cookies on POST)
            // In non-dev, strip token from the redirect URL.
            if (!isLocal && redirectUri.getQuery() != null && redirectUri.getQuery().contains("token=")) {
                String base = redirectUri.getScheme() + "://" + redirectUri.getAuthority() + redirectUri.getPath();
                redirectUri = URI.create(base);
            }
        } catch (Exception ignore) {}

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.FOUND).location(redirectUri);
        if (token != null && !token.isBlank()) {
            boolean isLocal = "localhost".equalsIgnoreCase(redirectUri.getHost()) || "127.0.0.1".equals(redirectUri.getHost());
            java.time.Duration maxAge = java.time.Duration.ofHours(2);
            org.springframework.http.ResponseCookie auth = org.springframework.http.ResponseCookie.from("EB_AUTH", token)
                    .httpOnly(true)
                    .secure(!isLocal)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(maxAge)
                    .build();
            builder.header(HttpHeaders.SET_COOKIE, auth.toString());
        }
        return builder.build();
    }
}
