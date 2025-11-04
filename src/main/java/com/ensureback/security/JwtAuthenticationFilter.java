package com.ensureback.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.ensureback.user.User;
import com.ensureback.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, UserRepository userRepository) {
        this.jwtTokenService = jwtTokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = null;
        
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            token = header.substring(BEARER_PREFIX.length());
            log.debug("Found token in Authorization header for {}", requestUri);
        } else if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie c : request.getCookies()) {
                if ("EB_AUTH".equals(c.getName())) {
                    token = c.getValue();
                    log.debug("Found token in EB_AUTH cookie for {}", requestUri);
                    break;
                }
            }
        }
        
        // Dev fallback: accept token via custom header if present
        if (token == null) {
            String alt = request.getHeader("X-Auth-Token");
            if (org.springframework.util.StringUtils.hasText(alt)) {
                token = alt;
                log.debug("Found token in X-Auth-Token header for {}", requestUri);
            }
        }

        var currentAuth = SecurityContextHolder.getContext().getAuthentication();
        boolean canAuthenticate = (currentAuth == null)
                || (currentAuth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)
                || !currentAuth.isAuthenticated();

        if (token != null && canAuthenticate) {
            try {
                var decoded = jwtTokenService.verify(token);
                JwtTokenService.JwtPayload payload = jwtTokenService.toPayload(decoded);
                
                log.debug("JWT payload for {}: userId={}, stripeAccountId={}, role={}", 
                    requestUri, payload.userId(), payload.stripeAccountId(), 
                    decoded.getClaim("role").asString());
                
                if (!StringUtils.hasText(payload.stripeAccountId())) {
                    log.warn("JWT token for {} missing Stripe account ID - userId={}", requestUri, payload.userId());
                    throw new UsernameNotFoundException("Missing Stripe account identifier in token");
                }
                
                Optional<User> userOptional = userRepository.findById(payload.userId())
                        .filter(user -> payload.stripeAccountId().equals(user.getStripeAccountId()));
                
                if (userOptional.isEmpty()) {
                    log.warn("User not found or Stripe account mismatch for {} - userId={}, stripeAccountId={}", 
                        requestUri, payload.userId(), payload.stripeAccountId());
                    throw new UsernameNotFoundException("User not found");
                }
                
                EnsurebackUserDetails userDetails = new EnsurebackUserDetails(userOptional.get());
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("Successfully authenticated user {} for {}", payload.userId(), requestUri);
                
            } catch (JWTVerificationException ex) {
                log.warn("JWT verification failed for {}: {}", requestUri, ex.getMessage());
                SecurityContextHolder.clearContext();
            } catch (IllegalArgumentException ex) {
                log.warn("JWT parsing failed for {}: {}", requestUri, ex.getMessage());
                SecurityContextHolder.clearContext();
            } catch (UsernameNotFoundException ex) {
                log.warn("User lookup failed for {}: {}", requestUri, ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}