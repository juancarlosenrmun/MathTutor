package com.mathtutor.auth.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para el matcher de CSRF (requiresCsrfProtection).
 * Verifica la lógica de decisión de qué endpoints requieren CSRF.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@DisplayName("SecurityConfig CSRF Matcher")
class SecurityConfigCsrfMatcherTest {

    @Mock
    private HttpServletRequest request;

    @Nested
    @DisplayName("Endpoints públicos ignorados")
    class PublicEndpointsIgnored {

        @Test
        @DisplayName("/api/auth/register no requiere CSRF")
        void registerDoesNotRequireCsrf() {
            when(request.getRequestURI()).thenReturn("/api/auth/register");
            when(request.getMethod()).thenReturn("POST");

            assertThat(SecurityConfig.requiresCsrfProtection(request)).isFalse();
        }

        @Test
        @DisplayName("/api/csrf-token no requiere CSRF")
        void csrfTokenDoesNotRequireCsrf() {
            when(request.getRequestURI()).thenReturn("/api/csrf-token");
            when(request.getMethod()).thenReturn("POST");

            assertThat(SecurityConfig.requiresCsrfProtection(request)).isFalse();
        }

        @Test
        @DisplayName("Otro endpoint /api/auth/* requiere CSRF en POST")
        void otherAuthEndpointRequiresCsrf() {
            when(request.getRequestURI()).thenReturn("/api/auth/login");
            when(request.getMethod()).thenReturn("POST");

            assertThat(SecurityConfig.requiresCsrfProtection(request)).isTrue();
        }

        @Test
        @DisplayName("Endpoint desconocido requiere CSRF en POST")
        void unknownEndpointRequiresCsrfOnPost() {
            when(request.getRequestURI()).thenReturn("/api/unknown");
            when(request.getMethod()).thenReturn("POST");

            assertThat(SecurityConfig.requiresCsrfProtection(request)).isTrue();
        }
    }

    @Nested
    @DisplayName("Métodos HTTP seguros (no requieren CSRF)")
    class SafeMethods {

        @Test
        @DisplayName("GET no requiere CSRF")
        void getDoesNotRequireCsrf() {
            when(request.getRequestURI()).thenReturn("/api/protected");
            when(request.getMethod()).thenReturn("GET");

            assertThat(SecurityConfig.requiresCsrfProtection(request)).isFalse();
        }

        @Test
        @DisplayName("HEAD no requiere CSRF")
        void headDoesNotRequireCsrf() {
            when(request.getRequestURI()).thenReturn("/api/protected");
            when(request.getMethod()).thenReturn("HEAD");

            assertThat(SecurityConfig.requiresCsrfProtection(request)).isFalse();
        }

        @Test
        @DisplayName("TRACE no requiere CSRF")
        void traceDoesNotRequireCsrf() {
            when(request.getRequestURI()).thenReturn("/api/protected");
            when(request.getMethod()).thenReturn("TRACE");

            assertThat(SecurityConfig.requiresCsrfProtection(request)).isFalse();
        }

        @Test
        @DisplayName("OPTIONS no requiere CSRF")
        void optionsDoesNotRequireCsrf() {
            when(request.getRequestURI()).thenReturn("/api/protected");
            when(request.getMethod()).thenReturn("OPTIONS");

            assertThat(SecurityConfig.requiresCsrfProtection(request)).isFalse();
        }
    }

    @Nested
    @DisplayName("Métodos no seguros (requieren CSRF)")
    class UnsafeMethods {

        @Test
        @DisplayName("POST requiere CSRF")
        void postRequiresCsrf() {
            when(request.getRequestURI()).thenReturn("/api/protected");
            when(request.getMethod()).thenReturn("POST");

            assertThat(SecurityConfig.requiresCsrfProtection(request)).isTrue();
        }

        @Test
        @DisplayName("PUT requiere CSRF")
        void putRequiresCsrf() {
            when(request.getRequestURI()).thenReturn("/api/protected");
            when(request.getMethod()).thenReturn("PUT");

            assertThat(SecurityConfig.requiresCsrfProtection(request)).isTrue();
        }

        @Test
        @DisplayName("DELETE requiere CSRF")
        void deleteRequiresCsrf() {
            when(request.getRequestURI()).thenReturn("/api/protected");
            when(request.getMethod()).thenReturn("DELETE");

            assertThat(SecurityConfig.requiresCsrfProtection(request)).isTrue();
        }

        @Test
        @DisplayName("PATCH requiere CSRF")
        void patchRequiresCsrf() {
            when(request.getRequestURI()).thenReturn("/api/protected");
            when(request.getMethod()).thenReturn("PATCH");

            assertThat(SecurityConfig.requiresCsrfProtection(request)).isTrue();
        }
    }

    @Nested
    @DisplayName("Combinaciones endpoint + método")
    class EndpointMethodCombinations {

        @Test
        @DisplayName("POST a /api/auth/register no requiere CSRF (endpoint público)")
        void postToRegisterNoCsrf() {
            when(request.getRequestURI()).thenReturn("/api/auth/register");
            when(request.getMethod()).thenReturn("POST");

            assertThat(SecurityConfig.requiresCsrfProtection(request)).isFalse();
        }

        @Test
        @DisplayName("GET a /api/auth/login no requiere CSRF (método seguro)")
        void getToLoginNoCsrf() {
            when(request.getRequestURI()).thenReturn("/api/auth/login");
            when(request.getMethod()).thenReturn("GET");

            assertThat(SecurityConfig.requiresCsrfProtection(request)).isFalse();
        }

        @Test
        @DisplayName("POST a /api/auth/login requiere CSRF (endpoint protegido + método no seguro)")
        void postToLoginRequiresCsrf() {
            when(request.getRequestURI()).thenReturn("/api/auth/login");
            when(request.getMethod()).thenReturn("POST");

            assertThat(SecurityConfig.requiresCsrfProtection(request)).isTrue();
        }

        @Test
        @DisplayName("PUT a /api/auth/register no requiere CSRF (endpoint público gana)")
        void putToRegisterNoCsrf() {
            when(request.getRequestURI()).thenReturn("/api/auth/register");
            when(request.getMethod()).thenReturn("PUT");

            assertThat(SecurityConfig.requiresCsrfProtection(request)).isFalse();
        }
    }
}