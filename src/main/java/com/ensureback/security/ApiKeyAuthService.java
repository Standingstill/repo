package com.ensureback.security;

import com.ensureback.developer.ApiKey;
import com.ensureback.developer.ApiKeyRepository;
import com.ensureback.merchant.Merchant;
import com.ensureback.merchant.MerchantRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class ApiKeyAuthService {

    private static final Duration MAX_SKEW = Duration.ofMinutes(5);

    private final ApiKeyRepository apiKeyRepository;
    private final MerchantRepository merchantRepository;

    public ApiKeyAuthService(ApiKeyRepository apiKeyRepository,
                             MerchantRepository merchantRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.merchantRepository = merchantRepository;
    }

    public Optional<ApiKeyAuthenticationToken> authenticate(UUID apiKeyId,
                                                             String rawKey,
                                                             String providedSignature,
                                                             Instant timestamp,
                                                             String canonicalRequest) {
        if (apiKeyId == null || !StringUtils.hasText(rawKey) || !StringUtils.hasText(providedSignature)) {
            return Optional.empty();
        }
        if (timestamp == null || Duration.between(timestamp, Instant.now()).abs().compareTo(MAX_SKEW) > 0) {
            return Optional.empty();
        }
        return apiKeyRepository.findById(apiKeyId)
                .filter(apiKey -> !apiKey.isRevoked())
                .flatMap(apiKey -> verifySignature(apiKey, rawKey, providedSignature, canonicalRequest));
    }

    private Optional<ApiKeyAuthenticationToken> verifySignature(ApiKey apiKey,
                                                                String rawKey,
                                                                String providedSignature,
                                                                String canonicalRequest) {
        String hashedCandidate = hashKey(rawKey);
        if (!MessageDigestUtil.constantTimeEquals(hashedCandidate, apiKey.getKeyHash())) {
            return Optional.empty();
        }
        String expectedSignature = hmacSha256(rawKey, canonicalRequest);
        if (!MessageDigestUtil.constantTimeEquals(expectedSignature, providedSignature)) {
            return Optional.empty();
        }
        Merchant merchant = apiKey.getMerchant();
        if (merchant == null) {
            return Optional.empty();
        }
        return merchantRepository.findById(merchant.getId())
                .filter(m -> m.getUser() != null)
                .map(m -> new ApiKeyAuthenticationToken(apiKey.getId(), m.getId(), m.getUser().getStripeAccountId()));
    }

    private String hmacSha256(String rawKey, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(rawKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            byte[] hmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmac);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute HMAC", ex);
        }
    }

    private String hashKey(String rawKey) {
        return MessageDigestUtil.sha256Base64(rawKey);
    }
}
