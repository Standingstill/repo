package com.ensureback.auth;

import com.ensureback.user.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeConnectSessionRepository extends JpaRepository<StripeConnectSession, UUID> {

    void deleteByUser(User user);
}
