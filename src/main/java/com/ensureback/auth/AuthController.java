package com.ensureback.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ensureback.auth.dto.LoginResponse;
import com.ensureback.auth.dto.StripeConnectCallbackRequest;
import com.ensureback.auth.dto.StripeConnectStartRequest;
import com.ensureback.auth.dto.StripeConnectStartResponse;
import com.stripe.exception.StripeException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final StripeConnectService stripeConnectService;

    public AuthController(StripeConnectService stripeConnectService) {
        this.stripeConnectService = stripeConnectService;
    }

    @PostMapping("/connect/start")
    public ResponseEntity<StripeConnectStartResponse> start(@Validated @RequestBody StripeConnectStartRequest request) {
        StripeConnectStartResponse response = stripeConnectService.start(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/connect/callback")
    public ResponseEntity<LoginResponse> callback(@Validated @RequestBody StripeConnectCallbackRequest request) throws StripeException {
        LoginResponse response = stripeConnectService.complete(request);
        return ResponseEntity.ok(response);
    }
}
