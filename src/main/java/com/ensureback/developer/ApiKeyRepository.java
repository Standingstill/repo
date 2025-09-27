package com.ensureback.developer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    List<ApiKey> findByMerchantUser_Id(UUID userId);

    Optional<ApiKey> findByIdAndMerchantUser_Id(UUID apiKeyId, UUID userId);
}