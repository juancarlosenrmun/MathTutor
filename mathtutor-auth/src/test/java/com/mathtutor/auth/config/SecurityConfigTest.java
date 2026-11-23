package com.mathtutor.auth.config;

import com.mathtutor.auth.adapter.TokenServiceAdapter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests para SecurityConfig.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig")
class SecurityConfigTest {

    @Mock
    private TokenServiceAdapter tokenServiceAdapter;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(tokenServiceAdapter, mock(com.mathtutor.common.auth.port.UserRepositoryPort.class),
                List.of("http://localhost:5173"), false);
    }

    @Test
    @DisplayName("Debe crear JwtAuthenticationFilter bean")
    void shouldCreateJwtAuthenticationFilterBean() {
        // When
        var filter = securityConfig.jwtAuthenticationFilter();

        // Then
        assertThat(filter).isNotNull();
        assertThat(filter).isInstanceOf(com.mathtutor.auth.filter.JwtAuthenticationFilter.class);
    }

    @Test
    @DisplayName("Debe crear AuthenticationManager bean")
    void shouldCreateAuthenticationManagerBean() throws Exception {
        // Given
        var authConfig = mock(AuthenticationConfiguration.class);
        var authManager = mock(AuthenticationManager.class);
        when(authConfig.getAuthenticationManager()).thenReturn(authManager);

        // When
        var result = securityConfig.authenticationManager(authConfig);

        // Then
        assertThat(result).isEqualTo(authManager);
        verify(authConfig).getAuthenticationManager();
    }

    @Test
    @DisplayName("Debe crear AuthenticationProvider bean")
    void shouldCreateAuthenticationProviderBean() {
        // Given
        var userDetailsService = mock(UserDetailsService.class);
        var passwordEncoder = mock(PasswordEncoder.class);

        // When
        var provider = securityConfig.authenticationProvider(userDetailsService, passwordEncoder);

        // Then
        assertThat(provider).isNotNull();
        assertThat(provider).isInstanceOf(DaoAuthenticationProvider.class);
    }

    @Test
    @DisplayName("Debe crear CorsConfigurationSource bean con configuración correcta")
    void shouldCreateCorsConfigurationSourceBean() {
        // When
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();

        // Then
        assertThat(source).isNotNull();
        // Usar MockHttpServletRequest para que coincida con la configuración registrada en /**
        HttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
        CorsConfiguration config = source.getCorsConfiguration(request);
        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins()).containsExactly("http://localhost:5173");
        assertThat(config.getAllowedMethods()).containsExactly("GET", "POST", "PUT", "DELETE", "OPTIONS");
        assertThat(config.getAllowedHeaders()).containsExactly("Authorization", "Content-Type", "X-XSRF-TOKEN");
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getMaxAge()).isEqualTo(3600L);
    }

    @Nested
    @DisplayName("Configuración CSRF")
    class CsrfConfig {

        @Test
        @DisplayName("Protege la cookie CSRF en HTTP local y en producción")
        void scopesTheCsrfCookieToItsHostInProduction() {
            var production = new SecurityConfig(tokenServiceAdapter,
                    mock(com.mathtutor.common.auth.port.UserRepositoryPort.class), List.of(), true);
            for (var config : List.of(securityConfig, production)) {
                var request = new MockHttpServletRequest();
                var response = new MockHttpServletResponse();
                var repository = config.csrfTokenRepository();
                repository.saveToken(repository.generateToken(request), request, response);
                boolean secure = config == production;
                var cookie = response.getCookie(secure ? "__Host-XSRF-TOKEN" : "XSRF-TOKEN");
                assertThat(cookie).isNotNull();
                assertThat(cookie.getSecure()).isEqualTo(secure);
                assertThat(cookie.isHttpOnly()).isTrue();
                assertThat(cookie.getAttribute("SameSite")).isEqualTo("Strict");
                assertThat(cookie.getPath()).isEqualTo("/");
                assertThat(cookie.getDomain()).isNull();
            }
        }
    }
}
