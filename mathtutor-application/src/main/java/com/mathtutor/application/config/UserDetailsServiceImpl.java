package com.mathtutor.application.config;

import com.mathtutor.common.auth.port.UserRepositoryPort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Implementación de UserDetailsService que usa el puerto UserRepositoryPort.
 * Adapta la entidad de dominio User a Spring Security UserDetails.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepositoryPort userRepositoryPort;

    public UserDetailsServiceImpl(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepositoryPort.findActiveByEmail(email)
                .map(user -> org.springframework.security.core.userdetails.User
                        .withUsername(user.email())
                        .password(user.passwordHash())
                        .authorities("ROLE_" + user.role().name())
                        .accountExpired(false)
                        .accountLocked(false)
                        .credentialsExpired(false)
                        .disabled(!user.active())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}