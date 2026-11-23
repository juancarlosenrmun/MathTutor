-- UC-24: completa la etapa de interpretación en los módulos que aún no la ofrecen.
INSERT INTO exercises (external_id, type, difficulty, statement_latex, reference_solution_latex, module_id, topic_id)
SELECT exercise.external_id, 'INTERPRETACION', 2, exercise.statement, exercise.solution, t.module_id, t.id
FROM (VALUES
    ('CAL-INT-001', 'Una función que mide una posición tiene derivada negativa en un instante. ¿Qué indica sobre el movimiento en ese instante?',
     'La posición está disminuyendo localmente. El signo negativo indica el sentido del movimiento respecto al eje elegido.', 'calculo-diferencial'),
    ('3D-INT-001', 'Dos vectores no nulos tienen producto escalar cero. ¿Cómo se interpreta geométricamente?',
     'Los vectores son perpendiculares; el ángulo entre ellos es recto.', 'geometria')
) AS exercise(external_id, statement, solution, topic_code)
JOIN topics t ON t.code = exercise.topic_code
ON CONFLICT (external_id) DO NOTHING;
