package com.ensureback.auth;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeConnectSessionRepository extends JpaRepository<StripeConnectSession, UUID> {
}
