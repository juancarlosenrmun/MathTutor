package com.mathtutor.persistence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * Aplicación de prueba mínima para el módulo de persistencia.
 * Excluye configuraciones que causan conflictos.
 */
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
public class PersistenceTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(PersistenceTestApplication.class, args);
    }
}