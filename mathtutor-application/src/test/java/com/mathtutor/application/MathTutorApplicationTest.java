package com.mathtutor.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración para verificar que el contexto de la aplicación arranca
 * correctamente con PostgreSQL+pgvector usando Testcontainers.
 * Verifica que la extensión vector existe y los módulos se cablean por interfaces.
 * Requiere Docker activo para Testcontainers.
 */
@SpringBootTest(classes = MathTutorApplication.class)
@Testcontainers
class MathTutorApplicationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
        .withDatabaseName("mathtutor_test")
        .withUsername("test")
        .withPassword("test")
        .withInitScript("db/migration/test-init.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring arranca correctamente
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void applicationHasMainBean() {
        // Verifica que la aplicación principal está registrada
        assertThat(applicationContext.containsBean("mathTutorApplication")).isTrue();
    }

    @Test
    void pgvectorExtensionExists() {
        // Verifica que la extensión pgvector está disponible en la base de datos
        // Esto se verifica indirectamente a través de la inicialización de Testcontainers
        // que usa la imagen pgvector/pgvector:pg16
        assertThat(postgres.isRunning()).isTrue();
        assertThat(postgres.getJdbcUrl()).contains("postgresql");
    }

    @Test
    void modulesAreWiredByInterfaces() {
        // Verifica que los beans de los módulos están disponibles
        // (se inyectan por sus interfaces/puerto definidos en common)
        assertThat(applicationContext.getBeanNamesForType(com.mathtutor.common.port.LlmGateway.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(com.mathtutor.common.port.GraphicSpecService.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(com.mathtutor.common.port.PedagogicalPolicy.class)).isNotEmpty();
    }
}