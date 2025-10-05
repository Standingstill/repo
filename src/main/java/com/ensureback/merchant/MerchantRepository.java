package com.ensureback.merchant;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    Optional<Merchant> findByUserId(UUID userId);

    Optional<Merchant> findByUser_StripeAccountId(String stripeAccountId);

    Optional<Merchant> findByStripeAccountId(String stripeAccountId);
}
