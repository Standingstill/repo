package com.ensureback.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ensureback.user.UserRepository;

@Service
public class EnsurebackUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public EnsurebackUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .map(EnsurebackUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
