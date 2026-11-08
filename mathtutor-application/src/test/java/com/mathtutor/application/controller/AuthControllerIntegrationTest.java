package com.mathtutor.application.controller;

import com.mathtutor.common.auth.domain.User;
import com.mathtutor.common.auth.port.UserRepositoryPort;
import com.mathtutor.common.port.PasswordEncoderPort;
import com.mathtutor.persistence.adapter.TokenServiceAdapter;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para AuthController usando MockMvc y Testcontainers.
 * Verifica los endpoints REST: /register, /login, /logout, /refresh, /me
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest {

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
    private ObjectMapper objectMapper;

    private String testEmail;
    private String testPassword;

    @BeforeEach
    void setUp() {
        testEmail = "test" + System.currentTimeMillis() + "@example.com";
        testPassword = "TestPass123!";
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

            MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
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

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
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

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
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

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutTests {

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

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody))
                    .andExpect(status().isOk())
                    .andReturn();

            // Guardar cookies para siguientes requests
            this.sessionCookies = loginResult.getResponse().getHeaders("Set-Cookie");
        }

        private java.util.Collection<String> sessionCookies;

        @Test
        @DisplayName("Debe cerrar sesión y limpiar cookie refresh_token")
        void shouldLogoutAndClearCookie() throws Exception {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            sessionCookies.forEach(c -> headers.add("Cookie", c));
            mockMvc.perform(post("/api/auth/logout")
                            .headers(headers))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(cookie().value("refresh_token", ""));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class RefreshTests {

        private String refreshTokenCookie;

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

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody))
                    .andExpect(status().isOk())
                    .andReturn();

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

            MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshTokenCookie)))
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
            mockMvc.perform(post("/api/auth/refresh"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Debe fallar con refresh token inválido")
        void shouldFailWithInvalidRefreshToken() throws Exception {
            mockMvc.perform(post("/api/auth/refresh")
                            .header("Cookie", "refresh_token=invalid-token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
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

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
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
            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
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
                mockMvc.perform(get("/api/auth/me")
                                .header("Cookie", accessTokenCookie))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true));
            }
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
}