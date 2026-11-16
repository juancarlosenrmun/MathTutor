$content = Get-Content "C:\Users\Juank\Documents\Ingenieria_Informatica\TFG\MathTutor\brain\04 Sprints\Sprint 01.md" -Raw

# Fix Justificación
$content = $content -replace 'Reduce riesgo tÃ©cnico: EN-04 valida LaTeXâ†’Symja y pgvector JDBC \(cadena crÃ­tica del tutor\). EN-01 deja estructura modular, BD y CI. UC-01 da autenticaciÃ³n a todo lo posterior. \*\*UC-03 y UC-27 movidos a Sprint 02\*\* \(replanificaciÃ³n 2026-11-12: 18 pts de 33 no completados en S1\).', 'Reduce riesgo técnico: EN-04 valida LaTeX→Symja y pgvector JDBC (cadena crítica del tutor). EN-01 deja estructura modular, BD y CI. UC-01 da autenticación a todo lo posterior. UC-03 y UC-27 permanecen en S1 (reversión de replanificación 2026-11-12 tras auditoría: no se cierra S1 sin ellos).'

# Fix Elementos table
$content = $content -replace '\| UC-03 \| Controlar el acceso por rol y recurso \| RF-22, RNF-04 \| 5 \| Must \| . \|', '| UC-03 | Controlar el acceso por rol y recurso | RF-22, RNF-04 | 5 | Must | 🔴 Pendiente |'
$content = $content -replace '\| UC-27 \| Abrir el visor WebGL con pipeline y cÃ¡mara \| RF-13 \| 13 \| Must \| . \|', '| UC-27 | Abrir el visor WebGL con pipeline y cámara | RF-13 | 13 | Must | 🔴 Pendiente |'
$content = $content -replace '\| EN-04 \| Spike: conversiÃ³n de LaTeX a Symja y pgvector Retrieval \| RNF-11 \| 5 \| Must \| . \|', '| EN-04 | Spike: conversión de LaTeX a Symja y pgvector Retrieval | RNF-11 | 5 | Must | 🔄 En verificación |'
$content = $content -replace '\*\*Total S1: 15/33 pts \(45%\) â€” UC-03 \(5\) \+ UC-27 \(13\) = 18 pts movidos a S2\*\*', '**Total S1: 10/33 pts cerrados (EN-01 + UC-01) — UC-03 (5), UC-27 (13), EN-04 (5) en curso**'

Set-Content "C:\Users\Juank\Documents\Ingenieria_Informatica\TFG\MathTutor\brain\04 Sprints\Sprint 01.md" -Value $content -Encoding UTF8
"Done v2"