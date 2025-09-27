package com.ensureback.developer;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiAuditLogRepository extends JpaRepository<ApiAuditLog, UUID> {
}
