package com.mathtutor.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de caja blanca para {@link ErrorType}.
 * Cobertura: todas las constantes del enum, ordinal, values(), valueOf().
 */
class ErrorTypeTest {

    @Test
    void shouldHaveFiveTypes() {
        ErrorType[] values = ErrorType.values();

        assertThat(values).hasSize(5);
        assertThat(values[0]).isEqualTo(ErrorType.NINGUNO);
        assertThat(values[1]).isEqualTo(ErrorType.SIGNO);
        assertThat(values[2]).isEqualTo(ErrorType.REGLA_CADENA);
        assertThat(values[3]).isEqualTo(ErrorType.REGLA_PRODUCTO);
        assertThat(values[4]).isEqualTo(ErrorType.OTRO);
    }

    @Test
    void shouldGetCorrectOrdinal() {
        assertThat(ErrorType.NINGUNO.ordinal()).isEqualTo(0);
        assertThat(ErrorType.SIGNO.ordinal()).isEqualTo(1);
        assertThat(ErrorType.REGLA_CADENA.ordinal()).isEqualTo(2);
        assertThat(ErrorType.REGLA_PRODUCTO.ordinal()).isEqualTo(3);
        assertThat(ErrorType.OTRO.ordinal()).isEqualTo(4);
    }

    @Test
    void shouldParseFromString() {
        assertThat(ErrorType.valueOf("NINGUNO")).isEqualTo(ErrorType.NINGUNO);
        assertThat(ErrorType.valueOf("SIGNO")).isEqualTo(ErrorType.SIGNO);
        assertThat(ErrorType.valueOf("REGLA_CADENA")).isEqualTo(ErrorType.REGLA_CADENA);
        assertThat(ErrorType.valueOf("REGLA_PRODUCTO")).isEqualTo(ErrorType.REGLA_PRODUCTO);
        assertThat(ErrorType.valueOf("OTRO")).isEqualTo(ErrorType.OTRO);
    }

    @Test
    void shouldThrowOnInvalidValue() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> ErrorType.valueOf("INVALID"));
    }
}