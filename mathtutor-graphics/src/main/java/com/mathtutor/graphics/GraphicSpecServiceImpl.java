package com.mathtutor.graphics;

import com.mathtutor.common.port.GraphicSpecService;
import com.mathtutor.common.dto.GraphicSpec;

/**
 * Esqueleto de implementación del servicio de especificaciones gráficas.
 * Implementación vacía preparada para UC-27 (WebGL pipeline).
 * No usa WebGL ni shaders - solo define la estructura.
 */
public class GraphicSpecServiceImpl implements GraphicSpecService {

    @Override
    public GraphicSpec build(GraphicSpec.GraphicType type, String function, GraphicSpec.Domain domain, java.util.List<GraphicSpec.Point> points) {
        // TODO: Implementar en UC-27 con pipeline WebGL
        // Por ahora devuelve especificación básica
        return GraphicSpec.builder()
            .type(type)
            .function(function)
            .domain(domain)
            .points(points != null ? points : java.util.Collections.emptyList())
            .build();
    }
}