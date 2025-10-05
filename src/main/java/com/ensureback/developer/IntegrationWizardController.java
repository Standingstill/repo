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
import com.stripe.exception.StripeException;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
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

    private final IntegrationWizardService integrationWizardService;
    private final MerchantRepository merchantRepository;

    public IntegrationWizardController(IntegrationWizardService integrationWizardService,
                                       MerchantRepository merchantRepository) {
        this.integrationWizardService = integrationWizardService;
        this.merchantRepository = merchantRepository;
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
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    public IntegrationWizardStatusResponse revokeApiKey(@AuthenticationPrincipal EnsurebackUserDetails principal,
                                                        @PathVariable UUID apiKeyId,
                                                        @RequestParam(value = "merchantId", required = false) UUID merchantId) {
        UUID resolvedMerchant = resolveMerchantId(principal, merchantId);
        return integrationWizardService.revokeApiKey(resolvedMerchant, apiKeyId);
    }

    @PostMapping("/webhook/register")
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    public IntegrationWizardStatusResponse registerWebhook(@AuthenticationPrincipal EnsurebackUserDetails principal,
                                                           @RequestParam(value = "merchantId", required = false) UUID merchantId,
                                                           @Valid @RequestBody WebhookRegisterRequest request) {
        UUID resolvedMerchant = resolveMerchantId(principal, merchantId);
        return integrationWizardService.registerWebhook(resolvedMerchant, request.url());
    }

    @GetMapping("/webhook/test")
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
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
            return apiKeyAuth.getMerchantId();
        }
        if (principal == null) {
            throw new IllegalStateException("Authenticated user required");
        }
        User user = principal.getUser();
        if (user.getRole() == User.Role.ADMIN) {
            if (requestedMerchantId == null) {
                throw new IllegalArgumentException("merchantId required for admin operations");
            }
            return requestedMerchantId;
        }
        return merchantRepository.findByUserId(user.getId())
                .map(Merchant::getId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found for user"));
    }
}
