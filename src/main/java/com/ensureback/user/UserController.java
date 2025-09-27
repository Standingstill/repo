package com.ensureback.user;

import com.ensureback.user.dto.CreateUserRequest;
import com.ensureback.user.dto.UpdateStripeAccountRequest;
import com.ensureback.user.dto.UserDto;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody CreateUserRequest request) {
        User.Role role;
        try {
            role = User.Role.valueOf(request.role().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
        Optional<UserDto> created = userService.create(request.email(), role);
        return created
                .map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> listByRole(@RequestParam("role") String roleValue) {
        User.Role role;
        try {
            role = User.Role.valueOf(roleValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(userService.listByRole(role));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> findById(@PathVariable UUID userId) {
        return ResponseEntity.of(userService.findById(userId));
    }

    @GetMapping("/lookup")
    public ResponseEntity<UserDto> findByEmail(@RequestParam("email") String email) {
        return ResponseEntity.of(userService.findByEmail(email));
    }

    @PostMapping("/{userId}/stripe-account")
    public ResponseEntity<UserDto> updateStripeAccount(@PathVariable UUID userId,
                                                       @Valid @RequestBody UpdateStripeAccountRequest request) {
        return ResponseEntity.of(userService.updateStripeAccount(userId, request.stripeAccountLinked()));
    }
}