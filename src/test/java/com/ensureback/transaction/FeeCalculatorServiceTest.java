package com.ensureback.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.ensureback.config.EnsurebackProperties;
import org.junit.jupiter.api.Test;

class FeeCalculatorServiceTest {

    private final EnsurebackProperties properties = new EnsurebackProperties(120, 49, 19, "http://localhost:8080");
    private final FeeCalculatorService service = new FeeCalculatorService(properties);

    @Test
    void calculatesFeeWithRounding() {
        EnsurebackFee fee = service.calculate(10_000);
        assertThat(fee.ensurebackFeeCents()).isEqualTo(68);
        assertThat(fee.netAmountCents()).isEqualTo(9_932);
    }

    @Test
    void capsFeeAtGrossAmount() {
        EnsurebackProperties highFeeProps = new EnsurebackProperties(120, 10_000, 100, "http://localhost:8080");
        FeeCalculatorService highFeeService = new FeeCalculatorService(highFeeProps);
        EnsurebackFee fee = highFeeService.calculate(50);
        assertThat(fee.ensurebackFeeCents()).isEqualTo(50);
        assertThat(fee.netAmountCents()).isZero();
    }
}
