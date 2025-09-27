package com.ensureback.transaction;

public record EnsurebackFee(int ensurebackFeeCents, int netAmountCents) {

    public EnsurebackFee {
        if (ensurebackFeeCents < 0) {
            throw new IllegalArgumentException("ensurebackFeeCents must be non-negative");
        }
        if (netAmountCents < 0) {
            throw new IllegalArgumentException("netAmountCents must be non-negative");
        }
    }
}