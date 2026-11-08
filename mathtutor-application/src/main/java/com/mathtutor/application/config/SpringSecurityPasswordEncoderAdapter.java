package com.mathtutor.application.config;

import com.mathtutor.common.port.PasswordEncoderPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adapter que expone PasswordEncoderPort como Spring Security PasswordEncoder.
 * Permite usar Argon2id con DaoAuthenticationProvider de Spring Security.
 */
@Component
public class SpringSecurityPasswordEncoderAdapter implements PasswordEncoder {

    private final PasswordEncoderPort passwordEncoderPort;

    public SpringSecurityPasswordEncoderAdapter(PasswordEncoderPort passwordEncoderPort) {
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return passwordEncoderPort.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return passwordEncoderPort.matches(rawPassword, encodedPassword);
    }
}