package com.ensureback;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ensureback.user.User;
import com.ensureback.user.User.Role;
import com.ensureback.user.UserRepository;

@SpringBootApplication
@EnableScheduling
public class EnsurebackApplication {

    private static final Logger log = LoggerFactory.getLogger(EnsurebackApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(EnsurebackApplication.class, args);
    }

    @Bean
    CommandLineRunner demoUserLoader(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            seedUser(userRepository, passwordEncoder,
                    "demo@ensureback.test",
                    Role.MERCHANT,
                    "demo-password-hash");

            seedUser(userRepository, passwordEncoder,
                    "admin@ensureback.test",
                    Role.ADMIN,
                    "admin-password-hash");
        };
    }

    private void seedUser(UserRepository userRepository, PasswordEncoder passwordEncoder, String email, Role role, String passwordSeed) {
        userRepository.findByEmail(email).ifPresentOrElse(
                user -> log.info("Seed user already exists: {}", user),
                () -> {
                    OffsetDateTime now = OffsetDateTime.now();
                    String encodedPassword = passwordEncoder.encode(passwordSeed);
                    User newUser = new User(
                            UUID.randomUUID(),
                            email,
                            role,
                            encodedPassword,
                            null,
                            now,
                            now
                    );
                    User savedUser = userRepository.save(newUser);
                    log.info("Inserted seed user: {}", savedUser);
                }
        );
    }
}
