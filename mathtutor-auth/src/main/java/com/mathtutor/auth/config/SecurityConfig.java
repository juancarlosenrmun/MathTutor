package com.mathtutor.auth.config;

import com.mathtutor.auth.adapter.TokenServiceAdapter;
import com.mathtutor.auth.filter.JwtAuthenticationFilter;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Set;

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

    // CSRF configuration constants
    private static final Set<String> CSRF_IGNORED_PATHS = Set.of("/api/auth/register", "/api/csrf-token");
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "TRACE", "OPTIONS");

    private final TokenServiceAdapter tokenServiceAdapter;

    public SecurityConfig(TokenServiceAdapter tokenServiceAdapter) {
        this.tokenServiceAdapter = tokenServiceAdapter;
    }

    /**
     * Determina si una petición requiere protección CSRF.
     * <p>
     * Reglas:
     * <ul>
     *   <li>Endpoints públicos ignorados ({@link #CSRF_IGNORED_PATHS}) nunca requieren CSRF</li>
     *   <li>Métodos HTTP seguros ({@link #SAFE_METHODS}) nunca requieren CSRF</li>
     *   <li>Cualquier otro endpoint con método no seguro (POST, PUT, DELETE, PATCH) requiere CSRF</li>
     * </ul>
     *
     * @param request la petición HTTP entrante
     * @return {@code true} si la petición requiere validación CSRF, {@code false} en caso contrario
     */
    public static boolean requiresCsrfProtection(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // Ignorar CSRF para endpoints públicos
        if (CSRF_IGNORED_PATHS.contains(uri)) {
            return false;
        }
        // Comportamiento por defecto: no requerir CSRF para métodos seguros
        String method = request.getMethod();
        return !SAFE_METHODS.contains(method);
    }

    @PostConstruct
    public void init() {
        log.info("SecurityConfig initialized - CSRF config: ignoringRequestMatchers=/api/auth/register,/api/csrf-token");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Usar CsrfTokenRequestAttributeHandler (sin XOR) para que el token en el request attribute
        // coincida con el de la cookie (necesario para double-submit cookie pattern)
        CsrfTokenRequestAttributeHandler csrfTokenRequestHandler = new CsrfTokenRequestAttributeHandler();

        http
                // CSRF habilitado con CookieCsrfTokenRepository (double-submit cookie pattern)
                // withHttpOnlyFalse() permite que JavaScript lea la cookie XSRF-TOKEN para AJAX
                // Ignorar CSRF para endpoints públicos. requireCsrfProtectionMatcher: CSRF solo para métodos no seguros (POST, PUT, DELETE, PATCH)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfTokenRequestHandler)
                        // Custom matcher que combina: ignorar paths específicos + solo métodos no seguros requieren CSRF
                        .requireCsrfProtectionMatcher(SecurityConfig::requiresCsrfProtection)
                )
                // CORS configurado
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Sesión stateless
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // Configuración de autorización
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/csrf-token").permitAll()
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
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Access denied\"}");
                        })
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(tokenServiceAdapter);
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
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173", "http://localhost:8080"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}