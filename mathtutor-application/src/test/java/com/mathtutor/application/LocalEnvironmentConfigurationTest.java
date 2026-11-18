package com.mathtutor.application;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** EN-01: configuración por dispositivo sin base de datos ni llamadas al proveedor. */
class LocalEnvironmentConfigurationTest {
    @TempDir Path directory;

    private ApplicationContextRunner configuration(String content) throws Exception {
        Path file = directory.resolve("entorno local.env");
        Files.writeString(file, content);
        return new ApplicationContextRunner()
                .withPropertyValues("spring.profiles.active=dev,google-ai-studio,notes-indexing",
                        "spring.config.import=" + file.toUri() + "[.properties]")
                .withInitializer(context -> context.getEnvironment().getPropertySources().replace(
                        "systemEnvironment", new SystemEnvironmentPropertySource("systemEnvironment", Map.of())))
                .withInitializer(new ConfigDataApplicationContextInitializer());
    }

    @Test void readsDatabaseAndAiFromDeviceEnvironmentFile() throws Exception {
        configuration("POSTGRES_HOST=equipo-local\nPOSTGRES_PORT=5544\nPOSTGRES_DB=tutor_local\n"
                + "POSTGRES_USER=docente\nPOSTGRES_PASSWORD=clave-de-este-equipo\n"
                + "JWT_SECRET=secreto-local-de-mas-de-treinta-y-dos-caracteres\nGOOGLE_AI_API_KEY=clave-simulada\n")
                .run(context -> {
                    var env = context.getEnvironment();
                    assertThat(env.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://equipo-local:5544/tutor_local");
                    assertThat(env.getProperty("spring.datasource.username")).isEqualTo("docente");
                    assertThat(env.getProperty("spring.datasource.password")).isEqualTo("clave-de-este-equipo");
                    assertThat(env.getProperty("jwt.secret")).isEqualTo("secreto-local-de-mas-de-treinta-y-dos-caracteres");
                    assertThat(env.getProperty("spring.ai.google.ai-studio.api-key")).isEqualTo("clave-simulada");
                    assertThat(env.matchesProfiles("spring-ai-pgvector")).isTrue();
                });
    }

    @Test void shellEnvironmentOverridesTheFile() throws Exception {
        configuration("POSTGRES_PASSWORD=clave-del-archivo\n")
                .withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
                        new SystemEnvironmentPropertySource("dispositivo", Map.of("POSTGRES_PASSWORD", "clave-de-terminal"))))
                .run(context -> assertThat(context.getEnvironment().getProperty("spring.datasource.password"))
                        .isEqualTo("clave-de-terminal"));
    }

    @Test void neverFallsBackToAPublicDatabasePassword() throws Exception {
        configuration("").run(context -> assertThatThrownBy(() -> context.getEnvironment()
                .getRequiredProperty("spring.datasource.password"))
                .isInstanceOf(IllegalArgumentException.class));
    }
}
