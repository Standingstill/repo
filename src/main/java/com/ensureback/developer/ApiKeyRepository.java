package com.ensureback.developer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    List<ApiKey> findByMerchant_Id(UUID merchantId);

    List<ApiKey> findByMerchant_IdAndRevokedFalse(UUID merchantId);

    Optional<ApiKey> findByIdAndMerchant_Id(UUID apiKeyId, UUID merchantId);
}
