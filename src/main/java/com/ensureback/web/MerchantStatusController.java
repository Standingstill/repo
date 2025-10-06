package com.ensureback.web;

import com.ensureback.security.ApiKeyAuthenticationToken;
import com.ensureback.security.EnsurebackUserDetails;
import com.ensureback.user.User;
import com.ensureback.user.UserRepository;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant")
public class MerchantStatusController {

    private static final Logger log = LoggerFactory.getLogger(MerchantStatusController.class);

    private final UserRepository userRepository;

    public MerchantStatusController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            log.info("Merchant status request rejected: unauthenticated");
            return unauthorized();
        }

        Optional<User> userOptional = resolveUser(authentication);
        boolean integrated = userOptional
                .map(User::getStripeAccountId)
                .filter(StringUtils::hasText)
                .isPresent();

        log.info("Merchant status request for principal '{}' resolved to integrated={}",
                authentication.getName(), integrated);

        return ResponseEntity.ok(new IntegrationStatusResponse(integrated));
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private Optional<User> resolveUser(Authentication authentication) {
        if (authentication instanceof ApiKeyAuthenticationToken apiKeyAuth) {
            Object principal = apiKeyAuth.getPrincipal();
            if (principal instanceof String stripeAccountId && StringUtils.hasText(stripeAccountId)) {
                return userRepository.findByStripeAccountId(stripeAccountId);
            }
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof EnsurebackUserDetails userDetails) {
            return Optional.ofNullable(userDetails.getUser());
        }
        if (principal instanceof User user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    private ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Unauthorized"));
    }

    public record IntegrationStatusResponse(boolean isIntegrated) {
    }
}
