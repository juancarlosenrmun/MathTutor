-- UC-22: banco orientativo versionado junto al código; IDs externos estables.
-- Los temas corresponden al manifiesto de apuntes de Industriales.
INSERT INTO topics (code, name, difficulty, module_id)
SELECT source.code, source.name, source.difficulty, m.id
FROM (VALUES
  ('numeros-reales-complejos', 'Números reales y complejos', 1, 'funciones'),
  ('funciones-continuidad', 'Funciones y continuidad', 2, 'funciones'),
  ('calculo-diferencial', 'Cálculo diferencial', 2, 'derivadas-integrales'),
  ('calculo-integral', 'Cálculo integral', 3, 'derivadas-integrales'),
  ('matrices', 'Matrices', 2, 'metodos-3d'),
  ('espacios-vectoriales', 'Espacios vectoriales', 3, 'metodos-3d'),
  ('diagonalizacion', 'Diagonalización', 4, 'metodos-3d'),
  ('geometria', 'Geometría', 2, 'metodos-3d')
) AS source(code, name, difficulty, module_code)
JOIN modules m ON m.code = source.module_code
ON CONFLICT (code) DO NOTHING;

INSERT INTO exercises (external_id, type, difficulty, statement_latex, reference_solution_latex, module_id, topic_id)
SELECT source.external_id, source.type, source.difficulty, source.statement, source.solution, m.id, t.id
FROM (VALUES
  ('FUN-TEO-001', 'TEORICO', 2, 'Define continuidad de f en a.',
   'f es continua en a si existe el límite y \lim_{x\to a}f(x)=f(a).', 'funciones-continuidad'),
  ('FUN-INT-001', 'INTERPRETACION', 1, 'Determina el dominio real de \sqrt{x}.',
   '[0,\infty)', 'numeros-reales-complejos'),
  ('CAL-TEO-001', 'TEORICO', 2, 'Enuncia la relación entre derivada e integral definida.',
   'Si F(x)=\int_a^x f(t)dt y f es continua, entonces F''(x)=f(x).', 'calculo-integral'),
  ('CAL-PROC-001', 'PROCEDIMENTAL', 2, 'Calcula la derivada de f(x)=x^2.',
   'f''(x)=2x', 'calculo-diferencial'),
  ('3D-TEO-001', 'TEORICO', 3, 'Define independencia lineal de tres vectores.',
   'v_1,v_2,v_3 son independientes si c_1v_1+c_2v_2+c_3v_3=0 implica c_1=c_2=c_3=0.', 'espacios-vectoriales'),
  ('3D-PROC-001', 'PROCEDIMENTAL', 2, 'Calcula e_1 \times e_2.',
   'e_3', 'geometria')
) AS source(external_id, type, difficulty, statement, solution, topic_code)
JOIN topics t ON t.code = source.topic_code
JOIN modules m ON m.id = t.module_id
ON CONFLICT (external_id) DO NOTHING;

INSERT INTO canonical_steps (position, expression_latex, equivalent_forms, exercise_id)
SELECT source.position, source.expression, source.forms, e.id
FROM (VALUES
  ('CAL-PROC-001', 1, '\frac{d}{dx}x^2=2x^{2-1}', '["2x"]'),
  ('CAL-PROC-001', 2, '2x', '["x+x"]'),
  ('3D-PROC-001', 1, 'e_1\times e_2', '["(1,0,0)\\times(0,1,0)"]'),
  ('3D-PROC-001', 2, 'e_3', '["(0,0,1)"]')
) AS source(external_id, position, expression, forms)
JOIN exercises e ON e.external_id = source.external_id;

INSERT INTO typical_errors (code, seed_hint, canonical_step_id)
SELECT source.code, source.hint, s.id
FROM (VALUES
  ('CAL-PROC-001', 2, 'POTENCIA_SIN_EXPONENTE', 'Revisa el exponente al aplicar la regla de la potencia.'),
  ('3D-PROC-001', 2, 'ORIENTACION_CRUZ', 'Comprueba el orden de los vectores con la regla de la mano derecha.')
) AS source(external_id, position, code, hint)
JOIN exercises e ON e.external_id = source.external_id
JOIN canonical_steps s ON s.exercise_id = e.id AND s.position = source.position;
