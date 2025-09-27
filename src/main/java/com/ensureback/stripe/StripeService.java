package com.ensureback.stripe;

import com.ensureback.merchant.Merchant;
import com.ensureback.order.Order;
import com.ensureback.transaction.EnsurebackFee;
import com.ensureback.transaction.Transaction;
import com.ensureback.transaction.TransactionRepository;
import com.ensureback.transfer.Transfer;
import com.ensureback.transfer.TransferRepository;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.TransferCreateParams;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StripeService {

    private static final Logger log = LoggerFactory.getLogger(StripeService.class);

    private final StripeClient stripeClient;
    private final TransactionRepository transactionRepository;
    private final TransferRepository transferRepository;

    public StripeService(StripeClient stripeClient,
                         TransactionRepository transactionRepository,
                         TransferRepository transferRepository) {
        this.stripeClient = stripeClient;
        this.transactionRepository = transactionRepository;
        this.transferRepository = transferRepository;
    }

    public Transaction createPlatformCharge(Merchant merchant, Order order, EnsurebackFee fee) throws StripeException {
        Objects.requireNonNull(merchant, "merchant must not be null");
        Objects.requireNonNull(order, "order must not be null");
        Objects.requireNonNull(fee, "fee must not be null");

        int grossAmountCents = order.calculateTotalAmountCents();
        if (grossAmountCents <= 0) {
            throw new IllegalArgumentException("grossAmountCents must be positive");
        }

        int feeCents = fee.ensurebackFeeCents();
        int netAmountCents = Math.subtractExact(grossAmountCents, feeCents);
        if (netAmountCents < 0) {
            throw new IllegalArgumentException("ensureback fee exceeds gross amount");
        }

        PaymentIntentCreateParams.AutomaticPaymentMethods automaticPaymentMethods =
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                        .build();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long) grossAmountCents)
                .setCurrency(order.getCurrency())
                .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.AUTOMATIC)
                .setTransferGroup(order.getOrderNumber())
                .setAutomaticPaymentMethods(automaticPaymentMethods)
                .putMetadata("orderId", String.valueOf(order.getId()))
                .putMetadata("merchantId", String.valueOf(merchant.getId()))
                .putMetadata("buyerEmail", order.getBuyerEmail())
                .build();

        log.info("Creating PaymentIntent for order {} and merchant {}", order.getOrderNumber(), merchant.getId());
        PaymentIntent paymentIntent = stripeClient.paymentIntents().create(params);

        String paymentIntentId = paymentIntent.getId();
        String latestCharge = paymentIntent.getLatestCharge();
        String chargeId = latestCharge != null ? latestCharge : paymentIntentId;

        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID());
        transaction.setOrder(order);
        transaction.setStripePaymentIntentId(paymentIntentId);
        transaction.setStripeChargeId(latestCharge);
        transaction.setPlatformChargeId(chargeId);
        transaction.setTransferGroup(paymentIntent.getTransferGroup());
        transaction.setEnsurebackFeeCents(feeCents);
        transaction.setGrossAmountCents(grossAmountCents);
        transaction.setNetAmountCents(netAmountCents);
        transaction.setCurrency(order.getCurrency());
        transaction.setEscrowStatus(Transaction.EscrowStatus.HELD);
        transaction.setCaptureMode(Transaction.CaptureMode.IMMEDIATE_CAPTURE_AND_HOLD_TRANSFER);
        transaction.setCreatedAt(OffsetDateTime.now());

        Transaction saved = transactionRepository.save(transaction);
        log.info("Persisted transaction {} for payment intent {}", saved.getId(), paymentIntentId);
        return saved;
    }

    public Transfer releaseEscrow(Transaction transaction) throws StripeException {
        Objects.requireNonNull(transaction, "transaction must not be null");
        Order order = transaction.getOrder();
        if (order == null) {
            throw new IllegalStateException("Transaction has no associated order");
        }
        Merchant merchant = order.getMerchant();
        if (merchant == null || merchant.getUser() == null || merchant.getUser().getStripeAccountId() == null) {
            throw new IllegalStateException("Merchant is missing a Stripe account id");
        }

        String destinationAccount = merchant.getUser().getStripeAccountId();
        TransferCreateParams params = TransferCreateParams.builder()
                .setAmount((long) transaction.getNetAmountCents())
                .setCurrency(transaction.getCurrency())
                .setDestination(destinationAccount)
                .setTransferGroup(transaction.getTransferGroup())
                .build();

        log.info("Creating transfer for transaction {}", transaction.getId());
        com.stripe.model.Transfer transferResponse = stripeClient.transfers().create(params);

        Transfer transfer = new Transfer();
        transfer.setId(UUID.randomUUID());
        transfer.setTransaction(transaction);
        transfer.setStripeTransferId(transferResponse.getId());
        transfer.setAmountCents(transaction.getNetAmountCents());
        transfer.setStatus(Transfer.Status.PENDING);
        transfer.setCreatedAt(OffsetDateTime.now());
        Transfer savedTransfer = transferRepository.save(transfer);

        transaction.setEscrowStatus(Transaction.EscrowStatus.RELEASED);
        transactionRepository.save(transaction);
        log.info("Escrow released for transaction {} via transfer {}", transaction.getId(), savedTransfer.getStripeTransferId());
        return savedTransfer;
    }

    public void refundFull(Transaction transaction) throws StripeException {
        refund(transaction, transaction.getGrossAmountCents());
    }

    public void refundPartial(Transaction transaction, int amountCents) throws StripeException {
        if (amountCents <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }
        if (amountCents > transaction.getGrossAmountCents()) {
            throw new IllegalArgumentException("Refund amount cannot exceed gross amount");
        }
        refund(transaction, amountCents);
    }

    private void refund(Transaction transaction, int amountCents) throws StripeException {
        Objects.requireNonNull(transaction, "transaction must not be null");
        if (transaction.getPlatformChargeId() == null) {
            throw new IllegalStateException("Transaction missing platform charge id");
        }

        RefundCreateParams params = RefundCreateParams.builder()
                .setCharge(transaction.getPlatformChargeId())
                .setAmount((long) amountCents)
                .build();

        log.info("Issuing refund of {} cents for transaction {}", amountCents, transaction.getId());
        Refund refund = stripeClient.refunds().create(params);
        log.debug("Refund {} created with status {}", refund.getId(), refund.getStatus());

        transaction.setEscrowStatus(Transaction.EscrowStatus.REFUNDED);
        transactionRepository.save(transaction);
    }

}


