package com.mathtutor.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Clase principal de la aplicación MathTutor.
 * Perfil de configuración para arranque con PostgreSQL+pgvector.
 * Los módulos se cablean solo por interfaces (puertos definidos en common).
 */
@SpringBootApplication(
    exclude = { FlywayAutoConfiguration.class }, // Flyway se configura manualmente por perfil
    scanBasePackages = { "com.mathtutor.application", "com.mathtutor.common", "com.mathtutor.persistence",
        "com.mathtutor.llm", "com.mathtutor.graphics", "com.mathtutor.tutor", "com.mathtutor.web" }
)
@ComponentScan(basePackages = { "com.mathtutor.application", "com.mathtutor.common", "com.mathtutor.persistence",
    "com.mathtutor.llm", "com.mathtutor.graphics", "com.mathtutor.tutor", "com.mathtutor.web" })
public class MathTutorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MathTutorApplication.class, args);
    }
}