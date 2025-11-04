package com.ensureback.auth;

import com.ensureback.config.EnsurebackProperties;
import com.ensureback.security.JwtTokenService;
import com.ensureback.merchant.Merchant;
import com.ensureback.merchant.MerchantRepository;
import com.ensureback.developer.IntegrationChecklist;
import com.ensureback.developer.IntegrationChecklistRepository;
import com.ensureback.stripe.StripeProperties;
import com.ensureback.user.User;
import com.ensureback.user.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.oauth.TokenResponse;
import com.stripe.net.OAuth;
import com.stripe.net.RequestOptions;
import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Transactional
public class StripeConnectService {

    private static final Logger log = LoggerFactory.getLogger(StripeConnectService.class);
    private static final Duration SESSION_TTL = Duration.ofHours(24);
    private static final String DASHBOARD_PATH = "/dashboard";

    private final StripeConnectSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final MerchantRepository merchantRepository;
    private final IntegrationChecklistRepository integrationChecklistRepository;
    private final StripeProperties stripeProperties;
    private final EnsurebackProperties ensurebackProperties;

    public StripeConnectService(StripeConnectSessionRepository sessionRepository,
                                UserRepository userRepository,
                                JwtTokenService jwtTokenService,
                                StripeProperties stripeProperties,
                                EnsurebackProperties ensurebackProperties,
                                MerchantRepository merchantRepository,
                                IntegrationChecklistRepository integrationChecklistRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.stripeProperties = stripeProperties;
        this.ensurebackProperties = ensurebackProperties;
        this.merchantRepository = merchantRepository;
        this.integrationChecklistRepository = integrationChecklistRepository;
    }

    public record OnboardingRedirect(URI redirectUri, UUID state, boolean alreadyConnected) {
    }

    public record CallbackResult(URI redirectUri, boolean connected, String stripeAccountId, String token) {
    }

    public OnboardingRedirect initiateOnboarding(UUID userId, String requestedReturnPath) {
        ensureStripeConfig();
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        }

        String normalizedReturnPath = normalizeReturnPath(requestedReturnPath);

        if (user != null && StringUtils.hasText(user.getStripeAccountId())) {
            URI redirectUri = buildAppRedirect(normalizedReturnPath, null, user.getStripeAccountId(), null, null);
            return new OnboardingRedirect(redirectUri, null, true);
        }

        if (user != null) {
            sessionRepository.deleteByUser(user);
        }

        User.Role targetRole = user != null ? user.getRole() : User.Role.MERCHANT;

        StripeConnectSession session = new StripeConnectSession(
                UUID.randomUUID(),
                user,
                targetRole,
                normalizedReturnPath,
                OffsetDateTime.now()
        );
        sessionRepository.save(session);

        Stripe.apiKey = stripeProperties.getSecretKey();
        Stripe.clientId = stripeProperties.getConnectClientId();

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUri(URI.create("https://connect.stripe.com/oauth/authorize"))
                .queryParam("response_type", "code")
                .queryParam("scope", "read_write")
                .queryParam("client_id", stripeProperties.getConnectClientId())
                .queryParam("redirect_uri", stripeProperties.getConnectRedirectUri())
                .queryParam("state", session.getState().toString());

        String authorizeUrl = builder.build(true).toUriString();
        if (user != null) {
            log.debug("Initialized Stripe Connect onboarding session {} for user {}", session.getState(), user.getId());
        } else {
            log.debug("Initialized Stripe Connect onboarding session {} for anonymous user", session.getState());
        }
        return new OnboardingRedirect(URI.create(authorizeUrl), session.getState(), false);
    }

    public CallbackResult completeOnboarding(String stateValue,
                                             String code,
                                             String error,
                                             String errorDescription) throws StripeException {
        ensureStripeConfig();
        UUID state = parseState(stateValue);
        StripeConnectSession session = sessionRepository.findById(state)
                .orElseThrow(() -> {
                    log.warn("Stripe Connect callback received unknown state {}", stateValue);
                    return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown or expired Stripe Connect session");
                });

        if (session.getCreatedAt().isBefore(OffsetDateTime.now().minus(SESSION_TTL))) {
            log.warn("Stripe Connect session {} expired at {}", stateValue, session.getCreatedAt());
            sessionRepository.delete(session);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe Connect session has expired");
        }

        User user = session.getUser();
        String normalizedReturnPath = normalizeReturnPath(session.getReturnPath());

        if (StringUtils.hasText(error)) {
            String existingAccountId = user != null ? user.getStripeAccountId() : null;
            sessionRepository.delete(session);
            URI redirectUri = buildAppRedirect(
                    normalizedReturnPath,
                    null,
                    existingAccountId,
                    error,
                    Optional.ofNullable(errorDescription).orElse(error)
            );
            return new CallbackResult(redirectUri, StringUtils.hasText(existingAccountId), existingAccountId, null);
        }

        String authorizationCode = Optional.ofNullable(code)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElseThrow(() -> {
                    log.warn("Stripe Connect callback missing authorization code for state {}", stateValue);
                    sessionRepository.delete(session);
                    return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Stripe authorization code");
                });

        Stripe.apiKey = stripeProperties.getSecretKey();
        Stripe.clientId = stripeProperties.getConnectClientId();

        TokenResponse tokenResponse = exchangeAuthorizationCode(authorizationCode);
        String stripeAccountId = Optional.ofNullable(tokenResponse.getStripeUserId())
                .map(String::trim)
                .filter(StringUtils::hasText)
                .orElseThrow(() -> {
                    log.warn("Stripe Connect token response did not include an account id for state {}", stateValue);
                    return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe did not return an account identifier");
                });

        if (user == null) {
            user = userRepository.findByStripeAccountId(stripeAccountId).orElse(null);
        }

        if (user == null) {
            user = new User(UUID.randomUUID(), session.getTargetRole(), stripeAccountId, null, null);
            user = userRepository.save(user);
            log.info("Created new user {} for Stripe account {}", user.getId(), stripeAccountId);
        } else if (!stripeAccountId.equals(user.getStripeAccountId())) {
            user.setStripeAccountId(stripeAccountId);
            user = userRepository.save(user);
            log.info("Updated Stripe account for user {} to {}", user.getId(), stripeAccountId);
        }

        sessionRepository.delete(session);

        Merchant merchant = ensureMerchantFor(user, stripeAccountId);
        markStripeConnected(merchant);
        log.info("Linked merchant {} to user {}", merchant.getId(), user.getId());

        JwtTokenService.Token token = jwtTokenService.createToken(user);
        URI redirectUri = buildAppRedirect(normalizedReturnPath, token.value(), stripeAccountId, null, null);
        log.info("Stripe Connect onboarding completed for user {} with account {}", user.getId(), stripeAccountId);
        return new CallbackResult(redirectUri, true, stripeAccountId, token.value());
    }

    private Merchant ensureMerchantFor(User user, String stripeAccountId) {
        return merchantRepository.findByUserId(user.getId())
                .or(() -> merchantRepository.findByStripeAccountId(stripeAccountId))
                .orElseGet(() -> {
                    Merchant m = new Merchant();
                    m.setId(UUID.randomUUID());
                    m.setUser(user);
                    m.setStripeAccountId(stripeAccountId);
                    if (m.getBusinessName() == null || m.getBusinessName().isBlank()) {
                        m.setBusinessName("New Merchant");
                    }
                    if (m.getSupportEmail() == null || m.getSupportEmail().isBlank()) {
                        m.setSupportEmail("support@merchant.local");
                    }
                    m.setDisputeWindowHours(120);
                    return merchantRepository.save(m);
                });
    }

    private void markStripeConnected(Merchant merchant) {
        IntegrationChecklist checklist = integrationChecklistRepository.findByMerchantId(merchant.getId())
                .orElseGet(() -> {
                    IntegrationChecklist ic = new IntegrationChecklist();
                    ic.setId(UUID.randomUUID());
                    ic.setMerchant(merchant);
                    ic.setStripeConnected(false);
                    ic.setWebhookRegistered(false);
                    ic.setTestChargePassed(false);
                    ic.setUpdatedAt(OffsetDateTime.now());
                    return integrationChecklistRepository.save(ic);
                });
        if (!checklist.isStripeConnected()) {
            checklist.setStripeConnected(true);
            checklist.setUpdatedAt(OffsetDateTime.now());
            integrationChecklistRepository.save(checklist);
        }
    }

    private TokenResponse exchangeAuthorizationCode(String code) throws StripeException {
        Map<String, Object> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("code", code);
        params.put("redirect_uri", stripeProperties.getConnectRedirectUri());

        RequestOptions requestOptions = RequestOptions.builder()
                .setApiKey(stripeProperties.getSecretKey())
                .setClientId(stripeProperties.getConnectClientId())
                .build();

        try {
            return OAuth.token(params, requestOptions);
        } catch (AuthenticationException | InvalidRequestException ex) {
            log.warn("Stripe OAuth token exchange failed: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to authorize Stripe account", ex);
        }
    }

    private void ensureStripeConfig() {
        if (!StringUtils.hasText(stripeProperties.getSecretKey())) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stripe secret key is not configured");
        }
        if (!StringUtils.hasText(stripeProperties.getConnectClientId())) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stripe Connect client id is not configured");
        }
        if (!StringUtils.hasText(stripeProperties.getConnectRedirectUri())) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stripe Connect redirect URI is not configured");
        }
        if (!StringUtils.hasText(ensurebackProperties.getAppBaseUrl())) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "EnsureBack app base URL is not configured");
        }
    }

    private UUID parseState(String stateValue) {
        try {
            return UUID.fromString(stateValue);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe Connect state parameter", ex);
        }
    }

    private String normalizeReturnPath(String returnPath) {
        if (!StringUtils.hasText(returnPath)) {
            return DASHBOARD_PATH;
        }
        String trimmed = returnPath.trim();
        if (!trimmed.startsWith("/")) {
            return DASHBOARD_PATH;
        }
        return trimmed;
    }

    private URI buildAppRedirect(String returnPath,
                                 String token,
                                 String stripeAccountId,
                                 String errorCode,
                                 String errorDescription) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(ensurebackProperties.getAppBaseUrl())
                .path(normalizeReturnPath(returnPath));
        if (StringUtils.hasText(token)) {
            builder.queryParam("token", token);
        }
        if (StringUtils.hasText(stripeAccountId)) {
            builder.queryParam("connected", true);
            builder.queryParam("stripeAccountId", stripeAccountId);
        }
        if (StringUtils.hasText(errorCode)) {
            builder.queryParam("stripe_error", errorCode);
            if (StringUtils.hasText(errorDescription) && !errorCode.equals(errorDescription)) {
                builder.queryParam("stripe_error_description", errorDescription);
            }
        }
        return builder.build(true).toUri();
    }
}







