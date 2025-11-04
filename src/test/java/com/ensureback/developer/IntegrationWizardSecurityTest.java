package com.ensureback.developer;

import com.ensureback.merchant.Merchant;
import com.ensureback.merchant.MerchantRepository;
import com.ensureback.security.JwtTokenService;
import com.ensureback.user.User;
import com.ensureback.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ensureback.developer.ApiKey;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IntegrationWizardSecurityTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired MerchantRepository merchantRepository;
    @Autowired ApiKeyRepository apiKeyRepository;
    @Autowired JwtTokenService jwtTokenService;
    @Autowired ObjectMapper objectMapper;

    private Merchant merchant;
    private User user;

    @BeforeEach
    void setup() {
        user = new User(UUID.randomUUID(), User.Role.MERCHANT, "acct_test_sec_123", null, null);
        user = userRepository.save(user);
        Merchant m = new Merchant();
        try {
            var ctor = Merchant.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            m = ctor.newInstance();
        } catch (Exception ignored) { }
        m.setId(UUID.randomUUID());
        m.setUser(user);
        m.setBusinessName("Sec Test");
        m.setSupportEmail("sec@example.com");
        m.setDisputeWindowHours(72);
        merchant = merchantRepository.save(m);
    }

    @Test
    void status_withJwtMerchant_returns200() throws Exception {
        var token = jwtTokenService.createToken(user).value();
        mockMvc.perform(get("/api/developer/wizard/status").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void status_withApiKey_returns200() throws Exception {
        String rawKey = base64Random();
        ApiKey apiKey = new ApiKey();
        apiKey.setId(UUID.randomUUID());
        apiKey.setMerchant(merchant);
        apiKey.setKeyHash(sha256Base64(rawKey));
        apiKey.setSigningSecret("test-signing");
        apiKey.setRevoked(false);
        apiKey.setCreatedAt(OffsetDateTime.now());
        apiKeyRepository.save(apiKey);

        String ts = java.time.Instant.now().toString();
        String canonical = ts + ":GET:/api/developer/wizard/status:";
        String sig = hmacBase64(rawKey, canonical);

        mockMvc.perform(get("/api/developer/wizard/status")
                        .header("X-EB-API-KEY-ID", apiKey.getId().toString())
                        .header("X-EB-API-KEY", rawKey)
                        .header("X-EB-API-SIGNATURE", sig)
                        .header("X-EB-API-TIMESTAMP", ts))
                .andExpect(status().isOk());
    }

    @Test
    void status_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/developer/wizard/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void status_apiKeyWithDifferentMerchantParam_returns403() throws Exception {
        String rawKey = base64Random();
        ApiKey apiKey = new ApiKey();
        apiKey.setId(UUID.randomUUID());
        apiKey.setMerchant(merchant);
        apiKey.setKeyHash(sha256Base64(rawKey));
        apiKey.setSigningSecret("test-signing");
        apiKey.setRevoked(false);
        apiKey.setCreatedAt(OffsetDateTime.now());
        apiKeyRepository.save(apiKey);

        String ts = java.time.Instant.now().toString();
        String canonical = ts + ":GET:/api/developer/wizard/status:";
        String sig = hmacBase64(rawKey, canonical);

        UUID otherMerchant = UUID.randomUUID();

        mockMvc.perform(get("/api/developer/wizard/status")
                        .param("merchantId", otherMerchant.toString())
                        .header("X-EB-API-KEY-ID", apiKey.getId().toString())
                        .header("X-EB-API-KEY", rawKey)
                        .header("X-EB-API-SIGNATURE", sig)
                        .header("X-EB-API-TIMESTAMP", ts))
                .andExpect(status().isForbidden());
    }

    private static String sha256Base64(String value) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.Base64.getEncoder().encodeToString(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String base64Random() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hmacBase64(String secret, String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] h = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(h);
    }
}
