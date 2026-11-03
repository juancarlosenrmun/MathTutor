package com.mathtutor.graphics;

import com.mathtutor.common.dto.GraphicSpec;
import com.mathtutor.common.port.GraphicSpecService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de caja blanca para GraphicSpecServiceImpl.
 * Verifica el comportamiento del esqueleto.
 */
class GraphicSpecServiceImplTest {

    private final GraphicSpecService service = new GraphicSpecServiceImpl();

    @Test
    void buildReturnsBasicGraphicSpec() {
        // Given
        GraphicSpec.GraphicType type = GraphicSpec.GraphicType.FUNCTION_2D;
        String function = "x^2";
        GraphicSpec.Domain domain = GraphicSpec.Domain.builder()
            .xMin(-10.0)
            .xMax(10.0)
            .yMin(-5.0)
            .yMax(50.0)
            .build();
        List<GraphicSpec.Point> points = List.of(
            GraphicSpec.Point.builder().x(0.0).y(0.0).label("origin").build()
        );

        // When
        GraphicSpec result = service.build(type, function, domain, points);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(type);
        assertThat(result.getFunction()).isEqualTo(function);
        assertThat(result.getDomain()).isEqualTo(domain);
        assertThat(result.getPoints()).isEqualTo(points);
    }

    @Test
    void buildWithNullPointsReturnsEmptyList() {
        // Given
        GraphicSpec.GraphicType type = GraphicSpec.GraphicType.FUNCTION_2D;
        String function = "x";
        GraphicSpec.Domain domain = GraphicSpec.Domain.builder()
            .xMin(-10.0)
            .xMax(10.0)
            .yMin(-10.0)
            .yMax(10.0)
            .build();

        // When
        GraphicSpec result = service.build(type, function, domain, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPoints()).isEmpty();
    }

    @Test
    void implementsGraphicSpecServiceInterface() {
        // Then
        assertThat(service).isInstanceOf(GraphicSpecService.class);
    }
}