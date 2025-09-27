package com.ensureback.transaction;

import com.ensureback.notification.NotificationService;
import com.ensureback.notification.dto.NotificationPublishRequest;
import com.ensureback.transaction.dto.TransactionChargeRequest;
import com.ensureback.transaction.dto.TransactionDto;
import com.ensureback.transaction.EnsurebackFee;
import com.ensureback.order.Order;
import com.ensureback.order.OrderRepository;
import com.ensureback.stripe.StripeService;
import com.ensureback.merchant.Merchant;
import com.ensureback.merchant.MerchantRepository;
import com.stripe.exception.StripeException;
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
import org.springframework.util.StringUtils;

@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final TransactionRepository transactionRepository;
    private final OrderRepository orderRepository;
    private final MerchantRepository merchantRepository;
    private final StripeService stripeService;
    private final NotificationService notificationService;
    private final FeeCalculatorService feeCalculatorService;

    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  OrderRepository orderRepository,
                                  MerchantRepository merchantRepository,
                                  StripeService stripeService,
                                  NotificationService notificationService,
                                  FeeCalculatorService feeCalculatorService) {
        this.transactionRepository = transactionRepository;
        this.orderRepository = orderRepository;
        this.merchantRepository = merchantRepository;
        this.stripeService = stripeService;
        this.notificationService = notificationService;
        this.feeCalculatorService = feeCalculatorService;
    }

    @Override
    public Optional<TransactionDto> create(TransactionChargeRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (!order.getMerchant().getId().equals(request.merchantId())) {
            throw new IllegalStateException("Merchant mismatch for order" );
        }
        int requestedAmount = request.amountCents();
        int orderTotal = order.calculateTotalAmountCents();
        if (requestedAmount != orderTotal) {
            throw new IllegalArgumentException("Charge amount must match order total");
        }
        Merchant merchant = merchantRepository.findById(request.merchantId())
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        EnsurebackFee fee = feeCalculatorService.calculate(orderTotal);
        try {
            Transaction transaction = stripeService.createPlatformCharge(merchant, order, fee);
            return Optional.of(toDto(transaction));
        } catch (StripeException ex) {
            throw new IllegalStateException("Failed to create platform charge", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TransactionDto> findById(UUID transactionId) {
        if (transactionId == null) {
            return Optional.empty();
        }
        return transactionRepository.findById(transactionId).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDto> listByMerchant(UUID merchantId) {
        if (merchantId == null) {
            return List.of();
        }
        List<TransactionDto> results = new ArrayList<>();
        transactionRepository.findByOrder_Merchant_Id(merchantId)
                .forEach(transaction -> results.add(toDto(transaction)));
        return results;
    }

    @Override
    public Optional<TransactionDto> updateEscrowStatus(UUID transactionId, String escrowStatus) {
        if (transactionId == null || !StringUtils.hasText(escrowStatus)) {
            return Optional.empty();
        }
        Transaction.EscrowStatus targetStatus = parseStatus(escrowStatus);
        return transactionRepository.findById(transactionId).map(transaction -> {
            Transaction.EscrowStatus originalStatus = transaction.getEscrowStatus();
            if (targetStatus == Transaction.EscrowStatus.RELEASED && originalStatus == Transaction.EscrowStatus.HELD) {
                releaseEscrow(transaction);
            } else if (targetStatus == Transaction.EscrowStatus.REFUNDED && originalStatus != Transaction.EscrowStatus.REFUNDED) {
                refundEscrow(transaction);
            } else if (targetStatus != originalStatus) {
                transaction.setEscrowStatus(targetStatus);
                transactionRepository.save(transaction);
            }
            Transaction refreshed = transactionRepository.findById(transaction.getId()).orElse(transaction);
            return toDto(refreshed);
        });
    }

    private void releaseEscrow(Transaction transaction) {
        try {
            stripeService.releaseEscrow(transaction);
            Transaction refreshed = transactionRepository.findById(transaction.getId()).orElse(transaction);
            Order order = refreshed.getOrder();
            notificationService.publish(new NotificationPublishRequest(
                    order.getMerchant().getUser().getId().toString(),
                    order.getMerchant().getSupportEmail(),
                    order.getMerchant().getId(),
                    false,
                    "transaction.escrow.released",
                    Map.of(
                            "transactionId", refreshed.getId().toString(),
                            "orderNumber", order.getOrderNumber(),
                            "status", refreshed.getEscrowStatus().name(),
                            "releasedAt", OffsetDateTime.now().toString()
                    )
            ));
        } catch (StripeException ex) {
            throw new IllegalStateException("Failed to release escrow", ex);
        }
    }

    private void refundEscrow(Transaction transaction) {
        try {
            stripeService.refundFull(transaction);
            Order order = transaction.getOrder();
            notificationService.publish(new NotificationPublishRequest(
                    order.getMerchant().getUser().getId().toString(),
                    order.getMerchant().getSupportEmail(),
                    order.getMerchant().getId(),
                    true,
                    "transaction.escrow.refunded",
                    Map.of(
                            "transactionId", transaction.getId().toString(),
                            "orderNumber", order.getOrderNumber(),
                            "status", Transaction.EscrowStatus.REFUNDED.name(),
                            "refundedAt", OffsetDateTime.now().toString()
                    )
            ));
        } catch (StripeException ex) {
            throw new IllegalStateException("Failed to refund escrow", ex);
        }
    }

    private TransactionDto toDto(Transaction transaction) {
        Order order = transaction.getOrder();
        return new TransactionDto(
                transaction.getId().toString(),
                order != null ? order.getOrderNumber() : null,
                transaction.getEscrowStatus().name(),
                transaction.getCaptureMode().name(),
                transaction.getGrossAmountCents(),
                transaction.getEnsurebackFeeCents(),
                transaction.getNetAmountCents(),
                transaction.getCurrency(),
                transaction.getCreatedAt()
        );
    }

    private Transaction.EscrowStatus parseStatus(String value) {
        try {
            return Transaction.EscrowStatus.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown escrow status: " + value, ex);
        }
    }
}






