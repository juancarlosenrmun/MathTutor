package com.mathtutor.common.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de caja blanca para {@link Prompt}.
 * Cobertura: campos, equals/hashCode, toString, valores límite.
 */
class PromptTest {

    @Test
    void shouldCreatePromptWithAllFields() {
        Prompt prompt = new Prompt(
            "Eres un tutor de matemáticas",
            "Ayuda al estudiante con: x^2",
            List.of("chunk1", "chunk2"),
            "No reveles la solución",
            500,
            0.7
        );

        assertThat(prompt.systemPrompt()).isEqualTo("Eres un tutor de matemáticas");
        assertThat(prompt.userPrompt()).isEqualTo("Ayuda al estudiante con: x^2");
        assertThat(prompt.contextChunks()).containsExactly("chunk1", "chunk2");
        assertThat(prompt.constraints()).isEqualTo("No reveles la solución");
        assertThat(prompt.maxTokens()).isEqualTo(500);
        assertThat(prompt.temperature()).isEqualTo(0.7);
    }

    @Test
    void shouldAllowEmptyContextChunks() {
        Prompt prompt = new Prompt(
            "system",
            "user",
            List.of(),
            "constraints",
            100,
            0.5
        );

        assertThat(prompt.contextChunks()).isEmpty();
    }

    @Test
    void shouldAllowBoundaryValues() {
        Prompt minPrompt = new Prompt("s", "u", List.of(), "c", 1, 0.0);
        Prompt maxPrompt = new Prompt("s", "u", List.of(), "c", 4096, 1.0);

        assertThat(minPrompt.maxTokens()).isEqualTo(1);
        assertThat(minPrompt.temperature()).isEqualTo(0.0);
        assertThat(maxPrompt.maxTokens()).isEqualTo(4096);
        assertThat(maxPrompt.temperature()).isEqualTo(1.0);
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        Prompt p1 = new Prompt("sys", "user", List.of("c1"), "const", 100, 0.7);
        Prompt p2 = new Prompt("sys", "user", List.of("c1"), "const", 100, 0.7);
        Prompt p3 = new Prompt("sys", "user", List.of("c2"), "const", 100, 0.7);

        assertThat(p1).isEqualTo(p2);
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
        assertThat(p1).isNotEqualTo(p3);
    }
}