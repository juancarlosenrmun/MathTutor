package com.mathtutor.common.port;

import com.mathtutor.common.dto.ErrorType;
import com.mathtutor.common.dto.HelpLevel;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Persistencia de S4. Cada operación termina su transacción antes de invocar al proveedor. */
public interface ConversationStore {
    Entry reserve(Long userId, Long sessionId, UUID requestId, String message);
    void delete(Long userId, Long sessionId);
    void finish(Long userId, Long sessionId, UUID requestId, Result result);
    List<Entry> context(Long userId, Long sessionId);
    List<Entry> history(Long userId, Long sessionId, int page, int size);
    List<Session> sessions(Long userId, int page, int size);
    List<Run> runs(Long userId, Long sessionId, int page, int size);
    Run select(Long userId, Long sessionId, Long exerciseId, String statement, String reference);
    Run current(Long userId, Long sessionId);
    List<BankExercise> bank(Long userId, Long sessionId, int page, int size);

    /** @brief Lee el punto de partida de la práctica y bloquea la sesión durante la selección. */
    Progression progression(Long userId, Long sessionId);

    record Progression(Run active, List<BankExercise> candidates, Set<String> practicedTypes) {}

    /** @brief Vincula un ejercicio reconocido al turno reservado cuando no hay objetivo activo. */
    Run attachRecognized(Long userId, Long sessionId, UUID requestId, Long exerciseId);

    void attachFree(Long userId, Long sessionId, UUID requestId, String message, String topic);

    record BankExercise(Long id, String statement, String topic, String type, int difficulty, String externalId) {
        public BankExercise(Long id, String statement, String topic, String type, int difficulty) {
            this(id, statement, topic, type, difficulty, null);
        }
    }

    record Entry(Long id, UUID requestId, String message, String response, String module, String topic,
                 ErrorType diagnosis, String helpLevel, long latencyMs, Integer tokensIn, Integer tokensOut,
                 String status, Instant at, Long runId, String verdict) {
        public Entry(Long id, UUID requestId, String message, String response, String module, String topic,
                     ErrorType diagnosis, String helpLevel, long latencyMs, Integer tokensIn, Integer tokensOut,
                     String status, Instant at, Long runId) {
            this(id,requestId,message,response,module,topic,diagnosis,helpLevel,latencyMs,tokensIn,tokensOut,status,at,runId,null);
        }
    }
    record Result(String response, String topic, ErrorType diagnosis, String status, long latencyMs,
                  Integer tokensIn, Integer tokensOut, int verifiedSteps, boolean completed, String verdict,
                  HelpLevel helpLevel, boolean attempted) {
        public Result(String response, String topic, ErrorType diagnosis, String status, long latencyMs,
                      Integer tokensIn, Integer tokensOut, int verifiedSteps, boolean completed, String verdict) {
            this(response, topic, diagnosis, status, latencyMs, tokensIn, tokensOut, verifiedSteps, completed,
                    verdict, HelpLevel.CONCEPTUAL, true);
        }

        public Result(String response, String topic, ErrorType diagnosis, String status, long latencyMs,
                      Integer tokensIn, Integer tokensOut, int verifiedSteps, boolean completed) {
            this(response,topic,diagnosis,status,latencyMs,tokensIn,tokensOut,verifiedSteps,completed,"NO_APLICA");
        }
    }
    record Session(Long id, String module, String status, Instant startedAt, Instant lastActivityAt) {}
    /** Referencia y pasos son datos internos: nunca devolver Run directamente en una API pública. */
    record Run(Long id, Long exerciseId, String statement, String reference, String topic, String state,
               double progress, int verifiedSteps, List<String> steps, int attempts,
               String bankReference, List<String> seedHints) {
        public Run(Long id, Long exerciseId, String statement, String reference, String topic, String state,
                   double progress, int verifiedSteps, List<String> steps, int attempts) {
            this(id, exerciseId, statement, reference, topic, state, progress, verifiedSteps, steps, attempts,
                    reference, List.of());
        }
        public Run(Long id, Long exerciseId, String statement, String reference, String topic, String state,
                   double progress, int verifiedSteps, List<String> steps) {
            this(id, exerciseId, statement, reference, topic, state, progress, verifiedSteps, steps, 0);
        }
    }
}
