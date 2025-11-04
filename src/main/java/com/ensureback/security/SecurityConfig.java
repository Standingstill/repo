package com.ensureback.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private final ApiKeyAuthFilter apiKeyAuthFilter;

    public SecurityConfig(ApiKeyAuthFilter apiKeyAuthFilter) {
        this.apiKeyAuthFilter = apiKeyAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                // FIXED: Only configure CSRF once and disable it completely
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(json401EntryPoint()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/health",
                                "/api/auth/stripe/**",
                                "/api/stripe/onboard",
                                "/api/stripe/callback",
                                "/api/developer/wizard/stripe/callback",
                                "/api/developer/docs",
                                "/ws/**",
                                "/dev/docs",
                                "/api/merchant/status").permitAll()
                        .requestMatchers(HttpMethod.POST, "/dev/register-keys").authenticated()
                        .requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().authenticated()
                )
                // FIXED: Removed duplicate csrf configuration that was re-enabling CSRF
                .headers(headers -> headers.frameOptions().sameOrigin())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(apiKeyAuthFilter, JwtAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint json401EntryPoint() {
        return new AuthenticationEntryPoint() {
            @Override
            public void commence(HttpServletRequest request, HttpServletResponse response,
                                 org.springframework.security.core.AuthenticationException authException) throws IOException {
                if (!response.isCommitted()) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"UNAUTHENTICATED\",\"message\":\"Session expired or missing\"}");
                }
            }
        };
    }
}