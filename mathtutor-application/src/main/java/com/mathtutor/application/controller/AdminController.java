package com.mathtutor.application.controller;

import com.mathtutor.application.security.PasswordResetMail;
import com.mathtutor.application.security.UserAdministration;
import com.mathtutor.auth.filter.JwtAuthenticationFilter.AuthenticatedUser;
import com.mathtutor.persistence.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** @brief Gestión de perfiles y recuperación asistida (UC-06, UC-07 y UC-08). */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserAdministration admin;
    private final PasswordResetMail mail;

    public AdminController(UserAdministration admin, PasswordResetMail mail) {
        this.admin = admin;
        this.mail = mail;
    }

    /** @brief Lista perfiles gestionables sin permitir guardar los datos en caché. */
    @GetMapping
    public ResponseEntity<List<UserAdministration.Profile>> profiles(@RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(admin.profiles(page, size));
    }

    /** @brief Desactiva el acceso del perfil e invalida sus credenciales de renovación. */
    @PutMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable long id) {
        admin.deactivate(actor.id(), id);
    }

    /** @brief Desvincula la identidad del perfil y suprime sus registros personales. */
    @PutMapping("/{id}/anonymize")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void anonymize(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable long id) {
        admin.anonymize(actor.id(), id);
    }

    /** @brief Actualiza el permiso del perfil. Se aplica en la siguiente petición autenticada. */
    @PutMapping("/{id}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeRole(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable long id,
                           @Valid @RequestBody RoleChange change) {
        admin.changeRole(actor.id(), id, change.role());
    }

    /** @brief Solicita el envío SMTP para una cuenta activa y devuelve 202 si se encola. */
    @PostMapping("/{id}/password-reset")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void reset(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable long id) {
        admin.requestReset(actor.id(), id);
        if (!mail.enqueue(id)) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "El envío de enlaces no está disponible.");
    }

    public record RoleChange(@NotNull User.Role role) {}
}
