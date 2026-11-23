package com.mathtutor.graphics;

import com.mathtutor.common.port.CasEngine;
import com.mathtutor.common.port.GraphicSpecService;
import com.mathtutor.common.port.GraphicSpecValidator;
import com.mathtutor.common.dto.GraphicSpec;
import org.springframework.stereotype.Component;
import java.util.List;

/** UC-35: gramática restringida y CAS antes de emitir datos al visor. */
@Component
public class GraphicSpecServiceImpl implements GraphicSpecService, GraphicSpecValidator {
    private final CasEngine cas;
    public GraphicSpecServiceImpl(CasEngine cas) { this.cas = cas; }
    @Override public GraphicSpec build(GraphicSpec.GraphicType type, String function, GraphicSpec.Domain domain, List<GraphicSpec.Point> points) {
        var spec = new GraphicSpec(type, function, domain, points == null ? List.of() : points);
        var result = validate(spec);
        if (!result.valid()) throw new IllegalArgumentException(result.errorMessage());
        return new GraphicSpec(type,function,domain,List.copyOf(spec.points()));
    }
    /** @brief UC-31/35. Valida el dominio y el punto antes de enviar la función al cliente. */
    @Override public ValidationResult validate(GraphicSpec spec) {
        if (spec == null || spec.type() == null)
            return ValidationResult.ofInvalid("Tipo gráfico no admitido");
        boolean plane = spec.type() == GraphicSpec.GraphicType.PLANO_TANGENTE;
        boolean line = spec.type() == GraphicSpec.GraphicType.RECTA_TANGENTE;
        var d = spec.domain();
        if (d == null || !Double.isFinite(d.min()) || !Double.isFinite(d.max()) || d.min() < -10000 || d.max() > 10000 || d.min() >= d.max() || d.steps() < 2 || d.steps() > 200)
            return ValidationResult.ofInvalid("Dominio gráfico no válido");
        if (spec.points() == null || spec.points().size() > 100 || spec.points().stream().anyMatch(p -> p == null || !Double.isFinite(p.x()) || !Double.isFinite(p.y()) || !Double.isFinite(p.z())))
            return ValidationResult.ofInvalid("Puntos no válidos");
        if (line || plane) {
            if (spec.points().size() != 1)
                return ValidationResult.ofInvalid("Indica un único punto de tangencia");
            var point = spec.points().getFirst();
            if (outside(point.x(), d) || (plane && outside(point.y(), d)))
                return ValidationResult.ofInvalid("El punto de tangencia debe estar dentro del dominio");
        }
        if (plane && (d.min() < -100 || d.max() > 100 || d.steps() > 128))
            return ValidationResult.ofInvalid("Dominio o resolución del plano tangente no válidos");
        if (!GraphicExpression.valid(spec.function(), plane || spec.type() == GraphicSpec.GraphicType.SUPERFICIE)
                || (plane && spec.function().length() > 150))
            return ValidationResult.ofInvalid("Función no admitida por el visor");
        var parsed = cas.parse(spec.function());
        if (parsed == null || parsed.normalized() == null || parsed.normalized().isBlank())
            return ValidationResult.ofInvalid("El CAS no puede interpretar la función");
        return ValidationResult.ofValid();
    }

    private boolean outside(double coordinate, GraphicSpec.Domain domain) {
        return coordinate < domain.min() || coordinate > domain.max();
    }
}
