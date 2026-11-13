#!/bin/bash
set -euo pipefail

EVIDENCE_DIR="docs/evidencias/EN-04"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

mkdir -p "$EVIDENCE_DIR"/{01-entorno,02-build,03-real-api,04-umbral,05-e2e,06-chat-gemini,07-perfiles,08-rnf03,09-fallback-numerico,10-corpus,11-cobertura,12-pit,13-mutaciones-manuales,14-licencias,15-seguridad,16-docs}

# Función para ejecutar comando y guardar salida con cabecera
run_and_save() {
    local cmd="$1"
    local outfile="$2"
    local description="${3:-}"

    {
        echo "=== COMANDO ==="
        echo "$cmd"
        echo ""
        echo "=== FECHA UTC ==="
        date -u +"%Y-%m-%d %H:%M:%S UTC"
        echo ""
        echo "=== COMMIT SHA ==="
        git rev-parse HEAD
        echo ""
        echo "=== DESCRIPCIÓN ==="
        echo "$description"
        echo ""
        echo "=== SALIDA ==="
        eval "$cmd" 2>&1
        echo ""
        echo "=== CÓDIGO DE SALIDA ==="
        echo $?
    } | sed -E 's/AIza[0-9A-Za-z_-]{35}/****/g; s/AQ\.[0-9A-Za-z_-]+/****/g; s/GOOGLE_AI_API_KEY=[^[:space:]]+/GOOGLE_AI_API_KEY=****/g' > "$outfile"
}

cd "$PROJECT_ROOT"

echo "=== 01-entorno ==="
run_and_save "git rev-parse HEAD" "$EVIDENCE_DIR/01-entorno/git-rev-parse-head.txt" "SHA del commit actual"
run_and_save "git status --short" "$EVIDENCE_DIR/01-entorno/git-status-short.txt" "Estado git short"
run_and_save "git log --oneline -30" "$EVIDENCE_DIR/01-entorno/git-log-oneline-30.txt" "Últimos 30 commits"
run_and_save "docker info" "$EVIDENCE_DIR/01-entorno/docker-info.txt" "Info Docker"
run_and_save "docker ps" "$EVIDENCE_DIR/01-entorno/docker-ps.txt" "Contenedores Docker corriendo"
run_and_save "java -version" "$EVIDENCE_DIR/01-entorno/java-version.txt" "Versión Java"
run_and_save "./gradlew --version" "$EVIDENCE_DIR/01-entorno/gradle-version.txt" "Versión Gradle"
run_and_save "gh auth status" "$EVIDENCE_DIR/01-entorno/gh-auth-status.txt" "Estado auth GitHub CLI"
run_and_save "gh run list -L 5" "$EVIDENCE_DIR/01-entorno/gh-run-list-5.txt" "Últimos 5 runs GitHub Actions"

# Get the latest run ID for the HEAD commit
RUN_ID=$(gh run list -L 1 --json databaseId --jq '.[0].databaseId' 2>/dev/null || echo "")
if [ -n "$RUN_ID" ]; then
    run_and_save "gh run view $RUN_ID --json jobs --jq '.jobs[] | {name: .name, conclusion: .conclusion, startedAt: .startedAt, completedAt: .completedAt}'" "$EVIDENCE_DIR/01-entorno/gh-run-view-head.txt" "Detalle del run del HEAD"
fi

echo "=== 02-build ==="
run_and_save "./gradlew clean check --no-daemon" "$EVIDENCE_DIR/02-build/gradle-clean-check.log" "Build completo clean check"

# Copiar resultados de tests
mkdir -p "$EVIDENCE_DIR/02-build/test-results"
find . -path "*/build/test-results/*" -name "*.xml" -exec cp {} "$EVIDENCE_DIR/02-build/test-results/" \; 2>/dev/null || true

# Generar resumen de tests desde XML
cat > "$EVIDENCE_DIR/02-build/resumen-tests.md" << 'RESUMEOF'
# Resumen de Tests por Módulo

Generado automáticamente desde XML de JUnit.

## mathtutor-auth
RESUMEOF

# Parse XML test results
for xml in "$EVIDENCE_DIR/02-build/test-results"/*.xml; do
    if [ -f "$xml" ]; then
        module=$(basename "$xml" .xml | sed 's/TEST-//')
        echo "### $module" >> "$EVIDENCE_DIR/02-build/resumen-tests.md"
        echo "" >> "$EVIDENCE_DIR/02-build/resumen-tests.md"
        echo "| Métrica | Valor |" >> "$EVIDENCE_DIR/02-build/resumen-tests.md"
        echo "|---------|-------|" >> "$EVIDENCE_DIR/02-build/resumen-tests.md"

        tests=$(grep -oP 'tests="\K[0-9]+' "$xml" | head -1)
        failures=$(grep -oP 'failures="\K[0-9]+' "$xml" | head -1)
        errors=$(grep -oP 'errors="\K[0-9]+' "$xml" | head -1)
        skipped=$(grep -oP 'skipped="\K[0-9]+' "$xml" | head -1)
        time=$(grep -oP 'time="\K[0-9.]+' "$xml" | head -1)

        echo "| Tests | ${tests:-0} |" >> "$EVIDENCE_DIR/02-build/resumen-tests.md"
        echo "| Fallos | ${failures:-0} |" >> "$EVIDENCE_DIR/02-build/resumen-tests.md"
        echo "| Errores | ${errors:-0} |" >> "$EVIDENCE_DIR/02-build/resumen-tests.md"
        echo "| Omitidos | ${skipped:-0} |" >> "$EVIDENCE_DIR/02-build/resumen-tests.md"
        echo "| Tiempo (s) | ${time:-0} |" >> "$EVIDENCE_DIR/02-build/resumen-tests.md"
        echo "" >> "$EVIDENCE_DIR/02-build/resumen-tests.md"

        # Listar tests omitidos con clase y motivo
        echo "#### Tests omitidos:" >> "$EVIDENCE_DIR/02-build/resumen-tests.md"
        grep -A2 'skipped' "$xml" | grep -E '(classname|name)' | paste - - | sed 's/.*classname="\([^"]*\)".*name="\([^"]*\)".*/- `\1#\2`/' >> "$EVIDENCE_DIR/02-build/resumen-tests.md" 2>/dev/null || echo "  (ninguno)" >> "$EVIDENCE_DIR/02-build/resumen-tests.md"
        echo "" >> "$EVIDENCE_DIR/02-build/resumen-tests.md"
    fi
done

echo "=== 03-real-api ==="
# Ejecutar realApiTest si existe
run_and_save "./gradlew :mathtutor-llm:realApiTest --no-daemon" "$EVIDENCE_DIR/03-real-api/real-api-test.log" "Test realApiTest con API key real"

# Modelos disponibles
run_and_save "curl -s -H \"Authorization: Bearer \${GOOGLE_AI_API_KEY}\" \"https://generativelanguage.googleapis.com/v1beta/models\" 2>/dev/null | jq -r '.models[] | select(.supportedGenerationMethods[] | contains(\"embedContent\") or contains(\"generateContent\")) | \"\\(.name) - \\(.supportedGenerationMethods | join(\", \"))\"'" "$EVIDENCE_DIR/03-real-api/models-list.txt" "Lista de modelos Google AI con embedContent/generateContent" 2>/dev/null || echo "Requiere GOOGLE_AI_API_KEY en entorno" > "$EVIDENCE_DIR/03-real-api/models-list.txt"

echo "=== 04-umbral ==="
# Similarity threshold validation
run_and_save "./gradlew :mathtutor-llm:test --tests SimilarityThresholdValidationTest --no-daemon" "$EVIDENCE_DIR/04-umbral/similarity-threshold-validation.log" "Validación umbral similitud"

echo "=== 05-e2e ==="
# E2E test
run_and_save "./gradlew :mathtutor-application:test --tests SpringAiPgVectorRetrieverIntegrationTest --no-daemon" "$EVIDENCE_DIR/05-e2e/e2e-integration-test.log" "Test E2E .tex -> extractor -> embeddings -> pgvector -> filtro -> umbral"

echo "=== 06-chat-gemini ==="
run_and_save "./gradlew :mathtutor-llm:test --tests *Chat*Stream* --no-daemon" "$EVIDENCE_DIR/06-chat-gemini/chat-gemini-sse-test.log" "Test streaming SSE Gemini"

echo "=== 07-perfiles ==="
# Tests de perfiles
run_and_save "./gradlew :mathtutor-application:test --tests *Profile* --no-daemon" "$EVIDENCE_DIR/07-perfiles/profile-tests.log" "Tests de perfiles de arranque"
run_and_save "./gradlew :mathtutor-application:test --tests *Ollama* --no-daemon" "$EVIDENCE_DIR/07-perfiles/ollama-tests.log" "Tests perfil Ollama"

echo "=== 08-rnf03 ==="
run_and_save "./gradlew :mathtutor-application:test --tests Rnf03NoOpenTransactionDuringLlmCallTest --no-daemon" "$EVIDENCE_DIR/08-rnf03/rnf03-test.log" "Test RNF-03 transacción durante llamada LLM"

echo "=== 09-fallback-numerico ==="
run_and_save "grep -rn \"evaluateNumerically\\|fallback\" --include=\"*.java\" mathtutor-cas/src/main/java/" "$EVIDENCE_DIR/09-fallback-numerico/ubicaciones.txt" "Ubicaciones fallback numérico"

echo "=== 10-corpus ==="
# Corpus LaTeX
run_and_save "find . -name \"*.json\" -o -name \"*.csv\" | grep -i corpus | head -5" "$EVIDENCE_DIR/10-corpus/corpus-files.txt" "Archivos de corpus"

echo "=== 11-cobertura ==="
# JaCoCo reports
run_and_save "find . -path \"*/build/reports/jacoco/*\" -name \"*.xml\" | head -20" "$EVIDENCE_DIR/11-cobertura/jacoco-xml-files.txt" "Archivos XML JaCoCo"

echo "=== 12-pit ==="
run_and_save "./gradlew pitest --no-daemon" "$EVIDENCE_DIR/12-pit/pitest.log" "PIT mutation testing"

echo "=== 13-mutaciones-manuales ==="
# Se creará manualmente

echo "=== 14-licencias ==="
run_and_save "./gradlew generateLicenseReport --no-daemon" "$EVIDENCE_DIR/14-licencias/generate-license-report.log" "Reporte de licencias"

echo "=== 15-seguridad ==="
run_and_save "git check-ignore -v .env" "$EVIDENCE_DIR/15-seguridad/git-check-ignore-env.txt" "Git ignore .env"
run_and_save "git status --short" "$EVIDENCE_DIR/15-seguridad/git-status-short.txt" "Git status"
run_and_save "git log -p --all | grep -cE \"AIza|AQ\\.\"" "$EVIDENCE_DIR/15-seguridad/git-log-grep-count.txt" "Cuenta claves en git log"
run_and_save "grep -rlE \"AIza|AQ\\.\" . --exclude-dir=.git --exclude=.env 2>/dev/null | head -20" "$EVIDENCE_DIR/15-seguridad/grep-keys-files.txt" "Archivos con claves"

echo "=== 16-docs ==="
# Copiar documentos
cp -r brain/04\ Sprints/Sprint\ 01.md "$EVIDENCE_DIR/16-docs/" 2>/dev/null || true
cp -r brain/03\ Planificación/Plan\ de\ sprints.md "$EVIDENCE_DIR/16-docs/" 2>/dev/null || true
cp -r brain/03\ Planificación/Riesgos\ de\ la\ planificación.md "$EVIDENCE_DIR/16-docs/" 2>/dev/null || true
cp -r brain/05\ Seguimiento/Progreso\ global.md "$EVIDENCE_DIR/16-docs/" 2>/dev/null || true
cp -r brain/05\ Seguimiento/Registro\ de\ iteraciones.md "$EVIDENCE_DIR/16-docs/" 2>/dev/null || true
cp -r brain/02\ Casos\ de\ uso/Casos\ de\ uso.md "$EVIDENCE_DIR/16-docs/" 2>/dev/null || true

echo "=== Generando INDEX y SHA256SUMS ==="
# Generar INDEX.md
cat > "$EVIDENCE_DIR/00-INDEX.md" << 'INDEXEOF'
# Índice de Evidencias EN-04

| Punto | Estado | Fichero(s) de evidencia | Comando | Resultado |
|-------|--------|------------------------|---------|-----------|
| 1.1 Umbral similitud | PENDIENTE | 04-umbral/ | ./gradlew :mathtutor-llm:test --tests SimilarityThresholdValidationTest | |
| 1.2 E2E .tex→pgvector | PENDIENTE | 05-e2e/ | ./gradlew :mathtutor-application:test --tests SpringAiPgVectorRetrieverIntegrationTest | |
| 1.3 Chat Gemini SSE | PENDIENTE | 06-chat-gemini/ | ./gradlew :mathtutor-llm:test --tests *Chat*Stream* | |
| 1.4 Perfiles arranque | PENDIENTE | 07-perfiles/ | ./gradlew :mathtutor-application:test --tests *Profile* | |
| 1.5 RNF-03 transacción | PENDIENTE | 08-rnf03/ | ./gradlew :mathtutor-application:test --tests Rnf03NoOpenTransactionDuringLlmCallTest | |
| 1.6 Fallback 3 estados | PENDIENTE | 09-fallback-numerico/ | grep -rn evaluateNumerically | |
| 1.7 Corpus 10 cat | PENDIENTE | 10-corpus/ | find corpus files | |
| 1.8 Cobertura sin excl | PENDIENTE | 11-cobertura/ | JaCoCo XML reports | |
| 1.9 PIT ≥80% clase | PENDIENTE | 12-pit/ | ./gradlew pitest | |
| 1.10 Licencias | PENDIENTE | 14-licencias/ | ./gradlew generateLicenseReport | |
| 1.11 Seguridad clave | PENDIENTE | 15-seguridad/ | git log -p --all | grep AIza | |

INDEXEOF

# SHA256SUMS
cd "$EVIDENCE_DIR"
find . -type f \( -name "*.txt" -o -name "*.log" -o -name "*.md" \) | xargs sha256sum > SHA256SUMS 2>/dev/null || true
cd "$PROJECT_ROOT"

# Crear ZIP
zip -r "$EVIDENCE_DIR/../EN-04-evidencias.zip" "$EVIDENCE_DIR" -x "*/test-results/*" 2>/dev/null || true

echo "=== EVIDENCIAS RECOLECTADAS EN $EVIDENCE_DIR ==="
echo "ZIP: $EVIDENCE_DIR/../EN-04-evidencias.zip"