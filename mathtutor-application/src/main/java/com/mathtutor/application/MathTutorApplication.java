package com.mathtutor.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Clase principal de la aplicación MathTutor.
 * Perfil de configuración para arranque con PostgreSQL+pgvector.
 * Los módulos se cablean solo por interfaces (puertos definidos en common).
 */
@SpringBootApplication(scanBasePackages = "com.mathtutor")
@EnableJpaRepositories(basePackages = { "com.mathtutor.persistence.repository", "com.mathtutor.auth.adapter" })
@EntityScan(basePackages = { "com.mathtutor.persistence.entity" })
public class MathTutorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MathTutorApplication.class, args);
    }
}
