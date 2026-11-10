#!/bin/bash
# Instala skills específicas del proyecto MathTutor
# Uso: ./scripts/install-skills.sh

set -e

echo "🔧 Instalando skills del proyecto MathTutor..."

skills=(
    "mattpocock/skills@tdd"
    "affaan-m/ecc@springboot-tdd"
    "affaan-m/ecc@springboot-security"
    "affaan-m/ecc@springboot-verification"
    "affaan-m/ecc@java-coding-standards"
    "affaan-m/ecc@jpa-patterns"
    "rrezartprebreza/spring-boot-skills@spring-ai-integration"
    "giuseppe-trisciuoglio/developer-kit@spring-ai-mcp-server-patterns"
    "omer-metin/skills-for-antigravity@threejs-3d-graphics"
)

for skill in "${skills[@]}"; do
    echo "📦 Instalando $skill..."
    npx skills add "$skill" -g -y
done

echo "✅ Todas las skills instaladas correctamente"
echo ""
echo "Skills disponibles:"
npx skills list