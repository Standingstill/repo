package com.ensureback.merchant;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ensureback.security.EnsurebackUserDetails;

@RestController
@RequestMapping("/api/merchant")
public class MerchantController {

    @GetMapping("/me")
    @PreAuthorize("hasRole('MERCHANT')")
    public MerchantProfile me(@AuthenticationPrincipal EnsurebackUserDetails principal) {
        return new MerchantProfile(principal.getUserId(), principal.getUsername(), principal.getUser().getRole().name(), principal.getUser().getCreatedAt());
    }

    public record MerchantProfile(UUID id, String email, String role, OffsetDateTime createdAt) {
    }
}
