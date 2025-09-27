package com.ensureback.developer;

import com.ensureback.developer.dto.ApiKeyCreateResponse;
import com.ensureback.developer.dto.ApiKeyDto;
import com.ensureback.developer.dto.IntegrationChecklistDto;
import com.ensureback.developer.dto.IntegrationChecklistUpdateRequest;
import com.ensureback.merchant.Merchant;
import com.ensureback.merchant.MerchantRepository;
import com.ensureback.user.User;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeveloperCenterServiceImpl implements DeveloperCenterService {

    private final MerchantRepository merchantRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final IntegrationChecklistRepository integrationChecklistRepository;
    private final ApiAuditLogRepository apiAuditLogRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public DeveloperCenterServiceImpl(MerchantRepository merchantRepository,
                                      ApiKeyRepository apiKeyRepository,
                                      IntegrationChecklistRepository integrationChecklistRepository,
                                      ApiAuditLogRepository apiAuditLogRepository) {
        this.merchantRepository = merchantRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.integrationChecklistRepository = integrationChecklistRepository;
        this.apiAuditLogRepository = apiAuditLogRepository;
    }

    @Override
    public ApiKeyCreateResponse generateApiKey(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        User merchantUser = merchant.getUser();
        if (merchantUser == null) {
            throw new IllegalStateException("Merchant missing associated user");
        }

        String rawKey = createRawKey();
        String keyHash = hashKey(rawKey);

        ApiKey apiKey = new ApiKey();
        apiKey.setId(UUID.randomUUID());
        apiKey.setMerchantUser(merchantUser);
        apiKey.setKeyHash(keyHash);
        apiKey.setStatus(ApiKey.Status.ACTIVE);
        apiKey.setCreatedAt(OffsetDateTime.now());
        ApiKey saved = apiKeyRepository.save(apiKey);

        recordAudit(saved, "POST", "/api/developer/api-keys", 201, "internal");

        return new ApiKeyCreateResponse(saved.getId(), rawKey, saved.getStatus().name(), saved.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiKeyDto> listApiKeys(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        UUID merchantUserId = merchant.getUser() != null ? merchant.getUser().getId() : null;
        if (merchantUserId == null) {
            return List.of();
        }
        return apiKeyRepository.findByMerchantUser_Id(merchantUserId).stream()
                .map(apiKey -> toDto(apiKey, merchantId))
                .toList();
    }

    @Override
    public ApiKeyDto revokeApiKey(UUID merchantId, UUID apiKeyId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        UUID merchantUserId = merchant.getUser() != null ? merchant.getUser().getId() : null;
        if (merchantUserId == null) {
            throw new IllegalStateException("Merchant missing associated user");
        }
        ApiKey apiKey = apiKeyRepository.findByIdAndMerchantUser_Id(apiKeyId, merchantUserId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));
        if (apiKey.getStatus() != ApiKey.Status.REVOKED) {
            apiKey.setStatus(ApiKey.Status.REVOKED);
            apiKeyRepository.save(apiKey);
            recordAudit(apiKey, "DELETE", "/api/developer/api-keys/" + apiKeyId, 204, "internal");
        }
        return toDto(apiKey, merchantId);
    }

    @Override
    public IntegrationChecklistDto getIntegrationChecklist(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        IntegrationChecklist checklist = integrationChecklistRepository.findByMerchantId(merchantId)
                .orElseGet(() -> createChecklist(merchant));
        return toDto(checklist);
    }

    @Override
    public IntegrationChecklistDto updateIntegrationChecklist(UUID merchantId, IntegrationChecklistUpdateRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        IntegrationChecklist checklist = integrationChecklistRepository.findByMerchantId(merchantId)
                .orElseGet(() -> createChecklist(merchant));

        if (request.connectedStripe() != null) {
            checklist.setConnectedStripe(request.connectedStripe());
        }
        if (request.webhookConfigured() != null) {
            checklist.setWebhookConfigured(request.webhookConfigured());
        }
        if (request.aftershipConfigured() != null) {
            checklist.setAftershipConfigured(request.aftershipConfigured());
        }
        if (request.testChargeDone() != null) {
            checklist.setTestChargeDone(request.testChargeDone());
        }
        if (request.lastCheckedAt() != null) {
            checklist.setLastCheckedAt(request.lastCheckedAt());
        } else {
            checklist.setLastCheckedAt(OffsetDateTime.now());
        }
        IntegrationChecklist saved = integrationChecklistRepository.save(checklist);
        return toDto(saved);
    }

    private IntegrationChecklist createChecklist(Merchant merchant) {
        IntegrationChecklist checklist = new IntegrationChecklist();
        checklist.setId(UUID.randomUUID());
        checklist.setMerchant(merchant);
        checklist.setConnectedStripe(false);
        checklist.setWebhookConfigured(false);
        checklist.setAftershipConfigured(false);
        checklist.setTestChargeDone(false);
        checklist.setLastCheckedAt(OffsetDateTime.now());
        return integrationChecklistRepository.save(checklist);
    }

    private void recordAudit(ApiKey apiKey, String method, String path, int status, String ip) {
        ApiAuditLog audit = new ApiAuditLog();
        audit.setId(UUID.randomUUID());
        audit.setApiKey(apiKey);
        audit.setMethod(method);
        audit.setPath(path);
        audit.setStatus(status);
        audit.setIp(ip);
        audit.setCreatedAt(OffsetDateTime.now());
        apiAuditLogRepository.save(audit);
    }

    private IntegrationChecklistDto toDto(IntegrationChecklist checklist) {
        return new IntegrationChecklistDto(
                checklist.getMerchant().getId(),
                checklist.isConnectedStripe(),
                checklist.isWebhookConfigured(),
                checklist.isAftershipConfigured(),
                checklist.isTestChargeDone(),
                checklist.getLastCheckedAt()
        );
    }

    private ApiKeyDto toDto(ApiKey apiKey, UUID merchantId) {
        return new ApiKeyDto(
                apiKey.getId(),
                merchantId,
                apiKey.getStatus().name(),
                apiKey.getCreatedAt()
        );
    }

    private String createRawKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawKey.getBytes());
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}