package com.ensureback.transaction.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateEscrowStatusRequest(@NotBlank String escrowStatus) {
}