package com.mathtutor.common.auth.port;

import com.mathtutor.common.auth.domain.User;

import java.util.Optional;

/**
 * Puerto para repositorio de usuarios.
 * Define el contrato para acceso a datos de usuarios sin acoplar a JPA.
 */
public interface UserRepositoryPort {

    /**
     * Busca un usuario por email.
     *
     * @param email email del usuario
     * @return usuario si existe
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifica si existe un usuario con el email dado.
     *
     * @param email email a verificar
     * @return true si existe
     */
    boolean existsByEmail(String email);

    /**
     * Busca un usuario activo por email.
     *
     * @param email email del usuario
     * @return usuario activo si existe
     */
    Optional<User> findActiveByEmail(String email);

    /**
     * Guarda un usuario.
     *
     * @param user usuario a guardar
     * @return usuario guardado con ID asignado
     */
    User save(User user);
}