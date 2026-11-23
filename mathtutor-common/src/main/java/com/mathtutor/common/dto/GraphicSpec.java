package com.mathtutor.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.util.List;

/**
 * Especificación gráfica que el backend envía al cliente para renderizar.
 */
public record GraphicSpec(
    GraphicType type,
    String function,
    Domain domain,
    List<Point> points
) {
    public record Domain(
        double min,
        double max,
        int steps
    ) {}

    /** @brief En una recta tangente se usa x y en un plano se usan x e y. El visor calcula la altura. */
    public record Point(
        @JsonProperty(required = true) @JsonSetter(nulls = Nulls.FAIL) double x,
        @JsonProperty(required = true) @JsonSetter(nulls = Nulls.FAIL) double y,
        @JsonProperty(required = true) @JsonSetter(nulls = Nulls.FAIL) double z
    ) {}

    public enum GraphicType {
        CURVA_2D,
        SUPERFICIE,
        RECTA_TANGENTE,
        PLANO_TANGENTE
    }
}
