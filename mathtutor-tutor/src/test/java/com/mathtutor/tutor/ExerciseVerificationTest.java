package com.mathtutor.tutor;

import com.mathtutor.common.dto.ErrorType;
import com.mathtutor.common.port.CasEngine;
import com.mathtutor.common.port.ConversationStore.Run;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExerciseVerificationTest {
    final CasEngine cas=mock(CasEngine.class);
    final ExerciseVerification verifier=new ExerciseVerification(cas);
    Run run(int verified) { return new Run(1L,1L,"deriva", "2*x","tema","COMENZADO",0,verified,List.of("2*x^1","2*x")); }
    @Test void verifiesResultAndSignWithoutDisclosingReference() {
        when(cas.compare("2*x","2*x")).thenReturn(CasEngine.Comparison.EQUIVALENT);
        assertThat(verifier.verify("2*x",run(0)).completed()).isTrue();
        when(cas.compare("-2*x","2*x")).thenReturn(CasEngine.Comparison.DIFFERENT);
        when(cas.compare("-2*x","2*x^1")).thenReturn(CasEngine.Comparison.DIFFERENT);
        when(cas.compare("-(-2*x)","2*x")).thenReturn(CasEngine.Comparison.EQUIVALENT);
        var result=verifier.verify("-2*x",run(0));
        assertThat(result.diagnosis()).isEqualTo(ErrorType.SIGNO);
        assertThat(result.hint()).doesNotContain("2*x");
    }
    @Test void advancesOnlyTheNextCanonicalStep() {
        when(cas.compare("x+x","2*x")).thenReturn(CasEngine.Comparison.DIFFERENT);
        when(cas.compare("x+x","2*x^1")).thenReturn(CasEngine.Comparison.EQUIVALENT);
        assertThat(verifier.verify("x+x",run(0)).verifiedSteps()).isEqualTo(1);
    }
    @Test void uncertaintyAndTheoryNeverCertifyAnError() {
        assertThat(verifier.verify("x",null).verdict()).isEqualTo("NO_APLICA");
        when(cas.compare("x","2*x")).thenReturn(CasEngine.Comparison.UNKNOWN);
        assertThat(verifier.verify("x",run(0)).verdict()).isEqualTo("INDETERMINADO");
        assertThat(verifier.verify("x",new Run(1L,null,"teoría",null,null,"COMENZADO",0,0,List.of())).verdict()).isEqualTo("NO_APLICA");
    }
    @Test void repeatedPreviousStepDoesNotAdvance() {
        when(cas.compare("x+x","2*x")).thenReturn(CasEngine.Comparison.DIFFERENT);
        when(cas.compare("x+x","2*x^1")).thenReturn(CasEngine.Comparison.EQUIVALENT);
        assertThat(verifier.verify("x+x",run(1)).verifiedSteps()).isEqualTo(1);
    }
    @Test void classifiesOtherErrorsWithoutInventingSpecificRules() {
        when(cas.compare(anyString(),anyString())).thenReturn(CasEngine.Comparison.DIFFERENT);
        assertThat(verifier.verify("x",run(0)).diagnosis()).isEqualTo(ErrorType.OTRO);
        var free=new Run(1L,null,"libre","x+1",null,"COMENZADO",0,0,List.of());
        assertThat(verifier.verify("x",free).verdict()).isEqualTo("INCORRECTO");
    }
}
