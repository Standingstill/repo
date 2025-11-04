package com.ensureback.merchant;

import com.ensureback.dispute.DisputeService;
import com.ensureback.dispute.dto.DisputeDto;
import com.ensureback.order.OrderService;
import com.ensureback.order.dto.OrderDto;
import com.ensureback.security.EnsurebackUserDetails;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/merchant")
public class MerchantController {

    private final OrderService orderService;
    private final DisputeService disputeService;
    private final MerchantRepository merchantRepository;

    public MerchantController(OrderService orderService,
                              DisputeService disputeService,
                              MerchantRepository merchantRepository) {
        this.orderService = orderService;
        this.disputeService = disputeService;
        this.merchantRepository = merchantRepository;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('MERCHANT')")
    public MerchantProfile me(@AuthenticationPrincipal EnsurebackUserDetails principal) {
        EnsurebackUserDetails user = requirePrincipal(principal);
        Merchant merchant = merchantRepository.findByUserId(user.getUserId())
                .or(() -> merchantRepository.findByStripeAccountId(user.getUser().getStripeAccountId()))
                .orElseGet(() -> {
                    Merchant m = new Merchant();
                    m.setId(UUID.randomUUID());
                    m.setUser(user.getUser());
                    m.setStripeAccountId(user.getUser().getStripeAccountId());
                    m.setBusinessName("New Merchant");
                    m.setSupportEmail("support@merchant.local");
                    m.setDisputeWindowHours(120);
                    return merchantRepository.save(m);
                });
        return new MerchantProfile(
                merchant.getId(),
                user.getUsername(),
                user.getUser().getRole().name(),
                merchant.getStripeAccountId(),
                merchant.isIntegrated(),
                user.getUser().getCreatedAt()
        );
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('MERCHANT')")
    public List<MerchantOrder> orders(@AuthenticationPrincipal EnsurebackUserDetails principal) {
        EnsurebackUserDetails user = requirePrincipal(principal);
        return loadOrders(user);
    }

    @GetMapping("/disputes")
    @PreAuthorize("hasRole('MERCHANT')")
    public List<MerchantDispute> disputes(@AuthenticationPrincipal EnsurebackUserDetails principal) {
        EnsurebackUserDetails user = requirePrincipal(principal);
        return loadDisputes(user);
    }

    @GetMapping("/balance")
    @PreAuthorize("hasRole('MERCHANT')")
    public MerchantBalance balance(@AuthenticationPrincipal EnsurebackUserDetails principal) {
        EnsurebackUserDetails user = requirePrincipal(principal);
        List<MerchantOrder> orders = loadOrders(user);
        long escrow = orders.stream()
                .filter(order -> !"fulfilled".equals(order.status()))
                .mapToLong(MerchantOrder::amountCents)
                .sum();
        long available = orders.stream()
                .filter(order -> "fulfilled".equals(order.status()))
                .mapToLong(MerchantOrder::amountCents)
                .sum();
        String currency = orders.stream()
                .map(MerchantOrder::currency)
                .filter(StringUtils::hasText)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .findFirst()
                .orElse("usd");
        return new MerchantBalance(escrow, available, currency);
    }

    @GetMapping("/policies")
    @PreAuthorize("hasRole('MERCHANT')")
    public MerchantPolicies policies(@AuthenticationPrincipal EnsurebackUserDetails principal) {
        EnsurebackUserDetails user = requirePrincipal(principal);
        Merchant merchant = merchantRepository.findByUserId(user.getUserId()).orElse(null);

        int window = 72;
        String supportEmail = "support@ensureback.example";
        if (merchant != null) {
            if (merchant.getDisputeWindowHours() != null) {
                window = merchant.getDisputeWindowHours();
            }
            if (StringUtils.hasText(merchant.getSupportEmail())) {
                supportEmail = merchant.getSupportEmail();
            }
        }

        String refundPolicy = "Refunds are accepted within " + window + " hours of delivery if the item is unopened or defective.";
        String cancellationPolicy = "Orders can be cancelled any time before fulfillment. Contact support if already shipped.";

        return new MerchantPolicies(window, refundPolicy, cancellationPolicy, supportEmail);
    }

    private List<MerchantOrder> loadOrders(EnsurebackUserDetails principal) {
        List<OrderDto> dtos = Optional.ofNullable(orderService.listByMerchant(principal.getUserId()))
                .orElse(List.of());
        if (dtos.isEmpty()) {
            return sampleOrders();
        }
        return dtos.stream()
                .map(this::toMerchantOrder)
                .sorted(Comparator.comparing(MerchantOrder::updatedAt).reversed())
                .toList();
    }

    private List<MerchantDispute> loadDisputes(EnsurebackUserDetails principal) {
        List<DisputeDto> dtos = Optional.ofNullable(disputeService.listByMerchant(principal.getUserId()))
                .orElse(List.of());
        if (dtos.isEmpty()) {
            return sampleDisputes();
        }
        return dtos.stream()
                .map(this::toMerchantDispute)
                .sorted(Comparator.comparing(MerchantDispute::openedAt).reversed())
                .toList();
    }

    private MerchantOrder toMerchantOrder(OrderDto dto) {
        OffsetDateTime updatedAt = Optional.ofNullable(dto.deliveredAt())
                .or(() -> Optional.ofNullable(dto.expectedDeliveryAt()))
                .orElse(dto.createdAt());
        String currency = Optional.ofNullable(dto.currency())
                .filter(StringUtils::hasText)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .orElse("usd");
        return new MerchantOrder(
                Optional.ofNullable(dto.orderNumber()).orElse(UUID.randomUUID().toString()),
                dto.buyerEmail(),
                normalizeOrderStatus(dto.status()),
                dto.totalAmountCents(),
                currency,
                updatedAt
        );
    }

    private MerchantDispute toMerchantDispute(DisputeDto dto) {
        String reason = "Customer inquiry";
        if (!CollectionUtils.isEmpty(dto.messages())) {
            reason = dto.messages().stream()
                    .map(message -> Optional.ofNullable(message.message()).orElse(""))
                    .filter(StringUtils::hasText)
                    .findFirst()
                    .orElse(reason);
        }
        OffsetDateTime openedAt = Optional.ofNullable(dto.createdAt()).orElse(OffsetDateTime.now());
        return new MerchantDispute(
                dto.disputeId(),
                dto.orderNumber(),
                normalizeDisputeStatus(dto.status()),
                reason,
                openedAt
        );
    }

    private String normalizeOrderStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "processing";
        }
        return status.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private String normalizeDisputeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "open";
        }
        return status.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private List<MerchantOrder> sampleOrders() {
        OffsetDateTime now = OffsetDateTime.now();
        return List.of(
                new MerchantOrder("ord_12345", "buyer1@example.com", "processing", 12900, "usd", now.minusMinutes(45)),
                new MerchantOrder("ord_67890", "buyer2@example.com", "fulfilled", 8900, "usd", now.minusHours(7)),
                new MerchantOrder("ord_24680", "buyer3@example.com", "disputed", 21000, "usd", now.minusHours(30))
        );
    }

    private List<MerchantDispute> sampleDisputes() {
        OffsetDateTime now = OffsetDateTime.now();
        return List.of(
                new MerchantDispute("dp_101", "ord_24680", "open", "Item not as described", now.minusDays(1)),
                new MerchantDispute("dp_205", "ord_13579", "partial_refund", "Shipping delay", now.minusDays(3)),
                new MerchantDispute("dp_309", "ord_54321", "closed", "Resolved in buyer favor", now.minusDays(4))
        );
    }

    private EnsurebackUserDetails requirePrincipal(EnsurebackUserDetails principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Merchant session not found");
        }
        return principal;
    }

    public record MerchantProfile(UUID id,
                                  String email,
                                  String role,
                                  String stripeAccountId,
                                  boolean isIntegrated,
                                  OffsetDateTime createdAt) {
    }

    public record MerchantOrder(String id,
                                String customerEmail,
                                String status,
                                int amountCents,
                                String currency,
                                OffsetDateTime updatedAt) {
    }

    public record MerchantDispute(String id,
                                  String orderId,
                                  String status,
                                  String reason,
                                  OffsetDateTime openedAt) {
    }

    public record MerchantBalance(long escrow,
                                  long available,
                                  String currency) {
    }

    public record MerchantPolicies(int disputeWindowHours,
                                   String refundPolicy,
                                   String cancellationPolicy,
                                   String supportEmail) {
    }
}
