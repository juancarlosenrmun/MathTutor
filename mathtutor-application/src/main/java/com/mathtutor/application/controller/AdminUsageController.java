package com.mathtutor.application.controller;

import com.mathtutor.application.security.AdminUsage;
import com.mathtutor.auth.filter.JwtAuthenticationFilter.AuthenticatedUser;
import com.mathtutor.persistence.repository.AdminUsageRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** @brief Consultas de uso para administradores (UC-09). */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsageController {
    private final AdminUsage usage;

    public AdminUsageController(AdminUsage usage) {
        this.usage = usage;
    }

    /** @brief Devuelve conteos por módulo y tema ordenados y paginados. */
    @GetMapping("/usage/topics")
    public ResponseEntity<AdminUsage.ResultPage<AdminUsageRepository.TopicUsage>> topics(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(usage.topics(page, size));
    }

    /** @brief Devuelve metadatos del perfil y registra una auditoría por página consultada. */
    @GetMapping("/users/{id}/sessions")
    public ResponseEntity<AdminUsage.ResultPage<AdminUsageRepository.SessionUsage>> sessions(
            @AuthenticationPrincipal AuthenticatedUser actor, @PathVariable long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(usage.sessions(actor.id(), id, page, size));
    }
}
