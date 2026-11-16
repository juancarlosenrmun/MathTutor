package com.mathtutor.auth.filter;

import com.mathtutor.auth.adapter.TokenServiceAdapter;
import com.mathtutor.common.auth.port.TokenServicePort;
import com.mathtutor.common.auth.port.UserRepositoryPort;
import com.mathtutor.common.auth.domain.User;
import java.util.Optional;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests para JwtAuthenticationFilter.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    @Mock
    private TokenServiceAdapter tokenServiceAdapter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private UserRepositoryPort users;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(tokenServiceAdapter, users);
        when(users.findActiveByEmail(anyString())).thenAnswer(invocation -> {
            String email = invocation.getArgument(0);
            long id = email.equals("user@test.com") ? 42L : email.equals("header@test.com") ? 2L : email.equals("test@role.com") ? 99L : 1L;
            User.Role role = email.equals("user@test.com") ? User.Role.ADMIN : email.equals("header@test.com") ? User.Role.PROFESOR : User.Role.ALUMNO;
            return Optional.of(User.withId(id, email, "hash", role, true, null, java.time.Instant.now()));
        });
        // Limpiar contexto de seguridad antes de cada test
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Aplica cambio de rol sin renovar JWT (RNF-04)")
    void shouldUseCurrentRoleInsteadOfJwtRole() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer old-admin");
        when(tokenServiceAdapter.validateToken("old-admin")).thenReturn(Map.of("userId", 1L, "email", "test@example.com", "role", "ADMIN"));
        filter.doFilterInternal(request, response, filterChain);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority").containsExactly("ROLE_ALUMNO");
    }

    @Test
    @DisplayName("Rechaza usuario desactivado y un email reasignado a otra identidad")
    void shouldRejectMissingAndReassignedUser() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer stale");
        when(tokenServiceAdapter.validateToken("stale")).thenReturn(Map.of("userId", 2L, "email", "test@example.com", "role", "ADMIN"));
        when(users.findActiveByEmail("test@example.com")).thenReturn(Optional.empty());
        filter.doFilterInternal(request, response, filterChain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        when(users.findActiveByEmail("test@example.com")).thenReturn(Optional.of(User.withId(3L, "test@example.com", "hash", User.Role.ADMIN, true, null, java.time.Instant.now())));
        filter.doFilterInternal(request, response, filterChain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Nested
    @DisplayName("doFilterInternal() - Extracción de token desde cookie")
    class ExtractTokenFromCookie {

        @Test
        @DisplayName("Debe extraer token de cookie access_token")
        void shouldExtractTokenFromCookie() throws ServletException, IOException, TokenServicePort.InvalidTokenException {
            // Given
            Cookie accessTokenCookie = new Cookie("access_token", "valid-jwt-token");
            Cookie otherCookie = new Cookie("other", "value");
            when(request.getCookies()).thenReturn(new Cookie[]{accessTokenCookie, otherCookie});
            doReturn(Map.of("userId", 1L, "email", "test@example.com", "role", "ALUMNO"))
                    .when(tokenServiceAdapter).validateToken("valid-jwt-token");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(tokenServiceAdapter).validateToken("valid-jwt-token");
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                    .isInstanceOf(JwtAuthenticationFilter.AuthenticatedUser.class);
        }

        @Test
        @DisplayName("Debe ignorar cookies con otro nombre")
        void shouldIgnoreOtherCookies() throws ServletException, IOException, TokenServicePort.InvalidTokenException {
            // Given
            Cookie otherCookie = new Cookie("other", "value");
            when(request.getCookies()).thenReturn(new Cookie[]{otherCookie});

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(tokenServiceAdapter, never()).validateToken(anyString());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Debe manejar array de cookies null")
        void shouldHandleNullCookies() throws ServletException, IOException, TokenServicePort.InvalidTokenException {
            // Given
            when(request.getCookies()).thenReturn(null);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(tokenServiceAdapter, never()).validateToken(anyString());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Debe priorizar cookie sobre header Authorization")
        void shouldPrioritizeCookieOverAuthorizationHeader() throws ServletException, IOException, TokenServicePort.InvalidTokenException {
            // Given
            Cookie accessTokenCookie = new Cookie("access_token", "cookie-token");
            when(request.getCookies()).thenReturn(new Cookie[]{accessTokenCookie});
            when(request.getHeader("Authorization")).thenReturn("Bearer header-token");

            // Use doReturn with specific argument to avoid strict stubbing issues
            doReturn(Map.of("userId", 1L, "email", "test@example.com", "role", "ALUMNO"))
                    .when(tokenServiceAdapter).validateToken("cookie-token");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then - verify it was called exactly once with the cookie token
            verify(tokenServiceAdapter, times(1)).validateToken("cookie-token");
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("doFilterInternal() - Extracción de token desde header Authorization")
    class ExtractTokenFromHeader {

        @Test
        @DisplayName("Debe extraer token de header Authorization Bearer")
        void shouldExtractTokenFromAuthorizationHeader() throws ServletException, IOException, TokenServicePort.InvalidTokenException {
            // Given
            when(request.getCookies()).thenReturn(new Cookie[]{});
            when(request.getHeader("Authorization")).thenReturn("Bearer header-jwt-token");
            doReturn(Map.of("userId", 2L, "email", "header@test.com", "role", "PROFESOR"))
                    .when(tokenServiceAdapter).validateToken("header-jwt-token");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(tokenServiceAdapter).validateToken("header-jwt-token");
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        }

        @Test
        @DisplayName("Debe ignorar header Authorization sin prefijo Bearer")
        void shouldIgnoreAuthorizationHeaderWithoutBearerPrefix() throws ServletException, IOException, TokenServicePort.InvalidTokenException {
            // Given
            when(request.getCookies()).thenReturn(new Cookie[]{});
            when(request.getHeader("Authorization")).thenReturn("InvalidToken header-token");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(tokenServiceAdapter, never()).validateToken(anyString());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Debe ignorar header Authorization vacío")
        void shouldIgnoreEmptyAuthorizationHeader() throws ServletException, IOException, TokenServicePort.InvalidTokenException {
            // Given
            when(request.getCookies()).thenReturn(new Cookie[]{});
            when(request.getHeader("Authorization")).thenReturn("");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(tokenServiceAdapter, never()).validateToken(anyString());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Debe manejar header Authorization null")
        void shouldHandleNullAuthorizationHeader() throws ServletException, IOException, TokenServicePort.InvalidTokenException {
            // Given
            when(request.getCookies()).thenReturn(new Cookie[]{});
            when(request.getHeader("Authorization")).thenReturn(null);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(tokenServiceAdapter, never()).validateToken(anyString());
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("doFilterInternal() - Validación de token")
    class TokenValidation {

        @Test
        @DisplayName("Debe establecer autenticación cuando token es válido")
        void shouldSetAuthenticationWhenTokenValid() throws ServletException, IOException, TokenServicePort.InvalidTokenException {
            // Given
            when(request.getCookies()).thenReturn(new Cookie[]{});
            when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
            doReturn(Map.of(
                            "userId", 42L,
                            "email", "user@test.com",
                            "role", "ADMIN"
                    ))
                    .when(tokenServiceAdapter).validateToken("valid-token");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getPrincipal()).isInstanceOf(JwtAuthenticationFilter.AuthenticatedUser.class);
            var user = (JwtAuthenticationFilter.AuthenticatedUser) auth.getPrincipal();
            assertThat(user.id()).isEqualTo(42L);
            assertThat(user.email()).isEqualTo("user@test.com");
            assertThat(user.role()).isEqualTo("ADMIN");
            assertThat(auth.getAuthorities()).hasSize(1);
            assertThat(auth.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
        }

        @Test
        @DisplayName("Debe NO establecer autenticación cuando token es inválido (excepción)")
        void shouldNotSetAuthenticationWhenTokenInvalid() throws ServletException, IOException, TokenServicePort.InvalidTokenException {
            // Given
            when(request.getCookies()).thenReturn(new Cookie[]{});
            when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
            doThrow(new TokenServicePort.InvalidTokenException("Token expired"))
                    .when(tokenServiceAdapter).validateToken("invalid-token");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Debe NO establecer autenticación cuando claims son incompletos (userId null)")
        void shouldNotSetAuthenticationWhenUserIdMissing() throws ServletException, IOException, TokenServicePort.InvalidTokenException {
            // Given
            when(request.getCookies()).thenReturn(new Cookie[]{});
            when(request.getHeader("Authorization")).thenReturn("Bearer token-without-userid");
            doReturn(Map.of("email", "test@example.com", "role", "ALUMNO"))
                    .when(tokenServiceAdapter).validateToken("token-without-userid");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Debe NO establecer autenticación cuando claims son incompletos (email null)")
        void shouldNotSetAuthenticationWhenEmailMissing() throws ServletException, IOException, TokenServicePort.InvalidTokenException {
            // Given
            when(request.getCookies()).thenReturn(new Cookie[]{});
            when(request.getHeader("Authorization")).thenReturn("Bearer token-without-email");
            doReturn(Map.of("userId", 1L, "role", "ALUMNO"))
                    .when(tokenServiceAdapter).validateToken("token-without-email");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Debe NO establecer autenticación cuando claims son incompletos (role null)")
        void shouldNotSetAuthenticationWhenRoleMissing() throws ServletException, IOException, TokenServicePort.InvalidTokenException {
            // Given
            when(request.getCookies()).thenReturn(new Cookie[]{});
            when(request.getHeader("Authorization")).thenReturn("Bearer token-without-role");
            doReturn(Map.of("userId", 1L, "email", "test@example.com"))
                    .when(tokenServiceAdapter).validateToken("token-without-role");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Debe NO establecer autenticación cuando email está vacío")
        void shouldNotSetAuthenticationWhenEmailEmpty() throws ServletException, IOException, TokenServicePort.InvalidTokenException {
            // Given
            when(request.getCookies()).thenReturn(new Cookie[]{});
            when(request.getHeader("Authorization")).thenReturn("Bearer token-with-empty-email");
            doReturn(Map.of("userId", 1L, "email", "", "role", "ALUMNO"))
                    .when(tokenServiceAdapter).validateToken("token-with-empty-email");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Debe NO establecer autenticación cuando role está vacío")
        void shouldNotSetAuthenticationWhenRoleEmpty() throws ServletException, IOException, TokenServicePort.InvalidTokenException {
            // Given
            when(request.getCookies()).thenReturn(new Cookie[]{});
            when(request.getHeader("Authorization")).thenReturn("Bearer token-with-empty-role");
            doReturn(Map.of("userId", 1L, "email", "test@example.com", "role", ""))
                    .when(tokenServiceAdapter).validateToken("token-with-empty-role");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("doFilterInternal() - Sin token")
    class NoToken {

        @Test
        @DisplayName("Debe continuar filtro sin autenticación cuando no hay token")
        void shouldContinueWithoutAuthenticationWhenNoToken() throws ServletException, IOException, TokenServicePort.InvalidTokenException {
            // Given
            when(request.getCookies()).thenReturn(new Cookie[]{});
            when(request.getHeader("Authorization")).thenReturn(null);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(tokenServiceAdapter, never()).validateToken(anyString());
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Debe continuar filtro sin autenticación cuando cookie está vacía")
        void shouldContinueWithoutAuthenticationWhenCookieEmpty() throws ServletException, IOException, TokenServicePort.InvalidTokenException {
            // Given
            Cookie emptyCookie = new Cookie("access_token", "");
            when(request.getCookies()).thenReturn(new Cookie[]{emptyCookie});
            // No need to stub getHeader - extractToken returns empty string for empty cookie value

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(tokenServiceAdapter, never()).validateToken(anyString());
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("extractToken() - Cobertura de ramas")
    class ExtractTokenBranches {

        @Test
        @DisplayName("Debe retornar null cuando no hay cookies ni header")
        void shouldReturnNullWhenNoCookiesAndNoHeader() throws ServletException, IOException {
            // Given
            when(request.getCookies()).thenReturn(new Cookie[]{});
            when(request.getHeader("Authorization")).thenReturn(null);

            // When - usar reflexión para probar método privado
            var result = invokeExtractToken(request);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Debe retornar null cuando cookie access_token tiene valor null")
        void shouldReturnNullWhenAccessTokenCookieHasNullValue() throws ServletException, IOException {
            // Given
            Cookie cookie = new Cookie("access_token", null);
            when(request.getCookies()).thenReturn(new Cookie[]{cookie});
            // No need to stub getHeader - extractToken returns early when cookie name matches

            // When
            var result = invokeExtractToken(request);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Debe retornar token de header cuando cookie existe pero es otro nombre")
        void shouldReturnHeaderTokenWhenCookieIsDifferentName() throws ServletException, IOException {
            // Given
            Cookie otherCookie = new Cookie("other_cookie", "cookie-value");
            when(request.getCookies()).thenReturn(new Cookie[]{otherCookie});
            when(request.getHeader("Authorization")).thenReturn("Bearer header-token-value");

            // When
            var result = invokeExtractToken(request);

            // Then
            assertThat(result).isEqualTo("header-token-value");
        }

        private String invokeExtractToken(HttpServletRequest request) {
            try {
                var method = JwtAuthenticationFilter.class.getDeclaredMethod("extractToken", HttpServletRequest.class);
                method.setAccessible(true);
                return (String) method.invoke(filter, request);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Nested
    @DisplayName("setAuthentication() - Cobertura de ramas")
    class SetAuthenticationBranches {

        @Test
        @DisplayName("Debe crear AuthenticatedUser con role correcto")
        void shouldCreateAuthenticatedUserWithCorrectRole() throws Exception {
            // Given
            Map<String, Object> claims = Map.of(
                    "userId", 99L,
                    "email", "test@role.com",
                    "role", "CUSTOM_ROLE"
            );

            // When
            invokeSetAuthentication(claims, request);

            // Then
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            var user = (JwtAuthenticationFilter.AuthenticatedUser) auth.getPrincipal();
            assertThat(user.id()).isEqualTo(99L);
            assertThat(user.email()).isEqualTo("test@role.com");
            assertThat(user.role()).isEqualTo("ALUMNO");
            assertThat(auth.getAuthorities()).hasSize(1);
            assertThat(auth.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ALUMNO");
        }

        @Test
        @DisplayName("Debe establecer detalles de autenticación con WebAuthenticationDetailsSource")
        void shouldSetAuthenticationDetails() throws Exception {
            // Given
            Map<String, Object> claims = Map.of(
                    "userId", 1L,
                    "email", "test@example.com",
                    "role", "ALUMNO"
            );
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            // When
            invokeSetAuthentication(claims, request);

            // Then
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getDetails()).isNotNull();
        }

        private void invokeSetAuthentication(Map<String, Object> claims, HttpServletRequest request) throws Exception {
            var method = JwtAuthenticationFilter.class.getDeclaredMethod("setAuthentication", Map.class, HttpServletRequest.class);
            method.setAccessible(true);
            method.invoke(filter, claims, request);
        }
    }
}