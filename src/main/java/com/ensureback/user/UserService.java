package com.ensureback.user;

import com.ensureback.user.dto.UserDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserService {

    Optional<UserDto> create(String email, User.Role role);

    Optional<UserDto> findById(UUID userId);

    Optional<UserDto> findByEmail(String email);

    List<UserDto> listByRole(User.Role role);

    Optional<UserDto> updateStripeAccount(UUID userId, boolean stripeAccountLinked);
}