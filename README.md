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

### Ejecutar la aplicación con IA en local

Necesitas **Java 21**, Docker con el motor iniciado y una clave de Google AI Studio con acceso a los modelos configurados. En Windows puedes usar Docker Desktop; en Linux/macOS, Docker Engine o Desktop con Compose v2. Los comandos se ejecutan desde la raíz del repositorio y no contienen rutas del equipo. No necesitas instalar Gradle.

**Primera vez en cada dispositivo:** genera el archivo privado de configuración.

Windows PowerShell:

```powershell
.\gradlew.bat initLocalEnv
```

Linux/macOS:

```bash
./gradlew initLocalEnv
```

Abre `.env` y configura `GOOGLE_AI_API_KEY`. Si reutilizas una base PostgreSQL existente, ajusta también `POSTGRES_PASSWORD`, `POSTGRES_USER`, `POSTGRES_DB` y el puerto para que coincidan con ella. Conserva el `JWT_SECRET` generado. No necesitas escribir ninguna contraseña en la terminal.

**Cada vez que quieras arrancar el tutor con IA**, inicia Docker y ejecuta un solo comando desde la raíz:

Windows PowerShell:

```powershell
.\gradlew.bat localAi
```

Linux/macOS:

```bash
./gradlew localAi
```

El comando inicia PostgreSQL, espera a que esté disponible y arranca la aplicación con IA y apuntes indexados. No necesitas generar un JWT temporal ni exportar los perfiles a mano. Una vez preparado `.env`, no vuelvas a generarlo para cada arranque.

`initLocalEnv` crea `.env` a partir de `.env.example`, con contraseña de PostgreSQL y secreto JWT aleatorios propios del dispositivo. Si `.env` ya existe, lo conserva sin cambios: añade manualmente las variables que falten. Este archivo está excluido de Git; no copies claves a los comandos ni lo publiques.

Variables de `.env`:

| Variable | Uso |
|---|---|
| `POSTGRES_HOST` | Servidor PostgreSQL; `localhost` con Docker local. |
| `POSTGRES_PORT` | Puerto del dispositivo; `5432` por defecto. Cámbialo si está ocupado. |
| `POSTGRES_DB` / `POSTGRES_USER` | Base y usuario de este dispositivo. |
| `POSTGRES_PASSWORD` | Contraseña local, sin valor público de respaldo. |
| `JWT_SECRET` | Secreto JWT local de al menos 32 caracteres. |
| `GOOGLE_AI_API_KEY` | Clave privada de [Google AI Studio](https://aistudio.google.com/app/apikey). |
| `GOOGLE_AI_CHAT_MODEL` / `GOOGLE_AI_EMBEDDING_MODEL` | Modelos disponibles para tu clave; comprueba su acceso en Google AI Studio. |
| `GOOGLE_AI_MAX_REQUESTS_PER_MINUTE` | Ajusta el límite a la cuota de tu proyecto. |

Usa el formato del ejemplo: `NOMBRE=valor`, sin comillas ni prefijo `export`; evita barras invertidas (el archivo se interpreta como propiedades Java). Los secretos generados ya son compatibles. Las variables de la terminal tienen prioridad sobre `.env`; comprueba que no contengan credenciales de otro dispositivo.

`localAi` valida las variables, inicia PostgreSQL+pgvector con Docker Compose, espera su disponibilidad y arranca el tutor con los perfiles `dev,google-ai-studio,notes-indexing`. El grupo `notes-indexing` activa también `spring-ai-pgvector`. El archivo se resuelve desde la raíz tanto con `bootRun` como con `localAi`; ya no necesitas el argumento `../.env`.

Abre [MathTutor en local](http://localhost:8080/visor/index.html), regístrate o inicia sesión y entra en **Tutor**. Las cuentas pertenecen a la base de datos de cada equipo. Selecciona **Funciones** y prueba la pregunta de ejemplo del módulo. La primera indexación puede tardar según la cuota de Google; las siguientes reutilizan el índice si los apuntes no han cambiado. No hay cambio automático al proveedor simulado cuando falla Google.

Puedes comprobar el servicio con `Invoke-RestMethod http://localhost:8080/actuator/health` en PowerShell o `curl http://localhost:8080/actuator/health` en Linux/macOS. Un estado `UP` verifica la aplicación y PostgreSQL; una respuesta del tutor comprueba además el acceso a la IA. Si el puerto HTTP está ocupado, define `SERVER_PORT` en la terminal antes de arrancar.

Si ya tenías un volumen PostgreSQL, **conserva en `.env` la contraseña y el usuario con los que se inicializó**. Cambiar `.env` no cambia las credenciales almacenadas en ese volumen. No borres el volumen para corregir una contraseña: contiene tus cuentas y conversaciones.

Para usar un PostgreSQL+pgvector que ya tengas disponible, configura su host, puerto y credenciales y omite el arranque de Docker:

```powershell
.\gradlew.bat localAi -PskipLocalPostgres
```

En Linux/macOS: `./gradlew localAi -PskipLocalPostgres`.
 Para probar otro archivo local puedes usar `'-PlocalEnvFile=ruta/archivo.env'` (entre comillas en PowerShell). Para detener el tutor pulsa `Ctrl+C`; Docker puede detenerse con `docker compose stop postgres`.


| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | admin.pruebas@mathtutor.local | Tutor2026!c202e820c1 |
| Alumno | alumno.pruebas@mathtutor.local | Tutor2026!fb88a58225 |

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
