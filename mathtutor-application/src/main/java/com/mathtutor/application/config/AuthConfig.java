package com.mathtutor.application.config;

import com.mathtutor.common.auth.domain.LoginUseCase;
import com.mathtutor.common.auth.domain.LogoutUseCase;
import com.mathtutor.common.auth.domain.RegisterUserUseCase;
import com.mathtutor.common.auth.port.AuditLogPort;
import com.mathtutor.common.auth.port.RateLimitPort;
import com.mathtutor.common.auth.port.UserRepositoryPort;
import com.mathtutor.common.port.PasswordEncoderPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración para cablear los casos de uso de autenticación con sus adaptadores.
 * Inyecta las implementaciones de puertos (adaptadores de persistencia) en los casos de uso de dominio.
 */
@Configuration
public class AuthConfig {

    @Bean
    public RegisterUserUseCase registerUserUseCase(
            UserRepositoryPort userRepositoryPort,
            PasswordEncoderPort passwordEncoderPort,
            AuditLogPort auditLogPort
    ) {
        return new RegisterUserUseCase(userRepositoryPort, passwordEncoderPort, auditLogPort);
    }

    @Bean
    public LoginUseCase loginUseCase(
            UserRepositoryPort userRepositoryPort,
            PasswordEncoderPort passwordEncoderPort,
            AuditLogPort auditLogPort,
            RateLimitPort rateLimitPort
    ) {
        return new LoginUseCase(userRepositoryPort, passwordEncoderPort, auditLogPort, rateLimitPort);
    }

    @Bean
    public LogoutUseCase logoutUseCase(AuditLogPort auditLogPort) {
        return new LogoutUseCase(auditLogPort);
    }
}