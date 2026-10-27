# TFG: tutor virtual adaptativo de Matemáticas

## Contexto
Tutor socrático con LLM y visor WebGL/GLSL para Matemáticas de Ingeniería.
Stack: Spring Boot 3.x, JPA, Spring AI, PostgreSQL con pgvector, WebGL/GLSL.
Un solo LLM por turno y orquestador determinista. Apuntes en LaTeX (carpeta apuntes/, plantilla apuntes.sty).

## Cerebro (brain/)
- brain/ es un vault de Obsidian compartido entre dispositivos por Git. Es la memoria del proyecto.
- Al empezar una sesión, lee brain/_index.md y las notas enlazadas que afecten a la tarea.
- Al terminar, crea o actualiza brain/30-Sesiones/AAAA-MM-DD.md con lo hecho, las decisiones y los pendientes.
- Cada decisión de diseño va en brain/10-Proyecto/decisiones/ como una nota: fecha, contexto, decisión y alternativas descartadas.
- Escribe en español, con frontmatter YAML (fecha, tags) y enlaces [[nota]].
- No borres ni renombres notas sin que se pida. No edites brain/.obsidian/.
- No guardes secretos, claves ni datos personales de alumnos en brain/.

## Git
- Antes de empezar: git pull --rebase.
<<<<<<< HEAD
- Commits pequeños, en español y cada commit empieza con `[nombredelarama]`. Al terminar: git push.
=======
- Commits pequeños y en español. Al terminar: git push.
>>>>>>> db583b8cc5a36e1acf1f46eebfc6b5a44a320b31
- Nunca subas claves ni .env.