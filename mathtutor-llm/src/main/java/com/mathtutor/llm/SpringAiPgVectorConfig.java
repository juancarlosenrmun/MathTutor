package com.mathtutor.llm;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Configuración de Spring AI PgVectorStore para tests de integración.
 * Perfil: "spring-ai-pgvector" para activar solo cuando hay BD PostgreSQL con pgvector y Spring AI.
 * Usa el SpringAiDeterministicEmbeddingModel para tests sin claves API.
 */
@Configuration
@Profile("spring-ai-pgvector")
public class SpringAiPgVectorConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Bean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate) {
        // PgVectorStore builder requiere JdbcTemplate y EmbeddingModel
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(384)
                .initializeSchema(true) // Crea tablas/índices si no existen
                .build();
    }
}