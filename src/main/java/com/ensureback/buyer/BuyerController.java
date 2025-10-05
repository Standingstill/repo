package com.ensureback.buyer;

import com.ensureback.dispute.Dispute;
import com.ensureback.dispute.DisputeRepository;
import com.ensureback.order.Order;
import com.ensureback.order.OrderRepository;
import com.ensureback.security.EnsurebackUserDetails;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/buyer")
public class BuyerController {

    private final OrderRepository orderRepository;
    private final DisputeRepository disputeRepository;

    public BuyerController(OrderRepository orderRepository, DisputeRepository disputeRepository) {
        this.orderRepository = orderRepository;
        this.disputeRepository = disputeRepository;
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('BUYER')")
    public List<BuyerOrder> orders(@AuthenticationPrincipal EnsurebackUserDetails principal) {
        EnsurebackUserDetails user = requirePrincipal(principal);
        List<Order> orders = orderRepository.findByBuyerUser_Id(user.getUserId());
        if (orders.isEmpty()) {
            return sampleOrders();
        }

        Map<UUID, Dispute> disputesByOrder = disputeRepository.findByOrder_BuyerUser_Id(user.getUserId()).stream()
                .collect(Collectors.groupingBy(dispute -> dispute.getOrder().getId(),
                        Collectors.collectingAndThen(Collectors.maxBy(Comparator.comparing(Dispute::getCreatedAt)),
                                optional -> optional.orElse(null))));

        return orders.stream()
                .map(order -> toBuyerOrder(order, disputesByOrder.get(order.getId())))
                .sorted(Comparator.comparing(BuyerOrder::updatedAt).reversed())
                .toList();
    }

    private BuyerOrder toBuyerOrder(Order order, Dispute dispute) {
        OffsetDateTime updatedAt = Optional.ofNullable(order.getDeliveredAt())
                .or(() -> Optional.ofNullable(order.getExpectedDeliveryAt()))
                .orElse(order.getCreatedAt());
        String currency = Optional.ofNullable(order.getCurrency())
                .filter(StringUtils::hasText)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .orElse("usd");
        String disputeStatus = dispute != null ? normalizeDisputeStatus(dispute.getStatus()) : null;
        Integer partialRefund = Optional.ofNullable(dispute)
                .map(Dispute::getPartialRefundOffer)
                .map(offer -> offer.getAmountCents())
                .orElse(null);
        return new BuyerOrder(
                order.getOrderNumber(),
                order.getProductName(),
                normalizeOrderStatus(order.getStatus()),
                order.calculateTotalAmountCents(),
                currency,
                updatedAt,
                disputeStatus,
                partialRefund
        );
    }

    private String normalizeOrderStatus(Order.Status status) {
        if (status == null) {
            return "processing";
        }
        return switch (status) {
            case PENDING_FULFILLMENT -> "processing";
            case SHIPPED -> "shipped";
            case DELIVERED, CLOSED -> "delivered";
            case CANCELLED -> "cancelled";
        };
    }

    private String normalizeDisputeStatus(Dispute.Status status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case OPEN, SELLER_RESPONDED, ESCALATED -> "open";
            case PARTIAL_REFUND_OFFERED, BUYER_ACCEPTED_PARTIAL -> "partial_refund";
            case RESOLVED_BUYER, RESOLVED_SELLER, CLOSED -> "resolved";
        };
    }

    private List<BuyerOrder> sampleOrders() {
        OffsetDateTime now = OffsetDateTime.now();
        return List.of(
                new BuyerOrder("ORDER-12345", "Wireless Earbuds", "processing", 12500, "usd", now.minusMinutes(15), null, null),
                new BuyerOrder("ORDER-67890", "Mechanical Keyboard", "delivered", 18900, "usd", now.minusHours(18), "open", 5000),
                new BuyerOrder("ORDER-24680", "4K Monitor", "disputed", 32900, "usd", now.minusHours(42), "partial_refund", 8000)
        );
    }

    private EnsurebackUserDetails requirePrincipal(EnsurebackUserDetails principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Buyer session not found");
        }
        return principal;
    }

    public record BuyerOrder(String orderId,
                             String productName,
                             String status,
                             int total,
                             String currency,
                             OffsetDateTime updatedAt,
                             String disputeStatus,
                             Integer partialRefundOffer) {
    }
}
