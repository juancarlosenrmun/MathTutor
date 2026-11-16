---
sprint: S1
estado: en curso
fecha: 2026-11-16
---
# Auditoría y corrección del Sprint 01

> [!note] Auditoría inicial y cierre posterior
> Los apartados siguientes conservan el alcance de la auditoría inicial, que excluía crear UC-03 y UC-27. La petición posterior de completar S1 sí autoriza ambos: están implementados y verificados con 863 pruebas Java, 11 pruebas del cliente, cuatro de la puerta CI y dos comprobaciones Chromium. Véanse [[Diagrama de clases#Cierre técnico S1 · UC-03 y UC-27 (2026-11-16)]], [[Diagrama de dominio#Control de propiedad S1 · 2026-11-16]] y el seguimiento vigente de [[Sprint 01]] en la rama de desarrollo. CI final e integración se registran allí cuando se confirman.

**Objetivo:** comprobar y corregir lo implementado de [[Sprint 01]], sin desarrollar casos de uso ausentes.

**Arquitectura:** monolito modular, contratos en common, adaptadores Spring/JPA/Spring AI, PostgreSQL con pgvector y Symja. Google AI Studio gratuito. Una generación por invocación, sin reintentos automáticos ni transacciones JDBC durante llamadas externas.

**Fuentes:** [[Casos de uso]], [[Requisitos funcionales]], [[Requisitos no funcionales]], [[Restricciones y alcance]], [[Arquitectura]], [[Diagrama de clases]], [[Diagrama de dominio]].

## Plan
- [ ] UC-01: probar y corregir escalada de rol, separación access/refresh, revocación y renovación atómica.
- [ ] UC-03 existente: permisos desde el usuario activo; documentar ausencia de autorización por recurso sin crear endpoints.
- [ ] EN-04 CAS: regresiones de falsos positivos y contaminación entre evaluaciones; corregir y ejecutar corpus estricto.
- [ ] EN-04 Google/pgvector: contratos HTTP locales, SSE, errores explícitos, query/documento y ausencia de transacciones durante llamadas remotas.
- [ ] EN-01: arquitectura con todos los módulos, Docker multiplataforma y umbrales efectivos por módulo.
- [ ] Ejecutar pruebas y JaCoCo; registrar cifras reales, pendientes e integración efectivamente realizada.

## Resultado verificable de la auditoría
- [x] UC-01: se corrigieron privilegio en registro, uso de access/refresh, revocación y rotación concurrente. La operación bloquea al propietario de la sesión y después relee el token con bloqueo, evitando una rotación doble.
- [ ] UC-03: pendiente; el rol vivo no sustituye una política de acceso por recurso.
- [x] EN-01: Flyway valida una base PostgreSQL+pgvector vacía y los módulos se componen por escaneo controlado. La suite usa Docker Desktop en Windows y Docker en CI.
- [x] EN-04: CAS y adaptadores Spring AI/Google se prueban sin red externa. El camino principal es `SpringAiPgVectorRetriever`; el recuperador JDBC legacy no se declara compatible hasta adecuarlo al almacenamiento vectorial actual.
- [ ] UC-27: pendiente; no existe cliente WebGL verificable.

La verificación local `clean check jacocoAggregateReport` termina en verde. Cobertura por módulo (líneas/ramas): application 97,6/100; auth 96,8/85,6; cas 92,3/87,3; common 88,5/97,9; llm 95,2/86,5; persistence 84,0/72,9; tutor 100/100; graphics 100/100.

## Restricciones
- No implementar UC-27 ni otros UC ausentes. No completar UC-03 creando nuevos flujos.
- No consumir cuotas gratuitas en pruebas ordinarias ni introducir proveedor de pago.
- No suponer cuotas universales del plan gratuito.
- Exclusiones JaCoCo solo arranque y DTO sin lógica; medir adaptadores y seguridad.
- Código y seguimiento en sprint-1; diseño en DOC, mediante copia de trabajo separada. Sin coautoría en commits.

## Revisión prioritaria
- Registro público ADMIN y refresh usado como access.
- Renovación simultánea o repetida del mismo token.
- Expresiones distintas que coinciden en muestras y estado Symja compartido.
- HTTP 429, respuestas bloqueadas, SSE malformado y cuotas por proyecto/modelo.
- Docker omitido silenciosamente y cobertura ocultada por exclusiones.

## Hallazgos de alcance
UC-03 y UC-27 figuraban completos sin autorización por recurso ni visor WebGL verificables. Permanecen pendientes por petición expresa de no implementar casos de uso nuevos. Las cifras históricas no prueban el estado actual.

## Google AI Studio gratuito
Verificación de fuentes oficiales el 16-11-2026:
- [Tarifas](https://aistudio.google.com/docs/pricing): `gemini-3.5-flash-lite` y `gemini-embedding-001` disponen de cuota gratuita; esto no garantiza acceso o cuota concreta para una clave determinada.
- [Retirada de modelos](https://aistudio.google.com/docs/deprecations): la configuración previa `gemini-1.5-flash` está obsoleta y la familia 2.5 tiene restricciones/retirada anunciada. El modelo queda configurable mediante variable de entorno.
- [Límites](https://aistudio.google.com/docs/rate-limits): RPM, TPM y RPD dependen de modelo/proyecto. Un límite local conservador no garantiza cumplir todas las cuotas ni elimina respuestas HTTP 429.
- [Condiciones vigentes](https://aistudio.google.com/docs/terms): exigen servicios de pago al ofrecer clientes API a usuarios del EEE/Suiza/Reino Unido. Hay excepciones regionales de tratamiento de datos que impiden afirmar indiscriminadamente que toda cuota gratuita entrena con los datos.

Se mantiene la modalidad gratuita solicitada para el prototipo técnico y no se activa facturación ni otro proveedor. El despliegue para estudiantes y el cumplimiento RNF-06/EN-09 permanecen pendientes de validación contractual y del DPD UJA; una prueba técnica no acredita ese cumplimiento. Las pruebas ordinarias usan servidores locales y datos sintéticos.
