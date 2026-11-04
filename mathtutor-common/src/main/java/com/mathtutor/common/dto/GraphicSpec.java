package com.mathtutor.common.dto;

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

    public record Point(
        double x,
        double y,
        double z
    ) {}

    public enum GraphicType {
        CURVA_2D,
        SUPERFICIE,
        RECTA_TANGENTE,
        PLANO_TANGENTE
    }
}