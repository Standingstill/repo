package com.ensureback.transaction;

import com.ensureback.transaction.dto.TransactionChargeRequest;
import com.ensureback.transaction.dto.TransactionDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionService {

    Optional<TransactionDto> create(TransactionChargeRequest request);

    Optional<TransactionDto> findById(UUID transactionId);

    List<TransactionDto> listByMerchant(UUID merchantId);

    Optional<TransactionDto> updateEscrowStatus(UUID transactionId, String escrowStatus);
}