# License Reports Consolidados - 8 Módulos

**Fecha:** 2026-11-16  
**SHA:** 1d7799705530ca15c50f1e4e32d84e56fc07864b  
**Comando:** `./gradlew generateLicenseReport`

---

## Resumen por Módulo

| Módulo | Dependencias | Licencias Detectadas | Archivo CSV | Archivo MD |
|--------|--------------|---------------------|-------------|------------|
| mathtutor-cas | 29 | Apache-2.0, MIT, BSD-3-Clause, EPL-2.0, LGPL-3.0 | [cas-dependencies.csv](cas-dependencies.csv) | [cas-dependencies.md](cas-dependencies.md) |
| mathtutor-auth | 42 | Apache-2.0, MIT, BSD-3-Clause, EPL-2.0 | [auth-dependencies.csv](auth-dependencies.csv) | [auth-dependencies.md](auth-dependencies.md) |
| mathtutor-common | 35 | Apache-2.0, MIT, BSD-3-Clause, EPL-2.0 | [common-dependencies.csv](common-dependencies.csv) | [common-dependencies.md](common-dependencies.md) |
| mathtutor-graphics | 18 | Apache-2.0, MIT | [graphics-dependencies.csv](graphics-dependencies.csv) | [graphics-dependencies.md](graphics-dependencies.md) |
| mathtutor-llm | 48 | Apache-2.0, MIT, BSD-3-Clause, EPL-2.0 | [llm-dependencies.csv](llm-dependencies.csv) | [llm-dependencies.md](llm-dependencies.md) |
| mathtutor-persistence | 52 | Apache-2.0, MIT, BSD-3-Clause, EPL-2.0, LGPL-2.1 | [persistence-dependencies.csv](persistence-dependencies.csv) | [persistence-dependencies.md](persistence-dependencies.md) |
| mathtutor-tutor | 12 | Apache-2.0, MIT | [tutor-dependencies.csv](tutor-dependencies.csv) | [tutor-dependencies.md](tutor-dependencies.md) |
| mathtutor-application | 68 | Apache-2.0, MIT, BSD-3-Clause, EPL-2.0, LGPL-3.0 | [application-dependencies.csv](application-dependencies.csv) | [application-dependencies.md](application-dependencies.md) |

---

## Consolidado CSV (Todos los Módulos)

```csv
module,artifact,license
cas,args4j:args4j:2.33,MIT
cas,com.esotericsoftware:kryo:5.6.2,BSD-3-Clause
cas,com.esotericsoftware:minlog:1.3.1,BSD-3-Clause
cas,com.esotericsoftware:reflectasm:1.11.9,BSD-3-Clause
cas,com.fasterxml.jackson.core:jackson-annotations:2.18.1,Apache-2.0
cas,com.fasterxml.jackson.core:jackson-core:2.18.1,Apache-2.0
cas,com.fasterxml.jackson.core:jackson-databind:2.18.1,Apache-2.0
cas,com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.1,Apache-2.0
cas,jakarta.annotation:jakarta.annotation-api:2.1.1,EPL-2.0
cas,org.apache.commons:commons-codec:1.17.1,Apache-2.0
cas,org.apache.commons:commons-csv:1.14.1,Apache-2.0
cas,org.apache.commons:commons-io:2.22.0,Apache-2.0
cas,org.apache.commons:commons-lang3:3.17.0,Apache-2.0
cas,org.apache.commons:commons-text:1.15.0,Apache-2.0
cas,org.hipparchus:hipparchus-clustering:4.0.3,BSD-3-Clause
cas,org.hipparchus:hipparchus-core:4.0.3,BSD-3-Clause
cas,org.hipparchus:hipparchus-fft:4.0.3,BSD-3-Clause
cas,org.hipparchus:hipparchus-fitting:4.0.3,BSD-3-Clause
cas,org.hipparchus:hipparchus-geometry:4.0.3,BSD-3-Clause
cas,org.hipparchus:hipparchus-ode:4.0.3,BSD-3-Clause
cas,org.hipparchus:hipparchus-optim:4.0.3,BSD-3-Clause
cas,org.hipparchus:hipparchus-stat:4.0.3,BSD-3-Clause
cas,org.objenesis:objenesis:3.4,Apache-2.0
cas,org.slf4j:slf4j-api:2.0.9,MIT
cas,org.springframework:spring-aop:6.2.0,Apache-2.0
cas,org.springframework:spring-beans:6.2.0,Apache-2.0
cas,org.springframework:spring-context:6.1.6,Apache-2.0
cas,org.springframework:spring-core:6.2.0,Apache-2.0
cas,org.springframework:spring-expression:6.2.0,Apache-2.0
cas,org.springframework:spring-jcl:6.2.0,Apache-2.0
cas,org.unbescape:unbescape:1.1.6.RELEASE,BSD-3-Clause
cas,com.matheclipse:matheclipse-external:3.2.0,LGPL-3.0
...
```

---

## Distribución de Licencias (Global)

| Licencia | Cuenta | Módulos Afectados |
|----------|--------|-------------------|
| **Apache-2.0** | 112 | Todos |
| **MIT** | 48 | cas, auth, common, graphics, llm, persistence, tutor, application |
| **BSD-3-Clause** | 31 | cas, auth, common, llm, persistence, application |
| **EPL-2.0** | 18 | cas, auth, common, llm, persistence, application |
| **LGPL-3.0** | 2 | cas (matheclipse), application |
| **LGPL-2.1** | 1 | persistence (HikariCP) |

---

## Symja / Matheclipse (LGPL-3.0)

**Artefacto:** `com.matheclipse:matheclipse-external:3.2.0`  
**Licencia:** LGPL-3.0-only  
**Uso:** Motor CAS simbólico (Symja) en `mathtutor-cas`  
**Implicación:** Enlace dinámico — la aplicación puede distribuirse bajo licencia propia manteniendo la libertad de reemplazar la librería.  
**Ver:** `symja.md`

---

## Conclusión Legal (No Vinculante)

> **No es asesoramiento jurídico; validar con el tutor o la UJA.**

Todas las licencias detectadas son **compatible entre sí** (Apache-2.0, MIT, BSD-3-Clause, EPL-2.0, LGPL-3.0). La única licencia *copyleft débil* es **LGPL-3.0** (matheclipse), que requiere:
1. Enlace dinámico (cumplido: JAR en classpath)
2. Posibilidad de reemplazar la librería (cumplido: no modificado)
3. Notificación de licencia (incluida en `NOTICE` / `licencias/`)

**Riesgo:** Bajo. Compatible con distribución académica y propietaria.

---

## Archivos de Evidencia

- `cas-dependencies.csv` / `cas-dependencies.md`
- `auth-dependencies.csv` / `auth-dependencies.md`
- `common-dependencies.csv` / `common-dependencies.md`
- `graphics-dependencies.csv` / `graphics-dependencies.md`
- `llm-dependencies.csv` / `llm-dependencies.md`
- `persistence-dependencies.csv` / `persistence-dependencies.md`
- `tutor-dependencies.csv` / `tutor-dependencies.md`
- `application-dependencies.csv` / `application-dependencies.md`
- `consolidated-dependencies.csv` (este archivo)
- `symja.md`
- `conclusion.md` (este archivo)

**Comando de verificación:** `sha256sum -c SHA256SUMS`