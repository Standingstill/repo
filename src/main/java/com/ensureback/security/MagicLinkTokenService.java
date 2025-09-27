package com.ensureback.security;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

@Component
public class MagicLinkTokenService {

    private static final String SUBJECT = "buyer-magic-link";
    private static final String CLAIM_ORDER_ID = "orderId";
    private static final String CLAIM_BUYER_EMAIL = "buyerEmail";

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final MagicLinkProperties properties;

    public MagicLinkTokenService(MagicLinkProperties properties) {
        this.properties = properties;
        this.algorithm = Algorithm.HMAC256(properties.getSecret());
        this.verifier = JWT.require(algorithm)
                .withIssuer(SUBJECT)
                .build();
    }

    public Token createToken(UUID orderId, String buyerEmail) {
        Instant expiresAt = Instant.now().plus(properties.getExpiration());
        String token = JWT.create()
                .withIssuer(SUBJECT)
                .withSubject(SUBJECT)
                .withClaim(CLAIM_ORDER_ID, orderId.toString())
                .withClaim(CLAIM_BUYER_EMAIL, buyerEmail)
                .withExpiresAt(Date.from(expiresAt))
                .sign(algorithm);
        return new Token(token, expiresAt);
    }

    public Payload validate(String token) throws JWTVerificationException {
        DecodedJWT decodedJWT = verifier.verify(token);
        String orderIdClaim = decodedJWT.getClaim(CLAIM_ORDER_ID).asString();
        String buyerEmail = decodedJWT.getClaim(CLAIM_BUYER_EMAIL).asString();
        if (orderIdClaim == null || buyerEmail == null) {
            throw new JWTVerificationException("Missing required claims");
        }
        UUID orderId = UUID.fromString(orderIdClaim);
        return new Payload(orderId, buyerEmail, decodedJWT.getExpiresAt().toInstant());
    }

    public record Token(String value, Instant expiresAt) {
    }

    public record Payload(UUID orderId, String buyerEmail, Instant expiresAt) {
    }
}
