package com.mathtutor.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de caja blanca para {@link HelpLevel}.
 * Cobertura: todas las constantes del enum, ordinal, values(), valueOf().
 */
class HelpLevelTest {

    @Test
    void shouldHaveFourLevelsInOrder() {
        HelpLevel[] values = HelpLevel.values();

        assertThat(values).hasSize(4);
        assertThat(values[0]).isEqualTo(HelpLevel.CONCEPTUAL);
        assertThat(values[1]).isEqualTo(HelpLevel.PROCEDIMENTAL);
        assertThat(values[2]).isEqualTo(HelpLevel.EJEMPLO_ANALOGO);
        assertThat(values[3]).isEqualTo(HelpLevel.REVELACION);
    }

    @Test
    void shouldGetCorrectOrdinal() {
        assertThat(HelpLevel.CONCEPTUAL.ordinal()).isEqualTo(0);
        assertThat(HelpLevel.PROCEDIMENTAL.ordinal()).isEqualTo(1);
        assertThat(HelpLevel.EJEMPLO_ANALOGO.ordinal()).isEqualTo(2);
        assertThat(HelpLevel.REVELACION.ordinal()).isEqualTo(3);
    }

    @Test
    void shouldParseFromString() {
        assertThat(HelpLevel.valueOf("CONCEPTUAL")).isEqualTo(HelpLevel.CONCEPTUAL);
        assertThat(HelpLevel.valueOf("PROCEDIMENTAL")).isEqualTo(HelpLevel.PROCEDIMENTAL);
        assertThat(HelpLevel.valueOf("EJEMPLO_ANALOGO")).isEqualTo(HelpLevel.EJEMPLO_ANALOGO);
        assertThat(HelpLevel.valueOf("REVELACION")).isEqualTo(HelpLevel.REVELACION);
    }

    @Test
    void shouldThrowOnInvalidValue() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> HelpLevel.valueOf("INVALID"));
    }

    @Test
    void shouldCompareCorrectly() {
        assertThat(HelpLevel.CONCEPTUAL.compareTo(HelpLevel.PROCEDIMENTAL)).isLessThan(0);
        assertThat(HelpLevel.REVELACION.compareTo(HelpLevel.CONCEPTUAL)).isGreaterThan(0);
        assertThat(HelpLevel.PROCEDIMENTAL.compareTo(HelpLevel.PROCEDIMENTAL)).isEqualTo(0);
    }
}