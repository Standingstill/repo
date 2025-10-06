package com.ensureback;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.ensureback.user.UserRepository;

@SpringBootApplication
@EnableScheduling
public class EnsurebackApplication {
    @Bean
    CommandLineRunner test(UserRepository repo) {
        return args -> {
            System.out.println("Users in DB: " + repo.findAll());
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(EnsurebackApplication.class, args);
    }
}
