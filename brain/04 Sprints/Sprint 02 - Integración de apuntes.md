# Sprint 02 · Integración de apuntes Industriales

Objetivo: incorporar los ocho capítulos de Matemáticas I aportados por el usuario a UC-14 y completar la CI e integración a DEV ya autorizadas.

Diseño: snapshot reproducible de los fuentes originales bajo `notes/sources/industriales`, excluido del glob del indexador. Importador Python sin dependencias externas ni compilación LaTeX, que expande las macros matemáticas conocidas, retira solo formato editorial y segmenta en bloques completos de 150–400 palabras. Ocho recursos generados al nivel de `notes/`, con módulo, tema, dificultad, procedencia y SHA-256 en un manifiesto. Los ejercicios propuestos finales se preservan en las fuentes y quedan fuera del índice de apuntes; no se adelanta el banco de UC-22/S3. El glosario de Mathematica se conserva como referencia auxiliar sin asignarlo artificialmente a un bloque matemático.

Distribución: capítulos 1–2 → funciones; 3–4 → derivadas-integrales; 5–8 → metodos-3d como fundamentos algebraicos y geometría. Se conserva autoría de Diego García Zamora y Nuria V. Castillo González (Universidad de Jaén), según la configuración original.

- [x] Añadir pruebas Python de normalización, comentarios, límites, entornos, segmentación e importación idempotente y observar fallos.
- [x] Implementar importador, snapshot y manifiesto; regenerar ocho recursos y sustituir las tres notas introductorias, sin modificar originales externos.
- [x] Adaptar pruebas Java de arranque al corpus real; verificar todos los temas, metadatos, fórmulas e idempotencia en PostgreSQL/pgvector.
- [x] Ejecutar build, JaCoCo, cliente, Chromium, regresiones Python y revisión independiente; commits pequeños sin cofirma.
- [x] Actualizar PR #9, repetir CI para el nuevo commit, integrar en DEV únicamente con CI verde y registrar evidencia y checklist.

Riesgos comprobados: macros con prefijo coincidente, comentarios escapados, fórmulas multilínea, entornos anidados, último fragmento pequeño, archivos alterados y materiales auxiliares que no deben indexarse como temario.
