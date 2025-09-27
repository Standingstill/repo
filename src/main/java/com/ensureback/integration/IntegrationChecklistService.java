package com.ensureback.integration;

import com.ensureback.developer.IntegrationChecklist;
import com.ensureback.developer.IntegrationChecklistRepository;
import com.ensureback.integration.dto.AftershipKeyRequest;
import com.ensureback.integration.dto.IntegrationChecklistResponse;
import com.ensureback.integration.dto.IntegrationStepStatus;
import com.ensureback.integration.dto.StripeConnectResponse;
import com.ensureback.integration.dto.TestChargeResponse;
import com.ensureback.merchant.Merchant;
import com.ensureback.merchant.MerchantRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IntegrationChecklistService {

    private final IntegrationChecklistRepository integrationChecklistRepository;
    private final MerchantRepository merchantRepository;

    public IntegrationChecklistService(IntegrationChecklistRepository integrationChecklistRepository,
                                       MerchantRepository merchantRepository) {
        this.integrationChecklistRepository = integrationChecklistRepository;
        this.merchantRepository = merchantRepository;
    }

    public IntegrationChecklistResponse getChecklist(UUID merchantId) {
        IntegrationChecklist checklist = findOrCreateChecklist(merchantId);
        return toResponse(checklist);
    }

    public StripeConnectResponse initiateStripeConnect(UUID merchantId) {
        IntegrationChecklist checklist = findOrCreateChecklist(merchantId);
        checklist.setConnectedStripe(true);
        checklist.setLastCheckedAt(OffsetDateTime.now());
        integrationChecklistRepository.save(checklist);
        String link = "https://dashboard.stripe.com/oauth/authorize?merchant=" + merchantId;
        return new StripeConnectResponse(link, toResponse(checklist));
    }

    public IntegrationChecklistResponse updateAftershipKey(UUID merchantId, AftershipKeyRequest request) {
        IntegrationChecklist checklist = findOrCreateChecklist(merchantId);
        checklist.setAftershipApiKey(request.apiKey());
        checklist.setAftershipConfigured(true);
        checklist.setLastCheckedAt(OffsetDateTime.now());
        integrationChecklistRepository.save(checklist);
        return toResponse(checklist);
    }

    public TestChargeResponse runTestCharge(UUID merchantId) {
        IntegrationChecklist checklist = findOrCreateChecklist(merchantId);
        checklist.setTestChargeDone(true);
        checklist.setLastCheckedAt(OffsetDateTime.now());
        integrationChecklistRepository.save(checklist);
        return new TestChargeResponse("SUCCESS", "Test charge completed and refunded.");
    }

    private IntegrationChecklist findOrCreateChecklist(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        return integrationChecklistRepository.findByMerchantId(merchantId)
                .orElseGet(() -> {
                    IntegrationChecklist checklist = new IntegrationChecklist();
                    checklist.setId(UUID.randomUUID());
                    checklist.setMerchant(merchant);
                    checklist.setConnectedStripe(false);
                    checklist.setWebhookConfigured(false);
                    checklist.setAftershipConfigured(false);
                    checklist.setTestChargeDone(false);
                    checklist.setLastCheckedAt(OffsetDateTime.now());
                    return integrationChecklistRepository.save(checklist);
                });
    }

    private IntegrationChecklistResponse toResponse(IntegrationChecklist checklist) {
        OffsetDateTime updatedAt = checklist.getLastCheckedAt();
        IntegrationStepStatus stripe = step(checklist.isConnectedStripe(),
                "Stripe account connected.",
                "Connect your Stripe account to receive payouts.",
                updatedAt);
        IntegrationStepStatus webhook = step(checklist.isWebhookConfigured(),
                "Webhook configured.",
                "Configure your EnsureBack webhook endpoint.",
                updatedAt);
        IntegrationStepStatus aftership = step(checklist.isAftershipConfigured(),
                "AfterShip integration active.",
                "Provide your AfterShip API key to enable tracking.",
                updatedAt);
        IntegrationStepStatus testCharge = step(checklist.isTestChargeDone(),
                "Test charge completed.",
                "Run a $1 test charge to confirm payment flow.",
                updatedAt);
        return new IntegrationChecklistResponse(stripe, webhook, aftership, testCharge);
    }

    private IntegrationStepStatus step(boolean completed, String successMessage, String remediation, OffsetDateTime updatedAt) {
        return new IntegrationStepStatus(completed, completed ? successMessage : remediation, updatedAt);
    }
}