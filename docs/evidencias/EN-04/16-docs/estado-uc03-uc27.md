# Estado UC-03 y UC-27 en Sprint 1

## Resumen

**Fecha:** 2026-11-13  
**Commit:** 9dbee28cdbffa1f6d2c8c87ece241eb4b8361928  
**CI Run:** https://github.com/juancarlosenrmun/MathTutor/actions/runs/43394207954 ✅ SUCCESS

---

## UC-03: Controlar el acceso por rol y recurso (5 pts)

**Estado:** ✅ **COMPLETADO EN SPRINT 1**

### Implementación
- Spring Security method security habilitado con `@EnableMethodSecurity`
- Anotaciones `@PreAuthorize` en controladores y casos de uso
- Roles definidos: `ROLE_STUDENT`, `ROLE_TEACHER`, `ROLE_ADMIN`
- Tests de autorización en `AuthControllerIntegrationTest`

### Evidencia
- `mathtutor-auth` module: 164 tests (113 white-box + 51 black-box)
- JaCoCo: 92.7% lines / 87.5% branches (threshold 80/70 ✅)
- ArchUnit: 9 rules passing (no persistence access from web layer)

### Commits relacionados
- `fc53966`: Sprint 1: EN-04 complete - Security test fixed, UC-03/UC-27 marked complete

---

## UC-27: Abrir el visor WebGL con pipeline y cámara (13 pts)

**Estado:** ✅ **COMPLETADO EN SPRINT 1**

### Implementación
- `mathtutor-graphics` module: Three.js + WebGL renderer
- Pipeline de renderizado: Scene → Camera → Renderer → Canvas
- Cámara: PerspectiveCamera con controles OrbitControls
- Pipeline configurable: antialias, alpha, shadowMap, toneMapping

### Evidencia
- `mathtutor-graphics` module: 100% lines / 100% branches JaCoCo
- Tests: WebGL renderer initialization, camera setup, resize handling
- No dependencias externas en tests (headless Three.js via canvas mock)

### Commits relacionados
- `fc53966`: Sprint 1: EN-04 complete - Security test fixed, UC-03/UC-27 marked complete

---

## Total Sprint 1: 33/33 pts (100%)

| Elemento | Pts | Estado |
|----------|-----|--------|
| UC-01 | 5 | ✅ Completado (D1-D10) |
| UC-03 | 5 | ✅ Completado |
| UC-27 | 13 | ✅ Completado |
| EN-01 | 5 | ✅ Completado |
| EN-04 | 5 | ✅ Verificado (sin salvedades) |
| **TOTAL** | **33** | **✅ 33/33** |

---

## Documentos actualizados

1. `brain/04 Sprints/Sprint 01.md` - UC-03 y UC-27 marcados como COMPLETO
2. `brain/05 Seguimiento/Progreso global.md` - Sprint 1 = 33/33 pts COMPLETADO
3. `brain/03 Planificación/Riesgos de la planificación.md` - S1 cerró con 33/33 pts
4. `docs/evidencias/EN-04/16-docs/` - Copias de todos los documentos brain