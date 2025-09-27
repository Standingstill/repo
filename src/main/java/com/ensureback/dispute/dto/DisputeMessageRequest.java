package com.ensureback.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record DisputeMessageRequest(
        @NotBlank String message,
        List<String> evidenceUrls
) {
}
