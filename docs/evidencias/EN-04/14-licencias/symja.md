# Symja / Matheclipse - Análisis LGPL-3.0

**Fecha:** 2026-11-16  
**SHA:** 1d7799705530ca15c50f1e4e32d84e56fc07864b  
**Artefacto:** `com.matheclipse:matheclipse-external:3.2.0`  
**Licencia:** LGPL-3.0-only

---

## Qué es Symja

Symja (Symbolic Java) es una biblioteca de álgebra computacional (CAS) escrita en Java. Proporciona:
- Manipulación simbólica de expresiones matemáticas
- Simplificación, expansión, factorización
- Cálculo (derivadas, integrales)
- Resolución de ecuaciones
- Álgebra lineal

En el proyecto: **motor CAS** detrás de `SymjaCasEngine` → `LatexToSymjaConverter` → `LatexContentExtractor`.

---

## Licencia LGPL-3.0-only

**Texto:** GNU Lesser General Public License v3.0 only  
**URL:** https://www.gnu.org/licenses/lgpl-3.0.html

### Cláusulas Relevantes

| Sección | Requisito | Cumplimiento en MathTutor |
|---------|-----------|---------------------------|
| **2(c)** | Obra combinada debe permitir reemplazar la librería | ✅ JAR en classpath (Gradle `implementation`), no embebido en JAR fat |
| **3** | No restringir derechos del usuario sobre la librería | ✅ Usuario puede actualizar `matheclipse-external` versión |
| **4** | Dar aviso prominente de uso de la librería | ✅ Incluido en `licencias/`, `NOTICE`, `license-reports-summary.md` |
| **4(d)** | Si la obra muestra avisos de copyright, incluir el de la librería | ✅ `matheclipse` copyright en reportes de licencia |
| **5** | Distribuir bajo términos compatibles (o GPL-3.0) | ✅ Apache-2.0/MIT/BSD compatibles con LGPL-3.0 |
| **6** | Proveer código fuente o oferta escrita | ✅ Maven Central (fuentes disponibles) |

---

## Análisis de Riesgo

| Factor | Evaluación |
|--------|------------|
| **Tipo de enlace** | Dinámico (classpath) — **cumple LGPL** |
| **Modificación de la librería** | No — uso directo API pública |
| **Distribución** | Académica (TFG) / JAR ejecutable — **compatible** |
| **Licencias compañeras** | Apache-2.0, MIT, BSD-3-Clause, EPL-2.0 — **todas compatibles** |
| **Copyleft viral** | No (LGPL ≠ GPL) — no obliga a abrir código de la aplicación |

**Conclusión:** **Riesgo BAJO**. Uso legítimo bajo LGPL-3.0.

---

## Recomendaciones

1. **Mantener** `matheclipse-external` como dependencia `implementation` (no `shadow`/`fatjar`)
2. **Documentar** en `NOTICE` / `README` el uso de Symja LGPL-3.0
3. **Verificar** al actualizar versiones que la licencia no cambie (historial: LGPL-3.0 desde v2.x)
4. **Alternativa futura** (si se requiere licencia permisiva): migrar a `exp4j` (Apache-2.0) o `janino` (BSD), pero perderían capacidades CAS completas.

---

> **No es asesoramiento jurídico; validar con el tutor o la UJA.**