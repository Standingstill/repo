package com.ensureback.auth;

import com.ensureback.auth.dto.LoginResponse;
import com.ensureback.auth.dto.StripeConnectCallbackRequest;
import com.ensureback.auth.dto.StripeConnectStartRequest;
import com.ensureback.auth.dto.StripeConnectStartResponse;
import com.ensureback.security.JwtTokenService;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Transactional
public class StripeConnectService {

    private static final Logger log = LoggerFactory.getLogger(StripeConnectService.class);
    private static final Duration SESSION_TTL = Duration.ofMinutes(15);

    private final StripeConnectSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final StripeProperties stripeProperties;

    public StripeConnectService(StripeConnectSessionRepository sessionRepository,
                                UserRepository userRepository,
                                JwtTokenService jwtTokenService,
                                StripeProperties stripeProperties) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.stripeProperties = stripeProperties;
    }

    public StripeConnectStartResponse start(StripeConnectStartRequest request) {
        ensureStripeConfig();
        Stripe.apiKey = stripeProperties.getSecretKey();
        Stripe.clientId = stripeProperties.getConnectClientId();
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown merchant"));
        if (user.getRole() != User.Role.MERCHANT) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Only merchant accounts can connect with Stripe");
        }

        StripeConnectSession session = new StripeConnectSession(UUID.randomUUID(), user.getId(), OffsetDateTime.now());
        sessionRepository.save(session);

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUri(URI.create("https://connect.stripe.com/oauth/authorize"))
                .queryParam("response_type", "code")
                .queryParam("scope", "read_write")
                .queryParam("client_id", stripeProperties.getConnectClientId())
                .queryParam("redirect_uri", stripeProperties.getConnectRedirectUri())
                .queryParam("state", session.getState().toString());

        builder.queryParam("stripe_user[email]", user.getEmail());

        String authorizeUrl = builder.build(true).toUriString();
        log.debug("Initialized Stripe Connect session {} for user {}", session.getState(), user.getId());
        return new StripeConnectStartResponse(authorizeUrl);
    }

    public LoginResponse complete(StripeConnectCallbackRequest request) throws StripeException {
        ensureStripeConfig();
        Stripe.apiKey = stripeProperties.getSecretKey();
        Stripe.clientId = stripeProperties.getConnectClientId();
        UUID state = parseState(request.state());
        Optional<StripeConnectSession> sessionOpt = sessionRepository.findById(state);
        if (sessionOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown or expired Stripe Connect session");
        }
        StripeConnectSession session = sessionOpt.get();
        if (session.getCreatedAt().isBefore(OffsetDateTime.now().minus(SESSION_TTL))) {
            sessionRepository.delete(session);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe Connect session has expired");
        }
        if (request.error() != null) {
            sessionRepository.delete(session);
            String message = Optional.ofNullable(request.errorDescription())
                    .orElse("Stripe authorization failed: " + request.error());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }

        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Merchant no longer exists"));

        String code = Optional.ofNullable(request.code())
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Stripe authorization code"));

        Map<String, Object> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("code", code);

        RequestOptions requestOptions = RequestOptions.builder()
                .setApiKey(stripeProperties.getSecretKey())
                .setClientId(stripeProperties.getConnectClientId())
                .build();

        TokenResponse tokenResponse;
        try {
            tokenResponse = OAuth.token(params, requestOptions);
        } catch (AuthenticationException | InvalidRequestException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to authorize Stripe account", ex);
        }

        String stripeAccountId = tokenResponse.getStripeUserId();
        if (stripeAccountId == null || stripeAccountId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe did not return an account identifier");
        }

        user.setStripeAccountId(stripeAccountId);
        userRepository.save(user);
        sessionRepository.delete(session);

        var token = jwtTokenService.createToken(user);
        log.info("Stripe Connect login complete for user {} with account {}", user.getId(), stripeAccountId);
        return new LoginResponse(token.value(), "Bearer", token.expiresAt(), user.getRole().name());
    }

    private void ensureStripeConfig() {
        if (stripeProperties.getSecretKey() == null || stripeProperties.getSecretKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stripe secret key is not configured");
        }
        if (stripeProperties.getConnectClientId() == null || stripeProperties.getConnectClientId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stripe Connect client id is not configured");
        }
        if (stripeProperties.getConnectRedirectUri() == null || stripeProperties.getConnectRedirectUri().isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stripe Connect redirect URI is not configured");
        }
    }

    private UUID parseState(String stateValue) {
        try {
            return UUID.fromString(stateValue);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe Connect state parameter", ex);
        }
    }
}
