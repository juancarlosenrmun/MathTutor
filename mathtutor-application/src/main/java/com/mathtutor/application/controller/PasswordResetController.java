package com.mathtutor.application.controller;

import com.mathtutor.application.security.PasswordResetMail;
import com.mathtutor.application.security.PasswordResetService;
import com.mathtutor.auth.controller.AuthController.ApiResponse;
import com.mathtutor.common.auth.domain.PasswordPolicy;
import com.mathtutor.common.auth.port.RateLimitPort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Locale;

/** @brief Contrato público de solicitud y consumo de enlaces de recuperación (UC-02). */
@RestController
@RequestMapping("/api/auth/password-reset")
public class PasswordResetController {
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private final PasswordResetService resets;
    private final PasswordResetMail mail;
    private final RateLimitPort limits;

    public PasswordResetController(PasswordResetService resets, PasswordResetMail mail, RateLimitPort limits) {
        this.resets = resets;
        this.mail = mail;
        this.limits = limits;
    }

    /** @brief Acepta la solicitud sin exponer el estado de la cuenta. */
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<Void>> request(@Valid @RequestBody Request body, HttpServletRequest http) {
        if (!limits.tryConsume("password-reset:request:" + http.getRemoteAddr(), 10, WINDOW)) return throttled();
        if (!mail.enqueue(body.email().toLowerCase(Locale.ROOT))) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("El envío de enlaces no está disponible. Vuelve a intentarlo más tarde."));
        }
        return ResponseEntity.accepted().body(ApiResponse.success(null));
    }

    /** @brief Verifica la nueva contraseña y consume el enlace si sigue vigente. */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Void>> confirm(@Valid @RequestBody Confirmation body, HttpServletRequest http) {
        if (!limits.tryConsume("password-reset:confirm:" + http.getRemoteAddr(), 20, WINDOW)) return throttled();
        var violation = PasswordPolicy.violation(body.password());
        if (violation.isPresent()) return ResponseEntity.badRequest().body(ApiResponse.error(violation.get()));
        if (!resets.confirm(body.token(), body.password())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("El enlace no es válido o ha caducado."));
        }
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private static ResponseEntity<ApiResponse<Void>> throttled() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "900")
                .body(ApiResponse.error("Demasiados intentos. Vuelve a intentarlo más tarde."));
    }

    public record Request(@NotBlank @Email @Size(max = 255) String email) {}

    public record Confirmation(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
                               @NotBlank @Size(max = 256) String password) {}
}
