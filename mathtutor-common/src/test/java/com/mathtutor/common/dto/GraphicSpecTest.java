package com.mathtutor.common.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de caja blanca para {@link GraphicSpec} y clases anidadas.
 * Cobertura: factory, campos, equals/hashCode, toString, enum GraphicType.
 */
class GraphicSpecTest {

    @Test
    void shouldCreateGraphicSpecWithAllFields() {
        GraphicSpec.Domain domain = new GraphicSpec.Domain(-5.0, 5.0, 100);
        GraphicSpec.Point point = new GraphicSpec.Point(1.0, 2.0, 0.0);
        GraphicSpec spec = new GraphicSpec(
            GraphicSpec.GraphicType.CURVA_2D,
            "x^2",
            domain,
            List.of(point)
        );

        assertThat(spec.type()).isEqualTo(GraphicSpec.GraphicType.CURVA_2D);
        assertThat(spec.function()).isEqualTo("x^2");
        assertThat(spec.domain()).isEqualTo(domain);
        assertThat(spec.points()).containsExactly(point);
    }

    @Test
    void shouldCreateDomainWithAllFields() {
        GraphicSpec.Domain domain = new GraphicSpec.Domain(-10.0, 10.0, 200);

        assertThat(domain.min()).isEqualTo(-10.0);
        assertThat(domain.max()).isEqualTo(10.0);
        assertThat(domain.steps()).isEqualTo(200);
    }

    @Test
    void shouldCreatePointWithAllFields() {
        GraphicSpec.Point point = new GraphicSpec.Point(1.5, 2.5, 3.5);

        assertThat(point.x()).isEqualTo(1.5);
        assertThat(point.y()).isEqualTo(2.5);
        assertThat(point.z()).isEqualTo(3.5);
    }

    @ParameterizedTest
    @MethodSource("graphicTypes")
    void shouldHaveAllGraphicTypes(GraphicSpec.GraphicType type) {
        assertThat(GraphicSpec.GraphicType.values()).contains(type);
    }

    static Stream<Arguments> graphicTypes() {
        return Stream.of(
            Arguments.of(GraphicSpec.GraphicType.CURVA_2D),
            Arguments.of(GraphicSpec.GraphicType.SUPERFICIE),
            Arguments.of(GraphicSpec.GraphicType.RECTA_TANGENTE),
            Arguments.of(GraphicSpec.GraphicType.PLANO_TANGENTE)
        );
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        GraphicSpec.Domain domain = new GraphicSpec.Domain(0.0, 1.0, 10);
        GraphicSpec.Point point = new GraphicSpec.Point(0.5, 0.5, 0.0);

        GraphicSpec spec1 = new GraphicSpec(GraphicSpec.GraphicType.CURVA_2D, "x", domain, List.of(point));
        GraphicSpec spec2 = new GraphicSpec(GraphicSpec.GraphicType.CURVA_2D, "x", domain, List.of(point));
        GraphicSpec spec3 = new GraphicSpec(GraphicSpec.GraphicType.SUPERFICIE, "x", domain, List.of(point));

        assertThat(spec1).isEqualTo(spec2);
        assertThat(spec1.hashCode()).isEqualTo(spec2.hashCode());
        assertThat(spec1).isNotEqualTo(spec3);
    }

    @Test
    void shouldHaveMeaningfulToString() {
        GraphicSpec spec = new GraphicSpec(
            GraphicSpec.GraphicType.CURVA_2D,
            "x^2",
            new GraphicSpec.Domain(-1.0, 1.0, 10),
            List.of()
        );

        String toString = spec.toString();

        assertThat(toString).contains("CURVA_2D");
        assertThat(toString).contains("x^2");
    }
}