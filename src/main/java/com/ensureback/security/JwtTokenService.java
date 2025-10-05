package com.ensureback.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.ensureback.user.User;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenService {

    private static final String CLAIM_STRIPE_ACCOUNT_ID = "stripe_account_id";
    private static final String CLAIM_ROLE = "role";

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final JwtProperties properties;

    public JwtTokenService(JwtProperties properties) {
        this.properties = properties;
        this.algorithm = Algorithm.HMAC256(properties.getSecret());
        this.verifier = JWT.require(algorithm).withIssuer("ensureback-api").build();
    }

    public Token createToken(User user) {
        Instant expiresAt = Instant.now().plus(properties.getExpiration());
        String token = JWT.create()
                .withIssuer("ensureback-api")
                .withSubject(user.getId().toString())
                .withClaim(CLAIM_STRIPE_ACCOUNT_ID, user.getStripeAccountId())
                .withClaim(CLAIM_ROLE, user.getRole().name())
                .withExpiresAt(Date.from(expiresAt))
                .sign(algorithm);
        return new Token(token, expiresAt);
    }

    public DecodedJWT verify(String token) throws JWTVerificationException {
        return verifier.verify(token);
    }

    public record Token(String value, Instant expiresAt) {
    }

    public record JwtPayload(UUID userId, String stripeAccountId, String role) {
    }

    public JwtPayload toPayload(DecodedJWT jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String stripeAccountId = jwt.getClaim(CLAIM_STRIPE_ACCOUNT_ID).asString();
        String role = jwt.getClaim(CLAIM_ROLE).asString();
        return new JwtPayload(userId, stripeAccountId, role);
    }
}
