package com.ensureback.notification;

import com.ensureback.email.EmailService;
import com.ensureback.notification.dto.NotificationDto;
import com.ensureback.notification.dto.NotificationPublishRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private static final int DEFAULT_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final NotificationWebSocketService notificationWebSocketService;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   NotificationWebSocketService notificationWebSocketService,
                                   EmailService emailService,
                                   ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.notificationWebSocketService = notificationWebSocketService;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> listByRecipient(String recipientEmail) {
        if (!StringUtils.hasText(recipientEmail)) {
            return List.of();
        }
        var page = PageRequest.of(0, DEFAULT_PAGE_SIZE);
        List<NotificationDto> responses = new ArrayList<>();
        notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(recipientEmail, page)
                .forEach(notification -> responses.add(toDto(notification)));
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> listByUser(String userId) {
        if (!StringUtils.hasText(userId)) {
            return List.of();
        }
        var page = PageRequest.of(0, DEFAULT_PAGE_SIZE);
        List<NotificationDto> responses = new ArrayList<>();
        notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, page)
                .forEach(notification -> responses.add(toDto(notification)));
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NotificationDto> findById(UUID notificationId) {
        if (notificationId == null) {
            return Optional.empty();
        }
        return notificationRepository.findById(notificationId).map(this::toDto);
    }

    @Override
    public Optional<NotificationDto> updateDeliveryStatus(UUID notificationId, boolean delivered) {
        if (notificationId == null) {
            return Optional.empty();
        }
        return notificationRepository.findById(notificationId).map(notification -> {
            notification.setDelivered(delivered);
            notification.setDeliveredAt(delivered ? OffsetDateTime.now() : null);
            Notification saved = notificationRepository.save(notification);
            return toDto(saved);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(String userId) {
        if (!StringUtils.hasText(userId)) {
            return 0L;
        }
        return notificationRepository.countByUserIdAndDeliveredFalse(userId);
    }

    @Override
    public NotificationDto publish(NotificationPublishRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setUserId(request.userId());
        notification.setRecipientEmail(request.recipientEmail());
        notification.setEventType(request.eventType());
        notification.setPayload(writePayload(request.payload()));
        notification.setDelivered(false);
        notification.setDeliveredAt(null);
        Notification saved = notificationRepository.save(notification);
        NotificationDto response = toDto(saved);

        if (StringUtils.hasText(request.userId())) {
            notificationWebSocketService.sendToUser(request.userId(), response);
        }
        if (request.merchantId() != null) {
            notificationWebSocketService.sendToMerchant(request.merchantId(), response);
        }
        if (request.notifyAdmins()) {
            notificationWebSocketService.sendToAdmins(response);
        }
        if (StringUtils.hasText(request.recipientEmail())) {
            String template = resolveTemplate(request.eventType());
            if (template != null) {
                Map<String, Object> model = new HashMap<>();
                model.put("payload", response.payload() != null ? response.payload().toPrettyString() : "");
                model.put("eventType", request.eventType());
                emailService.sendEmail(request.recipientEmail(), resolveSubject(request.eventType()), template, model);
            }
        }

        log.debug("Published notification {} with event {}", response.notificationId(), request.eventType());
        return response;
    }

    private String writePayload(Object payload) {
        try {
            JsonNode node = payload != null ? objectMapper.valueToTree(payload) : objectMapper.createObjectNode();
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize notification payload", ex);
        }
    }

    private NotificationDto toDto(Notification notification) {
        JsonNode payloadNode = objectMapper.createObjectNode();
        String payload = notification.getPayload();
        if (StringUtils.hasText(payload)) {
            try {
                payloadNode = objectMapper.readTree(payload);
            } catch (JsonProcessingException ex) {
                log.warn("Unable to parse payload for notification {}", notification.getId(), ex);
            }
        }
        return new NotificationDto(
                notification.getId().toString(),
                notification.getEventType(),
                payloadNode,
                notification.isDelivered(),
                notification.getDeliveredAt(),
                notification.getCreatedAt()
        );
    }

    private String resolveSubject(String eventType) {
        return switch (eventType) {
            case "order.confirmation.buyer" -> "Your EnsureBack order confirmation";
            case "order.confirmation.merchant" -> "New order confirmed";
            case "order.confirmation.admin" -> "Order recorded";
            case "dispute.created" -> "Dispute opened";
            case "dispute.partial_refund" -> "Partial refund offer";
            case "dispute.escalated" -> "Dispute escalated";
            case "dispute.decision" -> "Dispute decision";
            case "magic.link" -> "Access your EnsureBack order";
            case "transaction.escrow.released" -> "Escrow released";
            case "transaction.escrow.refunded" -> "Escrow refunded";
            default -> "EnsureBack notification";
        };
    }

    private String resolveTemplate(String eventType) {
        return switch (eventType) {
            case "order.confirmation.buyer" -> "order-confirmation-buyer";
            case "order.confirmation.merchant" -> "order-confirmation-merchant";
            case "order.confirmation.admin" -> "order-confirmation-admin";
            case "dispute.created" -> "dispute-update";
            case "dispute.partial_refund" -> "dispute-partial-offer";
            case "dispute.escalated" -> "dispute-escalation";
            case "dispute.decision" -> "dispute-decision";
            case "magic.link" -> "magic-link";
            case "transaction.escrow.released" -> "transaction-escrow-released";
            case "transaction.escrow.refunded" -> "transaction-escrow-refunded";
            default -> "notification-default";
        };
    }
}