# Requisitos no funcionales

| ID | Requisito | Criterio medible | Cubierto por |
|---|---|---|---|
| **RNF-01** | Rendimiento del tutor | Primer token ≤ 2 s; respuesta completa ≤ 15 s (p95), sin contar caídas del proveedor. | EN-06 |
| **RNF-02** | Rendimiento gráfico | ≥ 30 FPS con mallas de hasta 100 000 triángulos en GPU integrada media; escena lista ≤ 1 s. | EN-06 |
| **RNF-03** | Concurrencia | ≥ 30 conversaciones simultáneas; ninguna conexión JDBC ni transacción abierta durante una llamada al LLM. | EN-06 |
| **RNF-04** | Actualización de permisos | Cambio de rol efectivo en ≤ 5 s sin reautenticar; la interfaz lo refleja. | UC-03, UC-07 |
| **RNF-05** | Seguridad y control de acceso | Argon2id o BCrypt, TLS 1.2+, CSRF, cookie HttpOnly; Secure; SameSite, autorización por recurso, OWASP ASVS nivel 1, sin críticos en ZAP, claves del LLM fuera del repo. | EN-02 |
| **RNF-06** | Protección de datos y cookies | RGPD, LOPDGDD, LSSI 22.2: minimización, base jurídica, plazos de conservación, contrato de encargado con proveedor LLM (UE/garantías, sin entrenamiento). Cookies técnicas informadas; no esenciales (incl. localStorage) solo con consentimiento previo, Aceptar/Rechazar al mismo nivel. Validación con DPD UJA. | UC-04, EN-09 |
| **RNF-07** | Disponibilidad y degradación | 99 % mensual en horario lectivo; sin LLM, modo solo apuntes (circuit breaker, timeouts por etapa). | UC-26 |
| **RNF-08** | Fiabilidad pedagógica | Categorías A (procedimental banco), B (procedimental fuera de banco resoluble CAS), C (teórico banco), D (teórico/no computable fuera de banco). ≥ 50 ejercicios, ≥ 10 por categoría. Fuga ≤ 5 % A y B, ≤ 10 % C, ≤ 15 % D. Rechazo fuera de temario ≥ 90 %. | EN-05 |
| **RNF-09** | Control de coste | Cuota de tokens por usuario y día, alerta al 80 % del presupuesto, coste medio por turno registrado. | UC-21 |
| **RNF-10** | Usabilidad y accesibilidad | WCAG 2.1 AA, uso completo con teclado, lectores de pantalla en el chat, alternativa textual a visualizaciones, diseño adaptable a móvil. | EN-07 |
| **RNF-11** | Arquitectura y mantenibilidad | Monolito modular Spring Boot 3.x con módulos desacoplados por interfaces (tutorización, motor gráfico, acceso al LLM, persistencia). Cobertura ≥ 70 % lógica de negocio y ≥ 90 % política pedagógica (objetivo del proyecto: 100 %). Git y CI. | EN-01, EN-04 |
| **RNF-12** | Compatibilidad de navegadores | WebGL 2 en las dos últimas versiones de Chrome, Firefox, Edge y Safari. | EN-07 |
| **RNF-13** | Trazabilidad y observabilidad | Logs estructurados, métricas de latencia y tokens, auditoría de acciones de administrador. | EN-03, UC-09, UC-25 |
| **RNF-14** | Independencia del proveedor LLM | Cambio de proveedor/modelo solo por configuración (abstracción Spring AI). | EN-08 |
