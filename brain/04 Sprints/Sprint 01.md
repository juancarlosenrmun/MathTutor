---
sprint: S1
objetivo: Base técnica, acceso y visor inicial
puntos: 33
estado: pendiente
---
# Sprint 01 · Base técnica, acceso y visor inicial

Puntos: 33 · Anterior: — · Siguiente: [[Sprint 02]]

## Justificación
Reduce riesgo técnico: EN-04 valida LaTeX→Symja y Spring AI+pgvector (cadena crítica del tutor). EN-01 deja estructura modular, BD y CI. UC-01/UC-03 dan autenticación y autorización a todo lo posterior. UC-27 inicia el módulo gráfico en paralelo (no depende del backend).

## Elementos
| ID | Elemento | Req. | Pts | MoSCoW |
|---|---|---|---|---|
| UC-01 | Registrarse, iniciar y cerrar sesión | RF-21 | 5 | Must |
| UC-03 | Controlar el acceso por rol y recurso | RF-22, RNF-04 | 5 | Must |
| UC-27 | Abrir el visor WebGL con pipeline y cámara | RF-13 | 13 | Must |
| EN-01 | Monolito modular Spring Boot, BD con pgvector, Git y CI | RNF-11 | 5 | Must |
| EN-04 | Spike: conversión de LaTeX a Symja y Spring AI con pgvector | RNF-11 | 5 | Must |

## Checklist de implementación

### UC-01 · Registrarse, iniciar y cerrar sesión
- [ ] Implementación
- [ ] Pruebas de caja blanca
- [ ] Pruebas de caja negra
- [ ] Cobertura 100 % (líneas y ramas)
- [ ] Integrado en la rama principal

### UC-03 · Controlar el acceso por rol y recurso
- [ ] Implementación
- [ ] Pruebas de caja blanca
- [ ] Pruebas de caja negra
- [ ] Cobertura 100 % (líneas y ramas)
- [ ] Integrado en la rama principal

### UC-27 · Abrir el visor WebGL con pipeline y cámara
- [ ] Implementación
- [ ] Pruebas de caja blanca
- [ ] Pruebas de caja negra
- [ ] Cobertura 100 % (líneas y ramas)
- [ ] Integrado en la rama principal

### EN-01 · Monolito modular Spring Boot, BD con pgvector, Git y CI
- [ ] Implementación
- [ ] Pruebas de caja blanca
- [ ] Pruebas de caja negra
- [ ] Cobertura 100 % (líneas y ramas)
- [ ] Integrado en la rama principal

### EN-04 · Spike: conversión de LaTeX a Symja y Spring AI con pgvector
- [ ] Implementación
- [ ] Pruebas de caja blanca
- [ ] Pruebas de caja negra
- [ ] Cobertura 100 % (líneas y ramas)
- [ ] Integrado en la rama principal

## Definición de hecho del sprint
- [ ] Todos los elementos completos
- [ ] Build y CI en verde
- [ ] Cobertura 100 % verificada
- [ ] Velocidad real medida y anotada en [[Registro de iteraciones]]
- [ ] [[Progreso global]] actualizado

## Notas de iteración
