package com.ensureback.auth;

import com.ensureback.auth.dto.MagicLinkRequest;
import com.ensureback.auth.dto.MagicLinkResponse;
import com.ensureback.auth.dto.MagicLinkValidationResponse;
import com.ensureback.config.EnsurebackProperties;
import com.ensureback.email.EmailService;
import com.ensureback.security.MagicLinkTokenService;
import com.auth0.jwt.exceptions.JWTVerificationException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/buyer/magic-link")
public class MagicLinkController {

    private final MagicLinkTokenService magicLinkTokenService;
    private final EmailService emailService;
    private final EnsurebackProperties ensurebackProperties;

    public MagicLinkController(MagicLinkTokenService magicLinkTokenService,
                               EmailService emailService,
                               EnsurebackProperties ensurebackProperties) {
        this.magicLinkTokenService = magicLinkTokenService;
        this.emailService = emailService;
        this.ensurebackProperties = ensurebackProperties;
    }

    @PostMapping
    public ResponseEntity<MagicLinkResponse> generate(@Validated @RequestBody MagicLinkRequest request) {
        var token = magicLinkTokenService.createToken(request.orderId(), request.buyerEmail());
        String base = ensurebackProperties.getAppBaseUrl();
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        String link = base + "buyer/magic-link?token=" + token.value();
        emailService.sendEmail(request.buyerEmail(),
                "Access your EnsureBack order",
                "magic-link",
                Map.of(
                        "email", request.buyerEmail(),
                        "orderId", request.orderId(),
                        "token", token.value(),
                        "link", link
                ));
        return ResponseEntity.ok(new MagicLinkResponse(token.value(), token.expiresAt()));
    }

    @GetMapping("/validate")
    public ResponseEntity<MagicLinkValidationResponse> validate(@RequestParam("token") String token) {
        try {
            var payload = magicLinkTokenService.validate(token);
            return ResponseEntity.ok(new MagicLinkValidationResponse(true, payload.orderId(), payload.buyerEmail(), payload.expiresAt(), "Token valid"));
        } catch (JWTVerificationException | IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MagicLinkValidationResponse(false, null, null, null, "Invalid or expired token"));
        }
    }
}