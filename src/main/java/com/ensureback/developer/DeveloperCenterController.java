package com.ensureback.developer;

import com.ensureback.developer.dto.ApiKeyCreateRequest;
import com.ensureback.developer.dto.ApiKeyCreateResponse;
import com.ensureback.developer.dto.ApiKeyDto;
import com.ensureback.developer.dto.IntegrationChecklistDto;
import com.ensureback.developer.dto.IntegrationChecklistUpdateRequest;
import com.ensureback.merchant.Merchant;
import com.ensureback.merchant.MerchantRepository;
import com.ensureback.security.ApiKeyAuthenticationToken;
import com.ensureback.security.EnsurebackUserDetails;
import com.ensureback.user.User;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/developer")
public class DeveloperCenterController {

    private final DeveloperCenterService developerCenterService;
    private final MerchantRepository merchantRepository;

    public DeveloperCenterController(DeveloperCenterService developerCenterService,
                                     MerchantRepository merchantRepository) {
        this.developerCenterService = developerCenterService;
        this.merchantRepository = merchantRepository;
    }

    @PostMapping("/api-keys")
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    public ResponseEntity<ApiKeyCreateResponse> createApiKey(@AuthenticationPrincipal EnsurebackUserDetails principal,
                                                             @Valid @RequestBody ApiKeyCreateRequest request) {
        UUID merchantId = resolveMerchantId(principal, request.merchantId());
        ApiKeyCreateResponse response = developerCenterService.generateApiKey(merchantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api-keys")
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    public ResponseEntity<List<ApiKeyDto>> listApiKeys(@AuthenticationPrincipal EnsurebackUserDetails principal,
                                                       @RequestParam(value = "merchantId", required = false) UUID merchantId) {
        UUID resolvedMerchantId = resolveMerchantId(principal, merchantId);
        return ResponseEntity.ok(developerCenterService.listApiKeys(resolvedMerchantId));
    }

    @DeleteMapping("/api-keys/{apiKeyId}")
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    public ResponseEntity<Void> revokeApiKey(@AuthenticationPrincipal EnsurebackUserDetails principal,
                                             @PathVariable UUID apiKeyId,
                                             @RequestParam(value = "merchantId", required = false) UUID merchantId) {
        UUID resolvedMerchantId = resolveMerchantId(principal, merchantId);
        developerCenterService.revokeApiKey(resolvedMerchantId, apiKeyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status/{merchantId}")
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN','API_KEY')")
    public ResponseEntity<IntegrationChecklistDto> getStatus(@PathVariable UUID merchantId,
                                                             @AuthenticationPrincipal EnsurebackUserDetails principal) {
        if (!isAuthorizedForMerchant(merchantId, principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(developerCenterService.getIntegrationChecklist(merchantId));
    }

    @PatchMapping("/status/{merchantId}")
    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    public ResponseEntity<IntegrationChecklistDto> updateStatus(@PathVariable UUID merchantId,
                                                                @AuthenticationPrincipal EnsurebackUserDetails principal,
                                                                @RequestBody IntegrationChecklistUpdateRequest request) {
        if (!isAuthorizedForMerchant(merchantId, principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        IntegrationChecklistDto updated = developerCenterService.updateIntegrationChecklist(merchantId, request);
        return ResponseEntity.ok(updated);
    }

    private boolean isAuthorizedForMerchant(UUID merchantId, EnsurebackUserDetails principal) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof ApiKeyAuthenticationToken apiKeyAuth) {
            return merchantId.equals(apiKeyAuth.getMerchantId());
        }
        if (principal == null) {
            return false;
        }
        User user = principal.getUser();
        return switch (user.getRole()) {
            case ADMIN -> true;
            case MERCHANT -> merchantRepository.findByUserId(user.getId())
                    .map(Merchant::getId)
                    .filter(merchantId::equals)
                    .isPresent();
            default -> false;
        };
    }

    private UUID resolveMerchantId(EnsurebackUserDetails principal, UUID merchantIdFromRequest) {
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