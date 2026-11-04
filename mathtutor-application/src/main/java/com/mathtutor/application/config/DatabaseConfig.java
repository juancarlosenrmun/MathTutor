package com.mathtutor.application.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * Configuración de Flyway para migraciones de base de datos.
 * Se activa en perfil 'prod' o 'test' (no en 'dev' con H2).
 */
@Configuration
@Profile({"prod", "test"})
public class DatabaseConfig {

    @Value("${spring.flyway.enabled:true}")
    private boolean flywayEnabled;

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String flywayLocations;

    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations(flywayLocations)
            .baselineOnMigrate(true)
            .validateOnMigrate(true)
            .load();

        if (flywayEnabled) {
            flyway.migrate();
        }
        return flyway;
    }
}