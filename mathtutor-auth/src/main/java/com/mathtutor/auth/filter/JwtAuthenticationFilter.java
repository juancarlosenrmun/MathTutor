package com.mathtutor.auth.filter;

import com.mathtutor.common.auth.port.TokenServicePort;
import com.mathtutor.auth.adapter.TokenServiceAdapter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Filtro que extrae y valida el JWT de la cookie o header Authorization.
 * Establece el contexto de seguridad si el token es válido.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_COOKIE = "access_token";

    private final TokenServiceAdapter tokenServiceAdapter;

    public JwtAuthenticationFilter(TokenServiceAdapter tokenServiceAdapter) {
        this.tokenServiceAdapter = tokenServiceAdapter;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            try {
                Map<String, Object> claims = tokenServiceAdapter.validateToken(token);
                setAuthentication(claims, request);
            } catch (TokenServicePort.InvalidTokenException e) {
                // Token inválido o expirado - no establecer autenticación
                // El punto de entrada de autenticación manejará la respuesta 401
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        // Primero intentar desde cookie (más seguro para navegadores)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // Fallback: header Authorization
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    private void setAuthentication(Map<String, Object> claims, HttpServletRequest request) {
        Long userId = (Long) claims.get("userId");
        String email = (String) claims.get("email");
        String role = (String) claims.get("role");

        if (userId != null && StringUtils.hasText(email) && StringUtils.hasText(role)) {
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + role)
            );

            var principal = new AuthenticatedUser(userId, email, role);
            var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }

    /**
     * Usuario autenticado para el contexto de seguridad.
     * Implementa Principal para compatibilidad con MockMvc y Spring Security.
     */
    public record AuthenticatedUser(Long id, String email, String role) implements Principal {
        @Override
        public String getName() {
            return email;
        }
    }
}