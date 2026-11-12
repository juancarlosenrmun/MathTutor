package com.mathtutor.cas;

import com.mathtutor.common.dto.ErrorType;
import com.mathtutor.common.port.CasEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Additional tests for SymjaCasEngine to cover catch blocks.
 * Uses mocking to trigger exception paths that are defensive in the real implementation.
 */
@ExtendWith(MockitoExtension.class)
class SymjaCasEngineCatchBlockTest {

    @Mock
    private LatexToSymjaConverter mockConverter;

    private SymjaCasEngine engine;

    @Test
    @DisplayName("verify handles exception from converter.areEquivalentThreeState")
    void verifyHandlesConverterException() {
        // Given
        engine = new SymjaCasEngine(mockConverter);
        when(mockConverter.areEquivalentThreeState(anyString(), anyString())).thenThrow(new RuntimeException("Symja error"));

        // When
        CasEngine.Verification result = engine.verify("x^2", "x^2");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.correct()).isFalse();
        assertThat(result.errorType()).isEqualTo(ErrorType.OTRO);
        assertThat(result.hint()).contains("Error de evaluación: Symja error");
    }

    @Test
    @DisplayName("parse handles exception from converter.parseLatex")
    void parseHandlesParseLatexException() {
        // Given
        engine = new SymjaCasEngine(mockConverter);
        // convertToSymja is called OUTSIDE try-catch, so it must not throw
        // Mock it to return a normalized value (simulating successful conversion)
        when(mockConverter.convertToSymja(anyString())).thenReturn("normalized-x^2");
        // parseLatex is called INSIDE try-catch - mock it to throw
        when(mockConverter.parseLatex(anyString())).thenThrow(new RuntimeException("Parse error"));

        // When
        CasEngine.Expr result = engine.parse("x^2");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.original()).isEqualTo("x^2");
        // convertToSymja returned normalized value
        assertThat(result.normalized()).isEqualTo("normalized-x^2");
        // parseLatex throws, caught and ignored, so ast remains null (current behavior)
        assertThat(result.ast()).isNull();
    }

    @Test
    @DisplayName("analyzeDifference handles exception in numeric evaluation")
    void analyzeDifferenceHandlesNumericEvaluationException() {
        // Given
        engine = new SymjaCasEngine(mockConverter);
        // Make converter throw on parseLatex for analyzeDifference
        when(mockConverter.parseLatex(anyString())).thenThrow(new RuntimeException("Evaluation error"));
        when(mockConverter.areEquivalentThreeState(anyString(), anyString())).thenReturn(EquivalenceResult.NO_EQUIVALENTE);

        // When - verify with non-equivalent expressions triggers analyzeDifference
        CasEngine.Verification result = engine.verify("x^2", "x^3");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.correct()).isFalse();
        assertThat(result.errorType()).isEqualTo(ErrorType.OTRO);
        // The hint should contain the error message from the catch block
        assertThat(result.hint()).isNotNull();
    }

    @Test
    @DisplayName("analyzeDifference handles generic exception in analyzeDifference logic")
    void analyzeDifferenceHandlesGenericException() {
        // Given
        engine = new SymjaCasEngine(mockConverter);
        // Make areEquivalent return false to enter analyzeDifference
        when(mockConverter.areEquivalentThreeState(anyString(), anyString())).thenReturn(EquivalenceResult.NO_EQUIVALENTE);
        // Make parseLatex throw exception in analyzeDifference
        when(mockConverter.parseLatex(anyString())).thenThrow(new RuntimeException("Parse error in analyze"));

        // When
        CasEngine.Verification result = engine.verify("x^2", "x^3");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.correct()).isFalse();
        assertThat(result.errorType()).isEqualTo(ErrorType.OTRO);
        assertThat(result.hint()).isNotNull();
    }
}