package com.mathtutor.auth;

import com.mathtutor.common.dto.Role;
import com.mathtutor.common.port.AccessPolicy;
import org.springframework.stereotype.Component;

/** Permisos de UC-03; la propiedad del recurso se comprueba al acceder a sus datos. */
@Component
public final class RoleAccessPolicy implements AccessPolicy {
    @Override
    public boolean can(Role role, Action action, Resource resource) {
        if (role == null || action == null || resource == null) {
            return false;
        }
        if (role == Role.ADMIN) {
            return true;
        }
        if (action != Action.READ) {
            return false;
        }
        return switch (resource) {
            case MODULE, EXERCISE -> true;
            case SESSION -> role == Role.ALUMNO;
            case STATS -> role == Role.PROFESOR;
            default -> false;
        };
    }
}
