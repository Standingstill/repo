package com.ensureback.dispute;

import com.ensureback.dispute.dto.CreateDisputeRequest;
import com.ensureback.dispute.dto.DisputeDto;
import com.ensureback.dispute.dto.PartialOfferRequest;
import com.ensureback.dispute.dto.UpdateDisputeStatusRequest;
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
@RequestMapping("/api/disputes")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @PostMapping
    public ResponseEntity<DisputeDto> create(@Valid @RequestBody CreateDisputeRequest request) {
        Optional<DisputeDto> created = disputeService.create(request);
        return created
                .map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @GetMapping
    public ResponseEntity<List<DisputeDto>> listByMerchant(@RequestParam("merchantId") UUID merchantId) {
        List<DisputeDto> disputes = disputeService.listByMerchant(merchantId);
        return ResponseEntity.ok(disputes);
    }

    @GetMapping("/{disputeId}")
    public ResponseEntity<DisputeDto> findById(@PathVariable UUID disputeId) {
        return ResponseEntity.of(disputeService.findById(disputeId));
    }

    @PostMapping("/{disputeId}/status")
    public ResponseEntity<DisputeDto> updateStatus(@PathVariable UUID disputeId,
                                                   @Valid @RequestBody UpdateDisputeStatusRequest request) {
        return ResponseEntity.of(disputeService.updateStatus(disputeId, request.status()));
    }

    @PostMapping("/{disputeId}/partial-refund")
    public ResponseEntity<DisputeDto> partialRefund(@PathVariable UUID disputeId,
                                                    @Valid @RequestBody PartialOfferRequest request) {
        return ResponseEntity.of(disputeService.partialRefund(disputeId, request.amountCents()));
    }

    @PostMapping("/{disputeId}/escalate")
    public ResponseEntity<DisputeDto> escalate(@PathVariable UUID disputeId) {
        return ResponseEntity.of(disputeService.escalate(disputeId));
    }
}