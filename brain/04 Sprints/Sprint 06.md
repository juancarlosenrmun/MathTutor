---
sprint: S6
objetivo: Seguridad transversal, filtrado, banco de ejercicios y superficies iluminadas
puntos: 32
estado: completado
---
# Sprint 06 · Seguridad transversal, filtrado, banco de ejercicios y superficies iluminadas

Puntos: 32 · Anterior: [[Sprint 05]] · Siguiente: [[Sprint 07]]

## Justificación
EN-02 sobre sistema completo. UC-21 filtrado y cuota. UC-23 diferencia categorías A y B. UC-30 cierra el segundo incremento gráfico. Hasta cerrar S6 no se expone a alumnado real.

## Elementos
| ID | Elemento | Req. | Pts | MoSCoW |
|---|---|---|---|---|
| UC-21 | Filtrar mensajes inadecuados y aplicar cuota | RF-11, RNF-09 | 8 | Should |
| UC-23 | Reconocer ejercicio del banco y usar sus datos | RF-12 | 8 | Should |
| UC-30 | Ver superficies con iluminación y texturas | RF-15 | 8 | Should |
| EN-02 | Seguridad transversal (CSRF, sesión, TLS, secretos, ZAP) | RNF-05 | 8 | Must |

## Checklist de implementación

### UC-21 · Filtrar mensajes inadecuados y aplicar cuota
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

### UC-23 · Reconocer ejercicio del banco y usar sus datos
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

### UC-30 · Ver superficies con iluminación y texturas
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

### EN-02 · Seguridad transversal (CSRF, sesión, TLS, secretos, ZAP)
- [x] Implementación
- [x] Pruebas de caja blanca
- [x] Pruebas de caja negra
- [x] Cobertura en umbral (≥ 80 % líneas, ≥ 70 % ramas)
- [x] Integrado en la rama principal

## Definición de hecho del sprint
- [x] Todos los elementos completos
- [x] Build y CI en verde
- [x] Cobertura en umbral verificada
- [x] Velocidad real medida y anotada en [[Registro de iteraciones]]
- [x] [[Progreso global]] actualizado

## Notas de iteración

### 2026-11-24 10:18 · Revisión final del frontend

- Base integrada en `DEV` mediante la [PR #22](https://github.com/juancarlosenrmun/MathTutor/pull/22), merge `0c782af` y CI en verde.
- UC-23 permite elegir un enunciado del banco del módulo y preparar un borrador. Al enviarlo se usa el ID elegido. Superficies, iluminación y texturas siguen disponibles en el laboratorio.
- Checklist del sprint **25/25**. La revisión de interfaz queda en `Frontend`, con CI e integración propias pendientes. Véase [[Registro de iteraciones]].

- 2026-11-23 13:45. Se verificaron los cuatro elementos en Sprint-6. Pendientes la CI de la PR y la integración en DEV. El material de superficies usa iluminación difusa y cuadrícula procedural. El CAS compara expresiones en las plantillas de derivada y dominio. La cuota cuenta por día UTC, con 100 000 tokens por defecto. Configurar `TUTOR_DAILY_TOKENS` y las tarifas `TUTOR_USD_PER_MILLION_INPUT` / `TUTOR_USD_PER_MILLION_OUTPUT` cuando cambie el presupuesto o el proveedor.
- El perfil `production` usa un almacén PKCS12 en `TLS_KEYSTORE` y su contraseña en `TLS_KEYSTORE_PASSWORD`. TLS 1.2 y 1.3 aceptados, TLS 1.1 rechazado. ZAP pasivo del visor sin alertas altas o críticas. La alerta media corresponde a estilos en línea de KaTeX. Estas comprobaciones cubren los controles ensayados de RNF-05.
