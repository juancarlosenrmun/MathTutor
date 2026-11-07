package com.mathtutor.tutor;

import com.mathtutor.common.port.PedagogicalPolicy;
import com.mathtutor.common.dto.ExerciseRun;
import com.mathtutor.common.dto.ErrorType;
import com.mathtutor.common.dto.HelpLevel;
import com.mathtutor.common.dto.PedagogicalAction;
import org.springframework.stereotype.Component;

/**
 * Esqueleto de implementación de la política pedagógica determinista.
 * Implementación vacía preparada para el orquestador (Sprint 5).
 * No usa orquestador ni política compleja - solo define la estructura.
 */
@Component
public class PedagogicalPolicyImpl implements PedagogicalPolicy {

    @Override
    public PedagogicalAction decide(ExerciseRun exerciseRun, ErrorType diagnosis, int attempts) {
        // TODO: Implementar en Sprint 5 con orquestador determinista
        // Por ahora devuelve acción básica según intentos usando los valores reales de HelpLevel
        if (attempts == 0) {
            return new PedagogicalAction(HelpLevel.CONCEPTUAL, "Intenta resolver el ejercicio paso a paso.", false, null);
        } else if (attempts == 1) {
            return new PedagogicalAction(HelpLevel.PROCEDIMENTAL, "Revisa el primer paso de la solución.", false, null);
        } else {
            return new PedagogicalAction(HelpLevel.REVELACION, "La solución completa se muestra tras múltiples intentos.", true, null);
        }
    }
}