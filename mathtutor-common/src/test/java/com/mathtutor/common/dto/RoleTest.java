package com.mathtutor.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de caja blanca para {@link Role}.
 * Cobertura: todas las constantes del enum, ordinal, values(), valueOf().
 */
class RoleTest {

    @Test
    void shouldHaveThreeRoles() {
        Role[] values = Role.values();

        assertThat(values).hasSize(3);
        assertThat(values[0]).isEqualTo(Role.ALUMNO);
        assertThat(values[1]).isEqualTo(Role.PROFESOR);
        assertThat(values[2]).isEqualTo(Role.ADMIN);
    }

    @Test
    void shouldGetCorrectOrdinal() {
        assertThat(Role.ALUMNO.ordinal()).isEqualTo(0);
        assertThat(Role.PROFESOR.ordinal()).isEqualTo(1);
        assertThat(Role.ADMIN.ordinal()).isEqualTo(2);
    }

    @Test
    void shouldParseFromString() {
        assertThat(Role.valueOf("ALUMNO")).isEqualTo(Role.ALUMNO);
        assertThat(Role.valueOf("PROFESOR")).isEqualTo(Role.PROFESOR);
        assertThat(Role.valueOf("ADMIN")).isEqualTo(Role.ADMIN);
    }

    @Test
    void shouldThrowOnInvalidValue() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> Role.valueOf("INVALID"));
    }
}