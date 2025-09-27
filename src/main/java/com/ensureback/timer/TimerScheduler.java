package com.ensureback.timer;

import com.ensureback.stripe.StripeService;
import com.ensureback.transaction.Transaction;
import com.ensureback.transaction.TransactionRepository;
import com.stripe.exception.StripeException;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TimerScheduler {

    private static final Logger log = LoggerFactory.getLogger(TimerScheduler.class);

    private final TimerRepository timerRepository;
    private final TransactionRepository transactionRepository;
    private final StripeService stripeService;

    public TimerScheduler(TimerRepository timerRepository,
                          TransactionRepository transactionRepository,
                          StripeService stripeService) {
        this.timerRepository = timerRepository;
        this.transactionRepository = transactionRepository;
        this.stripeService = stripeService;
    }

    @Scheduled(fixedDelayString = "PT1M")
    public void processTimers() {
        List<Timer> dueTimers = timerRepository.findByStateAndExpiresAtBefore(Timer.State.SCHEDULED, OffsetDateTime.now());
        for (Timer timer : dueTimers) {
            try {
                handleTimer(timer);
                timer.setState(Timer.State.FIRED);
                timerRepository.save(timer);
            } catch (Exception ex) {
                log.error("Failed to handle timer {}", timer.getId(), ex);
            }
        }
    }

    private void handleTimer(Timer timer) throws StripeException {
        if (timer.getType() != Timer.Type.DISPUTE_WINDOW) {
            return;
        }

        Transaction transaction = transactionRepository.findFirstByOrder_IdOrderByCreatedAtDesc(timer.getOrder().getId())
                .orElse(null);
        if (transaction == null) {
            log.warn("No transaction found for order {} when processing timer {}", timer.getOrder().getId(), timer.getId());
            return;
        }

        if (transaction.getEscrowStatus() != Transaction.EscrowStatus.HELD) {
            log.info("Skipping timer {} because escrow already {}", timer.getId(), transaction.getEscrowStatus());
            return;
        }

        stripeService.releaseEscrow(transaction);
        log.info("Released escrow for transaction {} via timer {}", transaction.getId(), timer.getId());
    }
}