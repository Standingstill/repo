package com.ensureback.transaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByStripePaymentIntentId(String paymentIntentId);

    Optional<Transaction> findByPlatformChargeId(String platformChargeId);

    Optional<Transaction> findFirstByOrder_IdOrderByCreatedAtDesc(UUID orderId);

    List<Transaction> findByOrder_Merchant_Id(UUID merchantId);
}