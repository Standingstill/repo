package com.ensureback.developer;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationChecklistRepository extends JpaRepository<IntegrationChecklist, UUID> {

    Optional<IntegrationChecklist> findByMerchantId(UUID merchantId);
}
