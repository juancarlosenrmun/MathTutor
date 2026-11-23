package com.mathtutor.auth.config;

import com.mathtutor.auth.adapter.TokenServiceAdapter;
import com.mathtutor.common.auth.port.UserRepositoryPort;
import com.mathtutor.auth.filter.JwtAuthenticationFilter;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración de seguridad Spring Security para la API REST.
 * Implementa autenticación stateless con JWT, CSRF habilitado con double-submit cookie pattern,
 * cookies seguras (HttpOnly, Secure, SameSite).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final TokenServiceAdapter tokenServiceAdapter;

    private final UserRepositoryPort users;
    private final List<String> allowedOrigins;
    private final boolean secureCookies;

    public SecurityConfig(TokenServiceAdapter tokenServiceAdapter, UserRepositoryPort users,
                          @Value("${mathtutor.security.allowed-origins:}") List<String> allowedOrigins,
                          @Value("${mathtutor.security.secure-cookies:false}") boolean secureCookies) {
        this.tokenServiceAdapter = tokenServiceAdapter;
        this.users = users;
        this.allowedOrigins = List.copyOf(allowedOrigins);
        this.secureCookies = secureCookies;
    }

    @PostConstruct
    public void init() {
        log.info("Seguridad inicializada: CSRF protege todos los métodos no seguros");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Usar CsrfTokenRequestAttributeHandler (sin XOR) para que el token en el request attribute
        // coincida con el de la cookie (necesario para double-submit cookie pattern)
        CsrfTokenRequestAttributeHandler csrfTokenRequestHandler = new CsrfTokenRequestAttributeHandler();
        http
                // El cliente obtiene el token de la respuesta JSON y la cookie queda fuera del alcance de JavaScript.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                        .csrfTokenRequestHandler(csrfTokenRequestHandler)
                )
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
                                + "img-src 'self' data:; object-src 'none'; base-uri 'none'; "
                                + "form-action 'self'; frame-ancestors 'none'"))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER)))
                // CORS configurado
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Sesión stateless
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // Configuración de autorización
                .authorizeHttpRequests(auth -> auth
                        // La petición original ya fue autorizada; el redispatch SSE no lleva un nuevo JWT.
                        .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ASYNC).permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/logout", "/api/auth/refresh").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/auth/password-reset/request", "/api/auth/password-reset/confirm").permitAll()
                        .requestMatchers("/api/csrf-token").permitAll()
                        .requestMatchers("/visor/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/info").permitAll()
                        .anyRequest().authenticated()
                )
                // Filtro JWT personalizado
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                // Manejo de excepciones
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"success\":false,\"data\":null,\"error\":\"No autenticado\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"success\":false,\"data\":null,\"error\":\"Prohibido\"}");
                        })
                );

        return http.build();
    }

    /** @brief Usa una cookie restringida al host en producción. El cliente lee el token del JSON. */
    CookieCsrfTokenRepository csrfTokenRepository() {
        var repository = new CookieCsrfTokenRepository();
        if (secureCookies) repository.setCookieName("__Host-XSRF-TOKEN");
        repository.setCookieCustomizer(cookie -> {
            cookie.httpOnly(true).sameSite("Strict").path("/");
            if (secureCookies) cookie.secure(true);
        });
        return repository;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(tokenServiceAdapter, users);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
