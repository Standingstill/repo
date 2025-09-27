package com.ensureback.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ensureback.auth.dto.LoginRequest;
import com.ensureback.auth.dto.LoginResponse;
import com.ensureback.security.EnsurebackUserDetails;
import com.ensureback.security.JwtTokenService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenService jwtTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        EnsurebackUserDetails principal = (EnsurebackUserDetails) authentication.getPrincipal();
        var token = jwtTokenService.createToken(principal.getUser());
        return ResponseEntity.ok(new LoginResponse(token.value(), "Bearer", token.expiresAt(), principal.getUser().getRole().name()));
    }
}
