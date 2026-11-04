package com.mathtutor.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de caja blanca para {@link CanonicalStep}.
 * Cobertura: campos, equals/hashCode, toString.
 */
class CanonicalStepTest {

    @Test
    void shouldCreateCanonicalStepWithAllFields() {
        CanonicalStep step = new CanonicalStep(1, "2x", "[\"2*x\", \"2x\", \"x*2\"]");

        assertThat(step.position()).isEqualTo(1);
        assertThat(step.expressionLatex()).isEqualTo("2x");
        assertThat(step.equivalentForms()).isEqualTo("[\"2*x\", \"2x\", \"x*2\"]");
    }

    @Test
    void shouldAllowNullEquivalentForms() {
        CanonicalStep step = new CanonicalStep(1, "x^2", null);

        assertThat(step.equivalentForms()).isNull();
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        CanonicalStep s1 = new CanonicalStep(1, "2x", "[\"2*x\"]");
        CanonicalStep s2 = new CanonicalStep(1, "2x", "[\"2*x\"]");
        CanonicalStep s3 = new CanonicalStep(2, "2x", "[\"2*x\"]");

        assertThat(s1).isEqualTo(s2);
        assertThat(s1.hashCode()).isEqualTo(s2.hashCode());
        assertThat(s1).isNotEqualTo(s3);
    }
}