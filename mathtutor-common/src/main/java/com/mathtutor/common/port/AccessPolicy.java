package com.mathtutor.common.port;

import com.mathtutor.common.dto.Role;

/**
 * Puerto para la política de control de acceso por rol y recurso (RNF-04).
 */
public interface AccessPolicy {

    /**
     * Verifica si un usuario con un rol puede realizar una acción sobre un recurso.
     *
     * @param role     rol del usuario
     * @param action   acción a realizar (READ, WRITE, DELETE, ADMIN)
     * @param resource tipo de recurso (USER, EXERCISE, MODULE, SESSION, STATS, AUDIT, CONFIG)
     * @return true si tiene permiso
     */
    boolean can(Role role, Action action, Resource resource);

    enum Action {
        READ, WRITE, DELETE, ADMIN
    }

    enum Resource {
        USER, EXERCISE, MODULE, SESSION, STATS, AUDIT, CONFIG
    }
}