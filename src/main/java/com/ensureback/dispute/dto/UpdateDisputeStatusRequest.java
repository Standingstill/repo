package com.ensureback.dispute.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateDisputeStatusRequest(@NotBlank String status) {
}