package com.mathtutor.persistence.repository;

import com.mathtutor.persistence.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para entidad Module.
 */
@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {

    Optional<Module> findByCode(String code);

    boolean existsByCode(String code);
}