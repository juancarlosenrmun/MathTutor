#!/usr/bin/env bash
input=$(cat)
[ "$(jq -r '.stop_hook_active' <<<"$input")" = "true" ] && exit 0
cd "$CLAUDE_PROJECT_DIR" || exit 0

# Sin cambios fuera de brain/ y .claude/ → nada que comprobar
git status --porcelain -- . ':!brain' ':!.claude' | grep -q . || exit 0

# Tests + cobertura (el build falla bajo el umbral JaCoCo)
if [ -f ./mvnw ]; then
  if ! out=$(./mvnw -q verify 2>&1); then
    jq -n --arg log "$(tail -n 40 <<<"$out")" \
      '{decision:"block",reason:("Tests o cobertura 100 % fallan. Corrige antes de parar.\n"+$log)}'
    exit 0
  fi
fi

# Checklist de AGENTS.md
if ! git status --porcelain -- "brain/05 Seguimiento/Registro de iteraciones.md" | grep -q .; then
  jq -n '{decision:"block",reason:"Falta la checklist de AGENTS.md: actualiza Sprint NN.md, Progreso global.md y Registro de iteraciones.md y cierra con hechos/total."}'
fi
exit 0