@[[AGENTS]].md

## Skills disponibles (instaladas vía find-skills)
- **tdd** (mattpocock/skills@tdd) — Test-driven development, red-green-refactor, integration tests
- **springboot-tdd** (affaan-m/ecc@springboot-tdd) — TDD for Spring Boot with JUnit 5, Mockito, MockMvc, Testcontainers, JaCoCo
- **springboot-security** (affaan-m/ecc@springboot-security) — Spring Security best practices: authn/authz, CSRF, secrets, headers, rate limiting
- **springboot-verification** (affaan-m/ecc@springboot-verification) — Full verification loop: build, static analysis, tests, coverage, OWASP scans
- **java-coding-standards** (affaan-m/ecc@java-coding-standards) — Java coding standards for Spring Boot: naming, immutability, Optional, streams, exceptions, generics
- **jpa-patterns** (affaan-m/ecc@jpa-patterns) — JPA/Hibernate patterns: entity design, relationships, query optimization, transactions, auditing, N+1
- **spring-ai-integration** (rrezartprebreza/spring-boot-skills@spring-ai-integration) — Spring AI: ChatClient, prompt templates, embeddings, vector stores, RAG, structured output
- **spring-ai-mcp-server-patterns** (giuseppe-trisciuoglio/developer-kit@spring-ai-mcp-server-patterns) — MCP server patterns with Spring AI: tool handlers, resources, prompt templates, transports
- **threejs-3d-graphics** (omer-metin/skills-for-antigravity@threejs-3d-graphics) — Three.js, WebGL, shaders, GLSL, 3D graphics, performance optimization
- **superpowers:brainstorming** — Idear antes de crear
- **superpowers:systematic-debugging** — Depurar antes de arreglar
- **superpowers:writing-plans** — Planear antes de codificar
- **superpowers:test-driven-development** — Test primero (red-green-refactor)
- **superpowers:incremental-implementation** — Entregas finas y verificables
- **superpowers:verification-before-completion** — Evidencia antes de cerrar
- **superpowers:code-review** / **requesting-code-review** / **receiving-code-review**
- **agent-skills:api-and-interface-design** · **browser-testing-with-devtools** · **ci-cd-and-automation**
- **agent-skills:code-review-and-quality** · **code-simplification** · **context-engineering**
- **agent-skills:debugging-and-error-recovery** · **deprecation-and-migration**
- **agent-skills:documentation-and-adrs** · **frontend-ui-engineering**
- **agent-skills:git-workflow-and-versioning** · **observability-and-instrumentation**
- **agent-skills:performance-optimization** · **security-and-hardening**
- **agent-skills:shipping-and-launch** · **source-driven-development**
- **agent-skills:spec-driven-development** · **test-driven-development** · **using-agent-skills**
- **frontend-design** · **dataviz** · **artifact-design** · **artifact-diagramming**
- **update-config** · **keybindings-help** · **code-review** · **simplify**
- **fewer-permission-prompts** · **loop** · **claude-api** · **workflow-authoring**
- **run** · **plugin-authoring** · **init** · **security-review** · **find-skills**

**Uso de skills**: Antes de cada UC/EN, invocar `find-skills` si no hay skill conocida para la tarea. Usar skills instaladas según la petición lo requiera (seguridad → springboot-security, TDD → springboot-tdd, Spring AI → spring-ai-integration, WebGL → threejs-3d-graphics, JPA → jpa-patterns, verificación → springboot-verification).

## Routing (GOAT)
1. En toda tarea de código, llamar primero al agente triage.
2. Delegar en el agente de su ruta (quick, build o deep) con la tarea completa y el JSON de triage. Nunca pasar parámetro de modelo.
3. Llamar al verifier con el riesgo y los checks de triage.
4. Si el verifier da FAIL, reintentar una vez un escalón más arriba:
   - saltó un archivo o no ejecutó tests → más esfuerzo (quick → build)
   - tenía contexto completo y falló → modelo mayor (build → deep → escalate)
5. Tras cualquier escalado, pedir a triage guardar una línea: "<patrón de tarea> → <ruta que funcionó>".
6. Todos los agentes (workers y verifier) cumplen AGENTS.md y la checklist de `brain/`. El sprint activo lo indica el usuario; si no lo ha indicado, preguntarlo.