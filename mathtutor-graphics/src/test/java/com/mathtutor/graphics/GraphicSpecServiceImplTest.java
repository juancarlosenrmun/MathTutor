package com.mathtutor.graphics;

import com.mathtutor.common.dto.GraphicSpec;
import com.mathtutor.common.port.GraphicSpecService;
import org.junit.jupiter.api.Test;

import java.util.Collections;
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
        GraphicSpec.GraphicType type = GraphicSpec.GraphicType.CURVA_2D;
        String function = "x^2";
        GraphicSpec.Domain domain = new GraphicSpec.Domain(-10.0, 10.0, 100);
        List<GraphicSpec.Point> points = List.of(
            new GraphicSpec.Point(0.0, 0.0, 0.0)
        );

        // When
        GraphicSpec result = service.build(type, function, domain, points);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo(type);
        assertThat(result.function()).isEqualTo(function);
        assertThat(result.domain()).isEqualTo(domain);
        assertThat(result.points()).isEqualTo(points);
    }

    @Test
    void buildWithNullPointsReturnsEmptyList() {
        // Given
        GraphicSpec.GraphicType type = GraphicSpec.GraphicType.CURVA_2D;
        String function = "x";
        GraphicSpec.Domain domain = new GraphicSpec.Domain(-10.0, 10.0, 100);

        // When
        GraphicSpec result = service.build(type, function, domain, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.points()).isEqualTo(Collections.emptyList());
    }

    @Test
    void implementsGraphicSpecServiceInterface() {
        // Then
        assertThat(service).isInstanceOf(GraphicSpecService.class);
    }
}