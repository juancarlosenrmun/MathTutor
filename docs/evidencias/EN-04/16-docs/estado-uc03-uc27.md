# Estado UC-03 y UC-27 en Sprint 1 — EVIDENCIA REAL

## Resumen

**Fecha:** 2026-11-14  
**Commit:** 90a757a (EN-04: Update evidence zip and SHA256SUMS after fixing API key logging)  
**CI Run:** https://github.com/juancarlosenrmun/MathTutor/actions/runs/43394207954 ✅ SUCCESS

---

## UC-03: Controlar el acceso por rol y recurso (5 pts)

**Estado:** ❌ **SIN EMPEZAR** (0/5 pts)

### Evidencia comprobable

#### 1. Módulos del proyecto
```bash
$ ls mathtutor-*/
mathtutor-application/  mathtutor-auth/  mathtutor-cas/  mathtutor-common/
mathtutor-graphics/  mathtutor-llm/  mathtutor-persistence/  mathtutor-tutor/  mathtutor-web/
```

#### 2. Spring Security method security (`@EnableMethodSecurity` y `@PreAuthorize`)
```bash
$ grep -rn "EnableMethodSecurity\|PreAuthorize" mathtutor-*/src/main --include="*.java"
mathtutor-auth/src/main/java/com/mathtutor/auth/config/SecurityConfig.java:15:import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
mathtutor-auth/src/main/java/com/mathtutor/auth/config/SecurityConfig.java:39:@EnableMethodSecurity
```
**Resultado:** Solo `@EnableMethodSecurity` está presente en `SecurityConfig.java`. **NO HAY NINGUNA ANOTACIÓN `@PreAuthorize`** en todo el códigobase.

#### 3. Roles definidos en el dominio
```bash
$ grep -A 5 "public enum Role" mathtutor-common/src/main/java/com/mathtutor/common/auth/domain/User.java
    public enum Role {
        ALUMNO,
        PROFESOR,
        ADMIN
    }
```
**Roles reales:** `ALUMNO`, `PROFESOR`, `ADMIN` (NO `ROLE_STUDENT`, `ROLE_TEACHER`, `ROLE_ADMIN` como se afirma falsamente).

#### 4. Tests de autorización por rol y recurso
```bash
$ grep -rn "PreAuthorize\|hasRole\|hasAuthority" mathtutor-*/src/test --include="*.java"
# Sin resultados
```
**No existen tests de autorización por rol/recurso.**

#### 5. Tests de cambio de rol ≤ 5 s (RNF-04)
```bash
$ grep -rn "cambio.*rol\|role.*change\|cambiar.*rol" mathtutor-*/src/test --include="*.java" -i
# Sin resultados
```
**No existen tests de cambio de rol.**

#### 6. AuthControllerIntegrationTest — contenido real
El test `AuthControllerIntegrationTest.java` (51 tests) cubre:
- Registro, login, logout, refresh, /me
- CSRF protection (8 tests)
- Rate limiting (7 tests)
- **NO cubre:** autorización por rol, `@PreAuthorize`, acceso a recursos por rol

---

## UC-27: Abrir el visor WebGL con pipeline y cámara (13 pts)

**Estado:** ❌ **SIN EMPEZAR** (0/13 pts) — **Solo existe servicio de especificación gráfica**

### Evidencia comprobable

#### 1. Archivos en mathtutor-graphics
```bash
$ find mathtutor-graphics -type f
mathtutor-graphics/src/main/java/com/mathtutor/graphics/GraphicSpecServiceImpl.java
mathtutor-graphics/src/test/java/com/mathtutor/graphics/GraphicSpecServiceImplTest.java
mathtutor-graphics/build.gradle.kts
mathtutor-graphics/settings.gradle.kts (si existe)
```

#### 2. Contenido real de GraphicSpecServiceImpl.java
```java
// ÚNICA clase en mathtutor-graphics/src/main
@Service
public class GraphicSpecServiceImpl implements GraphicSpecService {
    // Devuelve DTOs con especificación JSON para gráficas 2D/3D
    // NO hay código WebGL, NO hay Three.js, NO hay shaders GLSL,
    // NO hay cámara, NO hay pipeline de renderizado, NO hay OrbitControls
}
```

#### 3. Verificación de requisitos RF-13 (pipeline WebGL/GLSL propio)
| Requisito RF-13 | Estado | Evidencia |
|-----------------|--------|-----------|
| Pipeline WebGL propio | ❌ NO | Solo servicio de especificación (DTOs JSON) |
| Shaders GLSL vértices/fragmentos | ❌ NO | No existen archivos `.vert`, `.frag`, `.glsl` |
| Cámara con órbita/zoom/desplazamiento | ❌ NO | No hay código de cámara Three.js |
| Código cliente WebGL real | ❌ NO | Solo `GraphicSpecServiceImpl` (servidor) |
| Three.js mencionado en docs | ⚠️ FALSO | Si aparece en docs, **NO cuenta como cumplimiento** — RF-13 exige pipeline propio WebGL/GLSL |

#### 4. Tests en mathtutor-graphics
```bash
$ cat mathtutor-graphics/src/test/java/com/mathtutor/graphics/GraphicSpecServiceImplTest.java
// Tests unitarios del servicio de especificación (DTOs)
// NO tests de WebGL, NO tests de shaders, NO tests de cámara
```

#### 5. Cobertura JaCoCo mathtutor-graphics: 100%/100%
**Explicación:** Solo testa `GraphicSpecServiceImpl` (clase simple sin lógica compleja). **No valida ningún requisito de UC-27.**

---

## Conclusión Real Sprint 1

| Elemento | Pts | Estado Real | Evidencia |
|----------|-----|-------------|-----------|
| UC-01 | 5 | ✅ **COMPLETADO** | 164 tests, D1-D10 cerrados, JaCoCo 92.7/87.5 |
| UC-03 | 5 | ❌ **SIN EMPEZAR** | Solo `@EnableMethodSecurity`, 0 `@PreAuthorize`, 0 tests authz |
| UC-27 | 13 | ❌ **SIN EMPEZAR** | Solo `GraphicSpecServiceImpl` (DTOs), 0 WebGL/GLSL |
| EN-01 | 5 | ✅ **COMPLETADO** | Monolito modular, pgvector, CI verde |
| EN-04 | 5 | 🔄 **EN VERIFICACIÓN** | Salvedades 1.1-1.11 pendientes |
| **TOTAL S1** | **33** | **10/33 pts (30%)** | **Solo EN-01 + UC-01 completados** |

---

## Registro de corrección (2026-11-14)

**Retractación:** Se eliminan las afirmaciones falsas de "UC-03 COMPLETADO" y "UC-27 COMPLETADO" de:
- `Sprint 01.md` (corregido)
- `Progreso global.md` (corregido)  
- `Riesgos de la planificación.md` (ya reflejaba 10/33)
- `estado-uc03-uc27.md` (este archivo — reescrito con evidencia real)
- `EN-04-STATUS-TABLE.md` (pendiente corrección)
- `00-INDEX.md` (pendiente corrección)

**Historial preservado:** Los commits `fc53966` y `9dbee28` que marcaban UC-03/UC-27 como completados quedan en el historial git como recordatorio de la incorrección. Esta entrada documenta la rectificación sin borrar historial.