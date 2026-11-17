package com.mathtutor.cas;

import com.mathtutor.common.port.CasEngine;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class CasComparisonTest {
    @Test void preservesAllThreeStatesWithoutNumericalGuessing() {
        var converter=mock(LatexToSymjaConverter.class);
        var engine=new SymjaCasEngine(converter);
        var expected=new CasEngine.Comparison[]{CasEngine.Comparison.EQUIVALENT,CasEngine.Comparison.DIFFERENT,CasEngine.Comparison.UNKNOWN};
        var values=EquivalenceResult.values();
        for (int i=0;i<values.length;i++) {
            when(converter.areEquivalentThreeState("a","b")).thenReturn(values[i]);
            assertThat(engine.compare("a","b")).isEqualTo(expected[i]);
        }
        assertThat(engine.compare(null,"b")).isEqualTo(CasEngine.Comparison.UNKNOWN);
        assertThat(engine.compare("a",null)).isEqualTo(CasEngine.Comparison.UNKNOWN);
    }
}
