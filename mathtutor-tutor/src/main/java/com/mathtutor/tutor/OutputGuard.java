package com.mathtutor.tutor;

import com.mathtutor.common.port.CasEngine;
import org.springframework.lang.Nullable;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
    private static final Pattern FUNCTION_LABEL = Pattern.compile("^[a-zA-Z]\\s*'*(?:\\([a-zA-Z]\\))\\s*=\\s*");
    private static final Pattern ALGEBRA = Pattern.compile("[a-zA-Z0-9_+*/^().\\-]+");
    private static final Pattern WORD = Pattern.compile("\\p{L}{2,}");
    private static final Set<String> SYMBOLIC_WORDS = Set.of("sin", "sen", "cos", "tan", "cot", "sec", "csc",
            "asin", "acos", "atan", "arcsin", "arccos", "arctan", "sinh", "cosh", "tanh",
            "log", "ln", "exp", "sqrt", "frac", "dfrac", "tfrac", "cdot", "times", "left", "right",
            "pi", "theta", "alpha", "beta", "gamma", "infty", "lim", "abs");
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
        if (FINAL_ANSWER.matcher(prose).find() || notes.stream().anyMatch(note -> copiesWorkedPassage(prose, note))) {
            return false;
        }
        if (reference == null || reference.isBlank()) {
            return true;
        }
        String answer = FUNCTION_LABEL.matcher(reference.strip()).replaceFirst("");
        String literal = compact(answer);
        if (!symbolic(answer)) {
            return !compact(response).contains(literal);
        }
        // Los números requieren límites explícitos para que 42 no coincida con 142.
        if (SHORT_NUMBER.matcher(literal).matches()) {
            if (Pattern.compile("(?<![\\p{Alnum}.,])" + Pattern.quote(literal)
                    + "(?![\\p{Alnum}]|[.,]\\d)").matcher(response).find()) {
                return false;
            }
        } else if (ALGEBRA.matcher(answer).matches()) {
            String math = normalizeMath(response).replaceAll("\\h*\\*\\h*", "");
            if (Pattern.compile("(?<![\\p{Alnum}_^])" + Pattern.quote(literal)
                    + "(?![\\p{Alnum}_^]|[.,]\\d)").matcher(math).find()) {
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
            if (compact(candidate).equals(literal)
                    || cas != null && symbolic(candidate) && cas.compare(candidate, answer) == CasEngine.Comparison.EQUIVALENT) {
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
        return normalizeMath(text).replaceAll("[\\s*$]", "");
    }

    /** @brief Mantiene los fragmentos de prosa fuera del simplificador del CAS. */
    private static boolean symbolic(String text) {
        return WORD.matcher(text).results()
                .allMatch(word -> SYMBOLIC_WORDS.contains(word.group().toLowerCase(Locale.ROOT)));
    }

    private static String normalizeMath(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFKC)
                .replace('−', '-').replace('·', '*').replace('×', '*');
    }
}
