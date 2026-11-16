# Diagrama de dominio

## Ampliación frontend S1 · 2026-11-16

[[Sprint 01 - Diseño frontend]] incorpora siete vistas —incluido el espacio autenticado con presentación por rol— y amplía el cliente de sesión, sin modificar entidades, relaciones, roles, propiedad ni esquema persistente. El registro público sigue restringido a `ALUMNO`; `User` y la familia de `RefreshToken` conservan su modelo. Las mutaciones de la cookie se coordinan entre pestañas con Web Locks. La lectura de los diez sprints orienta el diseño, sin adelantar su implementación. Véase [[Diagrama de clases]].

> Verificar: entidades, atributos, relaciones y multiplicidades antes de generar entidades JPA.

```mermaid
classDiagram
direction LR

class User {
  +Long id
  +String email
  +String passwordHash
  +Role role
  +boolean active
  +Instant anonymizedAt
  +Instant createdAt
}
class Role {
  <<enumeration>>
  ALUMNO
  PROFESOR
  ADMIN
}
class PasswordResetToken {
  +Long id
  +String tokenHash
  +Instant expiresAt
  +Instant usedAt
}
class RefreshToken {
  +Long id
  +String tokenHash
  +Long userId
  +String familyId
  +Instant expiresAt
  +boolean revoked
  +Instant revokedAt
  +Instant createdAt
}
class ConsentRecord {
  +Long id
  +ConsentType type
  +boolean accepted
  +String policyVersion
  +Instant at
}
class ConsentType {
  <<enumeration>>
  PRIVACIDAD
  COOKIES_TECNICAS
  COOKIES_NO_ESENCIALES
}
class AuditLog {
  +Long id
  +String action
  +String targetType
  +Long targetId
  +Instant at
}
class UsageQuota {
  +Long id
  +LocalDate day
  +long tokensUsed
  +long tokenLimit
}

class Module {
  +Long id
  +String code
  +String name
}
class Topic {
  +Long id
  +String code
  +String name
  +int difficulty
}
class NoteChunk {
  +Long id
  +String content
  +float[] embedding
  +String type
  +String sourceFile
  +String version
}
class Exercise {
  +Long id
  +String externalId
  +ExerciseType type
  +int difficulty
  +String statementLatex
  +String referenceSolutionLatex
}
class ExerciseType {
  <<enumeration>>
  TEORICO
  PROCEDIMENTAL
  INTERPRETACION
}
class CanonicalStep {
  +int position
  +String expressionLatex
  +String equivalentForms
}
class TypicalError {
  +String code
  +String seedHint
}
class KeyConcept {
  +Long id
  +String text
}

class TutorSession {
  +Long id
  +Instant startedAt
  +Instant lastActivityAt
  +SessionStatus status
  +String summary
}
class SessionStatus {
  <<enumeration>>
  ACTIVA
  CERRADA
}
class ExerciseRun {
  +Long id
  +String statementLatex
  +RunState state
  +double progress
  +HelpLevel currentLevel
  +int attempts
  +Instant startedAt
  +Instant endedAt
}
class RunState {
  <<enumeration>>
  COMENZADO
  COMPLETADO
  ABANDONADO
}
class AbandonPhase {
  <<enumeration>>
  INICIO
  CERCA_SOLUCION
}
class Turn {
  +Long id
  +String studentMessage
  +String tutorResponse
  +ErrorType diagnosis
  +HelpLevel helpLevel
  +long latencyMs
  +int tokensIn
  +int tokensOut
  +Instant at
}
class HelpLevel {
  <<enumeration>>
  CONCEPTUAL
  PROCEDIMENTAL
  EJEMPLO_ANALOGO
  REVELACION
}
class ErrorType {
  <<enumeration>>
  NINGUNO
  SIGNO
  REGLA_CADENA
  REGLA_PRODUCTO
  OTRO
}
class GraphicSpec {
  <<valueObject>>
  +GraphicType type
  +String function
  +Domain domain
  +List~Point~ points
}
class GraphicType {
  <<enumeration>>
  CURVA_2D
  SUPERFICIE
  RECTA_TANGENTE
  PLANO_TANGENTE
}

User "1" --> "1" Role
User "1" *-- "0..*" PasswordResetToken
User "1" *-- "0..*" RefreshToken
User "1" *-- "0..*" ConsentRecord
User "1" *-- "0..*" UsageQuota
User "1" --> "0..*" AuditLog : actor
ConsentRecord --> ConsentType

Module "1" *-- "1..*" Topic
Topic "1" o-- "0..*" NoteChunk
Module "1" o-- "0..*" Exercise
Topic "1" o-- "0..*" Exercise
Exercise --> ExerciseType
Exercise "1" *-- "0..*" CanonicalStep
CanonicalStep "1" *-- "0..*" TypicalError
Topic "1" *-- "3..7" KeyConcept

User "1" --> "0..*" TutorSession
TutorSession "0..*" --> "1" Module
TutorSession --> SessionStatus
TutorSession "1" *-- "0..*" ExerciseRun
ExerciseRun "0..*" --> "0..1" Exercise : banco (RF-12)
ExerciseRun "0..*" --> "0..1" Topic
ExerciseRun --> RunState
ExerciseRun --> AbandonPhase
ExerciseRun "1" *-- "1..*" Turn
Turn --> HelpLevel
Turn --> ErrorType
Turn "1" --> "0..1" GraphicSpec
GraphicSpec --> GraphicType
ExerciseRun --> HelpLevel
```

## Notas de verificación
- Auditoría S1: el diseño general incluye entidades de casos de uso futuros y no acredita que estén implementados; véase [[Auditoria Sprint 01 2026-11-16]].
- La sesión implementada incorpora `RefreshToken`: hash SHA-256 del token de alta entropía, usuario, familia, expiración y revocación. El hash es consultable por índice y la rotación bloquea el token anterior antes de emitir el siguiente. Las contraseñas conservan Argon2id.
- Migrar desde hashes Argon2id de refresh tokens obliga a iniciar sesión de nuevo; no cambia el hash de las contraseñas.
- Los roles del dominio son ALUMNO, PROFESOR y ADMIN. El alta pública no concede roles privilegiados; permisos y estado activo se obtienen del usuario persistido al autenticar cada petición.
- Los apuntes y el banco se cargan desde el código fuente (no hay carga por profesorado); `NoteChunk` y `Exercise` son de solo lectura en ejecución.
- `Exercise.externalId` es estable entre versiones (lo referencian registros y estadísticas).
- Un turno puede existir sin `ExerciseRun` de banco (ejercicio libre): `ExerciseRun.exercise` es opcional.
- Estadística docente (RF-24) se calcula agregando `ExerciseRun` por módulo y tema, sin exponer `User`.
- `GraphicSpec` no se persiste como entidad propia salvo necesidad (valor embebido en `Turn`).


## Control de propiedad S1 · 2026-11-16

- UC-03 utiliza la relación existente `User` → `TutorSession`; no incorpora entidades nuevas. La consulta de autorización combina id de sesión e id del usuario autenticado en la base de datos.
- El rol incluido en el JWT no decide los permisos: se consulta el usuario activo persistido en cada petición. Cambios de rol y desactivaciones se aplican sin renovar el token.
- El registro público solo admite ALUMNO; no permite elegir PROFESOR ni ADMIN. La provisión de roles privilegiados y los flujos administrativos continúan fuera del alcance de S1.
- UC-27 añade clases del cliente (`WebGLViewer`, `Camera`, `ShaderPipeline`, `SessionClient`), sin cambios en el modelo persistente. Véase [[Diagrama de clases]].

## Invariantes implementadas en Sprint 02

- Las nuevas sesiones de tutoría siempre tienen bloque temático y propietario autenticado. Cambiar de bloque crea otra `TutorSession`; las anteriores mantienen el suyo. La nulabilidad heredada de la columna `module_id` se conserva para datos existentes de S1.
- El catálogo contiene `funciones`, `derivadas-integrales` y `metodos-3d`, accesibles para cualquier alumno. No se incorpora el concepto de clase ni grupos de alumnos.
- Los recursos LaTeX incluidos en `notes/` llevan bloque, tema y dificultad 1–3. Cada sección autocontenida de 150–400 palabras produce un fragmento con fuente y versión SHA-256 truncada a 20 caracteres. Las fórmulas se conservan sin compilar LaTeX.
- Los fragmentos de UC-14 se indexan en `vector_store`, con metadatos `module_code`, `topic_code`, `source_file`, `type`, `version` y `managed_by`. No se duplica esa instantánea en la entidad JPA heredada `NoteChunk`.
- Las gráficas 2D son geometría efímera del cliente: no se añade una entidad ni se persiste una especificación gráfica. La derivada y la integral son aproximaciones numéricas; los intervalos del parser rechazan dominios potencialmente indefinidos y el control de cuadratura rechaza estimaciones inconsistentes.
