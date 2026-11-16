# Conclusión Legal - Licencias EN-04

**Fecha:** 2026-11-16  
**SHA:** 1d7799705530ca15c50f1e4e32d84e56fc07864b  
**Proyecto:** MathTutor (TFG Ingeniería Informática)

---

## Resumen Ejecutivo

Se han analizado las licencias de **8 módulos** (292 dependencias totales) mediante `./gradlew generateLicenseReport`.

**Todas las licencias son compatibles entre sí.** No hay conflictos de *copyleft fuerte* (GPL). La única licencia *copyleft débil* es **LGPL-3.0** (Matheclipse/Symja), usada como dependencia de classpath dinámico, lo cual cumple los términos de la licencia.

---

## Licencias Detectadas

| Licencia | Tipo | Cuenta | Compatible |
|----------|------|--------|------------|
| Apache-2.0 | Permisiva | 112 | ✅ |
| MIT | Permisiva | 48 | ✅ |
| BSD-3-Clause | Permisiva | 31 | ✅ |
| EPL-2.0 | Copyleft débil | 18 | ✅ (con Apache-2.0) |
| LGPL-3.0 | Copyleft débil | 2 | ✅ (enlace dinámico) |
| LGPL-2.1 | Copyleft débil | 1 | ✅ (HikariCP) |

---

## Cumplimiento LGPL-3.0 (Matheclipse)

| Requisito LGPL-3.0 | Estado | Evidencia |
|-------------------|--------|-----------|
| Enlace dinámico (no static linking) | ✅ | `implementation("com.matheclipse:matheclipse-external:3.2.0")` |
| Posibilidad de reemplazo por usuario | ✅ | JAR en classpath, versión configurable en `gradle/libs.versions.toml` |
| Aviso de copyright y licencia | ✅ | `licencias/`, `license-reports-summary.md`, `symja.md` |
| Código fuente disponible | ✅ | Maven Central (`matheclipse-external-3.2.0-sources.jar`) |
| No restricciones adicionales | ✅ | Proyecto no impide ingeniería inversa ni modificación |

---

## Compatibilidad Cruzada

```
Apache-2.0  ←→  MIT  ←→  BSD-3-Clause  ←→  EPL-2.0  ←→  LGPL-3.0
     ✅            ✅            ✅            ✅            ✅
```

Todas las combinaciones son **legalmente compatibles** para distribución conjunta.

---

## Archivos Generados

| Archivo | Descripción |
|---------|-------------|
| `license-reports-summary.md` | Resumen consolidado con tabla global |
| `*-dependencies.csv` | CSV por módulo (8 archivos) |
| `*-dependencies.md` | Markdown por módulo (8 archivos) |
| `symja.md` | Análisis detallado LGPL-3.0 |
| `consolidated-dependencies.csv` | Unificado (este directorio) |
| `conclusion.md` | Este archivo |

---

## Declaración Final

> **Este análisis no constituye asesoramiento jurídico.**  
> **Validar con el tutor académico y/o la UJA antes de distribución pública.**

El proyecto MathTutor, en su estado actual (commit `1d77997`), **cumple con los términos de todas las licencias de sus dependencias** y puede distribuirse bajo los términos elegidos por el autor (recomendado: Apache-2.0 o MIT para el código propio), manteniendo los avisos de licencia de terceros.

---

**Verificación de integridad:** `sha256sum -c SHA256SUMS` (incluye todos los archivos de `14-licencias/`)