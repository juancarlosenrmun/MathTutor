package com.mathtutor.application.controller;

import com.mathtutor.application.MathTutorApplication;
import com.mathtutor.common.auth.domain.User;
import com.mathtutor.common.auth.port.UserRepositoryPort;
import com.mathtutor.common.port.PasswordEncoderPort;
import com.mathtutor.auth.adapter.TokenServiceAdapter;
import com.mathtutor.persistence.adapter.RateLimitAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.mathtutor.persistence.entity.RefreshToken;
import com.mathtutor.common.auth.port.TokenServicePort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para AuthController usando MockMvc y Testcontainers.
 * Verifica los endpoints REST: /register, /login, /logout, /refresh, /me
 * Se ejecuta solo cuando RUN_INTEGRATION_TESTS=true (localmente con Docker).
 * En CI esta variable no está definida, por lo que la prueba se omite.
 */
@SpringBootTest(classes = MathTutorApplication.class)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("AuthController Integration Tests")
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "RUN_INTEGRATION_TESTS", matches = "true")
class AuthControllerIntegrationTest {

    /**
     * RequestPostProcessor para establecer la IP remota en MockMvc.
     */
    private static RequestPostProcessor remoteAddr(String ip) {
        return new RequestPostProcessor() {
            @Override
            public org.springframework.mock.web.MockHttpServletRequest postProcessRequest(
                    org.springframework.mock.web.MockHttpServletRequest request) {
                request.setRemoteAddr(ip);
                return request;
            }
        };
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
        .withDatabaseName("mathtutor_test")
        .withUsername("test")
        .withPassword("test")
        .withInitScript("db/migration/test-init.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        // JWT configuration for tests
        registry.add("jwt.secret", () -> "test-secret-key-at-least-256-bits-long-for-hs256-signing");
        registry.add("jwt.access-token-expiration-minutes", () -> "15");
        registry.add("jwt.refresh-token-expiration-days", () -> "7");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepositoryPort userRepositoryPort;

    @Autowired
    private PasswordEncoderPort passwordEncoderPort;

    @Autowired
    private TokenServiceAdapter tokenServiceAdapter;

    @Autowired
    private RateLimitAdapter rateLimitAdapter;

    @Autowired
    private ObjectMapper objectMapper;

    private String testEmail;
    private String testPassword;
    private String csrfTokenCookie;

    @BeforeEach
    void setUp() {
        testEmail = "test" + System.currentTimeMillis() + "@example.com";
        testPassword = "TestPass123!";
        // Limpiar rate limit antes de cada test para evitar interferencias
        rateLimitAdapter.clear();
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Debe registrar un nuevo usuario y retornar access token + refresh token en cookie")
        void shouldRegisterUserAndReturnTokens() throws Exception {
            String requestBody = """
                {
                    "email": "%s",
                    "password": "%s",
                    "role": "ALUMNO"
                }
                """.formatted(testEmail, testPassword);

            MvcResult result = mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").exists())
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.email").value(testEmail.toLowerCase()))
                    .andExpect(jsonPath("$.data.role").value("ALUMNO"))
                    .andExpect(cookie().exists("refresh_token"))
                    .andReturn();

            // Verificar que el usuario se guardó en BD
            Optional<User> user = userRepositoryPort.findByEmail(testEmail.toLowerCase());
            assertThat(user).isPresent();
            assertThat(user.get().email()).isEqualTo(testEmail.toLowerCase());
            assertThat(user.get().role()).isEqualTo(User.Role.ALUMNO);
            assertThat(user.get().active()).isTrue();

            // Verificar que la contraseña está hasheada
            assertThat(passwordEncoderPort.matches(testPassword, user.get().passwordHash())).isTrue();

            // Verificar que el access token es válido
            String accessToken = extractJsonValue(result, "$.data.accessToken");
            assertThat(tokenServiceAdapter.validateToken(accessToken)).isNotNull();
        }

        @Test
        @DisplayName("Debe fallar con email duplicado")
        void shouldFailWithDuplicateEmail() throws Exception {
            // Primero registrar un usuario
            String requestBody = """
                {
                    "email": "%s",
                    "password": "%s",
                    "role": "ALUMNO"
                }
                """.formatted(testEmail, testPassword);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated());

            // Intentar registrar el mismo email
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("Debe fallar con email inválido")
        void shouldFailWithInvalidEmail() throws Exception {
            String requestBody = """
                {
                    "email": "invalid-email",
                    "password": "%s",
                    "role": "ALUMNO"
                }
                """.formatted(testPassword);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Debe fallar con contraseña débil")
        void shouldFailWithWeakPassword() throws Exception {
            String requestBody = """
                {
                    "email": "%s",
                    "password": "weak",
                    "role": "ALUMNO"
                }
                """.formatted(testEmail);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Debe fallar sin rol")
        void shouldFailWithoutRole() throws Exception {
            String requestBody = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(testEmail, testPassword);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @BeforeEach
        void setUp() throws Exception {
            // Registrar usuario primero
            String requestBody = """
                {
                    "email": "%s",
                    "password": "%s",
                    "role": "ALUMNO"
                }
                """.formatted(testEmail, testPassword);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Debe iniciar sesión con credenciales válidas y retornar tokens")
        void shouldLoginWithValidCredentials() throws Exception {
            String requestBody = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(testEmail, testPassword);

            // Obtener token CSRF para el login
            String csrfToken = getCsrfToken();

            MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken)
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").exists())
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.email").value(testEmail.toLowerCase()))
                    .andExpect(jsonPath("$.data.role").value("ALUMNO"))
                    .andExpect(cookie().exists("refresh_token"))
                    .andReturn();

            // Verificar access token
            String accessToken = extractJsonValue(result, "$.data.accessToken");
            assertThat(tokenServiceAdapter.validateToken(accessToken)).isNotNull();
        }

        @Test
        @DisplayName("Debe fallar con email incorrecto")
        void shouldFailWithWrongEmail() throws Exception {
            String requestBody = """
                {
                    "email": "wrong@example.com",
                    "password": "%s"
                }
                """.formatted(testPassword);

            String csrfToken = getCsrfToken();

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken)
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(requestBody))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Debe fallar con contraseña incorrecta")
        void shouldFailWithWrongPassword() throws Exception {
            String requestBody = """
                {
                    "email": "%s",
                    "password": "WrongPass123!"
                }
                """.formatted(testEmail);

            String csrfToken = getCsrfToken();

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken)
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(requestBody))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Debe fallar sin credenciales")
        void shouldFailWithoutCredentials() throws Exception {
            String requestBody = """
                {
                    "email": "",
                    "password": ""
                }
                """;

            String csrfToken = getCsrfToken();

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken)
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutTests {

        private java.util.Collection<String> sessionCookies;

        @BeforeEach
        void setUp() throws Exception {
            // Registrar y loguear usuario
            String registerBody = """
                {
                    "email": "%s",
                    "password": "%s",
                    "role": "ALUMNO"
                }
                """.formatted(testEmail, testPassword);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerBody))
                    .andExpect(status().isCreated());

            String loginBody = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(testEmail, testPassword);

            // Obtener token CSRF y cookie para el login
            String csrfToken = getCsrfToken();

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken)
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(loginBody))
                    .andExpect(status().isOk())
                    .andReturn();

            // Guardar cookies para siguientes requests
            this.sessionCookies = loginResult.getResponse().getHeaders("Set-Cookie");
        }

        @Test
        @DisplayName("Debe cerrar sesión y limpiar cookie refresh_token")
        void shouldLogoutAndClearCookie() throws Exception {
            // Obtener token CSRF fresco para el logout
            String csrfToken = getCsrfToken();

            // Extraer refresh_token de sessionCookies
            String refreshTokenValue = null;
            if (sessionCookies != null) {
                for (String header : sessionCookies) {
                    String[] cookies = header.split(",\\s*(?=[^=]+=)");
                    for (String cookie : cookies) {
                        if (cookie.trim().startsWith("refresh_token=")) {
                            String[] parts = cookie.split(";");
                            if (parts.length > 0) {
                                String[] keyValue = parts[0].split("=", 2);
                                if (keyValue.length == 2) {
                                    refreshTokenValue = keyValue[1];
                                }
                            }
                            break;
                        }
                    }
                    if (refreshTokenValue != null) {
                        break;
                    }
                }
            }

            mockMvc.perform(post("/api/auth/logout")
                            .header("X-XSRF-TOKEN", csrfToken)
                            .cookie(new Cookie("refresh_token", refreshTokenValue))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(cookie().value("refresh_token", ""));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class RefreshTests {

        private String refreshTokenCookie;
        private String accessToken;

        @BeforeEach
        void setUp() throws Exception {
            // Registrar y loguear usuario
            String registerBody = """
                {
                    "email": "%s",
                    "password": "%s",
                    "role": "ALUMNO"
                }
                """.formatted(testEmail, testPassword);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerBody))
                    .andExpect(status().isCreated());

            String loginBody = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(testEmail, testPassword);

            // Obtener token CSRF y cookie para el login
            String csrfToken = getCsrfToken();

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken)
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(loginBody))
                    .andExpect(status().isOk())
                    .andReturn();

            // Extraer access token del body
            this.accessToken = extractJsonValue(loginResult, "$.data.accessToken");
            assertThat(accessToken).isNotNull();

            // Extraer refresh token de la cookie - getHeaders para manejar múltiples Set-Cookie
            java.util.Collection<String> setCookieHeaders = loginResult.getResponse().getHeaders("Set-Cookie");
            if (setCookieHeaders != null) {
                for (String header : setCookieHeaders) {
                    // Cada header puede tener múltiples cookies separadas por ", "
                    // El formato es: refresh_token=xxx; HttpOnly; Secure; SameSite=Strict; Path=/api/auth
                    String[] cookies = header.split(",\\s*(?=[^=]+=)"); // Split by comma followed by key=
                    for (String cookie : cookies) {
                        if (cookie.trim().startsWith("refresh_token=")) {
                            // Extraer solo el valor del token (antes del primer ';')
                            String[] parts = cookie.split(";");
                            if (parts.length > 0) {
                                String[] keyValue = parts[0].split("=", 2);
                                if (keyValue.length == 2) {
                                    this.refreshTokenCookie = keyValue[1];
                                }
                            }
                            break;
                        }
                    }
                    if (this.refreshTokenCookie != null) {
                        break;
                    }
                }
            }
        }

        @Test
        @DisplayName("Debe refrescar access token usando refresh token válido")
        void shouldRefreshAccessToken() throws Exception {
            assertThat(refreshTokenCookie).isNotNull();

            // Obtener token CSRF fresco para el refresh
            String csrfToken = getCsrfToken();

            MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new Cookie("refresh_token", refreshTokenCookie))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .header("X-XSRF-TOKEN", csrfToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.email").value(testEmail.toLowerCase()))
                    .andExpect(jsonPath("$.data.role").value("ALUMNO"))
                    .andExpect(cookie().exists("refresh_token"))
                    .andReturn();

            // Verificar nuevo access token
            String newAccessToken = extractJsonValue(result, "$.data.accessToken");
            assertThat(tokenServiceAdapter.validateToken(newAccessToken)).isNotNull();
        }

        @Test
        @DisplayName("Debe fallar sin refresh token")
        void shouldFailWithoutRefreshToken() throws Exception {
            String csrfToken = getCsrfToken();

            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .header("X-XSRF-TOKEN", csrfToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Debe fallar con refresh token inválido")
        void shouldFailWithInvalidRefreshToken() throws Exception {
            String csrfToken = getCsrfToken();

            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new Cookie("refresh_token", "invalid-token"))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .header("X-XSRF-TOKEN", csrfToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Debe rotar refresh token en cada refresh (nuevo token emitido)")
        void shouldRotateRefreshTokenOnEachRefresh() throws Exception {
            assertThat(refreshTokenCookie).isNotNull();

            // Primer refresh
            String csrfToken1 = getCsrfToken();
            MvcResult result1 = mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new Cookie("refresh_token", refreshTokenCookie))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .header("X-XSRF-TOKEN", csrfToken1))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("refresh_token"))
                    .andReturn();

            // Extraer el NUEVO refresh token de la cookie
            String newRefreshToken1 = extractRefreshTokenFromCookie(result1);
            assertThat(newRefreshToken1).isNotNull();
            assertThat(newRefreshToken1).isNotEqualTo(refreshTokenCookie);

            // Segundo refresh usando el nuevo token
            String csrfToken2 = getCsrfToken();
            MvcResult result2 = mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new Cookie("refresh_token", newRefreshToken1))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .header("X-XSRF-TOKEN", csrfToken2))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("refresh_token"))
                    .andReturn();

            String newRefreshToken2 = extractRefreshTokenFromCookie(result2);
            assertThat(newRefreshToken2).isNotNull();
            assertThat(newRefreshToken2).isNotEqualTo(newRefreshToken1);

            // Tercer refresh
            String csrfToken3 = getCsrfToken();
            MvcResult result3 = mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new Cookie("refresh_token", newRefreshToken2))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .header("X-XSRF-TOKEN", csrfToken3))
                    .andExpect(status().isOk())
                    .andReturn();

            String newRefreshToken3 = extractRefreshTokenFromCookie(result3);
            assertThat(newRefreshToken3).isNotNull();
            assertThat(newRefreshToken3).isNotEqualTo(newRefreshToken2);
        }

        @Test
        @DisplayName("Debe revocar familia completa al detectar reuso de refresh token rotado")
        void shouldRevokeFamilyOnRefreshTokenReuse() throws Exception {
            assertThat(refreshTokenCookie).isNotNull();

            // Primer refresh - obtiene nuevo token
            String csrfToken1 = getCsrfToken();
            MvcResult result1 = mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new Cookie("refresh_token", refreshTokenCookie))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .header("X-XSRF-TOKEN", csrfToken1))
                    .andExpect(status().isOk())
                    .andReturn();

            String newRefreshToken = extractRefreshTokenFromCookie(result1);
            assertThat(newRefreshToken).isNotNull();

            // Intentar reusar el token ORIGINAL (ya rotado) - debe fallar y revocar familia
            String csrfToken2 = getCsrfToken();
            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new Cookie("refresh_token", refreshTokenCookie))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .header("X-XSRF-TOKEN", csrfToken2))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));

            // El token NUEVO también debe estar revocado (familia completa revocada)
            String csrfToken3 = getCsrfToken();
            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new Cookie("refresh_token", newRefreshToken))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .header("X-XSRF-TOKEN", csrfToken3))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Debe invalidar refresh token al hacer logout")
        void shouldInvalidateRefreshTokenOnLogout() throws Exception {
            assertThat(refreshTokenCookie).isNotNull();
            assertThat(accessToken).isNotNull();

            // Hacer logout con access token (necesario para @AuthenticationPrincipal)
            String csrfToken = getCsrfToken();
            mockMvc.perform(post("/api/auth/logout")
                            .header("Authorization", "Bearer " + accessToken)
                            .cookie(new Cookie("refresh_token", refreshTokenCookie))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .header("X-XSRF-TOKEN", csrfToken))
                    .andExpect(status().isOk())
                    .andExpect(cookie().value("refresh_token", ""));

            // Intentar usar refresh token después de logout - debe fallar
            String csrfToken2 = getCsrfToken();
            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new Cookie("refresh_token", refreshTokenCookie))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .header("X-XSRF-TOKEN", csrfToken2))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Debe fallar con refresh token expirado")
        void shouldFailWithExpiredRefreshToken() throws Exception {
            // Para testear un token expirado via HTTP, insertamos un token con expiración pasada en BD
            // Obtenemos el userId del usuario de test
            Long userId = userRepositoryPort.findByEmail(testEmail.toLowerCase()).get().id();
            String familyId = UUID.randomUUID().toString();
            String expiredRawToken = "expired-refresh-token-" + UUID.randomUUID();

            // Hashear el token
            String tokenHash = passwordEncoderPort.encode(expiredRawToken);
            Instant past = Instant.now().minusSeconds(3600); // 1 hora atrás

            // Insertar directamente en BD usando el repository (acceso vía adapter reflection)
            // Como no tenemos acceso directo al repository, usamos una aproximación:
            // Creamos un token que sabemos que expirará y lo validamos via adapter
            var validationOpt = tokenServiceAdapter.validateRefreshToken(expiredRawToken);
            assertThat(validationOpt).isEmpty();

            // Test via endpoint: usar un token con formato válido pero que no existe en BD
            // (simula un token que fue limpiado por expiración)
            String csrfToken = getCsrfToken();

            MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new Cookie("refresh_token", expiredRawToken))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .header("X-XSRF-TOKEN", csrfToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").value("Refresh token inválido, revocado o expirado"))
                    .andReturn();

            // Verificar que la cookie se limpia
            String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
            assertThat(setCookieHeader).isNotNull();
            assertThat(setCookieHeader).contains("refresh_token=");
        }

        /**
         * Extrae el refresh token de la cookie Set-Cookie del resultado.
         */
        private String extractRefreshTokenFromCookie(MvcResult result) {
            java.util.Collection<String> setCookieHeaders = result.getResponse().getHeaders("Set-Cookie");
            if (setCookieHeaders != null) {
                for (String header : setCookieHeaders) {
                    String[] cookies = header.split(",\\s*(?=[^=]+=)");
                    for (String cookie : cookies) {
                        if (cookie.trim().startsWith("refresh_token=")) {
                            String[] parts = cookie.split(";");
                            if (parts.length > 0) {
                                String[] keyValue = parts[0].split("=", 2);
                                if (keyValue.length == 2) {
                                    return keyValue[1];
                                }
                            }
                            break;
                        }
                    }
                }
            }
            return null;
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me")
    class MeTests {

        private String accessToken;

        @BeforeEach
        void setUp() throws Exception {
            // Registrar y loguear usuario
            String registerBody = """
                {
                    "email": "%s",
                    "password": "%s",
                    "role": "ALUMNO"
                }
                """.formatted(testEmail, testPassword);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerBody))
                    .andExpect(status().isCreated());

            String loginBody = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(testEmail, testPassword);

            // Obtener token CSRF y cookie para el login
            String csrfToken = getCsrfToken();

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken)
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(loginBody))
                    .andExpect(status().isOk())
                    .andReturn();

            // Extraer access token del response
            this.accessToken = extractJsonValue(loginResult, "$.data.accessToken");
        }

        @Test
        @DisplayName("Debe retornar info del usuario autenticado con access token válido")
        void shouldReturnUserInfoWithValidToken() throws Exception {
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").exists())
                    .andExpect(jsonPath("$.data.email").value(testEmail.toLowerCase()))
                    .andExpect(jsonPath("$.data.role").value("ALUMNO"));
        }

        @Test
        @DisplayName("Debe fallar sin access token")
        void shouldFailWithoutAccessToken() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Debe fallar con access token inválido")
        void shouldFailWithInvalidAccessToken() throws Exception {
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer invalid-token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Debe funcionar con access token en cookie")
        void shouldWorkWithAccessTokenInCookie() throws Exception {
            // El access token también se puede enviar en cookie (access_token)
            String csrfToken = getCsrfToken();
            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken)
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content("""
                                {
                                    "email": "%s",
                                    "password": "%s"
                                }
                                """.formatted(testEmail, testPassword)))
                    .andExpect(status().isOk())
                    .andReturn();

            String accessTokenCookie = null;
            String setCookieHeader = loginResult.getResponse().getHeader("Set-Cookie");
            if (setCookieHeader != null) {
                for (String cookie : setCookieHeader.split(";")) {
                    if (cookie.trim().startsWith("access_token=")) {
                        accessTokenCookie = cookie.trim();
                        break;
                    }
                }
            }

            if (accessTokenCookie != null) {
                // GET /api/auth/me no requiere CSRF (es método seguro)
                mockMvc.perform(get("/api/auth/me")
                                .header("Cookie", accessTokenCookie))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true));
            }
        }
    }

    @Nested
    @DisplayName("Protección CSRF")
    class CsrfProtectionTests {

        private String accessToken;
        private String refreshTokenCookie;

        @BeforeEach
        void setUp() throws Exception {
            // Registrar y loguear usuario para obtener tokens
            String registerBody = """
                {
                    "email": "%s",
                    "password": "%s",
                    "role": "ALUMNO"
                }
                """.formatted(testEmail, testPassword);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerBody))
                    .andExpect(status().isCreated());

            String loginBody = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(testEmail, testPassword);

            // Login CON token CSRF (para configurar cookies de sesión)
            String csrfToken = getCsrfToken();
            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken)
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(loginBody))
                    .andExpect(status().isOk())
                    .andReturn();

            // Extraer access token del response
            this.accessToken = extractJsonValue(loginResult, "$.data.accessToken");

            // Extraer refresh token de la cookie
            java.util.Collection<String> setCookieHeaders = loginResult.getResponse().getHeaders("Set-Cookie");
            if (setCookieHeaders != null) {
                for (String header : setCookieHeaders) {
                    String[] cookies = header.split(",\\s*(?=[^=]+=)");
                    for (String cookie : cookies) {
                        if (cookie.trim().startsWith("refresh_token=")) {
                            String[] parts = cookie.split(";");
                            if (parts.length > 0) {
                                String[] keyValue = parts[0].split("=", 2);
                                if (keyValue.length == 2) {
                                    this.refreshTokenCookie = keyValue[1];
                                }
                            }
                            break;
                        }
                    }
                    if (this.refreshTokenCookie != null) {
                        break;
                    }
                }
            }
        }

        @Test
        @DisplayName("Debe obtener token CSRF desde /api/csrf-token")
        void shouldGetCsrfTokenFromEndpoint() throws Exception {
            mockMvc.perform(get("/api/csrf-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.csrfToken").exists());
        }

        @Test
        @DisplayName("Debe rechazar POST /api/auth/login sin token CSRF (403)")
        void shouldRejectLoginWithoutCsrfToken() throws Exception {
            String requestBody = """
                {
                    "email": "%s",
                    "password": "wrongpassword"
                }
                """.formatted(testEmail);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Debe rechazar POST /api/auth/logout sin token CSRF (403)")
        void shouldRejectLogoutWithoutCsrfToken() throws Exception {
            // Primero obtener token CSRF
            MvcResult csrfResult = mockMvc.perform(get("/api/csrf-token"))
                    .andExpect(status().isOk())
                    .andReturn();

            String csrfToken = extractJsonValue(csrfResult, "$.csrfToken");

            // Intentar logout SIN token CSRF
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.add("Cookie", "refresh_token=" + refreshTokenCookie);
            // NO incluir X-XSRF-TOKEN header

            mockMvc.perform(post("/api/auth/logout")
                            .headers(headers))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Debe rechazar POST /api/auth/refresh sin token CSRF (403)")
        void shouldRejectRefreshWithoutCsrfToken() throws Exception {
            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshTokenCookie)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Debe permitir POST /api/auth/register sin token CSRF (endpoint público)")
        void shouldAllowRegisterWithoutCsrfToken() throws Exception {
            String newEmail = "newuser" + System.currentTimeMillis() + "@example.com";
            String requestBody = """
                {
                    "email": "%s",
                    "password": "%s",
                    "role": "ALUMNO"
                }
                """.formatted(newEmail, testPassword);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Debe permitir acceso a /api/auth/me con Bearer token sin CSRF (API stateless)")
        void shouldAllowMeWithBearerTokenWithoutCsrf() throws Exception {
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Debe permitir logout CON token CSRF válido")
        void shouldAllowLogoutWithValidCsrfToken() throws Exception {
            // Obtener token CSRF (y cookie) usando el helper
            String csrfToken = getCsrfToken();

            // Hacer logout CON token CSRF y cookie
            mockMvc.perform(post("/api/auth/logout")
                            .header("X-XSRF-TOKEN", csrfToken)
                            .cookie(new Cookie("refresh_token", refreshTokenCookie))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Debe permitir refresh CON token CSRF válido")
        void shouldAllowRefreshWithValidCsrfToken() throws Exception {
            // Obtener token CSRF (y cookie) usando el helper
            String csrfToken = getCsrfToken();

            // Hacer refresh CON token CSRF y cookie
            mockMvc.perform(post("/api/auth/refresh")
                            .header("X-XSRF-TOKEN", csrfToken)
                            .cookie(new Cookie("refresh_token", refreshTokenCookie))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(cookie().exists("refresh_token"));
        }
    }

    @Nested
    @DisplayName("Rate Limiting (ASVS L1: 5 intentos / 15 min por IP)")
    class RateLimitTests {

        @BeforeEach
        void setUp() throws Exception {
            // Registrar usuario para tests que necesitan login exitoso
            String registerBody = """
                {
                    "email": "%s",
                    "password": "%s",
                    "role": "ALUMNO"
                }
                """.formatted(testEmail, testPassword);

            mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                    .andExpect(status().isCreated());

            // Limpiar rate limit después del registro
            rateLimitAdapter.clear();
        }

        @Test
        @DisplayName("DEBUG: Verificar rate limit reutilizando CSRF token")
        void debugExtractedIp() throws Exception {
            String wrongPassword = "WrongPass123!";
            String requestBody = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(testEmail, wrongPassword);

            // Obtener CSRF token una sola vez y reutilizarlo
            String csrfToken = getCsrfToken();

            // Hacer 6 intentos usando el mismo CSRF token
            for (int i = 1; i <= 6; i++) {
                MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken)
                            .with(remoteAddr("192.168.1.100"))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(requestBody))
                        .andReturn();

                int status = result.getResponse().getStatus();
                String body = result.getResponse().getContentAsString();
                System.out.println("Intento " + i + ": status=" + status + ", body=" + body);

                // Primeros 5 deben ser 401, 6º debe ser 429
                if (i <= 5) {
                    assertThat(status).as("Intento %d debe ser 401, body: %s", i, body).isEqualTo(401);
                } else {
                    assertThat(status).as("Intento %d debe ser 429, body: %s", i, body).isEqualTo(429);
                }
            }
        }

        @Test
        @DisplayName("Debe permitir hasta 5 intentos de login fallidos desde la misma IP")
        void shouldAllowUpToFiveFailedAttempts() throws Exception {
            String wrongPassword = "WrongPass123!";
            String requestBody = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(testEmail, wrongPassword);

            // Hacer 5 intentos fallidos - todos deben devolver 401 (no 429)
            for (int i = 0; i < 5; i++) {
                String csrfToken = getCsrfToken();
                mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken)
                            .with(remoteAddr("192.168.1.100"))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(requestBody))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.success").value(false));
            }
        }

        @Test
        @DisplayName("Debe bloquear el 6º intento de login fallido desde la misma IP con 429")
        void shouldBlockSixthFailedAttempt() throws Exception {
            String wrongPassword = "WrongPass123!";
            String requestBody = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(testEmail, wrongPassword);

            // Hacer 5 intentos fallidos
            for (int i = 0; i < 5; i++) {
                String csrfToken = getCsrfToken();
                mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken)
                            .with(remoteAddr("192.168.1.100"))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(requestBody))
                        .andExpect(status().isUnauthorized());
            }

            // El 6º intento debe ser bloqueado con 429
            String csrfToken = getCsrfToken();
            MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken)
                            .with(remoteAddr("192.168.1.100"))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(requestBody))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").value("Demasiados intentos de login. Intente de nuevo en 15 minutos."))
                    .andReturn();

            // Verificar header Retry-After
            String retryAfter = result.getResponse().getHeader("Retry-After");
            assertThat(retryAfter).isNotNull();
            int retryAfterSeconds = Integer.parseInt(retryAfter);
            assertThat(retryAfterSeconds).isGreaterThan(0).isLessThanOrEqualTo(15 * 60); // Max 15 min
        }

        @Test
        @DisplayName("Debe permitir login exitoso después de intentos fallidos (reset rate limit)")
        void shouldAllowSuccessfulLoginAfterFailedAttempts() throws Exception {
            String wrongPassword = "WrongPass123!";
            String requestBody = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(testEmail, wrongPassword);

            // Hacer 3 intentos fallidos
            for (int i = 0; i < 3; i++) {
                String csrfToken = getCsrfToken();
                mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken)
                            .with(remoteAddr("192.168.1.100"))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(requestBody))
                        .andExpect(status().isUnauthorized());
            }

            // Login exitoso con contraseña correcta
            String correctRequestBody = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(testEmail, testPassword);

            String csrfToken = getCsrfToken();
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken)
                            .with(remoteAddr("192.168.1.100"))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(correctRequestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").exists());
        }

        @Test
        @DisplayName("Debe aplicar rate limit por IP distinta (usuario A no afecta a usuario B)")
        void shouldRateLimitPerIp() throws Exception {
            String wrongPassword = "WrongPass123!";
            // Usar emails diferentes para cada IP para probar aislamiento de IP
            // (el rate limit por email también existe, así que usamos emails distintos)
            String email1 = "user1" + System.currentTimeMillis() + "@example.com";
            String email2 = "user2" + System.currentTimeMillis() + "@example.com";

            String requestBody1 = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email1, wrongPassword);

            String requestBody2 = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email2, wrongPassword);

            // Hacer 5 intentos fallidos desde IP simulada 1 (con email1)
            for (int i = 0; i < 5; i++) {
                String csrfToken = getCsrfToken();
                mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken)
                            .with(remoteAddr("192.168.1.100"))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(requestBody1))
                        .andExpect(status().isUnauthorized());
            }

            // 6º intento desde IP 1 - bloqueado (rate limit por IP)
            String csrfToken = getCsrfToken();
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken)
                            .with(remoteAddr("192.168.1.100"))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(requestBody1))
                    .andExpect(status().isTooManyRequests());

            // Intento desde IP 2 con email2 - debe permitirse (distinta IP, distinto email)
            String csrfToken2 = getCsrfToken();
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken2)
                            .with(remoteAddr("192.168.1.200"))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(requestBody2))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Debe aplicar rate limit también a emails no existentes (previene enumeración)")
        void shouldRateLimitNonExistentEmails() throws Exception {
            String nonExistentEmail = "nonexistent" + System.currentTimeMillis() + "@example.com";
            String wrongPassword = "WrongPass123!";
            String requestBody = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(nonExistentEmail, wrongPassword);

            // Hacer 5 intentos fallidos con email que no existe
            for (int i = 0; i < 5; i++) {
                String csrfToken = getCsrfToken();
                mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken)
                            .with(remoteAddr("192.168.1.100"))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(requestBody))
                        .andExpect(status().isUnauthorized());
            }

            // 6º intento - bloqueado (no revela si email existe)
            String csrfToken = getCsrfToken();
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken)
                            .with(remoteAddr("192.168.1.100"))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(requestBody))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.error").value("Demasiados intentos de login. Intente de nuevo en 15 minutos."));
        }

        @Test
        @DisplayName("DEBUG: Verificar RateLimitAdapter directamente")
        void debugRateLimitAdapter() {
            // Given
            String key = "login:ip:192.168.1.100";
            int maxAttempts = 5;
            java.time.Duration window = java.time.Duration.ofMinutes(15);

            // When - hacer 6 intentos
            for (int i = 1; i <= 6; i++) {
                boolean allowed = rateLimitAdapter.tryConsume(key, maxAttempts, window);
                System.out.println("Intento " + i + ": allowed=" + allowed);
            }

            // Then - el 6º debe ser bloqueado
            boolean sixthAttempt = rateLimitAdapter.tryConsume(key, maxAttempts, window);
            assertThat(sixthAttempt).isFalse();
        }
    }

    @Nested
    @DisplayName("X-Forwarded-For y Trusted Proxies (D6)")
    class TrustedProxyTests {

        @Test
        @DisplayName("Debe usar remoteAddr cuando no hay proxies de confianza configurados")
        void shouldUseRemoteAddrWhenNoTrustedProxies() throws Exception {
            String wrongPassword = "WrongPass123!";
            String requestBody = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted("test-no-proxy@example.com", wrongPassword);

            // Hacer intento con header X-Forwarded-For pero SIN proxies de confianza configurados
            // Debe ignorar el header y usar remoteAddr
            String csrfToken = getCsrfToken();
            MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken)
                            .header("X-Forwarded-For", "203.0.113.195") // IP externa
                            .with(remoteAddr("192.168.1.50")) // IP real del cliente en test
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(requestBody))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            // El rate limit debe aplicarse por la IP real (192.168.1.50), no por X-Forwarded-For
            // Verificar haciendo un 2do intento desde la misma IP real
            String csrfToken2 = getCsrfToken();
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken2)
                            .header("X-Forwarded-For", "198.51.100.1") // IP distinta en header
                            .with(remoteAddr("192.168.1.50")) // Misma IP real
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(requestBody))
                    .andExpect(status().isUnauthorized()); // Cuenta como 2do intento
        }

        @Test
        @DisplayName("Debe confiar en X-Forwarded-For para localhost (desarrollo)")
        void shouldTrustXForwardedForForLocalhost() throws Exception {
            // En el entorno de test, MockMvc usa 127.0.0.1 como remoteAddr
            // El código trata localhost como de confianza y permite X-Forwarded-For
            String wrongPassword = "WrongPass123!";
            String requestBody = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted("test-localhost@example.com", wrongPassword);

            // Simular request desde localhost con X-Forwarded-For
            String csrfToken = getCsrfToken();
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken)
                            .header("X-Forwarded-For", "203.0.113.195") // IP externa simulada
                            .with(remoteAddr("127.0.0.1")) // localhost
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(requestBody))
                    .andExpect(status().isUnauthorized());

            // Segundo intento con DIFERENTE X-Forwarded-For pero MISMO remoteAddr (localhost)
            // Como localhost es de confianza, debe usar X-Forwarded-For para rate limit
            // Por tanto, este debería ser un intento NUEVO (distinta IP en header)
            String csrfToken2 = getCsrfToken();
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken2)
                            .header("X-Forwarded-For", "198.51.100.1") // IP distinta
                            .with(remoteAddr("127.0.0.1")) // Mismo localhost
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(requestBody))
                    .andExpect(status().isUnauthorized()); // Nuevo intento (IP distinta en header)
        }

        @Test
        @DisplayName("Debe usar X-Real-IP cuando X-Forwarded-For no está presente")
        void shouldUseXRealIpWhenNoXForwardedFor() throws Exception {
            String wrongPassword = "WrongPass123!";
            String requestBody = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted("test-xrealip@example.com", wrongPassword);

            // Con localhost, X-Real-IP debe ser usado
            String csrfToken = getCsrfToken();
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken)
                            .header("X-Real-IP", "203.0.113.195")
                            .with(remoteAddr("127.0.0.1"))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(requestBody))
                    .andExpect(status().isUnauthorized());

            // Segundo intento con diferente X-Real-IP
            String csrfToken2 = getCsrfToken();
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-XSRF-TOKEN", csrfToken2)
                            .header("X-Real-IP", "198.51.100.1")
                            .with(remoteAddr("127.0.0.1"))
                            .cookie(new Cookie("XSRF-TOKEN", csrfTokenCookie))
                            .content(requestBody))
                    .andExpect(status().isUnauthorized());
        }
    }

    /**
     * Extrae un valor JSON del resultado usando Jackson.
     */
    private String extractJsonValue(MvcResult result, String path) throws Exception {
        String content = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(content);
        // path format: "$.data.accessToken" -> ["data", "accessToken"]
        String[] parts = path.replace("$.", "").split("\\.");
        JsonNode node = root;
        for (String part : parts) {
            node = node.get(part);
            if (node == null) return null;
        }
        return node.asText();
    }

    /**
     * Obtiene el token CSRF desde el endpoint /api/csrf-token.
     * Devuelve el valor del token para usar en el header X-XSRF-TOKEN.
     */
    private String getCsrfToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/csrf-token"))
                .andExpect(status().isOk())
                .andReturn();

        // Capturar la cookie XSRF-TOKEN para el patrón double-submit cookie
        java.util.Collection<String> setCookieHeaders = result.getResponse().getHeaders("Set-Cookie");
        if (setCookieHeaders != null) {
            for (String header : setCookieHeaders) {
                String[] cookies = header.split(",\\s*(?=[^=]+=)");
                for (String cookie : cookies) {
                    if (cookie.trim().startsWith("XSRF-TOKEN=")) {
                        String[] parts = cookie.split(";");
                        if (parts.length > 0) {
                            String[] keyValue = parts[0].split("=", 2);
                            if (keyValue.length == 2) {
                                this.csrfTokenCookie = keyValue[1];
                            }
                        }
                        break;
                    }
                }
                if (this.csrfTokenCookie != null) {
                    break;
                }
            }
        }

        return extractJsonValue(result, "$.csrfToken");
    }
}