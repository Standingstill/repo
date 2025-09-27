package com.ensureback.user;

import com.ensureback.user.dto.UserDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<UserDto> create(String email, User.Role role) {
        return Optional.empty();
    }

    @Override
    public Optional<UserDto> findById(UUID userId) {
        return Optional.empty();
    }

    @Override
    public Optional<UserDto> findByEmail(String email) {
        return Optional.empty();
    }

    @Override
    public List<UserDto> listByRole(User.Role role) {
        return List.of();
    }

    @Override
    public Optional<UserDto> updateStripeAccount(UUID userId, boolean stripeAccountLinked) {
        return Optional.empty();
    }
}