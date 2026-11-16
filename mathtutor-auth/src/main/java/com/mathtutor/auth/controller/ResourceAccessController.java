package com.mathtutor.auth.controller;

import com.mathtutor.auth.filter.JwtAuthenticationFilter.AuthenticatedUser;
import com.mathtutor.common.dto.Role;
import com.mathtutor.common.port.AccessPolicy;
import com.mathtutor.common.port.AccessPolicy.Action;
import com.mathtutor.common.port.AccessPolicy.Resource;
import com.mathtutor.persistence.repository.TutorSessionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** UC-03: comprueba permisos sin revelar contenido ni existencia de sesiones ajenas. */
@RestController
@RequestMapping("/api/access")
public class ResourceAccessController {
    private final AccessPolicy policy;
    private final TutorSessionRepository sessions;

    public ResourceAccessController(AccessPolicy policy, TutorSessionRepository sessions) {
        this.policy = policy;
        this.sessions = sessions;
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<Void> session(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        if (!policy.can(Role.valueOf(user.role()), Action.READ, Resource.SESSION)
                || id <= 0 || !sessions.existsByIdAndUserId(id, user.id())) {
            // UC-09 añadirá acceso administrativo individual con auditoría obligatoria.
            throw new AccessDeniedException("Prohibido");
        }
        return ResponseEntity.noContent().build();
    }
}
