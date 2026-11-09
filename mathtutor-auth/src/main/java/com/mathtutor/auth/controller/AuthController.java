package com.mathtutor.auth.controller;

import com.mathtutor.common.auth.domain.LoginUseCase;
import com.mathtutor.common.auth.domain.LogoutUseCase;
import com.mathtutor.common.auth.domain.RegisterUserUseCase;
import com.mathtutor.common.auth.domain.Result;
import com.mathtutor.common.auth.domain.User;
import com.mathtutor.auth.filter.JwtAuthenticationFilter;
import com.mathtutor.common.auth.port.TokenServicePort;
import com.mathtutor.auth.adapter.TokenServiceAdapter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Controlador REST para autenticación.
 * Endpoints: register, login, logout, refresh, me.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

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
        String email = request.email().toLowerCase();
        String role = request.role().name();
        String accessToken = tokenServiceAdapter.generateAccessToken(userId, email, role);

        // Almacenar refresh token en BD (genera JWT + guarda hash con familyId)
        String refreshToken = tokenServiceAdapter.storeRefreshToken(userId, email, role, UUID.randomUUID().toString());

        addRefreshTokenCookie(response, refreshToken);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(new RegisterResponse(userId, accessToken, email, request.role())));
    }

    /**
     * Inicio de sesión.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        // Obtener IP del cliente para auditoría y rate limiting
        String ipAddress = extractClientIp(httpRequest);

        Result<LoginUseCase.LoginResult> result = loginUseCase.execute(
                request.email(),
                request.password(),
                ipAddress
        );

        if (result.isFailure()) {
            Result.Failure<LoginUseCase.LoginResult> failure = (Result.Failure<LoginUseCase.LoginResult>) result;
            // Devolver 429 para rate limit, 401 para otros errores de autenticación
            HttpStatus status = failure.code() == Result.ErrorCode.TOO_MANY_REQUESTS
                    ? HttpStatus.TOO_MANY_REQUESTS
                    : HttpStatus.UNAUTHORIZED;
            // Agregar header Retry-After para rate limit (15 minutos = 900 segundos)
            ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(status);
            if (failure.code() == Result.ErrorCode.TOO_MANY_REQUESTS) {
                responseBuilder.header("Retry-After", "900");
            }
            return responseBuilder.body(ApiResponse.error(failure.message()));
        }

        LoginUseCase.LoginResult loginResult = result.getOrThrow();
        String email = request.email().toLowerCase();
        String role = loginResult.role().name();
        String accessToken = tokenServiceAdapter.generateAccessToken(loginResult.userId(), email, role);

        // Almacenar refresh token en BD (genera JWT + guarda hash con familyId)
        String refreshToken = tokenServiceAdapter.storeRefreshToken(loginResult.userId(), email, role, UUID.randomUUID().toString());

        addRefreshTokenCookie(response, refreshToken);

        return ResponseEntity.ok(ApiResponse.success(
                new LoginResponse(loginResult.userId(), accessToken, email, loginResult.role())
        ));
    }

    /**
     * Cierre de sesión.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal JwtAuthenticationFilter.AuthenticatedUser currentUser,
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (currentUser != null) {
            logoutUseCase.execute(currentUser.id(), currentUser.email());
            // Revocar refresh token en BD si se proporciona
            if (StringUtils.hasText(refreshToken)) {
                try {
                    tokenServiceAdapter.validateRefreshToken(refreshToken).ifPresent(tokenValidationResult -> {
                        tokenServiceAdapter.revokeRefreshToken(tokenValidationResult.tokenId());
                    });
                } catch (Exception ignored) {
                    // Token ya inválido o expirado, no hacer nada
                }
            }
        }

        // Limpiar cookie de refresh token
        clearRefreshTokenCookie(response);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Refrescar access token usando refresh token (con rotación y detección de reuso).
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

        // Validar refresh token contra BD
        Optional<TokenServicePort.TokenValidationResult> validationOpt =
                tokenServiceAdapter.validateRefreshToken(refreshToken);

        if (validationOpt.isEmpty()) {
            clearRefreshTokenCookie(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Refresh token inválido, revocado o expirado"));
        }

        TokenServicePort.TokenValidationResult validation = validationOpt.get();
        String familyId = validation.familyId();
        String tokenId = validation.tokenId();

        // Generar nuevos tokens con el MISMO familyId (rotación)
        String newAccessToken = tokenServiceAdapter.generateAccessToken(
                validation.userId(), validation.email(), validation.role());

        // IMPORTANTE: Para rotación, usamos el MISMO familyId
        // Generamos un nuevo tokenId para el nuevo token
        String newTokenId = UUID.randomUUID().toString();
        String newRefreshToken = tokenServiceAdapter.generateRefreshToken(
                validation.userId(), validation.email(), validation.role(), newTokenId);

        // Almacenar nuevo refresh token en BD con el MISMO familyId
        String storedRefreshToken = tokenServiceAdapter.storeRefreshTokenWithFamily(
                validation.userId(), validation.email(), validation.role(),
                newRefreshToken, familyId);

        // Revocar el token anterior (rotación normal)
        tokenServiceAdapter.revokeRefreshToken(tokenId);

        addRefreshTokenCookie(response, storedRefreshToken);

        return ResponseEntity.ok(ApiResponse.success(
                new RefreshResponse(newAccessToken, validation.email(), validation.role())
        ));
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

    /**
     * Extrae la IP real del cliente considerando proxies (X-Forwarded-For).
     */
    private String extractClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        log.info("extractClientIp: X-Forwarded-For header = '{}'", xForwardedFor);
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For puede tener múltiples IPs: "client, proxy1, proxy2"
            // La primera es la del cliente original
            String[] ips = xForwardedFor.split(",");
            String ip = ips[0].trim();
            log.info("extractClientIp: extracted IP from X-Forwarded-For = '{}'", ip);
            return ip;
        }
        String xRealIp = request.getHeader("X-Real-IP");
        log.info("extractClientIp: X-Real-IP header = '{}'", xRealIp);
        if (xRealIp != null && !xRealIp.isEmpty()) {
            String ip = xRealIp.trim();
            log.info("extractClientIp: extracted IP from X-Real-IP = '{}'", ip);
            return ip;
        }
        String remoteAddr = request.getRemoteAddr();
        log.info("extractClientIp: using remoteAddr = '{}'", remoteAddr);
        return remoteAddr;
    }

    // DTOs de request/response

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 12) String password,
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