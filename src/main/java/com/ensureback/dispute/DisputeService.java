package com.ensureback.dispute;

import com.ensureback.dispute.dto.CreateDisputeRequest;
import com.ensureback.dispute.dto.DisputeDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DisputeService {

    Optional<DisputeDto> create(CreateDisputeRequest request);

    Optional<DisputeDto> findById(UUID disputeId);

    List<DisputeDto> listByMerchant(UUID merchantId);

    Optional<DisputeDto> updateStatus(UUID disputeId, String status);

    Optional<DisputeDto> partialRefund(UUID disputeId, int amountCents);

    Optional<DisputeDto> escalate(UUID disputeId);
}