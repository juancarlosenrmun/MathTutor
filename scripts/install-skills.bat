@echo off
REM Instala skills específicas del proyecto MathTutor (Windows)
REM Uso: .\scripts\install-skills.bat

echo 🔧 Instalando skills del proyecto MathTutor...

set SKILLS=(
    mattpocock/skills@tdd
    affaan-m/ecc@springboot-tdd
    affaan-m/ecc@springboot-security
    affaan-m/ecc@springboot-verification
    affaan-m/ecc@java-coding-standards
    affaan-m/ecc@jpa-patterns
    rrezartprebreza/spring-boot-skills@spring-ai-integration
    giuseppe-trisciuoglio/developer-kit@spring-ai-mcp-server-patterns
    omer-metin/skills-for-antigravity@threejs-3d-graphics
)

for %%s in %SKILLS% do (
    echo 📦 Instalando %%s...
    npx skills add "%%s" -g -y
)

echo ✅ Todas las skills instaladas correctamente
echo.
echo Skills disponibles:
npx skills list