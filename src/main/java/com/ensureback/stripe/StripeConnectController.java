package com.ensureback.stripe;

import com.ensureback.auth.StripeConnectService;
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

    public StripeConnectController(StripeConnectService stripeConnectService,
                                   IntegrationWizardService integrationWizardService) {
        this.stripeConnectService = stripeConnectService;
        this.integrationWizardService = integrationWizardService;
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
    public ResponseEntity<?> callback(@RequestParam("state") String state,
                                      @RequestParam(value = "code", required = false) String code,
                                      @RequestParam(value = "error", required = false) String error,
                                      @RequestParam(value = "error_description", required = false) String errorDescription,
                                      @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                                      @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String acceptHeader)
            throws StripeException {

        if (!StringUtils.hasText(state)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Stripe session state");
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
            return ResponseEntity.ok(payload);
        }

        URI redirectUri = response.redirectUrl() != null ? URI.create(response.redirectUrl()) : null;
        if (redirectUri == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stripe callback missing redirect URL");
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(redirectUri)
                .build();
    }
}
