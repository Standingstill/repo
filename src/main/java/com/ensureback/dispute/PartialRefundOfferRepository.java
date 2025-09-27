package com.ensureback.dispute;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartialRefundOfferRepository extends JpaRepository<PartialRefundOffer, UUID> {

    Optional<PartialRefundOffer> findByDispute_Id(UUID disputeId);
}
