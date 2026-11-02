package com.mathtutor.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de caja blanca para {@link TypicalError}.
 * Cobertura: campos, equals/hashCode, toString.
 */
class TypicalErrorTest {

    @Test
    void shouldCreateTypicalErrorWithAllFields() {
        TypicalError error = new TypicalError("CHAIN_RULE", "Aplica la regla de la cadena: d/dx f(g(x)) = f'(g(x)) * g'(x)");

        assertThat(error.code()).isEqualTo("CHAIN_RULE");
        assertThat(error.seedHint()).isEqualTo("Aplica la regla de la cadena: d/dx f(g(x)) = f'(g(x)) * g'(x)");
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        TypicalError e1 = new TypicalError("CODE1", "hint1");
        TypicalError e2 = new TypicalError("CODE1", "hint1");
        TypicalError e3 = new TypicalError("CODE2", "hint1");

        assertThat(e1).isEqualTo(e2);
        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
        assertThat(e1).isNotEqualTo(e3);
    }
}