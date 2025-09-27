package com.ensureback.dispute;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    List<Dispute> findByOrder_Id(UUID orderId);

    Optional<Dispute> findByIdAndBuyerEmail(UUID id, String buyerEmail);

    Optional<Dispute> findFirstByOrder_IdAndStatusOrderByCreatedAtDesc(UUID orderId, Dispute.Status status);

    List<Dispute> findByOrder_Merchant_Id(UUID merchantId);
}