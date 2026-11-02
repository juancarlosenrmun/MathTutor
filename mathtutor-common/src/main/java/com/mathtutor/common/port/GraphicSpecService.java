package com.mathtutor.common.port;

import com.mathtutor.common.dto.GraphicSpec;

/**
 * Puerto para construir especificaciones gráficas validadas.
 */
public interface GraphicSpecService {

    /**
     * Construye una especificación gráfica para una función.
     *
     * @param type     tipo de gráfica
     * @param function expresión de la función
     * @param domain   dominio de la función
     * @param points   puntos específicos a resaltar (opcional)
     * @return especificación gráfica validada
     */
    GraphicSpec build(GraphicSpec.GraphicType type, String function, GraphicSpec.Domain domain, java.util.List<GraphicSpec.Point> points);
}