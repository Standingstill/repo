package com.ensureback.developer;

import com.ensureback.developer.dto.ApiKeyCreateResponse;
import com.ensureback.developer.dto.ApiKeyDto;
import com.ensureback.developer.dto.IntegrationChecklistDto;
import com.ensureback.developer.dto.IntegrationChecklistUpdateRequest;
import java.util.List;
import java.util.UUID;

public interface DeveloperCenterService {

    ApiKeyCreateResponse generateApiKey(UUID merchantId);

    List<ApiKeyDto> listApiKeys(UUID merchantId);

    ApiKeyDto revokeApiKey(UUID merchantId, UUID apiKeyId);

    IntegrationChecklistDto getIntegrationChecklist(UUID merchantId);

    IntegrationChecklistDto updateIntegrationChecklist(UUID merchantId, IntegrationChecklistUpdateRequest request);
}