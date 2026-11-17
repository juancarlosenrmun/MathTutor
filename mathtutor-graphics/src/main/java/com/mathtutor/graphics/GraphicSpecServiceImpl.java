package com.mathtutor.graphics;

import com.mathtutor.common.port.CasEngine;
import com.mathtutor.common.port.GraphicSpecService;
import com.mathtutor.common.port.GraphicSpecValidator;
import com.mathtutor.common.dto.GraphicSpec;
import org.springframework.stereotype.Component;
import java.util.List;

/** UC-35: gram?tica restringida y CAS antes de emitir datos al visor. */
@Component
public class GraphicSpecServiceImpl implements GraphicSpecService, GraphicSpecValidator {
    private final CasEngine cas;
    public GraphicSpecServiceImpl(CasEngine cas) { this.cas = cas; }
    @Override public GraphicSpec build(GraphicSpec.GraphicType type, String function, GraphicSpec.Domain domain, List<GraphicSpec.Point> points) {
        var spec = new GraphicSpec(type, function, domain, points == null ? List.of() : List.copyOf(points));
        var result = validate(spec);
        if (!result.valid()) throw new IllegalArgumentException(result.errorMessage());
        return spec;
    }
    @Override public ValidationResult validate(GraphicSpec spec) {
        if (spec == null || (spec.type() != GraphicSpec.GraphicType.CURVA_2D && spec.type() != GraphicSpec.GraphicType.SUPERFICIE))
            return ValidationResult.ofInvalid("Tipo gr?fico no admitido");
        var d = spec.domain();
        if (d == null || !Double.isFinite(d.min()) || !Double.isFinite(d.max()) || d.min() < -10000 || d.max() > 10000 || d.min() >= d.max() || d.steps() < 2 || d.steps() > 200)
            return ValidationResult.ofInvalid("Dominio gr?fico no v?lido");
        if (spec.points() == null || spec.points().size() > 100 || spec.points().stream().anyMatch(p -> p == null || !Double.isFinite(p.x()) || !Double.isFinite(p.y()) || !Double.isFinite(p.z())))
            return ValidationResult.ofInvalid("Puntos no v?lidos");
        if (!GraphicExpression.valid(spec.function(), spec.type() == GraphicSpec.GraphicType.SUPERFICIE))
            return ValidationResult.ofInvalid("Funci?n no admitida por el visor");
        var parsed = cas.parse(spec.function());
        if (parsed == null || parsed.normalized() == null || parsed.normalized().isBlank())
            return ValidationResult.ofInvalid("El CAS no puede interpretar la funci?n");
        return ValidationResult.ofValid();
    }
}
