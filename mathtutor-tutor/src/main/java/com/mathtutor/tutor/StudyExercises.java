package com.mathtutor.tutor;

import com.mathtutor.common.port.CasEngine;
import com.mathtutor.common.port.ConversationStore;
import org.springframework.stereotype.Service;

/** Selección explícita de objetivo, sin reconocimiento automático del banco (S6). */
@Service
public class StudyExercises {
    private final ConversationStore store;
    private final TutorSessions sessions;
    private final CasEngine cas;
    public StudyExercises(ConversationStore store,TutorSessions sessions,CasEngine cas) { this.store=store; this.sessions=sessions; this.cas=cas; }
    public ConversationStore.Run select(Long user,Long session,Long exercise,String operation,String expression) {
        sessions.active(user,session);
        if (exercise!=null) {
            if (operation!=null || expression!=null) throw new IllegalArgumentException("Elige banco u operación libre");
            return store.select(user,session,exercise,null,null);
        }
        if (expression==null || expression.isBlank() || expression.length()>200 || !("DERIVADA".equals(operation) || "SIMPLIFICAR".equals(operation)))
            throw new IllegalArgumentException("Operación libre no admitida");
        var parsed=cas.parse(expression);
        if (parsed==null || parsed.normalized()==null || parsed.normalized().isBlank()) throw new IllegalArgumentException("Expresión no interpretable");
        String reference="DERIVADA".equals(operation) ? cas.derive(expression,"x") : expression;
        if (reference==null || reference.startsWith("No se pudo")) throw new IllegalArgumentException("No se puede calcular la referencia");
        return store.select(user,session,null,operation+": "+expression,reference);
    }
}
