# Restricciones y alcance

- Apuntes y banco de ejercicios en LaTeX, cargados desde el código fuente; el profesorado no los sube.
- Módulo temático elegido antes del chat; todos los alumnos acceden a todos los módulos; no existe el concepto de clase.
- Apuntes redactados por personal ajeno al desarrollo (recomendaciones en el Anexo A de la memoria).
- Pasos canónicos y errores típicos solo en ejercicios procedimentales del banco; sin ellos, ejercicio libre.
- Idioma de interfaz y tutor: español.
- Una sola llamada al LLM por turno; flujo decidido por orquestador determinista.
- Sin verificación CAS en preguntas teóricas o no computables: fiabilidad en RAG y control de salida.
- Fuera de alcance: voz, app móvil nativa, evaluación calificable, generación automática de ejercicios.
- Primeros candidatos a recortar: RF-19, RF-18, RF-09.

## Formato de apuntes LaTeX (contrato resumido)
- Un `.tex` por tema; macros de plantilla común (`\bloque{derivadas}`, tema, dificultad 1–3).
- Entornos de plantilla: ejercicio, enunciado, solución, paso, error típico, concepto clave.
- Secciones autocontenidas de 150–400 palabras.
- Expresiones evaluadas: LaTeX explícito (`\cdot`, `\sin(x)`, `\frac{}{}`, `^{}`, `\sqrt{}`), sin macros propias dentro, un paso por expresión.
- Symja convierte fórmulas (ToExpression/TeXForm), no compila LaTeX: el sistema extrae antes entornos y fórmulas del `.tex`.
- Equivalencia por simplificación de la diferencia (posibles falsos negativos).
- Licencia Symja: core, parser y external LGPL; otros módulos GPL → comprobar compatibilidad.
