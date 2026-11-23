-- UC-18: los contadores de S4 también incluían preguntas e intentos indeterminados.
WITH verified_attempts AS (
    SELECT run.id, COUNT(turn.id)::integer AS total
    FROM exercise_runs run
    LEFT JOIN conversation_turns turn ON turn.exercise_run_id = run.id
        AND turn.status = 'COMPLETADO' AND turn.verdict IN ('CORRECTO', 'INCORRECTO')
    WHERE run.reference_solution IS NOT NULL
    GROUP BY run.id
)
UPDATE exercise_runs run
SET attempts = verified_attempts.total
FROM verified_attempts
WHERE run.id = verified_attempts.id
    AND run.attempts <> verified_attempts.total;
