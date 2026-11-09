package com.mathtutor.auth.controller;

import com.mathtutor.common.auth.domain.LoginUseCase;
import com.mathtutor.common.auth.domain.LogoutUseCase;
import com.mathtutor.common.auth.domain.RegisterUserUseCase;
import com.mathtutor.common.auth.domain.Result;
import com.mathtutor.common.auth.domain.User;
import com.mathtutor.auth.adapter.TokenServiceAdapter;
import com.mathtutor.auth.filter.JwtAuthenticationFilter;
import com.mathtutor.common.auth.port.TokenServicePort;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests para AuthController usando MockMvc.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Mock
    private RegisterUserUseCase registerUserUseCase;

    @Mock
    private LoginUseCase loginUseCase;

    @Mock
    private LogoutUseCase logoutUseCase;

    @Mock
    private TokenServiceAdapter tokenServiceAdapter;

    private AuthController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new AuthController(registerUserUseCase, loginUseCase, logoutUseCase, tokenServiceAdapter);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /**
     * Helper para establecer la IP remota en la request de MockMvc.
     * Simula request.getRemoteAddr() en tests standalone.
     */
    private RequestPostProcessor remoteAddr(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("Debe registrar usuario y retornar 201 con tokens")
        void shouldRegisterUserAndReturn201() throws Exception {
            // Given
            when(registerUserUseCase.execute(anyString(), anyString(), any(User.Role.class)))
                    .thenReturn(Result.ok(1L));
            when(tokenServiceAdapter.generateAccessToken(eq(1L), eq("test@example.com"), eq("ALUMNO")))
                    .thenReturn("access-token-jwt");
            when(tokenServiceAdapter.storeRefreshToken(eq(1L), eq("test@example.com"), eq("ALUMNO"), anyString()))
                    .thenReturn("refresh-token-jwt");

            // When / Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@example.com",
                                        "password": "password1234",
                                        "role": "ALUMNO"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(1))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token-jwt"))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.role").value("ALUMNO"))
                    .andExpect(cookie().exists("refresh_token"))
                    .andExpect(cookie().httpOnly("refresh_token", true))
                    .andExpect(cookie().secure("refresh_token", true))
                    .andExpect(cookie().path("refresh_token", "/api/auth"));

            verify(registerUserUseCase).execute("test@example.com", "password1234", User.Role.ALUMNO);
            verify(tokenServiceAdapter).generateAccessToken(1L, "test@example.com", "ALUMNO");
            verify(tokenServiceAdapter).storeRefreshToken(eq(1L), eq("test@example.com"), eq("ALUMNO"), anyString());
        }

        @Test
        @DisplayName("Debe retornar 400 con error genérico cuando email ya existe (no enumeración)")
        void shouldReturn400WithGenericErrorWhenEmailExists() throws Exception {
            // Given - mock returns generic error (ASVS L1: no user enumeration)
            @SuppressWarnings("unchecked")
            Result<Long> failResult = Result.fail(Result.ErrorCode.VALIDATION_ERROR, "Credenciales inválidas");
            when(registerUserUseCase.execute(anyString(), anyString(), any(User.Role.class)))
                    .thenReturn(failResult);

            // When / Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "existing@test.com",
                                        "password": "password1234",
                                        "role": "ALUMNO"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").value("Credenciales inválidas"));

            verify(registerUserUseCase).execute("existing@test.com", "password1234", User.Role.ALUMNO);
            verify(tokenServiceAdapter, never()).generateAccessToken(anyLong(), anyString(), anyString());
            verify(tokenServiceAdapter, never()).storeRefreshToken(anyLong(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Debe retornar 400 para email inválido (validación)")
        void shouldReturn400ForInvalidEmail() throws Exception {
            // When / Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "invalid-email",
                                        "password": "password1234",
                                        "role": "ALUMNO"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 para contraseña muy corta (validación)")
        void shouldReturn400ForShortPassword() throws Exception {
            // When / Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@example.com",
                                        "password": "short",
                                        "role": "ALUMNO"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 para role nulo (validación)")
        void shouldReturn400ForNullRole() throws Exception {
            // When / Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@example.com",
                                        "password": "password1234",
                                        "role": null
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe registrar profesor y retornar role PROFESOR")
        void shouldRegisterProfessor() throws Exception {
            // Given
            when(registerUserUseCase.execute(anyString(), anyString(), eq(User.Role.PROFESOR)))
                    .thenReturn(Result.ok(2L));
            when(tokenServiceAdapter.generateAccessToken(eq(2L), eq("prof@test.com"), eq("PROFESOR")))
                    .thenReturn("prof-access-token");
            when(tokenServiceAdapter.storeRefreshToken(eq(2L), eq("prof@test.com"), eq("PROFESOR"), anyString()))
                    .thenReturn("prof-refresh-token");

            // When / Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "prof@test.com",
                                        "password": "password1234",
                                        "role": "PROFESOR"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.role").value("PROFESOR"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("Debe hacer login y retornar 200 con tokens")
        void shouldLoginAndReturn200() throws Exception {
            // Given
            LoginUseCase.LoginResult loginResult = new LoginUseCase.LoginResult(1L, User.Role.ALUMNO);
            when(loginUseCase.execute(eq("test@example.com"), eq("password1234"), eq("192.168.1.1")))
                    .thenReturn(Result.ok(loginResult));
            when(tokenServiceAdapter.generateAccessToken(eq(1L), eq("test@example.com"), eq("ALUMNO")))
                    .thenReturn("login-access-token");
            when(tokenServiceAdapter.storeRefreshToken(eq(1L), eq("test@example.com"), eq("ALUMNO"), anyString()))
                    .thenReturn("login-refresh-token");

            // When / Then - use remoteAddr helper to simulate client IP
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@example.com",
                                        "password": "password1234"
                                    }
                                    """)
                            .with(remoteAddr("192.168.1.1")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(1))
                    .andExpect(jsonPath("$.data.accessToken").value("login-access-token"))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.role").value("ALUMNO"))
                    .andExpect(cookie().exists("refresh_token"))
                    .andExpect(cookie().httpOnly("refresh_token", true))
                    .andExpect(cookie().secure("refresh_token", true))
                    .andExpect(cookie().path("refresh_token", "/api/auth"));

            verify(loginUseCase).execute("test@example.com", "password1234", "192.168.1.1");
        }

        @Test
        @DisplayName("Debe usar IP unknown cuando no se proporciona")
        void shouldUseUnknownIpWhenNotProvided() throws Exception {
            // Given
            LoginUseCase.LoginResult loginResult = new LoginUseCase.LoginResult(1L, User.Role.ALUMNO);
            when(loginUseCase.execute(eq("test@example.com"), eq("password1234"), eq("unknown")))
                    .thenReturn(Result.ok(loginResult));
            when(tokenServiceAdapter.generateAccessToken(anyLong(), anyString(), anyString()))
                    .thenReturn("access-token");
            when(tokenServiceAdapter.storeRefreshToken(anyLong(), anyString(), anyString(), anyString()))
                    .thenReturn("refresh-token");

            // When / Then - use remoteAddr helper with "unknown"
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@example.com",
                                        "password": "password1234"
                                    }
                                    """)
                            .with(remoteAddr("unknown")))
                    .andExpect(status().isOk());

            verify(loginUseCase).execute("test@example.com", "password1234", "unknown");
        }

        @Test
        @DisplayName("Debe retornar 401 para credenciales inválidas")
        void shouldReturn401ForInvalidCredentials() throws Exception {
            // Given
            @SuppressWarnings("unchecked")
            Result<LoginUseCase.LoginResult> failResult = Result.fail(Result.ErrorCode.INVALID_PASSWORD, "Credenciales inválidas");
            when(loginUseCase.execute(anyString(), anyString(), anyString()))
                    .thenReturn(failResult);

            // When / Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@example.com",
                                        "password": "wrongpassword",
                                        "ipAddress": "192.168.1.1"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").value("Credenciales inválidas"));

            verify(tokenServiceAdapter, never()).generateAccessToken(anyLong(), anyString(), anyString());
            verify(tokenServiceAdapter, never()).storeRefreshToken(anyLong(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Debe retornar 400 para email inválido (validación)")
        void shouldReturn400ForInvalidEmail() throws Exception {
            // When / Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "invalid-email",
                                        "password": "password1234"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 para contraseña vacía (validación)")
        void shouldReturn400ForEmptyPassword() throws Exception {
            // When / Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@example.com",
                                        "password": ""
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class Logout {

        @Test
        @DisplayName("Debe hacer logout y limpiar cookie cuando usuario autenticado")
        void shouldLogoutAndClearCookieWhenAuthenticated() throws Exception {
            // Given
            var authUser = new JwtAuthenticationFilter.AuthenticatedUser(1L, "test@example.com", "ALUMNO");

            // When / Then - use principal() to set @AuthenticationPrincipal
            mockMvc.perform(post("/api/auth/logout")
                            .principal(authUser))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andExpect(cookie().exists("refresh_token"))
                    .andExpect(cookie().maxAge("refresh_token", 0));

            verify(logoutUseCase).execute(1L, "test@example.com");
        }

        @Test
        @DisplayName("Debe limpiar cookie incluso sin usuario autenticado")
        void shouldClearCookieEvenWithoutAuthenticatedUser() throws Exception {
            // When / Then - no principal
            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(cookie().maxAge("refresh_token", 0));

            verify(logoutUseCase, never()).execute(anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("Debe refrescar tokens y retornar nuevos tokens con rotación")
        void shouldRefreshTokensAndRotate() throws Exception {
            // Given
            String oldRefreshToken = "old-refresh-token";
            String oldTokenId = UUID.randomUUID().toString();
            String familyId = "family-123";

            when(tokenServiceAdapter.validateRefreshToken(oldRefreshToken))
                    .thenReturn(Optional.of(new TokenServicePort.TokenValidationResult(1L, "test@example.com", "ALUMNO", familyId, oldTokenId)));
            when(tokenServiceAdapter.generateAccessToken(1L, "test@example.com", "ALUMNO"))
                    .thenReturn("new-access-token");
            when(tokenServiceAdapter.generateRefreshToken(eq(1L), eq("test@example.com"), eq("ALUMNO"), anyString()))
                    .thenReturn("new-refresh-token-jwt");
            when(tokenServiceAdapter.storeRefreshTokenWithFamily(eq(1L), eq("test@example.com"), eq("ALUMNO"), anyString(), eq(familyId)))
                    .thenReturn("new-stored-refresh-token");

            // When / Then
            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new Cookie("refresh_token", oldRefreshToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.role").value("ALUMNO"))
                    .andExpect(cookie().exists("refresh_token"))
                    .andExpect(cookie().httpOnly("refresh_token", true))
                    .andExpect(cookie().secure("refresh_token", true));

            verify(tokenServiceAdapter).validateRefreshToken(oldRefreshToken);
            verify(tokenServiceAdapter).revokeRefreshToken(oldTokenId);
            verify(tokenServiceAdapter).generateAccessToken(1L, "test@example.com", "ALUMNO");
            verify(tokenServiceAdapter).generateRefreshToken(eq(1L), eq("test@example.com"), eq("ALUMNO"), anyString());
            verify(tokenServiceAdapter).storeRefreshTokenWithFamily(eq(1L), eq("test@example.com"), eq("ALUMNO"), anyString(), eq(familyId));
        }

        @Test
        @DisplayName("Debe retornar 401 cuando no hay refresh token en cookie")
        void shouldReturn401WhenNoRefreshToken() throws Exception {
            // When / Then
            mockMvc.perform(post("/api/auth/refresh"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").value("Refresh token no proporcionado"));
        }

        @Test
        @DisplayName("Debe retornar 401 cuando refresh token es inválido (no encontrado en BD)")
        void shouldReturn401WhenRefreshTokenInvalid() throws Exception {
            // Given
            when(tokenServiceAdapter.validateRefreshToken("invalid-token"))
                    .thenReturn(Optional.empty());

            // When / Then
            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new Cookie("refresh_token", "invalid-token")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").value("Refresh token inválido, revocado o expirado"))
                    .andExpect(cookie().maxAge("refresh_token", 0));

            verify(tokenServiceAdapter).validateRefreshToken("invalid-token");
        }

        @Test
        @DisplayName("Debe rotar token incluso cuando oldTokenId es null")
        void shouldRotateTokenEvenWhenOldTokenIdNull() throws Exception {
            // Given
            String familyId = "family-123";
            when(tokenServiceAdapter.validateRefreshToken("token-without-id"))
                    .thenReturn(Optional.of(new TokenServicePort.TokenValidationResult(1L, "test@example.com", "ALUMNO", familyId, null)));
            when(tokenServiceAdapter.generateAccessToken(1L, "test@example.com", "ALUMNO"))
                    .thenReturn("new-access");
            when(tokenServiceAdapter.generateRefreshToken(eq(1L), eq("test@example.com"), eq("ALUMNO"), anyString()))
                    .thenReturn("new-refresh");
            when(tokenServiceAdapter.storeRefreshTokenWithFamily(eq(1L), eq("test@example.com"), eq("ALUMNO"), anyString(), eq(familyId)))
                    .thenReturn("new-stored-refresh");

            // When / Then
            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new Cookie("refresh_token", "token-without-id")))
                    .andExpect(status().isOk());

            verify(tokenServiceAdapter, never()).revokeRefreshToken(anyString());
            verify(tokenServiceAdapter).storeRefreshTokenWithFamily(eq(1L), eq("test@example.com"), eq("ALUMNO"), anyString(), eq(familyId));
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me")
    class Me {

        @Test
        @DisplayName("Debe retornar info del usuario autenticado")
        void shouldReturnAuthenticatedUserInfo() throws Exception {
            // Given
            var authUser = new JwtAuthenticationFilter.AuthenticatedUser(42L, "me@test.com", "PROFESOR");

            // When / Then
            mockMvc.perform(get("/api/auth/me")
                            .principal(authUser))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(42))
                    .andExpect(jsonPath("$.data.email").value("me@test.com"))
                    .andExpect(jsonPath("$.data.role").value("PROFESOR"));
        }

        @Test
        @DisplayName("Debe retornar 401 cuando no hay usuario autenticado")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // When / Then - no principal
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").value("No autenticado"));
        }
    }

    @Nested
    @DisplayName("Validación de cookies")
    class CookieValidation {

        @Test
        @DisplayName("Cookie refresh_token debe tener SameSite=Strict")
        void shouldHaveSameSiteStrict() throws Exception {
            // Given
            when(registerUserUseCase.execute(anyString(), anyString(), any(User.Role.class)))
                    .thenReturn(Result.ok(1L));
            when(tokenServiceAdapter.generateAccessToken(anyLong(), anyString(), anyString()))
                    .thenReturn("access");
            when(tokenServiceAdapter.storeRefreshToken(anyLong(), anyString(), anyString(), anyString()))
                    .thenReturn("refresh");

            // When / Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@example.com",
                                        "password": "password1234",
                                        "role": "ALUMNO"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(cookie().attribute("refresh_token", "SameSite", "Strict"));
        }

        @Test
        @DisplayName("Cookie refresh_token debe tener path /api/auth")
        void shouldHaveCorrectPath() throws Exception {
            // Given
            when(registerUserUseCase.execute(anyString(), anyString(), any(User.Role.class)))
                    .thenReturn(Result.ok(1L));
            when(tokenServiceAdapter.generateAccessToken(anyLong(), anyString(), anyString()))
                    .thenReturn("access");
            when(tokenServiceAdapter.storeRefreshToken(anyLong(), anyString(), anyString(), anyString()))
                    .thenReturn("refresh");

            // When / Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@example.com",
                                        "password": "password1234",
                                        "role": "ALUMNO"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(cookie().path("refresh_token", "/api/auth"));
        }

        @Test
        @DisplayName("Cookie refresh_token debe ser HttpOnly y Secure")
        void shouldBeHttpOnlyAndSecure() throws Exception {
            // Given
            when(registerUserUseCase.execute(anyString(), anyString(), any(User.Role.class)))
                    .thenReturn(Result.ok(1L));
            when(tokenServiceAdapter.generateAccessToken(anyLong(), anyString(), anyString()))
                    .thenReturn("access");
            when(tokenServiceAdapter.storeRefreshToken(anyLong(), anyString(), anyString(), anyString()))
                    .thenReturn("refresh");

            // When / Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@example.com",
                                        "password": "password1234",
                                        "role": "ALUMNO"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(cookie().httpOnly("refresh_token", true))
                    .andExpect(cookie().secure("refresh_token", true));
        }
    }
}