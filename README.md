# MathTutor
## Abrir el frontend en local (Windows PowerShell)

Desde la raíz del proyecto, ejecuta estos comandos **en este orden**. Espera a que PostgreSQL esté saludable antes de iniciar Spring Boot:

```powershell
docker compose up -d postgres
$bytes = New-Object byte[] 48
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
.\gradlew.bat :mathtutor-application:bootRun
```

Abre `http://localhost:8080/visor/index.html` en el navegador. La clave JWT solo vive en la terminal actual; genera otra al abrir una nueva.
