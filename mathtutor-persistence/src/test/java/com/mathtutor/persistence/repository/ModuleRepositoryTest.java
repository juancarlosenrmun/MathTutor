package com.mathtutor.persistence.repository;


import com.mathtutor.persistence.PersistenceTestApplication;
import com.mathtutor.persistence.PersistenceTestConfig;
import com.mathtutor.persistence.entity.Module;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración para ModuleRepository.
 */
@SpringBootTest(classes = PersistenceTestApplication.class)
@Import(PersistenceTestConfig.class)
@Transactional

class ModuleRepositoryTest {

    @Autowired
    private ModuleRepository moduleRepository;

    @Test
    void shouldSaveAndFindModuleByCode() {
        // Given
        Module module = Module.builder()
                .code("ALGEBRA")
                .name("Álgebra")
                .build();

        // When
        Module saved = moduleRepository.save(module);
        Optional<Module> found = moduleRepository.findByCode("ALGEBRA");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("ALGEBRA");
        assertThat(found.get().getName()).isEqualTo("Álgebra");
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void shouldReturnEmptyWhenCodeNotExists() {
        // When
        Optional<Module> found = moduleRepository.findByCode("NONEXISTENT");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldReturnTrueWhenCodeExists() {
        // Given
        Module module = Module.builder().code("CALCULUS").name("Cálculo").build();
        moduleRepository.save(module);

        // When
        boolean exists = moduleRepository.existsByCode("CALCULUS");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseWhenCodeNotExists() {
        // When
        boolean exists = moduleRepository.existsByCode("NONEXISTENT");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void shouldCascadeDeleteTopics() {
        // Given
        Module module = Module.builder().code("GEOMETRY").name("Geometría").build();
        moduleRepository.save(module);

        // When
        moduleRepository.delete(module);

        // Then
        assertThat(moduleRepository.findByCode("GEOMETRY")).isEmpty();
    }
}
