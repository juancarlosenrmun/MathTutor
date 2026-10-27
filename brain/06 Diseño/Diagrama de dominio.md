# Diagrama de dominio

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
- Los apuntes y el banco se cargan desde el código fuente (no hay carga por profesorado); `NoteChunk` y `Exercise` son de solo lectura en ejecución.
- `Exercise.externalId` es estable entre versiones (lo referencian registros y estadísticas).
- Un turno puede existir sin `ExerciseRun` de banco (ejercicio libre): `ExerciseRun.exercise` es opcional.
- Estadística docente (RF-24) se calcula agregando `ExerciseRun` por módulo y tema, sin exponer `User`.
- `GraphicSpec` no se persiste como entidad propia salvo necesidad (valor embebido en `Turn`).
