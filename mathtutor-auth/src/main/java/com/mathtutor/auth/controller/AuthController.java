package com.mathtutor.auth.controller;

import com.mathtutor.common.auth.domain.LoginUseCase;
import com.mathtutor.common.auth.domain.LogoutUseCase;
import com.mathtutor.common.auth.domain.RegisterUserUseCase;
import com.mathtutor.common.auth.domain.Result;
import com.mathtutor.common.auth.domain.User;
import com.mathtutor.auth.filter.JwtAuthenticationFilter;
import com.mathtutor.auth.adapter.TokenServiceAdapter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @Value("${jwt.refresh-token-expiration-days:7}")
    private long refreshTokenExpirationDays = 7;

    /**
     * Lista de CIDR de proxies de confianza.
     * Si está vacía o no configurada, NO se confía en X-Forwarded-For.
     * Configurar via: app.trusted-proxies=10.0.0.0/8,192.168.0.0/16
     */
    @Value("${app.trusted-proxies:}")
    private String trustedProxiesConfig;

    /**
     * Lista parseada de rangos CIDR de confianza.
     * Vacía si no se configuró app.trusted-proxies.
     */
    private List<String> trustedProxyCidrs = List.of();

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
     * Parsea la configuración de proxies de confianza.
     * Formato: "10.0.0.0/8, 192.168.0.0/16" (comma-separated CIDR)
     *
     * @param config string de configuración (puede ser null o vacío)
     * @return lista de CIDR válidos, vacía si no hay configuración
     */
    @jakarta.annotation.PostConstruct
    public void initializeTrustedProxies() {
        trustedProxyCidrs = parseTrustedProxies(trustedProxiesConfig);
        for (String cidr : trustedProxyCidrs) {
            new org.springframework.security.web.util.matcher.IpAddressMatcher(cidr);
        }
    }

    private List<String> parseTrustedProxies(String config) {
        if (config == null || config.trim().isEmpty()) {
            return List.of();
        }
        return List.of(config.split("\\s*,\\s*"))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
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
        }
        // Revocar incluso cuando el access token haya expirado.
        if (StringUtils.hasText(refreshToken)) {
            tokenServiceAdapter.revokeRefreshSession(refreshToken);
        }

        // Limpiar cookie de refresh token
        clearRefreshTokenCookie(response);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** @brief Descarta la sesión inicial si la cuenta se cerró durante la autenticación. */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> unavailableAccount() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Credenciales inválidas o cuenta no disponible"));
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

        var rotation = tokenServiceAdapter.rotateRefreshToken(refreshToken);
        if (rotation.isEmpty()) {
            clearRefreshTokenCookie(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Refresh token inválido, revocado o expirado"));
        }
        var tokens = rotation.get();
        addRefreshTokenCookie(response, tokens.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(
                new RefreshResponse(tokens.accessToken(), tokens.email(), tokens.role())));
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
        cookie.setMaxAge(Math.toIntExact(refreshTokenExpirationDays * 86400));
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

    /** Solo utiliza cabeceras reenviadas cuando el emisor inmediato es un proxy configurado. */
    private String extractClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (!isTrustedProxy(remote)) {
            return remote;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            String[] chain = forwarded.split(",", -1);
            String current = remote;
            for (int index = chain.length - 1; index >= 0 && isTrustedProxy(current); index--) {
                String candidate = chain[index].trim();
                if (!isIpLiteral(candidate)) {
                    return remote;
                }
                current = candidate;
            }
            return current;
        }
        String real = request.getHeader("X-Real-IP");
        return StringUtils.hasText(real) && isIpLiteral(real.trim()) ? real.trim() : remote;
    }

    private boolean isTrustedProxy(String ip) {
        if (!isIpLiteral(ip)) {
            return false;
        }
        return trustedProxyCidrs.stream().anyMatch(cidr ->
                new org.springframework.security.web.util.matcher.IpAddressMatcher(cidr).matches(ip));
    }

    private boolean isIpLiteral(String ip) {
        if (ip == null || ip.isBlank() || !ip.matches("[0-9a-fA-F:.]+")
                || (!ip.contains(":") && !ip.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+"))) {
            return false;
        }
        try {
            java.net.InetAddress.getByName(ip);
            return true;
        } catch (java.net.UnknownHostException ignored) {
            return false;
        }
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
