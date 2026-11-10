# Skills del Proyecto MathTutor

Este proyecto usa skills de [skills.sh](https://skills.sh/) para extender las capacidades del agente.

## Instalación rápida

```bash
# Linux/macOS
./scripts/install-skills.sh

# Windows
.\scripts\install-skills.bat
```

O manualmente:

```bash
npx skills add mattpocock/skills@tdd affaan-m/ecc@springboot-tdd affaan-m/ecc@springboot-security affaan-m/ecc@springboot-verification affaan-m/ecc@java-coding-standards affaan-m/ecc@jpa-patterns rrezartprebreza/spring-boot-skills@spring-ai-integration giuseppe-trisciuoglio/developer-kit@spring-ai-mcp-server-patterns omer-metin/skills-for-antigravity@threejs-3d-graphics -g -y
```

## Skills instaladas

| Skill | Descripción | Cuándo usar |
|-------|-------------|-------------|
| `tdd` | TDD general (red-green-refactor) | Features, bugs, refactors |
| `springboot-tdd` | TDD Spring Boot (JUnit 5, Mockito, Testcontainers, JaCoCo) | Tests unitarios/integración Spring |
| `springboot-security` | Spring Security best practices | AuthN/AuthZ, CSRF, JWT, headers |
| `springboot-verification` | Pipeline completo verificación | Pre-PR, cobertura, OWASP scans |
| `java-coding-standards` | Estándares Java Spring Boot | Naming, inmutabilidad, Optional, streams |
| `jpa-patterns` | JPA/Hibernate patterns | Entidades, N+1, transacciones, auditoría |
| `spring-ai-integration` | Spring AI (ChatClient, embeddings, RAG) | LLMs, vector stores, prompt templates |
| `spring-ai-mcp-server-patterns` | MCP servers con Spring AI | Tools, resources, function calling |
| `threejs-3d-graphics` | Three.js, WebGL, GLSL, shaders | Visualización 3D, WebGL2 |

## Uso en CLAUDE.md

Las skills se invocan automáticamente según la tarea (ver `CLAUDE.md`):

- **Seguridad/JWT** → `springboot-security`
- **TDD/Tests** → `springboot-tdd` / `tdd`
- **Spring AI** → `spring-ai-integration`
- **WebGL/3D** → `threejs-3d-graphics`
- **JPA/DB** → `jpa-patterns`
- **Verificación completa** → `springboot-verification`
- **Estándares Java** → `java-coding-standards`

## Verificar instalación

```bash
npx skills list
```