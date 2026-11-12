package com.mathtutor.llm;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Test unitario para verificar que SpringAiPgVectorConfig no provee EmbeddingModel
 * por defecto (sin perfil de proveedor activo).
 */
class SpringAiPgVectorConfigTest {

    @Configuration
    static class TestConfig {
        @Bean
        @Primary
        JdbcTemplate jdbcTemplate() {
            return mock(JdbcTemplate.class);
        }
    }

    @Test
    void noDefaultEmbeddingModelWhenNoProviderProfile() {
        // Given: ApplicationContext with only spring-ai-pgvector profile active
        // (no ollama, openai, google-ai-studio, deterministic)
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles("spring-ai-pgvector");
        context.register(TestConfig.class, SpringAiPgVectorConfig.class);

        // When: Try to refresh context
        // Then: Should fail because no EmbeddingModel bean is available
        // (the vectorStore bean also needs EmbeddingModel, so it will fail)
        assertThatThrownBy(context::refresh)
                .isInstanceOf(org.springframework.beans.factory.UnsatisfiedDependencyException.class)
                .hasMessageContaining("EmbeddingModel");

        context.close();
    }

    @Test
    void hasEmbeddingModelWithDeterministicProfile() {
        // Given: ApplicationContext with deterministic profile active
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles("spring-ai-pgvector", "deterministic");
        context.register(TestConfig.class, SpringAiPgVectorConfig.class);

        // When: Refresh context
        context.refresh();

        // Then: Should have EmbeddingModel bean (SlowDeterministicEmbeddingModel wrapped)
        assertThat(context.getBean(EmbeddingModel.class)).isNotNull();
        assertThat(context.getBean(SlowDeterministicEmbeddingModel.class)).isNotNull();

        context.close();
    }
}