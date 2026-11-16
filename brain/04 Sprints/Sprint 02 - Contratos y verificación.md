# Sprint 02 · Contratos y verificación

Enlaces: [[Sprint 02]], [[Sprint 02 - Diseño y plan]], [[Requisitos funcionales]], [[Diagrama de clases]], [[Diagrama de dominio]].

## API del alumno

Todos los endpoints requieren JWT vigente y rol ALUMNO; los POST también requieren CSRF. Ninguna credencial viaja en URL.

| Método y ruta | Entrada | Respuesta |
|---|---|---|
| GET `/api/tutor/modules` | — | Lista de `code`, `name` |
| POST `/api/tutor/sessions` | `moduleCode` | 201: `id`, `moduleCode`, `status` |
| GET `/api/tutor/sessions/{id}` | — | Instantánea propia activa; 404 uniforme si no está disponible |
| POST `/api/tutor/sessions/{id}/messages` | `message`, 1–4000 caracteres no vacíos | `text/event-stream`: datos JSON con `type` y `content` |

Los eventos son TOKEN (delta original), DONE (fin normal) o ERROR (mensaje genérico). El cliente no reconecta ni reenvía turnos automáticamente. Cancelar interrumpe el lector; el gateway cancela la llamada remota. El timeout entre deltas es 30 segundos. No se añade RAG por consulta, CAS conversacional, memoria persistente ni política socrática de sprints posteriores.

## Indexación y contrato de autores

Activar el perfil `notes-indexing` junto al proveedor de embeddings y la infraestructura PostgreSQL. Ese perfil incluye `spring-ai-pgvector`; por ejemplo, con el proveedor ya configurado por entorno: `dev,google-ai-studio,notes-indexing`. El perfil `dev` por sí solo conserva el stub de S1 y no efectúa llamadas externas. Las claves permanecen fuera del repositorio. El test de arranque suministra un modelo determinista en lugar de credenciales.

Cada recurso `mathtutor-application/src/main/resources/notes/*.tex` representa un tema. La versión inicial incluye tres notas introductorias; no constituye el temario completo que aportarán los autores externos.

```latex
\bloque{funciones}
\tema{dominio}{Dominio de una función}
\dificultad{1}
\section{Dominio y valores}
% Texto autocontenido de 150–400 palabras. Fórmulas LaTeX explícitas.
```

Códigos de tema: letras minúsculas, dígitos y guiones, hasta 50 caracteres. Bloques: `funciones`, `derivadas-integrales`, `metodos-3d`. Dificultad: 1, 2 o 3. Los comentarios de línea se eliminan y se conserva el porcentaje escapado. Las secciones y los metadatos se validan antes de escribir. Los recursos solo se cargan del classpath, sin subidas ni ejecución de comandos LaTeX.

El manifiesto se deriva de la fuente, posición y versión. Si coincide con el índice gestionado, se omiten embeddings y escrituras. Si cambia, se generan los embeddings de documentos sin retener JDBC y se reemplaza la instantánea en una transacción. Un fallo conserva los datos anteriores. Las filas de otros procesos no se eliminan.

## Gráficas y límites numéricos

Gramática permitida: `x`, `pi`, `e`, números, paréntesis, `+ - * / ^`, `sin`, `cos`, `tan`, `exp`, `log`, `sqrt`, `abs`. Máximo 200 caracteres y profundidad 32, sin `eval` ni compilación dinámica de shaders. Dominio finito y creciente dentro de ±10000; de 16 a 2048 intervalos, 256 por defecto. Valores mayores de 1000 en magnitud se consideran fuera del rango representable.

La derivada usa diferencias centrales. La integral con signo usa Simpson con comparación entre estimaciones en puntos medios y cuartos; si no es consistente, se informa de muestreo insuficiente. Los intervalos conservadores del AST detectan posibles divisiones por cero y dominios inválidos. Si el argumento de seno o coseno varía más de un cuarto de período por intervalo, se rechaza ese muestreo. El cálculo de extremos está acotado también para argumentos muy grandes. Esto puede omitir segmentos cerca de fronteras de dominio; no sustituye la verificación simbólica. El visor ajusta centro y proyección a función, derivada y área, también al redimensionar y restaurar el contexto WebGL.

## Evidencias reproducibles

- `gradlew.bat build jacocoAggregateReport`: tests, arquitectura, puertas JaCoCo por módulo y empaquetado.
- `gradlew.bat :mathtutor-graphics:clientTest`: Node/V8, umbral 80 % líneas y 70 % ramas; informe `mathtutor-graphics/build/reports/client/lcov.info`.
- `python scripts/verify-integration-results.py`: impide omitir integraciones PostgreSQL/pgvector, incluidas API y arranque de S2.
- `python -m unittest discover -s scripts -p "test_*.py"`: regresiones de la puerta de integración.
- `python scripts/smoke-webgl.py`: Chromium real, píxeles de función/derivada/área y contrato de navegación, sesión y SSE a 1200/375 px.
- `doxygen Doxyfile`: API Java en `build/doxygen/html/index.html`. Los comentarios usan sintaxis Doxygen. Las advertencias heredadas de Result y NoteChunkStore quedan registradas en `build/doxygen-warnings.log`; no proceden de las clases nuevas.

Exclusiones: se mantienen únicamente la clase de arranque `MathTutorApplication`, los DTO sin lógica de `common/dto` y el bootstrap cliente `main.mjs`. No se excluyen servicios, adaptadores, controladores, parser, cliente ni política pedagógica. Las cifras finales y el estado de integración se registran en [[Registro de iteraciones]] de la rama de desarrollo.
