package com.mathtutor.application.security;

import com.mathtutor.persistence.entity.AuditLog;
import com.mathtutor.persistence.entity.User;
import com.mathtutor.persistence.repository.AuditLogRepository;
import com.mathtutor.persistence.repository.PasswordResetTokenRepository;
import com.mathtutor.persistence.repository.PersonalDataStore;
import com.mathtutor.persistence.repository.RefreshTokenRepository;
import com.mathtutor.persistence.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** @brief Gestiona perfiles y registra las operaciones sobre cuentas individuales. */
@Service
public class UserAdministration {
    private final UserRepository users;
    private final PersonalDataStore data;
    private final RefreshTokenRepository refresh;
    private final PasswordResetTokenRepository resets;
    private final AuditLogRepository audit;

    public UserAdministration(UserRepository users, PersonalDataStore data, RefreshTokenRepository refresh,
                              PasswordResetTokenRepository resets, AuditLogRepository audit) {
        this.users = users;
        this.data = data;
        this.refresh = refresh;
        this.resets = resets;
        this.audit = audit;
    }

    /** @brief Lista paginada de perfiles gestionables con los campos que necesita la interfaz. */
    @Transactional(readOnly = true)
    public List<Profile> profiles(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Paginación no válida (1–100)");
        return users.findByAnonymizedAtIsNull(PageRequest.of(page, size, Sort.by("id"))).stream()
                .map(user -> new Profile(user.getId(), user.getEmail(), user.getRole(), user.isActive()))
                .toList();
    }

    /** @brief Invalida inmediatamente el acceso y consume los enlaces de recuperación pendientes. */
    @Transactional
    public void deactivate(long adminId, long id) {
        User user = manageable(adminId, id);
        if (!user.isActive()) return;
        user.setActive(false);
        Instant now = Instant.now();
        refresh.revokeAllForUser(id, now);
        resets.consumePending(id, now);
        record(adminId, id, "USER_DEACTIVATED");
    }

    /** @brief Desvincula el perfil de su identidad y borra sus datos personales asociados. */
    @Transactional
    public void anonymize(long adminId, long id) {
        User user = manageable(adminId, id);
        data.eraseRelated(id);
        user.setActive(false);
        user.setEmail("anon-" + UUID.randomUUID());
        user.setPasswordHash("!");
        user.setRole(User.Role.ALUMNO);
        user.setAnonymizedAt(Instant.now());
        record(adminId, null, "USER_ANONYMIZED");
    }

    /** @brief Cambia el permiso conservando la sesión del usuario. */
    @Transactional
    public void changeRole(long adminId, long id, User.Role role) {
        User user = manageable(adminId, id);
        if (!user.isActive()) throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta está desactivada.");
        if (user.getRole() == role) return;
        user.setRole(role);
        record(adminId, id, "USER_ROLE_CHANGED");
    }

    /** @brief Registra la solicitud antes de encolarla y verifica que el destinatario siga activo. */
    @Transactional
    public void requestReset(long adminId, long id) {
        User user = manageable(adminId, id);
        if (!user.isActive()) throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta está desactivada.");
        record(adminId, id, "USER_PASSWORD_RESET_REQUESTED");
    }

    private User manageable(long adminId, long id) {
        if (adminId == id) throw new ResponseStatusException(HttpStatus.CONFLICT, "Usa las opciones de tu propia cuenta.");
        data.lockAccountChanges();
        var administrator = users.findByIdForUpdate(adminId).filter(User::isActive);
        if (administrator.isEmpty() || administrator.get().getRole() != User.Role.ADMIN) {
            throw new AccessDeniedException("Se requieren permisos de administración.");
        }
        return users.findByIdForUpdate(id).filter(user -> user.getAnonymizedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no disponible."));
    }

    private void record(long adminId, Long targetId, String action) {
        audit.save(AuditLog.builder().actor(users.getReferenceById(adminId)).targetType("USER")
                .targetId(targetId).action(action).build());
    }

    public record Profile(Long id, String email, User.Role role, boolean active) {}
}
