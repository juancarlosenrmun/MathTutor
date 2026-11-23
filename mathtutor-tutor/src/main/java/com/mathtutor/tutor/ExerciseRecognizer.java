package com.mathtutor.tutor;

import com.mathtutor.common.port.CasEngine;
import com.mathtutor.common.port.ConversationStore.BankExercise;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** @brief Reconoce ejercicios del módulo mediante identificadores y equivalencia determinista. */
@Component
public class ExerciseRecognizer {
    private static final Pattern SPACES = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern EXPRESSION_STATEMENT = Pattern.compile(
            "(calcula la derivada de|determina el dominio real de)\\s+(?:f\\s*\\(x\\)\\s*=\\s*)?(.{1,150})");
    private final CasEngine cas;

    public ExerciseRecognizer(CasEngine cas) {
        this.cas = cas;
    }

    /**
     * @brief Da prioridad al identificador explícito. Un enunciado ambiguo sigue como consulta libre.
     * @param message Mensaje del alumno.
     * @param bank Catálogo limitado al módulo activo.
     * @return Identificador interno del único ejercicio reconocido.
     */
    public Optional<Long> recognize(String message, List<BankExercise> bank) {
        String query = normalize(message);
        String id = query.replaceFirst("^ejercicio\\s+", "");
        var identified = bank.stream().filter(e -> e.id().toString().equals(id)
                || e.externalId() != null && e.externalId().equalsIgnoreCase(id)).findFirst();
        if (identified.isPresent()) return identified.map(BankExercise::id);

        var matches = bank.stream().filter(e -> matches(query, normalize(e.statement()))).limit(2).toList();
        return matches.size() == 1 ? Optional.of(matches.getFirst().id()) : Optional.empty();
    }

    private boolean matches(String query, String statement) {
        if (Objects.equals(query, statement)) return true;
        var left = EXPRESSION_STATEMENT.matcher(query);
        var right = EXPRESSION_STATEMENT.matcher(statement);
        return left.matches() && right.matches() && left.group(1).equals(right.group(1))
                && cas.compare(left.group(2), right.group(2)) == CasEngine.Comparison.EQUIVALENT;
    }

    private String normalize(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD).toLowerCase(Locale.ROOT);
        return SPACES.matcher(MARKS.matcher(normalized).replaceAll("").strip()).replaceAll(" ")
                .replaceFirst("^[¿¡]", "").replaceFirst("[.?!]$", "").strip();
    }
}
