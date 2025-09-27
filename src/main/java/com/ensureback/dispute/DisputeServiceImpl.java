package com.ensureback.dispute;

import com.ensureback.dispute.dto.CreateDisputeRequest;
import com.ensureback.dispute.dto.DisputeDto;
import com.ensureback.dispute.dto.DisputeMessageDto;
import com.ensureback.dispute.dto.PartialRefundOfferDto;
import com.ensureback.notification.NotificationService;
import com.ensureback.notification.dto.NotificationPublishRequest;
import com.ensureback.order.Order;
import com.ensureback.order.OrderRepository;
import com.ensureback.transaction.Transaction;
import com.ensureback.transaction.TransactionRepository;
import com.ensureback.stripe.StripeService;
import com.stripe.exception.StripeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class DisputeServiceImpl implements DisputeService {

    private static final Logger log = LoggerFactory.getLogger(DisputeServiceImpl.class);

    private final DisputeRepository disputeRepository;
    private final DisputeMessageRepository disputeMessageRepository;
    private final PartialRefundOfferRepository partialRefundOfferRepository;
    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;
    private final StripeService stripeService;
    private final ObjectMapper objectMapper;

    public DisputeServiceImpl(DisputeRepository disputeRepository,
                              DisputeMessageRepository disputeMessageRepository,
                              PartialRefundOfferRepository partialRefundOfferRepository,
                              OrderRepository orderRepository,
                              TransactionRepository transactionRepository,
                              NotificationService notificationService,
                              StripeService stripeService,
                              ObjectMapper objectMapper) {
        this.disputeRepository = disputeRepository;
        this.disputeMessageRepository = disputeMessageRepository;
        this.partialRefundOfferRepository = partialRefundOfferRepository;
        this.orderRepository = orderRepository;
        this.transactionRepository = transactionRepository;
        this.notificationService = notificationService;
        this.stripeService = stripeService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<DisputeDto> create(CreateDisputeRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        Dispute dispute = new Dispute();
        dispute.setId(UUID.randomUUID());
        dispute.setOrder(order);
        dispute.setBuyerEmail(request.buyerEmail());
        dispute.setStatus(Dispute.Status.OPEN);
        dispute.setReturnRequired(!order.isDigital());
        dispute.setCreatedAt(OffsetDateTime.now());
        Dispute saved = disputeRepository.save(dispute);

        DisputeMessage message = new DisputeMessage();
        message.setId(UUID.randomUUID());
        message.setDispute(saved);
        message.setAuthorRole(DisputeMessage.AuthorRole.BUYER);
        message.setMessage(request.message());
        message.setEvidenceUrls(writeEvidence(request.evidenceUrls()));

        disputeMessageRepository.save(message);

        notifyDispute(saved, "dispute.created", Map.of(
                "disputeId", saved.getId().toString(),
                "orderNumber", order.getOrderNumber(),
                "status", saved.getStatus().name()
        ), true);

        return Optional.of(loadDto(saved.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DisputeDto> findById(UUID disputeId) {
        if (disputeId == null) {
            return Optional.empty();
        }
        return disputeRepository.findById(disputeId).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisputeDto> listByMerchant(UUID merchantId) {
        if (merchantId == null) {
            return List.of();
        }
        List<DisputeDto> results = new ArrayList<>();
        disputeRepository.findByOrder_Merchant_Id(merchantId).forEach(dispute -> results.add(toDto(dispute)));
        return results;
    }

    @Override
    public Optional<DisputeDto> updateStatus(UUID disputeId, String status) {
        if (disputeId == null || status == null) {
            return Optional.empty();
        }
        return disputeRepository.findById(disputeId).map(dispute -> {
            Dispute.Status target = parseStatus(status);
            dispute.setStatus(target);
            if (target == Dispute.Status.ESCALATED) {
                dispute.setEscalationAt(OffsetDateTime.now());
            }
            disputeRepository.save(dispute);
            notifyDispute(dispute, "dispute.status.updated", Map.of(
                    "disputeId", dispute.getId().toString(),
                    "status", dispute.getStatus().name()
            ), target == Dispute.Status.ESCALATED);
            return toDto(dispute);
        });
    }

    @Override
    public Optional<DisputeDto> partialRefund(UUID disputeId, int amountCents) {
        if (disputeId == null || amountCents <= 0) {
            return Optional.empty();
        }
        return disputeRepository.findById(disputeId).map(dispute -> {
            Transaction transaction = transactionRepository.findFirstByOrder_IdOrderByCreatedAtDesc(dispute.getOrder().getId())
                    .orElseThrow(() -> new IllegalStateException("No transaction found for dispute"));
            if (amountCents > transaction.getGrossAmountCents()) {
                throw new IllegalArgumentException("Partial refund exceeds transaction amount");
            }
            try {
                stripeService.refundPartial(transaction, amountCents);
            } catch (StripeException ex) {
                throw new IllegalStateException("Failed to refund partial amount", ex);
            }
            PartialRefundOffer offer = partialRefundOfferRepository.findByDispute_Id(disputeId)
                    .orElseGet(() -> {
                        PartialRefundOffer newOffer = new PartialRefundOffer();
                        newOffer.setId(UUID.randomUUID());
                        newOffer.setDispute(dispute);

                        return newOffer;
                    });
            offer.setAmountCents(amountCents);
            offer.setStatus(PartialRefundOffer.Status.ACCEPTED);
            offer.setDecidedAt(OffsetDateTime.now());
            partialRefundOfferRepository.save(offer);

            dispute.setStatus(Dispute.Status.BUYER_ACCEPTED_PARTIAL);
            disputeRepository.save(dispute);

            notifyDispute(dispute, "dispute.partial_refund", Map.of(
                    "disputeId", dispute.getId().toString(),
                    "amountCents", amountCents,
                    "status", dispute.getStatus().name()
            ), true);
            return toDto(dispute);
        });
    }

    @Override
    public Optional<DisputeDto> escalate(UUID disputeId) {
        if (disputeId == null) {
            return Optional.empty();
        }
        return disputeRepository.findById(disputeId).map(dispute -> {
            dispute.setStatus(Dispute.Status.ESCALATED);
            dispute.setEscalationAt(OffsetDateTime.now());
            disputeRepository.save(dispute);
            notifyDispute(dispute, "dispute.escalated", Map.of(
                    "disputeId", dispute.getId().toString(),
                    "status", dispute.getStatus().name()
            ), true);
            return toDto(dispute);
        });
    }

    private DisputeDto loadDto(UUID disputeId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found"));
        return toDto(dispute);
    }

    private DisputeDto toDto(Dispute dispute) {
        List<DisputeMessageDto> messages = new ArrayList<>();
        disputeMessageRepository.findByDispute_IdOrderByCreatedAtAsc(dispute.getId())
                .forEach(message -> messages.add(new DisputeMessageDto(
                        message.getAuthorRole().name(),
                        message.getMessage(),
                        readEvidence(message.getEvidenceUrls()),
                        message.getCreatedAt()
                )));
        PartialRefundOfferDto offerDto = partialRefundOfferRepository.findByDispute_Id(dispute.getId())
                .map(offer -> new PartialRefundOfferDto(
                        offer.getAmountCents(),
                        offer.getStatus().name(),
                        offer.getCreatedAt(),
                        offer.getDecidedAt()
                ))
                .orElse(null);
        Order order = dispute.getOrder();
        return new DisputeDto(
                dispute.getId().toString(),
                order != null ? order.getOrderNumber() : null,
                dispute.getBuyerEmail(),
                dispute.getStatus().name(),
                dispute.isReturnRequired(),
                dispute.getCreatedAt(),
                dispute.getEscalationAt(),
                messages,
                offerDto
        );
    }

    private List<String> readEvidence(String evidenceJson) {
        if (!StringUtils.hasText(evidenceJson)) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(evidenceJson);
            if (node.isArray()) {
                List<String> values = new ArrayList<>();
                node.forEach(child -> values.add(child.asText()));
                return values;
            }
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse evidence URLs: {}", ex.getMessage());
        }
        return List.of();
    }

    private String writeEvidence(List<String> evidenceUrls) {
        if (CollectionUtils.isEmpty(evidenceUrls)) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(evidenceUrls);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize evidence URLs", ex);
        }
    }

    private Dispute.Status parseStatus(String status) {
        try {
            return Dispute.Status.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown dispute status: " + status, ex);
        }
    }

    private void notifyDispute(Dispute dispute, String eventType, Object payload, boolean notifyAdmins) {
        Order order = dispute.getOrder();
        if (order == null || order.getMerchant() == null) {
            return;
        }
        var merchant = order.getMerchant();
        notificationService.publish(new NotificationPublishRequest(
                merchant.getUser().getId().toString(),
                merchant.getSupportEmail(),
                merchant.getId(),
                notifyAdmins,
                eventType,
                payload
        ));
        if (StringUtils.hasText(dispute.getBuyerEmail())) {
            String buyerUserId = order.getBuyerUser() != null ? order.getBuyerUser().getId().toString() : null;
            notificationService.publish(new NotificationPublishRequest(
                    buyerUserId,
                    dispute.getBuyerEmail(),
                    null,
                    false,
                    eventType,
                    payload
            ));
        }
    }
}
