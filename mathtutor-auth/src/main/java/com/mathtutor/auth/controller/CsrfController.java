package com.mathtutor.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador para obtener el token CSRF.
 * Endpoint: GET /api/csrf-token
 *
 * El token CSRF se almacena en una cookie (XSRF-TOKEN) y debe enviarse
 * en el header X-XSRF-TOKEN para peticiones que modifican estado.
 */
@RestController
@RequestMapping("/api")
public class CsrfController {

    /**
     * Obtiene el token CSRF para el cliente SPA.
     * Spring Security establece automáticamente la cookie XSRF-TOKEN
     * cuando se usa CookieCsrfTokenRepository.
     *
     * @param request HttpServletRequest para obtener el CsrfToken
     * @return Mapa con el token CSRF
     */
    @GetMapping("/csrf-token")
    public Map<String, String> getCsrfToken(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        String token = csrfToken != null ? csrfToken.getToken() : null;
        return Map.of("csrfToken", token);
    }
}