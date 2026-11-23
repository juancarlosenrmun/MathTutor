-- EN-05: pistas de apertura vinculadas a errores frecuentes del banco teórico.
WITH hints(external_id, step, error_code, hint) AS (
  VALUES
    ('FUN-TEO-001', 'Separar el valor puntual del comportamiento al acercarse.',
     'CONFUNDIR_VALOR_Y_LIMITE', '¿Qué dos cantidades conviene anotar por separado al estudiar una función cerca del punto a?'),
    ('FUN-TEO-002', 'Representar el complejo como un punto del plano.',
     'CONFUNDIR_MODULO_CON_PARTE_REAL', '¿Cómo dibujarías a+bi en unos ejes y dónde situarías el origen respecto a ese punto?'),
    ('FUN-TEO-003', 'Identificar condiciones sobre el intervalo.',
     'OMITIR_HIPOTESIS_DEL_INTERVALO', '¿Qué hipótesis tendría que cumplir la gráfica en el intervalo para que no pueda saltarse valores?'),
    ('CAL-TEO-001', 'Distinguir extremo fijo y extremo variable.',
     'DERIVAR_RESPECTO_A_VARIABLE_INTERNA', 'Al escribir una integral con extremo superior x, ¿qué símbolo cambia al derivar y cuál está dentro de la integral?'),
    ('CAL-TEO-002', 'Comparar incrementos en la gráfica.',
     'CONFUNDIR_SECANTE_CON_TANGENTE', 'Si dibujas dos puntos cercanos de la gráfica, ¿qué cociente mide la pendiente de la recta que pasa por ambos?'),
    ('CAL-TEO-003', 'Considerar la diferencia entre las primitivas.',
     'OLVIDAR_CONSTANTE_DE_INTEGRACION', 'Si F y G tienen la misma derivada en un intervalo, ¿qué obtienes al derivar su diferencia?'),
    ('3D-TEO-001', 'Plantear una combinación lineal con tres coeficientes.',
     'CONFUNDIR_INDEPENDENCIA_CON_NO_NULIDAD', '¿Qué ecuación escribirías para comprobar si una combinación de los tres vectores puede producir el vector cero?'),
    ('3D-TEO-002', 'Relacionar invertibilidad con el sistema homogéneo.',
     'USAR_TRAZA_EN_VEZ_DE_DETERMINANTE', '¿Qué información sobre las soluciones de Ax=0 buscarías para decidir si A puede tener inversa?'),
    ('3D-TEO-003', 'Examinar si faltan direcciones o sobran vectores.',
     'CONFUNDIR_GENERADOR_CON_BASE', 'Dado un conjunto de vectores, ¿cómo comprobarías si con sus combinaciones puedes alcanzar cualquier vector del espacio?'),
    ('3D-TEO-004', 'Observar qué ocurre con los vectores en las nuevas coordenadas.',
     'ELEGIR_BASE_SIN_EXAMINAR_LA_MATRIZ', 'Cuando una matriz es diagonal, ¿qué ocurre con las direcciones de los ejes al aplicar la transformación?')
), saved_steps AS (
  INSERT INTO canonical_steps (position, expression_latex, exercise_id)
  SELECT 1, h.step, e.id
  FROM hints h
  JOIN exercises e ON e.external_id = h.external_id AND e.type = 'TEORICO'
  RETURNING id, exercise_id
)
INSERT INTO typical_errors (code, seed_hint, canonical_step_id)
SELECT h.error_code, h.hint, s.id
FROM saved_steps s
JOIN exercises e ON e.id = s.exercise_id
JOIN hints h ON h.external_id = e.external_id;
