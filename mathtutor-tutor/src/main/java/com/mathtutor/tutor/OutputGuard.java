package com.mathtutor.tutor;

import com.mathtutor.common.port.CasEngine;
import org.springframework.lang.Nullable;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** @brief UC-19. Contrasta la respuesta completa con la referencia o los pasajes resueltos. */
final class OutputGuard {
    private static final Pattern FINAL_ANSWER = Pattern.compile(
            "\\b(?:solucion|resultado|respuesta)\\s+(?:final|complet[ao])\\b|"
                    + "\\b(?:la solucion|el resultado|la respuesta)\\s*(?:es\\b|:)");
    private static final Pattern WORKED_PASSAGE = Pattern.compile(
            "(?is)\\\\begin\\{(solution|solucion|example)\\}(?:\\[[^\\]\\n]*\\])?(.*?)\\\\end\\{\\1\\}");
    private static final Pattern MATH = Pattern.compile(
            "\\$\\$?([^$\\n]{1,240})\\$\\$?|\\\\[\\[(]([^\\n]{1,240}?)\\\\[\\])]"
                    + "|(?:=|\\b(?:queda|obtenemos|es)\\h+)\\h*([\\p{Alnum}+*/^(){}\\\\.\\- ]{1,240})");
    private static final Pattern SHORT_NUMBER = Pattern.compile("[+-]?\\d+(?:[.,]\\d+)?");
    private static final int MAX_COMPARISONS = 12;
    private final CasEngine cas;

    OutputGuard(@Nullable CasEngine cas) {
        this.cas = cas;
    }

    /**
     * @param response Texto íntegro del proveedor, antes de persistirlo o enviarlo.
     * @param reference Referencia simbólica opcional del ejercicio activo.
     * @param notes Fragmentos recuperados para este turno.
     * @return Si puede publicarse como pista.
     */
    boolean safe(String response, @Nullable String reference, List<String> notes) {
        String prose = plain(response);
        if (FINAL_ANSWER.matcher(prose).find()) {
            return false;
        }
        if (reference == null || reference.isBlank()) {
            return notes.stream().noneMatch(note -> copiesWorkedPassage(prose, note));
        }
        String literal = compact(reference);
        // Los números requieren límites explícitos para que 42 no coincida con 142.
        if (SHORT_NUMBER.matcher(literal).matches()) {
            if (Pattern.compile("(?<![\\p{Alnum}.,])" + Pattern.quote(literal)
                    + "(?![\\p{Alnum}]|[.,]\\d)").matcher(response).find()) {
                return false;
            }
        } else if (compact(response).contains(literal)) {
            return false;
        }
        var matches = MATH.matcher(response);
        for (int count = 0; matches.find(); count++) {
            if (count == MAX_COMPARISONS) {
                return false;
            }
            String expression = matches.group(1) != null ? matches.group(1)
                    : matches.group(2) != null ? matches.group(2) : matches.group(3);
            String candidate = expression.substring(expression.lastIndexOf('=') + 1).strip();
            if (candidate.endsWith(".")) {
                candidate = candidate.substring(0, candidate.length() - 1);
            }
            if (cas != null && cas.compare(candidate, reference) == CasEngine.Comparison.EQUIVALENT) {
                return false;
            }
        }
        return true;
    }

    private static boolean copiesWorkedPassage(String response, String note) {
        var passages = WORKED_PASSAGE.matcher(note);
        while (passages.find()) {
            for (String line : passages.group(2).split("\\R")) {
                String solution = plain(line);
                if (solution.length() >= 24 && response.contains(solution)) {
                    return true;
                }
            }
        }
        // También cubre apuntes convertidos a texto que identifican la resolución con un rótulo.
        for (String line : note.split("\\R")) {
            String solution = plain(line);
            var label = FINAL_ANSWER.matcher(solution);
            if (label.find()) {
                String body = solution.substring(label.end()).replaceFirst("^\\s*[:=]\\s*", "").strip();
                if (body.length() >= 12 && response.contains(body)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String plain(String text) {
        return Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}|[*_\u0060]", "").replaceAll("\\s+", " ").strip();
    }

    private static String compact(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFKC)
                .replace('−', '-').replace('·', '*').replace('×', '*')
                .replaceAll("[\\s*$]", "");
    }
}
