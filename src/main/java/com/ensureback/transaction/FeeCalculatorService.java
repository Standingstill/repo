package com.ensureback.transaction;

import com.ensureback.config.EnsurebackProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class FeeCalculatorService {

    private static final BigDecimal TEN_THOUSAND = BigDecimal.valueOf(10_000);

    private final EnsurebackProperties properties;

    public FeeCalculatorService(EnsurebackProperties properties) {
        this.properties = properties;
    }

    public EnsurebackFee calculate(int grossAmountCents) {
        if (grossAmountCents < 0) {
            throw new IllegalArgumentException("grossAmountCents must be non-negative");
        }
        BigDecimal gross = BigDecimal.valueOf(grossAmountCents);
        BigDecimal bps = BigDecimal.valueOf(properties.getFeeBps());
        BigDecimal percentageFee = gross.multiply(bps).divide(TEN_THOUSAND, 6, RoundingMode.HALF_UP);
        int percentageFeeCents = percentageFee.setScale(0, RoundingMode.HALF_UP).intValueExact();
        int totalFee = Math.addExact(percentageFeeCents, properties.getFixedFeeCents());
        if (totalFee > grossAmountCents) {
            totalFee = grossAmountCents;
        }
        int net = grossAmountCents - totalFee;
        return new EnsurebackFee(totalFee, net);
    }
}