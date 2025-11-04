package com.ensureback.developer;

import com.ensureback.developer.dto.ApiKeyCreateResult;
import com.ensureback.developer.dto.ApiKeyDto;
import com.ensureback.developer.dto.IntegrationWizardStatusResponse;
import com.ensureback.developer.dto.IntegrationWizardUpdateRequest;
import com.ensureback.developer.dto.StripeCallbackResponse;
import com.ensureback.developer.dto.StripeConnectStartResponse;
import com.ensureback.developer.dto.WebhookRegisterRequest;
import com.ensureback.developer.dto.WebhookTestResult;
import com.ensureback.merchant.Merchant;
import com.ensureback.merchant.MerchantRepository;
import com.ensureback.security.ApiKeyAuthenticationToken;
import com.ensureback.security.EnsurebackUserDetails;
import com.ensureback.user.User;
import com.ensureback.user.UserRepository;
import com.ensureback.security.JwtTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import com.stripe.exception.StripeException;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/developer/wizard")
public class IntegrationWizardController {

    private static final Logger log = LoggerFactory.getLogger(IntegrationWizardController.class);

    private final IntegrationWizardService integrationWizardService;
    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;

    public IntegrationWizardController(IntegrationWizardService integrationWizardService,
                                       MerchantRepository merchantRepository,
                                       UserRepository userRepository,
                                       JwtTokenService jwtTokenService) {
        this.integrationWizardService = integrationWizardService;
        this.merchantRepository = merchantRepository;
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN','API_KEY')")
    public IntegrationWizardStatusResponse status(@AuthenticationPrincipal EnsurebackUserDetails principal,
                                                  @RequestParam(value = "merchantId", required = false) UUID merchantId) {
        UUID resolvedMerchant = resolveMerchantId(principal, merchantId);
        return integrationWizardService.getStatus(resolvedMerchant);
    }

    @PatchMapping("/status")
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    public IntegrationWizardStatusResponse updateStatus(@AuthenticationPrincipal EnsurebackUserDetails principal,
                                                        @RequestParam(value = "merchantId", required = false) UUID merchantId,
                                                        @RequestBody IntegrationWizardUpdateRequest request) {
        UUID resolvedMerchant = resolveMerchantId(principal, merchantId);
        return integrationWizardService.updateStatus(resolvedMerchant, request);
    }

    @PostMapping("/api-keys")
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    public ApiKeyCreateResult createApiKey(@AuthenticationPrincipal EnsurebackUserDetails principal,
                                           @RequestParam(value = "merchantId", required = false) UUID merchantId) {
        UUID resolvedMerchant = resolveMerchantId(principal, merchantId);
        return integrationWizardService.generateApiKey(resolvedMerchant);
    }

    @GetMapping("/api-keys")
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN','API_KEY')")
    public List<ApiKeyDto> listApiKeys(@AuthenticationPrincipal EnsurebackUserDetails principal,
                                       @RequestParam(value = "merchantId", required = false) UUID merchantId) {
        UUID resolvedMerchant = resolveMerchantId(principal, merchantId);
        return integrationWizardService.listApiKeys(resolvedMerchant);
    }

    @DeleteMapping("/api-keys/{apiKeyId}")
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN','API_KEY')")
    public IntegrationWizardStatusResponse revokeApiKey(@AuthenticationPrincipal EnsurebackUserDetails principal,
                                                        @PathVariable UUID apiKeyId,
                                                        @RequestParam(value = "merchantId", required = false) UUID merchantId) {
        UUID resolvedMerchant = resolveMerchantId(principal, merchantId);
        return integrationWizardService.revokeApiKey(resolvedMerchant, apiKeyId);
    }

    @PostMapping("/webhook/register")
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN','API_KEY')")
    public IntegrationWizardStatusResponse registerWebhook(@AuthenticationPrincipal EnsurebackUserDetails principal,
                                                           @RequestParam(value = "merchantId", required = false) UUID merchantId,
                                                           @Valid @RequestBody WebhookRegisterRequest request) {
        UUID resolvedMerchant = resolveMerchantId(principal, merchantId);
        return integrationWizardService.registerWebhook(resolvedMerchant, request.url());
    }

    @GetMapping("/webhook/test")
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN','API_KEY')")
    public WebhookTestResult sendTest(@AuthenticationPrincipal EnsurebackUserDetails principal,
                                      @RequestParam(value = "merchantId", required = false) UUID merchantId) {
        UUID resolvedMerchant = resolveMerchantId(principal, merchantId);
        return integrationWizardService.sendTestWebhook(resolvedMerchant);
    }

    @GetMapping("/stripe/connect")
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    public StripeConnectStartResponse startStripe(@AuthenticationPrincipal EnsurebackUserDetails principal,
                                                  @RequestParam(value = "returnPath", required = false, defaultValue = "/developer") String returnPath) {
        UUID userId = principal != null ? principal.getUserId() : null;
        return integrationWizardService.startStripeConnect(userId, returnPath);
    }

    @GetMapping("/stripe/callback")
    @PermitAll
    public ResponseEntity<StripeCallbackResponse> stripeCallback(@RequestParam("state") String state,
                                                                 @RequestParam(value = "code", required = false) String code,
                                                                 @RequestParam(value = "error", required = false) String error,
                                                                 @RequestParam(value = "error_description", required = false) String errorDescription) throws StripeException {
        StripeCallbackResponse response = integrationWizardService.handleStripeCallback(state, code, error, errorDescription);
        return ResponseEntity.ok(response);
    }

    private UUID resolveMerchantId(EnsurebackUserDetails principal, UUID requestedMerchantId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof ApiKeyAuthenticationToken apiKeyAuth) {
            UUID merchantId = apiKeyAuth.getMerchantId();
            if (requestedMerchantId != null && !requestedMerchantId.equals(merchantId)) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Merchant context mismatch");
            }
            if (log.isDebugEnabled()) {
                log.debug("Resolved merchant via API key: merchantId={}", merchantId);
            }
            return merchantId;
        }
        if (principal == null) {
            throw new IllegalStateException("Authenticated user required");
        }
        User user = principal.getUser();
        if (user.getRole() == User.Role.ADMIN) {
            if (requestedMerchantId == null) {
                throw new IllegalArgumentException("merchantId required for admin operations");
            }
            if (log.isDebugEnabled()) {
                log.debug("Resolved merchant via ADMIN principal: merchantId={}", requestedMerchantId);
            }
            return requestedMerchantId;
        }
        UUID resolved = merchantRepository.findByUserId(user.getId())
                .map(Merchant::getId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found for user"));
        if (log.isDebugEnabled()) {
            log.debug("Resolved merchant via MERCHANT principal: merchantId={}", resolved);
        }
        return resolved;
    }

   /* private UUID resolveMerchantIdWithCookieFallback(EnsurebackUserDetails principal, UUID requestedMerchantId, HttpServletRequest request) {
        try {
            return resolveMerchantId(principal, requestedMerchantId);
        } catch (IllegalStateException ex) {
            // Try EB_AUTH cookie-based resolution if principal not injected
            if (request != null && request.getCookies() != null) {
                for (Cookie c : request.getCookies()) {
                    if ("EB_AUTH".equals(c.getName())) {
                        try {
                            var decoded = jwtTokenService.verify(c.getValue());
                            JwtTokenService.JwtPayload payload = jwtTokenService.toPayload(decoded);
                            UUID userId = payload.userId();
                            User user = userRepository.findById(userId).orElse(null);
                            if (user == null) break;
                            if (user.getRole() == User.Role.ADMIN) {
                                if (requestedMerchantId == null) {
                                    throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "merchantId required for admin operations");
                                }
                                return requestedMerchantId;
                            }
                            return merchantRepository.findByUserId(user.getId())
                                    .map(Merchant::getId)
                                    .orElseThrow(() -> new IllegalArgumentException("Merchant not found for user"));
                        } catch (Exception ignored) {
                            break;
                        }
                    }
                }
            }
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Session expired or missing");
        }
    }*/
}
