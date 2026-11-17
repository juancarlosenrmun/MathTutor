param(
    [ValidateRange(1, 65535)]
    [int]$Port = 8080
)

$projectRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $projectRoot '.env'
if (-not (Test-Path -LiteralPath $envPath)) {
    throw 'Falta .env con GOOGLE_AI_API_KEY.'
}

$entries = Get-Content -LiteralPath $envPath
# bootRun se ejecuta desde el submódulo y no importa el .env de la raíz.
foreach ($name in @('GOOGLE_AI_API_KEY', 'GOOGLE_AI_EMBEDDING_MODEL', 'GOOGLE_AI_CHAT_MODEL', 'GOOGLE_AI_MAX_REQUESTS_PER_MINUTE')) {
    $entry = $entries | Where-Object { $_ -match "^$name=" } | Select-Object -Last 1
    if ($entry) {
        $value = ($entry -split '=', 2)[1].Trim()
        if ($value) { [Environment]::SetEnvironmentVariable($name, $value, 'Process') }
    }
}
if (-not $env:GOOGLE_AI_API_KEY) {
    throw 'Configura GOOGLE_AI_API_KEY en .env antes de iniciar la IA.'
}

# Spring resuelve los perfiles antes de importar .env; se activan en el proceso.
$env:SPRING_PROFILES_ACTIVE = 'dev,google-ai-studio,notes-indexing'
if (-not $env:GRADLE_USER_HOME) {
    $env:GRADLE_USER_HOME = Join-Path $env:USERPROFILE '.gradle'
}
if (-not $env:JWT_SECRET) {
    $bytes = New-Object byte[] 48
    $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $random.GetBytes($bytes) } finally { $random.Dispose() }
    $env:JWT_SECRET = [Convert]::ToBase64String($bytes)
}

Push-Location $projectRoot
try {
    & (Join-Path $projectRoot 'gradlew.bat') ':mathtutor-application:bootRun' "--args=--server.port=$Port" '--no-daemon' '--no-scan'
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
