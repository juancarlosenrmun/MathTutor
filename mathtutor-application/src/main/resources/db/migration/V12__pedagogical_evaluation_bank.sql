-- EN-05: al menos diez ejercicios distintos de banco en las categorías A y C.
INSERT INTO exercises (external_id, type, difficulty, statement_latex, reference_solution_latex, module_id, topic_id)
SELECT source.external_id, source.type, source.difficulty, source.statement, source.solution, t.module_id, t.id
FROM (VALUES
  ('FUN-PROC-001', 'PROCEDIMENTAL', 2, 'Calcula el cuadrado de la unidad imaginaria.', '-1', 'numeros-reales-complejos'),
  ('FUN-PROC-002', 'PROCEDIMENTAL', 2, 'Calcula el módulo de 5+12i.', '13', 'numeros-reales-complejos'),
  ('FUN-PROC-003', 'PROCEDIMENTAL', 2, 'Calcula el límite de 3x+2 cuando x tiende a 5.', '17', 'funciones-continuidad'),
  ('CAL-PROC-002', 'PROCEDIMENTAL', 2, 'Calcula la derivada de 7x^3.', '21*x^2', 'calculo-diferencial'),
  ('CAL-PROC-003', 'PROCEDIMENTAL', 2, 'Calcula la derivada de 4x^5.', '20*x^4', 'calculo-diferencial'),
  ('CAL-PROC-004', 'PROCEDIMENTAL', 2, 'Calcula la integral definida de 6x entre 0 y 3.', '27', 'calculo-integral'),
  ('3D-PROC-002', 'PROCEDIMENTAL', 2, 'Calcula el determinante de la matriz diagonal con entradas 7 y 5.', '35', 'matrices'),
  ('3D-PROC-003', 'PROCEDIMENTAL', 2, 'Calcula el producto escalar de (2,3,1) y (1,-1,4).', '3', 'geometria'),
  ('FUN-TEO-002', 'TEORICO', 2, 'Define el módulo de un número complejo.', 'El módulo de a+bi es la raíz cuadrada de la suma de los cuadrados de a y b.', 'numeros-reales-complejos'),
  ('FUN-TEO-003', 'TEORICO', 2, 'Enuncia el teorema del valor intermedio.', 'Una función continua en un intervalo cerrado toma todos los valores comprendidos entre los valores de sus extremos.', 'funciones-continuidad'),
  ('CAL-TEO-002', 'TEORICO', 2, 'Define la derivada en un punto mediante un límite.', 'La derivada es el límite del cociente entre el incremento de la función y el incremento de la variable cuando este último tiende a cero.', 'calculo-diferencial'),
  ('CAL-TEO-003', 'TEORICO', 2, 'Explica qué condición convierte dos primitivas en la misma salvo una constante.', 'En un intervalo dos primitivas de la misma función difieren en una constante porque su diferencia tiene derivada nula.', 'calculo-integral'),
  ('3D-TEO-002', 'TEORICO', 2, 'Enuncia el criterio de invertibilidad de una matriz cuadrada mediante el determinante.', 'Una matriz cuadrada es invertible si y solo si su determinante es distinto de cero.', 'matrices'),
  ('3D-TEO-003', 'TEORICO', 2, 'Define una base de un espacio vectorial.', 'Una base es un conjunto de vectores linealmente independientes que genera el espacio vectorial.', 'espacios-vectoriales'),
  ('3D-TEO-004', 'TEORICO', 2, 'Explica qué debe aportar una base para diagonalizar una matriz.', 'Una matriz es diagonalizable si existe una base del espacio formada por vectores propios de la matriz.', 'diagonalizacion')
) AS source(external_id, type, difficulty, statement, solution, topic_code)
JOIN topics t ON t.code = source.topic_code
ON CONFLICT (external_id) DO NOTHING;
