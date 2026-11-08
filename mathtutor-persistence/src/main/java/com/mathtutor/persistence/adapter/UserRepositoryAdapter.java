package com.mathtutor.persistence.adapter;

import com.mathtutor.common.auth.port.UserRepositoryPort;
import com.mathtutor.persistence.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Adaptador que implementa UserRepositoryPort usando Spring Data JPA.
 * Convierte entre entidad JPA y entidad de dominio.
 */
@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserRepository userRepository;

    public UserRepositoryAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<com.mathtutor.common.auth.domain.User> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public Optional<com.mathtutor.common.auth.domain.User> findActiveByEmail(String email) {
        return userRepository.findActiveByEmail(email)
                .map(this::toDomain);
    }

    @Override
    public com.mathtutor.common.auth.domain.User save(com.mathtutor.common.auth.domain.User user) {
        com.mathtutor.persistence.entity.User entity = toEntity(user);
        com.mathtutor.persistence.entity.User savedEntity = userRepository.save(entity);
        return toDomain(savedEntity);
    }

    /**
     * Convierte entidad JPA a entidad de dominio.
     */
    private com.mathtutor.common.auth.domain.User toDomain(com.mathtutor.persistence.entity.User entity) {
        return com.mathtutor.common.auth.domain.User.withId(
                entity.getId(),
                entity.getEmail(),
                entity.getPasswordHash(),
                com.mathtutor.common.auth.domain.User.Role.valueOf(entity.getRole().name()),
                entity.isActive(),
                entity.getAnonymizedAt(),
                entity.getCreatedAt()
        );
    }

    /**
     * Convierte entidad de dominio a entidad JPA.
     */
    private com.mathtutor.persistence.entity.User toEntity(com.mathtutor.common.auth.domain.User user) {
        if (user.id() != null) {
            // Usuario existente - buscar y actualizar
            return userRepository.findById(user.id())
                    .map(existing -> {
                        existing.setEmail(user.email());
                        existing.setPasswordHash(user.passwordHash());
                        existing.setRole(com.mathtutor.persistence.entity.User.Role.valueOf(user.role().name()));
                        existing.setActive(user.active());
                        existing.setAnonymizedAt(user.anonymizedAt());
                        // createdAt no se actualiza
                        return existing;
                    })
                    .orElseGet(() -> createNewEntity(user));
        } else {
            // Usuario nuevo
            return createNewEntity(user);
        }
    }

    private com.mathtutor.persistence.entity.User createNewEntity(com.mathtutor.common.auth.domain.User user) {
        com.mathtutor.persistence.entity.User entity = new com.mathtutor.persistence.entity.User();
        entity.setEmail(user.email());
        entity.setPasswordHash(user.passwordHash());
        entity.setRole(com.mathtutor.persistence.entity.User.Role.valueOf(user.role().name()));
        entity.setActive(user.active());
        entity.setAnonymizedAt(user.anonymizedAt());
        entity.setCreatedAt(user.createdAt() != null ? user.createdAt() : Instant.now());
        return entity;
    }
}