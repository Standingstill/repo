package com.ensureback.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth/stripe")
@Deprecated
public class AuthController {

    @PostMapping("/start")
    public ResponseEntity<Void> start(@RequestBody(required = false) Object ignored) {
        throw new ResponseStatusException(HttpStatus.GONE, "Use /api/stripe/onboard instead");
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> callback(@RequestBody Object ignored) {
        throw new ResponseStatusException(HttpStatus.GONE, "Use /api/stripe/callback instead");
    }
}
