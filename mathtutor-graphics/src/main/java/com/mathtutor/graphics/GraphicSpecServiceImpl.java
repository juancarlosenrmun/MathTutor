package com.mathtutor.graphics;

import com.mathtutor.common.port.GraphicSpecService;
import com.mathtutor.common.dto.GraphicSpec;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Esqueleto de implementación del servicio de especificaciones gráficas.
 * Implementación vacía preparada para UC-27 (WebGL pipeline).
 * No usa WebGL ni shaders - solo define la estructura.
 */
@Component
public class GraphicSpecServiceImpl implements GraphicSpecService {

    @Override
    public GraphicSpec build(GraphicSpec.GraphicType type, String function, GraphicSpec.Domain domain, List<GraphicSpec.Point> points) {
        // TODO: Implementar en UC-27 con pipeline WebGL
        // Por ahora devuelve especificación básica usando el constructor canónico del record
        return new GraphicSpec(
            type,
            function,
            domain,
            points != null ? points : Collections.emptyList()
        );
    }
}