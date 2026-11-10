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

- Java 21+
- Node.js 20+ (para skills y frontend)
- Docker (Testcontainers / PostgreSQL+pgvector)
- Gradle 8.x (wrapper incluido)

## 🏗️ Build y tests

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
- [brain/](brain/) — Bóveda de contexto (requisitos, sprints, diseño, seguimiento)# CI trigger
