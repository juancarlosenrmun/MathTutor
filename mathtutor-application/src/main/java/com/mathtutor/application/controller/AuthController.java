package com.mathtutor.application.controller;

import com.mathtutor.common.auth.domain.LoginUseCase;
import com.mathtutor.common.auth.domain.LogoutUseCase;
import com.mathtutor.common.auth.domain.RegisterUserUseCase;
import com.mathtutor.common.auth.domain.Result;
import com.mathtutor.common.auth.domain.User;
import com.mathtutor.application.config.JwtAuthenticationFilter;
import com.mathtutor.common.auth.port.TokenServicePort;
import com.mathtutor.persistence.adapter.TokenServiceAdapter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST para autenticación.
 * Endpoints: register, login, logout, refresh, me.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final TokenServiceAdapter tokenServiceAdapter;

    public AuthController(
            RegisterUserUseCase registerUserUseCase,
            LoginUseCase loginUseCase,
            LogoutUseCase logoutUseCase,
            TokenServiceAdapter tokenServiceAdapter
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUseCase = loginUseCase;
        this.logoutUseCase = logoutUseCase;
        this.tokenServiceAdapter = tokenServiceAdapter;
    }

    /**
     * Registro de nuevo usuario.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response
    ) {
        Result<Long> result = registerUserUseCase.execute(
                request.email(),
                request.password(),
                request.role()
        );

        if (result.isFailure()) {
            Result.Failure<Long> failure = (Result.Failure<Long>) result;
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(failure.message()));
        }

        Long userId = result.getOrThrow();
        String accessToken = tokenServiceAdapter.generateAccessToken(userId, request.email().toLowerCase(), request.role().name());
        String refreshTokenId = UUID.randomUUID().toString();
        String refreshToken = tokenServiceAdapter.generateRefreshToken(userId, request.email().toLowerCase(), request.role().name(), refreshTokenId);

        // Guardar refresh token en cookie HttpOnly, Secure, SameSite=Strict
        addRefreshTokenCookie(response, refreshToken);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(new RegisterResponse(userId, accessToken, request.email().toLowerCase(), request.role())));
    }

    /**
     * Inicio de sesión.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        // Obtener IP del cliente para auditoría
        String ipAddress = request.ipAddress() != null ? request.ipAddress() : "unknown";

        Result<LoginUseCase.LoginResult> result = loginUseCase.execute(
                request.email(),
                request.password(),
                ipAddress
        );

        if (result.isFailure()) {
            Result.Failure<LoginUseCase.LoginResult> failure = (Result.Failure<LoginUseCase.LoginResult>) result;
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(failure.message()));
        }

        LoginUseCase.LoginResult loginResult = result.getOrThrow();
        String accessToken = tokenServiceAdapter.generateAccessToken(
                loginResult.userId(), request.email().toLowerCase(), loginResult.role().name()
        );
        String refreshTokenId = UUID.randomUUID().toString();
        String refreshToken = tokenServiceAdapter.generateRefreshToken(loginResult.userId(), request.email().toLowerCase(), loginResult.role().name(), refreshTokenId);

        addRefreshTokenCookie(response, refreshToken);

        return ResponseEntity.ok(ApiResponse.success(
                new LoginResponse(loginResult.userId(), accessToken, request.email().toLowerCase(), loginResult.role())
        ));
    }

    /**
     * Cierre de sesión.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal JwtAuthenticationFilter.AuthenticatedUser currentUser,
            HttpServletResponse response
    ) {
        if (currentUser != null) {
            logoutUseCase.execute(currentUser.id(), currentUser.email());
            // Revocar refresh token si existe en la request
            // Nota: en implementación real, extraeríamos el refresh token de la cookie
        }

        // Limpiar cookie de refresh token
        clearRefreshTokenCookie(response);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Refrescar access token usando refresh token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (!StringUtils.hasText(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Refresh token no proporcionado"));
        }

        try {
            Map<String, Object> claims = tokenServiceAdapter.validateToken(refreshToken);
            Long userId = (Long) claims.get("userId");
            String email = (String) claims.get("email");
            String role = (String) claims.get("role");
            String oldTokenId = (String) claims.get("tokenId");

            if (userId == null || !StringUtils.hasText(email) || !StringUtils.hasText(role)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Token de refresco inválido"));
            }

            // Revocar el token de refresco anterior (rotación)
            if (StringUtils.hasText(oldTokenId)) {
                tokenServiceAdapter.revokeRefreshToken(oldTokenId);
            }

            // Generar nuevos tokens
            String newAccessToken = tokenServiceAdapter.generateAccessToken(userId, email, role);
            String newRefreshTokenId = UUID.randomUUID().toString();
            String newRefreshToken = tokenServiceAdapter.generateRefreshToken(userId, email, role, newRefreshTokenId);

            addRefreshTokenCookie(response, newRefreshToken);

            return ResponseEntity.ok(ApiResponse.success(
                    new RefreshResponse(newAccessToken, email, role)
            ));

        } catch (TokenServicePort.InvalidTokenException e) {
            clearRefreshTokenCookie(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Refresh token inválido o expirado: " + e.getMessage()));
        }
    }

    /**
     * Obtener información del usuario autenticado.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> me(
            @AuthenticationPrincipal JwtAuthenticationFilter.AuthenticatedUser currentUser
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("No autenticado"));
        }

        return ResponseEntity.ok(ApiResponse.success(
                new MeResponse(currentUser.id(), currentUser.email(), currentUser.role())
        ));
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // Requiere HTTPS en producción
        cookie.setPath("/api/auth");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 días
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0); // Expirar inmediatamente
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    // DTOs de request/response

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String password,
            @NotNull User.Role role
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            String ipAddress
    ) {}

    public record RegisterResponse(Long userId, String accessToken, String email, User.Role role) {}

    public record LoginResponse(Long userId, String accessToken, String email, User.Role role) {}

    public record RefreshResponse(String accessToken, String email, String role) {}

    public record MeResponse(Long userId, String email, String role) {}

    // Respuesta API estándar
    public record ApiResponse<T>(boolean success, T data, String error) {
        public static <T> ApiResponse<T> success(T data) {
            return new ApiResponse<>(true, data, null);
        }

        public static <T> ApiResponse<T> error(String error) {
            return new ApiResponse<>(false, null, error);
        }
    }
}