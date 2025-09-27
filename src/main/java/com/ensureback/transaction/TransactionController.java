package com.ensureback.transaction;

import com.ensureback.transaction.dto.TransactionChargeRequest;
import com.ensureback.transaction.dto.TransactionDto;
import com.ensureback.transaction.dto.UpdateEscrowStatusRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionDto> create(@Valid @RequestBody TransactionChargeRequest request) {
        Optional<TransactionDto> created = transactionService.create(request);
        return created
                .map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @GetMapping
    public ResponseEntity<List<TransactionDto>> listByMerchant(@RequestParam("merchantId") UUID merchantId) {
        List<TransactionDto> transactions = transactionService.listByMerchant(merchantId);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionDto> findById(@PathVariable UUID transactionId) {
        return ResponseEntity.of(transactionService.findById(transactionId));
    }

    @PostMapping("/{transactionId}/escrow-status")
    public ResponseEntity<TransactionDto> updateEscrowStatus(@PathVariable UUID transactionId,
                                                             @Valid @RequestBody UpdateEscrowStatusRequest request) {
        return ResponseEntity.of(transactionService.updateEscrowStatus(transactionId, request.escrowStatus()));
    }
}