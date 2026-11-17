package com.mathtutor.graphics;

import com.mathtutor.common.dto.GraphicSpec;
import com.mathtutor.common.port.CasEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GraphicValidationTest {
    final CasEngine cas = mock(CasEngine.class);
    final GraphicSpecServiceImpl service = new GraphicSpecServiceImpl(cas);
    final GraphicSpec.Domain domain = new GraphicSpec.Domain(-10, 10, 100);

    @ParameterizedTest @ValueSource(strings = {"x", "x^2", "sin(x)+cos(x)", "-sqrt(abs(x))", "1.2e-3*x", "exp(x)/log(2)", "tan(x)", "(x+1)*2"})
    void acceptsOnlyParsedExpressions(String expression) {
        when(cas.parse(expression)).thenReturn(new CasEngine.Expr(expression, "AST", "AST"));
        assertThat(service.build(GraphicSpec.GraphicType.CURVA_2D, expression, domain, null).function()).isEqualTo(expression);
        verify(cas).parse(expression);
    }
    @ParameterizedTest @ValueSource(strings = {"", " ", "x=1", "eval(x)", "x;alert(1)", "y", "z", "sin", "sin()", "x+", "2x", "(x", "x)", "x..2", "Infinity", "NaN", "sin(x,2)", "x**2"})
    void rejectsUnsafeOrMalformedExpressions(String expression) {
        assertThatThrownBy(() -> service.build(GraphicSpec.GraphicType.CURVA_2D, expression, domain, null)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(cas);
    }
    @Test void surfaceAllowsBothVariables() {
        when(cas.parse("x*y")).thenReturn(new CasEngine.Expr("x*y", "AST", "AST"));
        assertThat(service.build(GraphicSpec.GraphicType.SUPERFICIE, "x*y", domain, null).points()).isEmpty();
    }
    @Test void rejectsCasFailure() {
        assertThatThrownBy(() -> service.build(GraphicSpec.GraphicType.CURVA_2D, "x", domain, null)).isInstanceOf(IllegalArgumentException.class);
        when(cas.parse("x")).thenReturn(new CasEngine.Expr("x", "", null));
        assertThatThrownBy(() -> service.build(GraphicSpec.GraphicType.CURVA_2D, "x", domain, null)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsInvalidShapeAndLimits() {
        for (var d : java.util.List.of(new GraphicSpec.Domain(1, 1, 2), new GraphicSpec.Domain(2, 1, 2),
                new GraphicSpec.Domain(-10001, 1, 2), new GraphicSpec.Domain(0, 10001, 2),
                new GraphicSpec.Domain(Double.NaN, 1, 2), new GraphicSpec.Domain(0, Double.POSITIVE_INFINITY, 2),
                new GraphicSpec.Domain(0, 1, 1), new GraphicSpec.Domain(0, 1, 201))) {
            assertThatThrownBy(() -> service.build(GraphicSpec.GraphicType.CURVA_2D, "x", d, null)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> service.build(null, "x", domain, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.build(GraphicSpec.GraphicType.RECTA_TANGENTE, "x", domain, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.build(GraphicSpec.GraphicType.CURVA_2D, null, domain, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.build(GraphicSpec.GraphicType.CURVA_2D, "x", null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.build(GraphicSpec.GraphicType.CURVA_2D, "x".repeat(201), domain, null)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void checksPointsAndCopiesList() {
        when(cas.parse("x")).thenReturn(new CasEngine.Expr("x", "x", "x"));
        var points = new java.util.ArrayList<GraphicSpec.Point>();
        points.add(new GraphicSpec.Point(0, 0, 0));
        var spec = service.build(GraphicSpec.GraphicType.CURVA_2D, "x", domain, points);
        points.clear(); assertThat(spec.points()).hasSize(1);
        for (var p : java.util.List.of(new GraphicSpec.Point(Double.NaN, 0, 0), new GraphicSpec.Point(0, Double.NaN, 0), new GraphicSpec.Point(0, 0, Double.NaN))) {
            assertThatThrownBy(() -> service.build(GraphicSpec.GraphicType.CURVA_2D, "x", domain, java.util.List.of(p))).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> service.build(GraphicSpec.GraphicType.CURVA_2D, "x", domain, java.util.Collections.nCopies(101, new GraphicSpec.Point(0, 0, 0)))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.build(GraphicSpec.GraphicType.CURVA_2D, "x", domain, java.util.Arrays.asList((GraphicSpec.Point)null))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void acceptsExactDomainResolutionAndPointLimits() {
        when(cas.parse("x")).thenReturn(new CasEngine.Expr("x","x",null));
        for (int steps:new int[]{2,200})
            assertThat(service.build(GraphicSpec.GraphicType.CURVA_2D,"x",new GraphicSpec.Domain(-10000,10000,steps),java.util.Collections.nCopies(100,new GraphicSpec.Point(0,0,0))).points()).hasSize(100);
    }
}
