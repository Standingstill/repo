package com.ensureback.dispute.dto;

import jakarta.validation.constraints.Min;

public record PartialOfferRequest(@Min(1) int amountCents) {
}
