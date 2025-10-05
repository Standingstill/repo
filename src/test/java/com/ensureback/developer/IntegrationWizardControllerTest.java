package com.ensureback.developer;

import com.ensureback.auth.StripeConnectService;
import com.ensureback.merchant.Merchant;
import com.ensureback.merchant.MerchantRepository;
import com.ensureback.user.User;
import com.ensureback.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@WithMockUser(roles = "ADMIN")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class IntegrationWizardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private IntegrationChecklistRepository integrationChecklistRepository;

    @MockBean
    private WebhookService webhookService;

    @MockBean
    private StripeConnectService stripeConnectService;

    private Merchant merchant;

    @BeforeEach
    void setupMerchant() {
        User user = new User(UUID.randomUUID(), User.Role.MERCHANT, "acct_test_123", null, null);
        user = userRepository.save(user);

        Merchant merchantEntity = instantiateMerchant();
        merchantEntity.setId(UUID.randomUUID());
        merchantEntity.setUser(user);
        merchantEntity.setBusinessName("Test Merchant LLC");
        merchantEntity.setSupportEmail("merchant@example.com");
        merchantEntity.setDisputeWindowHours(72);
        merchant = merchantRepository.save(merchantEntity);
    }

    @Test
    void apiKeyLifecycle() throws Exception {
        String merchantId = merchant.getId().toString();

        String createResponse = mockMvc.perform(post("/api/developer/wizard/api-keys").param("merchantId", merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiKey.apiKey").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdNode = objectMapper.readTree(createResponse);
        String apiKeyId = createdNode.get("apiKey").get("id").asText();

        mockMvc.perform(get("/api/developer/wizard/api-keys").param("merchantId", merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].revoked").value(false));

        mockMvc.perform(delete("/api/developer/wizard/api-keys/" + apiKeyId).param("merchantId", merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiKeys[0].revoked").value(true));
    }

    @Test
    void webhookRegisterAndTestFlow() throws Exception {
        createActiveApiKey("test-secret-value");

        WebhookEvent verifyEvent = new WebhookEvent();
        verifyEvent.setId(UUID.randomUUID());
        verifyEvent.setEventType("ensureback.webhook.verify");
        verifyEvent.setPayload("{}");
        verifyEvent.setDelivered(true);
        verifyEvent.setTimestamp(OffsetDateTime.now());

        WebhookEvent testEvent = new WebhookEvent();
        testEvent.setId(UUID.randomUUID());
        testEvent.setEventType("ensureback.integration.test");
        testEvent.setPayload("{}");
        testEvent.setDelivered(true);
        testEvent.setTimestamp(OffsetDateTime.now());

        when(webhookService.deliverEvent(any(WebhookEndpoint.class), anyString(), any(), anyString()))
                .thenAnswer(invocation -> {
                    String eventType = invocation.getArgument(1);
                    if ("ensureback.webhook.verify".equals(eventType)) {
                        return new WebhookService.WebhookDeliveryResult(verifyEvent, "sig", Instant.now(), true);
                    }
                    return new WebhookService.WebhookDeliveryResult(testEvent, "sig", Instant.now(), true);
                });

        mockMvc.perform(post("/api/developer/wizard/webhook/register")
                        .param("merchantId", merchant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://merchant.test/webhook\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.webhook.completed").value(true))
                .andExpect(jsonPath("$.webhookStatus.verified").value(true));

        mockMvc.perform(get("/api/developer/wizard/webhook/test").param("merchantId", merchant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivery.delivered").value(true))
                .andExpect(jsonPath("$.status.verification.completed").value(true));
    }

    @Test
    void checklistPatchUpdatesSteps() throws Exception {
        mockMvc.perform(patch("/api/developer/wizard/status")
                        .param("merchantId", merchant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stripeConnected\":true,\"webhookRegistered\":true,\"testChargePassed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complete").value(true));
    }

    @Test
    void stripeCallbackMarksChecklistConnected() throws Exception {
        when(stripeConnectService.completeOnboarding(any(), any(), any(), any()))
                .thenReturn(new StripeConnectService.CallbackResult(URI.create("https://app.ensureback.test"), true, merchant.getUser().getStripeAccountId()));

        mockMvc.perform(get("/api/developer/wizard/stripe/callback")
                        .param("state", UUID.randomUUID().toString())
                        .param("code", "auth_code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checklistUpdated").value(true));

        IntegrationChecklist checklist = integrationChecklistRepository.findByMerchantId(merchant.getId()).orElseThrow();
        assertThat(checklist.isStripeConnected()).isTrue();
    }

    private void createActiveApiKey(String rawKey) {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(UUID.randomUUID());
        apiKey.setMerchant(merchant);
        apiKey.setKeyHash(hashSha256(rawKey));
        apiKey.setRevoked(false);
        apiKey.setCreatedAt(OffsetDateTime.now());
        apiKeyRepository.save(apiKey);
    }

    private String hashSha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to hash key", ex);
        }
    }

    private Merchant instantiateMerchant() {
        try {
            var constructor = Merchant.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to construct merchant for tests", ex);
        }
    }
}

