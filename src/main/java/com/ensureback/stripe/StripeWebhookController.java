package com.ensureback.stripe;

import com.ensureback.transaction.Transaction;
import com.ensureback.transaction.TransactionRepository;
import com.ensureback.transfer.Transfer;
import com.ensureback.transfer.TransferRepository;
import com.ensureback.webhook.WebhookIdempotencyService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeProperties stripeProperties;
    private final StripeEventLogRepository eventLogRepository;
    private final TransactionRepository transactionRepository;
    private final TransferRepository transferRepository;
    private final WebhookIdempotencyService webhookIdempotencyService;

    public StripeWebhookController(StripeProperties stripeProperties,
                                   StripeEventLogRepository eventLogRepository,
                                   TransactionRepository transactionRepository,
                                   TransferRepository transferRepository,
                                   WebhookIdempotencyService webhookIdempotencyService) {
        this.stripeProperties = stripeProperties;
        this.eventLogRepository = eventLogRepository;
        this.transactionRepository = transactionRepository;
        this.transferRepository = transferRepository;
        this.webhookIdempotencyService = webhookIdempotencyService;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Void> receive(@RequestBody String payload,
                                        @RequestHeader("Stripe-Signature") String signature,
                                        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey)
        throws StripeException {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, stripeProperties.getWebhookSecret());
        } catch (SignatureVerificationException ex) {
            log.warn("Invalid Stripe signature: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        String eventId = event.getId();
        String effectiveKey = idempotencyKey != null && !idempotencyKey.isBlank() ? idempotencyKey : eventId;
        if (!webhookIdempotencyService.registerInvocation("stripe", effectiveKey, payload)) {
            log.debug("Ignoring Stripe webhook due to idempotency key {}", effectiveKey);
            return ResponseEntity.ok().build();
        }

        if (eventId != null && eventLogRepository.findByEventId(eventId).isPresent()) {
            log.debug("Ignoring duplicate Stripe event {}", eventId);
            return ResponseEntity.ok().build();
        }

        handleEvent(event);

        if (eventId != null) {
            StripeEventLog logEntry = new StripeEventLog(UUID.randomUUID(), eventId, OffsetDateTime.now());
            eventLogRepository.save(logEntry);
        }
        return ResponseEntity.ok().build();
    }

    private void handleEvent(Event event) throws StripeException {
        String eventType = event.getType();
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (!deserializer.getObject().isPresent()) {
            log.warn("Unable to deserialize object for event {}", event.getId());
            return;
        }
        StripeObject stripeObject = deserializer.getObject().get();

        switch (eventType) {
            case "payment_intent.succeeded" -> handlePaymentIntentSucceeded((PaymentIntent) stripeObject);
            case "charge.refunded" -> handleChargeRefunded((Charge) stripeObject);
            case "transfer.created" -> handleTransferUpdate((com.stripe.model.Transfer) stripeObject, Transfer.Status.SUCCEEDED);
            case "transfer.failed" -> handleTransferUpdate((com.stripe.model.Transfer) stripeObject, Transfer.Status.FAILED);
            default -> log.debug("Unhandled Stripe event type: {}", eventType);
        }
    }

    private void handlePaymentIntentSucceeded(PaymentIntent paymentIntent) {
        String paymentIntentId = paymentIntent.getId();
        if (paymentIntentId == null) {
            log.warn("payment_intent.succeeded without id");
            return;
        }
        Optional<Transaction> transactionOpt = transactionRepository.findByStripePaymentIntentId(paymentIntentId);
        if (transactionOpt.isEmpty()) {
            log.warn("No transaction found for payment intent {}", paymentIntentId);
            return;
        }
        Transaction transaction = transactionOpt.get();
        transaction.setEscrowStatus(Transaction.EscrowStatus.HELD);
        transactionRepository.save(transaction);
        log.info("Marked transaction {} as HELD", transaction.getId());
    }

    private void handleChargeRefunded(Charge charge) {
        String chargeId = charge.getId();
        if (chargeId == null) {
            log.warn("charge.refunded without id");
            return;
        }
        Optional<Transaction> transactionOpt = transactionRepository.findByPlatformChargeId(chargeId);
        if (transactionOpt.isEmpty()) {
            String paymentIntentId = charge.getPaymentIntent();
            if (paymentIntentId != null) {
                transactionOpt = transactionRepository.findByStripePaymentIntentId(paymentIntentId);
            }
        }
        if (transactionOpt.isEmpty()) {
            log.warn("No transaction found for charge {}", chargeId);
            return;
        }
        Transaction transaction = transactionOpt.get();
        transaction.setEscrowStatus(Transaction.EscrowStatus.REFUNDED);
        transactionRepository.save(transaction);
        log.info("Marked transaction {} as REFUNDED due to charge {}", transaction.getId(), chargeId);
    }

    private void handleTransferUpdate(com.stripe.model.Transfer stripeTransfer, Transfer.Status status) {
        String transferId = stripeTransfer.getId();
        if (transferId == null) {
            log.warn("transfer event without id");
            return;
        }
        Optional<Transfer> transferOpt = transferRepository.findByStripeTransferId(transferId);
        if (transferOpt.isEmpty()) {
            log.warn("No transfer record found for Stripe transfer {}", transferId);
            return;
        }
        Transfer transfer = transferOpt.get();
        transfer.setStatus(status);
        transferRepository.save(transfer);
        log.info("Updated transfer {} status to {}", transferId, status);
    }

    @ExceptionHandler(StripeException.class)
    public ResponseEntity<Void> handleStripeException(StripeException ex) {
        log.error("Stripe exception while processing webhook", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}