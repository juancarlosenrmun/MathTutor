package com.mathtutor.application;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de depuración para verificar qué variables de entorno y propiedades de sistema
 * recibe el JVM de pruebas.
 */
class DebugEnvironmentTest {

    @Test
    void printEnvironmentVariables() {
        System.out.println("=== Environment Variables ===");
        Map<String, String> env = System.getenv();
        env.entrySet().stream()
            .filter(e -> e.getKey().contains("DOCKER")
                      || e.getKey().contains("TESTCONTAINERS")
                      || e.getKey().contains("SPRING_PROFILES"))
            .forEach(e -> System.out.println(e.getKey() + "=" + e.getValue()));

        System.out.println("=== System Properties ===");
        System.getProperties().stringPropertyNames().stream()
            .filter(k -> k.contains("docker")
                      || k.contains("testcontainers")
                      || k.contains("spring.profiles"))
            .forEach(k -> System.out.println(k + "=" + System.getProperty(k)));

        // Verificar que al menos una variable de Docker está presente
        boolean hasDockerEnv = env.containsKey("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE")
                            || env.containsKey("DOCKER_HOST");
        boolean hasDockerProp = System.getProperty("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE") != null
                             || System.getProperty("DOCKER_HOST") != null;

        System.out.println("Has Docker env: " + hasDockerEnv);
        System.out.println("Has Docker prop: " + hasDockerProp);

        // This test always passes - it's just for debugging
        assertThat(true).isTrue();
    }
}