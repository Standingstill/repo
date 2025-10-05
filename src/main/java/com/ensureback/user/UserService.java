package com.ensureback.user;

import com.ensureback.user.dto.UserDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserService {

    Optional<UserDto> create(String stripeAccountId, User.Role role);

    Optional<UserDto> findById(UUID userId);

    Optional<UserDto> findByStripeAccountId(String stripeAccountId);

    List<UserDto> listByRole(User.Role role);

    Optional<UserDto> updateStripeAccount(UUID userId, String stripeAccountId);
}
