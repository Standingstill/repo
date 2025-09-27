package com.ensureback.integration;

import com.ensureback.integration.dto.AftershipKeyRequest;
import com.ensureback.integration.dto.IntegrationChecklistResponse;
import com.ensureback.integration.dto.StripeConnectResponse;
import com.ensureback.integration.dto.TestChargeResponse;
import com.ensureback.merchant.Merchant;
import com.ensureback.merchant.MerchantRepository;
import com.ensureback.security.ApiKeyAuthenticationToken;
import com.ensureback.security.EnsurebackUserDetails;
import com.ensureback.user.User;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/integration")
public class IntegrationController {

    private final IntegrationChecklistService integrationChecklistService;
    private final MerchantRepository merchantRepository;

    public IntegrationController(IntegrationChecklistService integrationChecklistService,
                                 MerchantRepository merchantRepository) {
        this.integrationChecklistService = integrationChecklistService;
        this.merchantRepository = merchantRepository;
    }

    @GetMapping("/checklist")
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN','API_KEY')")
    public ResponseEntity<IntegrationChecklistResponse> checklist(@AuthenticationPrincipal EnsurebackUserDetails principal,
                                                                  @RequestParam(value = "merchantId", required = false) UUID merchantId) {
        UUID resolvedMerchantId = resolveMerchantId(principal, merchantId);
        IntegrationChecklistResponse response = integrationChecklistService.getChecklist(resolvedMerchantId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/stripe/connect")
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    public ResponseEntity<StripeConnectResponse> stripeConnect(@AuthenticationPrincipal EnsurebackUserDetails principal,
                                                               @RequestParam(value = "merchantId", required = false) UUID merchantId) {
        UUID resolvedMerchantId = resolveMerchantId(principal, merchantId);
        StripeConnectResponse response = integrationChecklistService.initiateStripeConnect(resolvedMerchantId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/aftership/key")
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    public ResponseEntity<IntegrationChecklistResponse> aftershipKey(@AuthenticationPrincipal EnsurebackUserDetails principal,
                                                                     @RequestParam(value = "merchantId", required = false) UUID merchantId,
                                                                     @Valid @RequestBody AftershipKeyRequest request) {
        UUID resolvedMerchantId = resolveMerchantId(principal, merchantId);
        IntegrationChecklistResponse response = integrationChecklistService.updateAftershipKey(resolvedMerchantId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/test-charge")
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    public ResponseEntity<TestChargeResponse> testCharge(@AuthenticationPrincipal EnsurebackUserDetails principal,
                                                         @RequestParam(value = "merchantId", required = false) UUID merchantId) {
        UUID resolvedMerchantId = resolveMerchantId(principal, merchantId);
        TestChargeResponse response = integrationChecklistService.runTestCharge(resolvedMerchantId);
        return ResponseEntity.ok(response);
    }

    private UUID resolveMerchantId(EnsurebackUserDetails principal, UUID merchantIdFromRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof ApiKeyAuthenticationToken apiKeyAuth) {
            return apiKeyAuth.getMerchantId();
        }
        if (principal == null) {
            throw new IllegalStateException("Authenticated user required");
        }
        User user = principal.getUser();
        if (user.getRole() == User.Role.ADMIN) {
            if (merchantIdFromRequest == null) {
                throw new IllegalArgumentException("merchantId must be provided for admin operations");
            }
            return merchantIdFromRequest;
        }
        return merchantRepository.findByUserId(user.getId())
                .map(Merchant::getId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found for user"));
    }
}