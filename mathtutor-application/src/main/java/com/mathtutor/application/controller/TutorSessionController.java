package com.mathtutor.application.controller;

import com.mathtutor.auth.filter.JwtAuthenticationFilter.AuthenticatedUser;
import com.mathtutor.common.dto.TutorModule;
import com.mathtutor.common.dto.TutorSessionInfo;
import com.mathtutor.tutor.TutorSessions;
import com.mathtutor.tutor.SessionUnavailableException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** @brief UC-12: catálogo y sesiones del alumno; el módulo no admite actualización. */
@RestController
@RequestMapping("/api/tutor")
@PreAuthorize("hasRole('ALUMNO')")
public class TutorSessionController {
    private final TutorSessions sessions;
    public TutorSessionController(TutorSessions sessions) { this.sessions = sessions; }

    @GetMapping("/modules")
    public List<TutorModule> modules() { return sessions.modules(); }

    @PostMapping("/sessions") @ResponseStatus(HttpStatus.CREATED)
    public TutorSessionInfo create(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody Selection body) {
        return sessions.start(user.id(), body.moduleCode());
    }

    @GetMapping("/sessions/{id}")
    public TutorSessionInfo get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return sessions.active(user.id(), id);
    }

    @ExceptionHandler(SessionUnavailableException.class) @ResponseStatus(HttpStatus.NOT_FOUND)
    public String unavailable() { return "Sesión no disponible"; }

    public record Selection(@NotBlank @Size(max = 50) String moduleCode) {}
}
