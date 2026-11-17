package com.mathtutor.tutor;

import com.mathtutor.common.dto.ErrorType;
import com.mathtutor.common.port.CasEngine;
import com.mathtutor.common.port.ConversationStore.Run;
import org.springframework.stereotype.Service;

/** UC-17: diagnóstico conservador; no revela las referencias internas. */
@Service
public class ExerciseVerification {
    private final CasEngine cas;
    public ExerciseVerification(CasEngine cas) { this.cas=cas; }
    public Diagnosis verify(String expression, Run run) {
        if (run==null || run.reference()==null) return new Diagnosis("NO_APLICA",ErrorType.OTRO,"Sin objetivo simbólico verificable",0,false);
        int verified=run.verifiedSteps();
        var comparison=cas.compare(expression,run.reference());
        if (comparison==CasEngine.Comparison.EQUIVALENT)
            return new Diagnosis("CORRECTO",ErrorType.NINGUNO,"Resultado verificado",run.steps().size(),true);
        // Repetir un paso anterior nunca incrementa el progreso.
        for (int i=0;i<verified;i++) {
            if (cas.compare(expression,run.steps().get(i))==CasEngine.Comparison.EQUIVALENT)
                return new Diagnosis("CORRECTO",ErrorType.NINGUNO,"Paso ya verificado",verified,false);
        }
        if (verified<run.steps().size() && cas.compare(expression,run.steps().get(verified))==CasEngine.Comparison.EQUIVALENT)
            return new Diagnosis("CORRECTO",ErrorType.NINGUNO,"Paso verificado",verified+1,false);
        if (comparison!=CasEngine.Comparison.DIFFERENT)
            return new Diagnosis("INDETERMINADO",ErrorType.OTRO,"El CAS no pudo determinar la equivalencia",verified,false);
        if (cas.compare("-("+expression+")",run.reference())==CasEngine.Comparison.EQUIVALENT)
            return new Diagnosis("INCORRECTO",ErrorType.SIGNO,"Revisa el signo",verified,false);
        return new Diagnosis("INCORRECTO",ErrorType.OTRO,"Revisa el paso y la operación aplicada",verified,false);
    }
    public record Diagnosis(String verdict, ErrorType diagnosis, String hint, int verifiedSteps, boolean completed) {}
}
