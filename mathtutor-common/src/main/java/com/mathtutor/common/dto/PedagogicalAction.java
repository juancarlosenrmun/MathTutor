package com.mathtutor.common.dto;

/**
 * Acción pedagógica decidida por la política determinista.
 */
public record PedagogicalAction(
    HelpLevel helpLevel,
    String hint,
    boolean shouldRevealSolution,
    String analogyExample
) {
    public static PedagogicalAction conceptual(String hint) {
        return new PedagogicalAction(HelpLevel.CONCEPTUAL, hint, false, null);
    }

    public static PedagogicalAction procedural(String hint) {
        return new PedagogicalAction(HelpLevel.PROCEDIMENTAL, hint, false, null);
    }

    public static PedagogicalAction analogy(String hint, String analogyExample) {
        return new PedagogicalAction(HelpLevel.EJEMPLO_ANALOGO, hint, false, analogyExample);
    }

    public static PedagogicalAction revelation(String solution) {
        return new PedagogicalAction(HelpLevel.REVELACION, solution, true, null);
    }
}