# MathTutor

Tutor virtual adaptativo de Matemáticas (Spring Boot 3.x, Spring AI, PostgreSQL+pgvector, WebGL2/GLSL).

---

## 🚀 Instalación de Skills (agente)

Las skills extienden las capacidades del agente para este stack tecnológico.

### Instalación rápida

```bash
# Linux/macOS
./scripts/install-skills.sh

# Windows (PowerShell/CMD)
.\scripts\install-skills.bat
```

### Instalación manual

```bash
npx skills add mattpocock/skills@tdd affaan-m/ecc@springboot-tdd affaan-m/ecc@springboot-security affaan-m/ecc@springboot-verification affaan-m/ecc@java-coding-standards affaan-m/ecc@jpa-patterns rrezartprebreza/spring-boot-skills@spring-ai-integration giuseppe-trisciuoglio/developer-kit@spring-ai-mcp-server-patterns omer-metin/skills-for-antigravity@threejs-3d-graphics -g -y
```

### Verificar instalación

```bash
npx skills list
```

### Skills incluidas

| Skill | Descripción | Uso principal |
|-------|-------------|---------------|
| `tdd` | TDD general (red-green-refactor) | Features, bugs, refactors |
| `springboot-tdd` | TDD Spring Boot (JUnit 5, Mockito, Testcontainers, JaCoCo) | Tests unitarios/integración |
| `springboot-security` | Spring Security best practices | AuthN/AuthZ, CSRF, JWT, headers |
| `springboot-verification` | Pipeline verificación completa | Pre-PR, cobertura, OWASP |
| `java-coding-standards` | Estándares Java Spring Boot | Naming, inmutabilidad, streams |
| `jpa-patterns` | JPA/Hibernate patterns | Entidades, N+1, transacciones |
| `spring-ai-integration` | Spring AI (ChatClient, embeddings, RAG) | LLMs, vector stores |
| `spring-ai-mcp-server-patterns` | MCP servers con Spring AI | Tools, function calling |
| `threejs-3d-graphics` | Three.js, WebGL, GLSL, shaders | Visualización 3D |

---

## 📦 Requisitos

- Java 21
- Node.js 24 (para las comprobaciones del cliente y las skills)
- Docker (Testcontainers / PostgreSQL+pgvector)
- Gradle Wrapper incluido; no necesitas instalar Gradle por separado

## 🏗️ Build y tests

### Ejecutar la aplicación con IA en local (Windows PowerShell)

Abre PowerShell **en la raíz del proyecto**, donde están `gradlew.bat`, `docker-compose.yml` y `.env`. Necesitas Docker Desktop iniciado, Java 21 y una clave válida de Google AI Studio en el archivo local `.env`:

```dotenv
GOOGLE_AI_API_KEY=tu_clave_local
```

No publiques ese archivo ni copies la clave a los comandos. Si aún no tienes una, puedes crearla en [Google AI Studio](https://aistudio.google.com/app/apikey).

```powershell
docker compose up -d --wait postgres
if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL no está disponible. Comprueba Docker Desktop.' }

# Secreto JWT temporal para esta ejecución; no se muestra ni se guarda en el repositorio.
$startupBytes = New-Object byte[] 48
$startupRng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$startupRng.GetBytes($startupBytes)
$startupRng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($startupBytes)

$env:SPRING_PROFILES_ACTIVE = 'dev,google-ai-studio,notes-indexing'
.\gradlew.bat :mathtutor-application:bootRun --args="--spring.config.import=optional:file:../.env[.properties]"
```

**La ruta `../.env` es necesaria con `bootRun`**: Gradle ejecuta la aplicación desde `mathtutor-application`, por lo que la importación predeterminada de `.env` no encuentra el archivo de la raíz. El perfil `notes-indexing` carga los apuntes y activa también `spring-ai-pgvector`; activar solo este último no ejecuta la indexación. Mantén los tres perfiles indicados para usar PostgreSQL, el modelo real y los apuntes.

Al terminar el arranque, abre [MathTutor en local](http://localhost:8080/visor/index.html), inicia sesión como alumno —o regístrate si aún no tienes cuenta— y entra en **Tutor**. Selecciona **Funciones** y prueba «¿Qué es la continuidad de una función?» o pulsa **Pregunta de ejemplo del módulo**. Una entrada fuera del temario puede rechazarse sin llamar a la IA; eso no indica que el proveedor esté desconectado.

La primera indexación puede tardar varios minutos según la cuota del proveedor. Si el índice ya contiene los apuntes vigentes, el arranque los reutiliza. En otra terminal puedes comprobar el servicio:

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | admin.pruebas@mathtutor.local | Tutor2026!c202e820c1 |
| Alumno | alumno.pruebas@mathtutor.local | Tutor2026!fb88a58225 |

```powershell
(Invoke-RestMethod 'http://localhost:8080/actuator/health').status
# Debe mostrar UP. Comprueba también una respuesta del tutor para verificar la IA.
```

El chat usa `GOOGLE_AI_CHAT_MODEL` (por defecto `gemini-3.5-flash-lite`) y los embeddings usan `GOOGLE_AI_EMBEDDING_MODEL` (por defecto `gemini-embedding-001`). Puedes definir esos valores y `GOOGLE_AI_MAX_REQUESTS_PER_MINUTE` en `.env`; el límite debe ajustarse a la cuota de tu proyecto en Google AI Studio. Las variables de entorno de la terminal tienen prioridad sobre el archivo.

Si aparece `Google AI Studio API key must be set`, comprueba que `.env` existe en la raíz y que has copiado el argumento `--spring.config.import` completo. Los errores HTTP de cuota o del proveedor deben revisarse en Google AI Studio; el servicio no cambia al modelo simulado cuando está activo `google-ai-studio`.

Para detener la aplicación, pulsa `Ctrl+C`; PostgreSQL puede pararse con `docker compose stop postgres`. Al cerrar la terminal desaparece el secreto JWT temporal; la clave de IA permanece en el archivo `.env` local.

### Alternativa: ejecutar el JAR desde la raíz

Después de preparar Docker y el secreto JWT con el bloque anterior, puedes empaquetar y arrancar sin `bootRun`:

```powershell
.\gradlew.bat :mathtutor-application:bootJar
java -jar .\mathtutor-application\build\libs\mathtutor-application-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev,google-ai-studio,notes-indexing
```

En esta alternativa el directorio de trabajo sí es la raíz y se utiliza la importación predeterminada de `.env`. El puerto 8080 debe estar libre; detén la instancia anterior antes de iniciar otra.

```bash
# Build completo
./gradlew build

# Solo tests
./gradlew test

# Cobertura JaCoCo
./gradlew jacocoTestReport

# Verificación completa (build + static analysis + tests + cobertura)
./gradlew check
```

## 📚 Documentación

- [Skills del proyecto](docs/SKILLS.md)
- [AGENTS.md](AGENTS.md) — Instrucciones base del agente
- [brain/](brain/) — Bóveda de contexto (requisitos, sprints, diseño, seguimiento)
