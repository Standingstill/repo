package com.ensureback.user;

import com.ensureback.user.dto.UserDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<UserDto> create(String stripeAccountId, User.Role role) {
        if (!StringUtils.hasText(stripeAccountId)) {
            return Optional.empty();
        }
        if (userRepository.findByStripeAccountId(stripeAccountId).isPresent()) {
            return Optional.empty();
        }
        User user = new User(UUID.randomUUID(), role, stripeAccountId.trim(), null, null);
        return Optional.of(toDto(userRepository.save(user)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDto> findById(UUID userId) {
        return userRepository.findById(userId).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDto> findByStripeAccountId(String stripeAccountId) {
        if (!StringUtils.hasText(stripeAccountId)) {
            return Optional.empty();
        }
        return userRepository.findByStripeAccountId(stripeAccountId.trim()).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> listByRole(User.Role role) {
        return userRepository.findByRole(role).stream().map(this::toDto).toList();
    }

    @Override
    public Optional<UserDto> updateStripeAccount(UUID userId, String stripeAccountId) {
        if (userId == null || !StringUtils.hasText(stripeAccountId)) {
            return Optional.empty();
        }
        return userRepository.findById(userId)
                .map(user -> {
                    String normalized = stripeAccountId.trim();
                    userRepository.findByStripeAccountId(normalized)
                            .filter(existing -> !existing.getId().equals(userId))
                            .ifPresent(existing -> {
                                throw new IllegalArgumentException("Stripe account already assigned to another user");
                            });
                    user.setStripeAccountId(normalized);
                    return toDto(userRepository.save(user));
                });
    }

    private UserDto toDto(User user) {
        return new UserDto(
                user.getId().toString(),
                user.getStripeAccountId(),
                user.getRole().name(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
