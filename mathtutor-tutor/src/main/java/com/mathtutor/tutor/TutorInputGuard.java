package com.mathtutor.tutor;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** @brief Detecta peticiones explícitas de ocultar las reglas del tutor o revelar una solución. */
final class TutorInputGuard {
    static final String REPLY = "Puedo ayudarte con una pista. Indica qué has intentado o qué paso te genera dudas.";

    private static final Pattern MARKS = Pattern.compile("[\\p{M}\\p{Cf}]+");
    private static final Pattern BLOCKED = Pattern.compile(
            "\\b(?:ignora(?:r)?|olvida|omite|desobedece|ignore|disregard|forget)\\s+"
            + "(?:(?:todas?|todos?|las?|los?|tus|mis|the|all|your|previous|prior|anteriores)\\s+){0,6}"
            + "(?:instrucciones?|reglas?|restricciones?|politicas?|instructions?|rules?|constraints?)\\b"
            + "|\\b(?:prompt|mensaje|instrucciones)\\s+(?:interno|oculto|del sistema)\\b|\\bsystem prompt\\b"
            + "|\\b(?:dame|dime|muestra(?:me)?|revela(?:me)?|escribe|entrega|devuelve|give|show|reveal)\\s+"
            + "(?:(?:solo|directamente|toda|la|el|me|just|the|full|final|complete)\\s+){0,5}"
            + "(?:solucion|respuesta|resultado|solution|answer)\\s+(?:complet[oa]|final|exact[oa])\\b"
            + "|\\b(?:dame|dime|revela(?:me)?|muestra(?:me)?)\\s+(?:solo\\s+)?(?:la solucion|la respuesta)\\s*$"
            + "|\\b(?:solve|resuelve)\\b.{0,160}\\b(?:sin pistas|sin explicar|sin explicacion|just the answer)\\b");

    private TutorInputGuard() {}

    /** @brief Comprueba el texto normalizado antes de consultar el CAS, los apuntes o el generador. */
    static boolean rejected(String message) {
        String normalized = Normalizer.normalize(message, Normalizer.Form.NFKD).toLowerCase(Locale.ROOT);
        normalized = MARKS.matcher(normalized).replaceAll("").replaceAll("[\\p{Z}\\s]+", " ")
                .replaceFirst("[.?!]+\\s*$", "").strip();
        return BLOCKED.matcher(normalized).find();
    }
}
