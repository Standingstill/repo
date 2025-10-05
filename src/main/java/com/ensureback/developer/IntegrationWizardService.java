package com.ensureback.developer;

import com.ensureback.auth.StripeConnectService;
import com.ensureback.developer.dto.ApiKeyCreateResponse;
import com.ensureback.developer.dto.ApiKeyCreateResult;
import com.ensureback.developer.dto.ApiKeyDto;
import com.ensureback.developer.dto.IntegrationWizardStatusResponse;
import com.ensureback.developer.dto.IntegrationWizardStepDto;
import com.ensureback.developer.dto.IntegrationWizardUpdateRequest;
import com.ensureback.developer.dto.StripeCallbackResponse;
import com.ensureback.developer.dto.StripeConnectStartResponse;
import com.ensureback.developer.dto.StripeStatusDto;
import com.ensureback.developer.dto.WebhookEventDto;
import com.ensureback.developer.dto.WebhookStatusDto;
import com.ensureback.developer.dto.WebhookTestResponse;
import com.ensureback.developer.dto.WebhookTestResult;
import com.ensureback.merchant.Merchant;
import com.ensureback.merchant.MerchantRepository;
import com.stripe.exception.StripeException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class IntegrationWizardService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationWizardService.class);
    private static final String STEP_STRIPE = "stripe";
    private static final String STEP_API_KEY = "apiKey";
    private static final String STEP_WEBHOOK = "webhook";
    private static final String STEP_VERIFICATION = "verification";

    private final MerchantRepository merchantRepository;
    private final IntegrationChecklistRepository integrationChecklistRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final WebhookEndpointRepository webhookEndpointRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final StripeConnectService stripeConnectService;
    private final WebhookService webhookService;

    private final SecureRandom secureRandom = new SecureRandom();

    public IntegrationWizardService(MerchantRepository merchantRepository,
                                    IntegrationChecklistRepository integrationChecklistRepository,
                                    ApiKeyRepository apiKeyRepository,
                                    WebhookEndpointRepository webhookEndpointRepository,
                                    WebhookEventRepository webhookEventRepository,
                                    StripeConnectService stripeConnectService,
                                    WebhookService webhookService) {
        this.merchantRepository = merchantRepository;
        this.integrationChecklistRepository = integrationChecklistRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.webhookEndpointRepository = webhookEndpointRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.stripeConnectService = stripeConnectService;
        this.webhookService = webhookService;
    }

    @Transactional(readOnly = true)
    public IntegrationWizardStatusResponse getStatus(UUID merchantId) {
        Merchant merchant = loadMerchant(merchantId);
        IntegrationChecklist checklist = findOrCreateChecklist(merchant);
        return buildStatus(merchant, checklist);
    }

    public IntegrationWizardStatusResponse updateStatus(UUID merchantId, IntegrationWizardUpdateRequest request) {
        Merchant merchant = loadMerchant(merchantId);
        IntegrationChecklist checklist = findOrCreateChecklist(merchant);
        boolean changed = false;
        if (request.stripeConnected() != null && request.stripeConnected() != checklist.isStripeConnected()) {
            checklist.setStripeConnected(request.stripeConnected());
            changed = true;
        }
        if (request.webhookRegistered() != null && request.webhookRegistered() != checklist.isWebhookRegistered()) {
            checklist.setWebhookRegistered(request.webhookRegistered());
            changed = true;
        }
        if (request.testChargePassed() != null && request.testChargePassed() != checklist.isTestChargePassed()) {
            checklist.setTestChargePassed(request.testChargePassed());
            changed = true;
        }
        if (changed) {
            checklist.setUpdatedAt(OffsetDateTime.now());
            integrationChecklistRepository.save(checklist);
        }
        return buildStatus(merchant, checklist);
    }

    public ApiKeyCreateResult generateApiKey(UUID merchantId) {
        Merchant merchant = loadMerchant(merchantId);
        IntegrationChecklist checklist = findOrCreateChecklist(merchant);

        String rawKey = generateRawKey();
        String keyHash = hashSha256(rawKey);

        ApiKey apiKey = new ApiKey();
        apiKey.setId(UUID.randomUUID());
        apiKey.setMerchant(merchant);
        apiKey.setKeyHash(keyHash);
        apiKey.setRevoked(false);
        apiKey.setCreatedAt(OffsetDateTime.now());
        apiKeyRepository.save(apiKey);

        checklist.setUpdatedAt(OffsetDateTime.now());
        integrationChecklistRepository.save(checklist);

        log.info("Generated API key {} for merchant {}", apiKey.getId(), merchantId);

        ApiKeyCreateResponse response = new ApiKeyCreateResponse(apiKey.getId(), rawKey, apiKey.getCreatedAt());
        IntegrationWizardStatusResponse status = buildStatus(merchant, checklist);
        return new ApiKeyCreateResult(response, status);
    }

    @Transactional(readOnly = true)
    public List<ApiKeyDto> listApiKeys(UUID merchantId) {
        return apiKeyRepository.findByMerchant_Id(merchantId).stream()
                .sorted(Comparator.comparing(ApiKey::getCreatedAt).reversed())
                .map(this::toDto)
                .toList();
    }

    public IntegrationWizardStatusResponse revokeApiKey(UUID merchantId, UUID apiKeyId) {
        Merchant merchant = loadMerchant(merchantId);
        IntegrationChecklist checklist = findOrCreateChecklist(merchant);
        ApiKey apiKey = apiKeyRepository.findByIdAndMerchant_Id(apiKeyId, merchantId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));
        if (!apiKey.isRevoked()) {
            apiKey.setRevoked(true);
            apiKeyRepository.save(apiKey);
            log.info("Revoked API key {} for merchant {}", apiKeyId, merchantId);
        }
        checklist.setUpdatedAt(OffsetDateTime.now());
        integrationChecklistRepository.save(checklist);
        return buildStatus(merchant, checklist);
    }

    public IntegrationWizardStatusResponse registerWebhook(UUID merchantId, String webhookUrl) {
        Merchant merchant = loadMerchant(merchantId);
        IntegrationChecklist checklist = findOrCreateChecklist(merchant);
        WebhookEndpoint endpoint = webhookEndpointRepository.findByMerchant_Id(merchantId)
                .orElseGet(() -> {
                    WebhookEndpoint created = new WebhookEndpoint();
                    created.setId(UUID.randomUUID());
                    created.setMerchant(merchant);
                    created.setVerified(false);
                    return created;
                });
        endpoint.setUrl(webhookUrl);
        webhookEndpointRepository.save(endpoint);

        String signingSecret = resolveSigningSecret(merchantId)
                .orElseThrow(() -> new IllegalStateException("Active API key required to sign webhook payloads"));

        Map<String, Object> payload = Map.of(
                "event", "ensureback.webhook.verify",
                "challenge", UUID.randomUUID().toString(),
                "timestamp", OffsetDateTime.now().toString()
        );
        WebhookService.WebhookDeliveryResult result = webhookService.deliverEvent(endpoint, "ensureback.webhook.verify", payload, signingSecret);
        endpoint.setVerified(result.delivered());
        webhookEndpointRepository.save(endpoint);

        if (result.delivered() && !checklist.isWebhookRegistered()) {
            checklist.setWebhookRegistered(true);
            checklist.setUpdatedAt(OffsetDateTime.now());
            integrationChecklistRepository.save(checklist);
        }

        log.info("Registered webhook for merchant {} (verified={})", merchantId, result.delivered());

        return buildStatus(merchant, checklist);
    }

    public WebhookTestResult sendTestWebhook(UUID merchantId) {
        Merchant merchant = loadMerchant(merchantId);
        IntegrationChecklist checklist = findOrCreateChecklist(merchant);
        WebhookEndpoint endpoint = webhookEndpointRepository.findByMerchant_Id(merchantId)
                .orElseThrow(() -> new IllegalStateException("Webhook endpoint not registered"));

        String signingSecret = resolveSigningSecret(merchantId)
                .orElseThrow(() -> new IllegalStateException("Active API key required to sign webhook payloads"));

        Map<String, Object> payload = Map.of(
                "event", "ensureback.integration.test",
                "type", "integration.test",
                "data", Map.of(
                        "message", "Test event from EnsureBack",
                        "merchantId", merchantId.toString(),
                        "timestamp", OffsetDateTime.now().toString()
                )
        );
        WebhookService.WebhookDeliveryResult result = webhookService.deliverEvent(endpoint, "ensureback.integration.test", payload, signingSecret);
        if (result.delivered()) {
            checklist.setTestChargePassed(true);
            checklist.setUpdatedAt(OffsetDateTime.now());
            integrationChecklistRepository.save(checklist);
        }

        log.info("Sent webhook test event {} for merchant {} delivered={}", result.event().getId(), merchantId, result.delivered());

        WebhookTestResponse response = new WebhookTestResponse(result.event().getId(), result.delivered(), result.event().getTimestamp());
        IntegrationWizardStatusResponse status = buildStatus(merchant, checklist);
        return new WebhookTestResult(response, status);
    }

    public StripeConnectStartResponse startStripeConnect(UUID userId, String returnPath) {
        StripeConnectService.OnboardingRedirect redirect = stripeConnectService.initiateOnboarding(userId, returnPath);
        return new StripeConnectStartResponse(redirect.alreadyConnected(),
                redirect.redirectUri() != null ? redirect.redirectUri().toString() : null,
                redirect.state() != null ? redirect.state().toString() : null);
    }

    public StripeCallbackResponse handleStripeCallback(String state,
                                                       String code,
                                                       String error,
                                                       String errorDescription) throws StripeException {
        StripeConnectService.CallbackResult callback = stripeConnectService.completeOnboarding(state, code, error, errorDescription);
        boolean checklistUpdated = false;
        boolean integrated = false;
        String nextStep = "DASHBOARD";
        String message = "Welcome back! Redirecting to dashboard...";
        String stripeAccountId = callback.stripeAccountId();
        if (StringUtils.hasText(stripeAccountId)) {
            Merchant merchant = merchantRepository.findByStripeAccountId(stripeAccountId)
                    .or(() -> merchantRepository.findByUser_StripeAccountId(stripeAccountId))
                    .orElse(null);
            if (merchant != null) {
                if (!StringUtils.hasText(merchant.getStripeAccountId())) {
                    merchant.setStripeAccountId(stripeAccountId);
                    merchantRepository.save(merchant);
                }
                IntegrationChecklist checklist = findOrCreateChecklist(merchant);
                if (!checklist.isStripeConnected()) {
                    checklist.setStripeConnected(true);
                    checklist.setUpdatedAt(OffsetDateTime.now());
                    integrationChecklistRepository.save(checklist);
                    checklistUpdated = true;
                }
                IntegrationWizardStatusResponse status = buildStatus(merchant, checklist);
                integrated = status.complete();
                if (!integrated) {
                    nextStep = "INTEGRATION_WIZARD";
                    message = "Your Stripe account is connected. Please complete integration setup.";
                }
            } else {
                log.warn("Stripe callback received for account {} but merchant not found", stripeAccountId);
                nextStep = "INTEGRATION_WIZARD";
                message = "Your Stripe account is connected. Please complete integration setup.";
            }
        } else {
            log.warn("Stripe callback completed without a Stripe account id");
            nextStep = "INTEGRATION_WIZARD";
            message = "Your Stripe account is connected. Please complete integration setup.";
        }
        String redirectUrl = callback.redirectUri() != null ? callback.redirectUri().toString() : null;
        return new StripeCallbackResponse(callback.connected(), stripeAccountId, checklistUpdated, integrated, nextStep, message, redirectUrl);
    }

    private Merchant loadMerchant(UUID merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
    }

    private IntegrationChecklist findOrCreateChecklist(Merchant merchant) {
        return integrationChecklistRepository.findByMerchantId(merchant.getId())
                .orElseGet(() -> {
                    IntegrationChecklist checklist = new IntegrationChecklist();
                    checklist.setId(UUID.randomUUID());
                    checklist.setMerchant(merchant);
                    checklist.setStripeConnected(false);
                    checklist.setWebhookRegistered(false);
                    checklist.setTestChargePassed(false);
                    checklist.setUpdatedAt(OffsetDateTime.now());
                    return integrationChecklistRepository.save(checklist);
                });
    }

    private IntegrationWizardStatusResponse buildStatus(Merchant merchant, IntegrationChecklist checklist) {
        UUID merchantId = merchant.getId();
        List<ApiKeyDto> apiKeys = apiKeyRepository.findByMerchant_Id(merchantId).stream()
                .sorted(Comparator.comparing(ApiKey::getCreatedAt).reversed())
                .map(this::toDto)
                .toList();

        String merchantStripeAccount = merchant.getStripeAccountId();
        if (!StringUtils.hasText(merchantStripeAccount) && merchant.getUser() != null) {
            merchantStripeAccount = merchant.getUser().getStripeAccountId();
        }

        WebhookStatusDto webhookStatus = buildWebhookStatus(merchantId);
        StripeStatusDto stripeStatus = new StripeStatusDto(checklist.isStripeConnected(),
                merchantStripeAccount,
                checklist.isStripeConnected() ? checklist.getUpdatedAt() : null);

        boolean apiKeyComplete = apiKeys.stream().anyMatch(apiKeyDto -> !apiKeyDto.revoked());
        boolean webhookComplete = webhookStatus.registered() && webhookStatus.verified();
        boolean verificationComplete = checklist.isTestChargePassed();

        IntegrationWizardStepDto stripeStep = new IntegrationWizardStepDto(
                STEP_STRIPE,
                "Connect Stripe",
                checklist.isStripeConnected(),
                checklist.isStripeConnected() ? checklist.getUpdatedAt() : null,
                checklist.isStripeConnected() ? "Stripe account connected." : "Connect your Stripe account to enable payouts."
        );

        IntegrationWizardStepDto apiKeyStep = new IntegrationWizardStepDto(
                STEP_API_KEY,
                "Generate API Key",
                apiKeyComplete,
                apiKeyComplete ? apiKeys.stream()
                        .filter(key -> !key.revoked())
                        .findFirst()
                        .map(ApiKeyDto::createdAt)
                        .orElse(null) : null,
                apiKeyComplete ? "At least one active API key is available." : "Generate an API key to authenticate API requests."
        );

        IntegrationWizardStepDto webhookStep = new IntegrationWizardStepDto(
                STEP_WEBHOOK,
                "Configure Webhook",
                webhookComplete,
                webhookComplete ? checklist.getUpdatedAt() : null,
                webhookComplete ? "Webhook endpoint verified." : "Provide a webhook URL so EnsureBack can send events."
        );

        IntegrationWizardStepDto verificationStep = new IntegrationWizardStepDto(
                STEP_VERIFICATION,
                "Verify Integration",
                verificationComplete,
                verificationComplete ? checklist.getUpdatedAt() : null,
                verificationComplete ? "Test event delivered successfully." : "Trigger a test event to verify your integration."
        );

        boolean complete = stripeStep.completed() && apiKeyStep.completed() && webhookStep.completed() && verificationStep.completed();

        boolean merchantUpdated = false;
        if (!StringUtils.hasText(merchant.getStripeAccountId()) && StringUtils.hasText(merchantStripeAccount)) {
            merchant.setStripeAccountId(merchantStripeAccount);
            merchantUpdated = true;
        }
        if (merchant.isIntegrated() != complete) {
            merchant.setIntegrated(complete);
            merchantUpdated = true;
        }
        if (merchantUpdated) {
            merchantRepository.save(merchant);
        }

        return new IntegrationWizardStatusResponse(
                merchantId,
                stripeStep,
                apiKeyStep,
                webhookStep,
                verificationStep,
                complete,
                checklist.getUpdatedAt(),
                stripeStatus,
                webhookStatus,
                apiKeys
        );
    }

    private WebhookStatusDto buildWebhookStatus(UUID merchantId) {
        Optional<WebhookEndpoint> endpointOpt = webhookEndpointRepository.findByMerchant_Id(merchantId);
        if (endpointOpt.isEmpty()) {
            return new WebhookStatusDto(false, false, null, List.of());
        }
        WebhookEndpoint endpoint = endpointOpt.get();
        List<WebhookEventDto> events = webhookEventRepository.findTop10ByWebhookEndpoint_IdOrderByTimestampDesc(endpoint.getId()).stream()
                .map(event -> new WebhookEventDto(event.getId(), event.getEventType(), event.getTimestamp(), event.isDelivered(), event.getPayload()))
                .toList();
        return new WebhookStatusDto(true, endpoint.isVerified(), endpoint.getUrl(), events);
    }

    private ApiKeyDto toDto(ApiKey apiKey) {
        return new ApiKeyDto(apiKey.getId(), apiKey.getCreatedAt(), apiKey.isRevoked());
    }

    private Optional<String> resolveSigningSecret(UUID merchantId) {
        return apiKeyRepository.findByMerchant_IdAndRevokedFalse(merchantId).stream()
                .sorted(Comparator.comparing(ApiKey::getCreatedAt).reversed())
                .map(ApiKey::getKeyHash)
                .findFirst();
    }

    private String generateRawKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashSha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
