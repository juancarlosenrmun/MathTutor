# Resumen de Tests por Módulo

Generado automáticamente desde XML de JUnit.

## mathtutor-auth
### com.mathtutor.application.DebugEnvironmentTest

| Métrica | Valor |
|---------|-------|
| Tests | 1 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.127 |

#### Tests omitidos:
<testsuite name="com.mathtutor.application.DebugEnvironmentTest" tests="1" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:49:42.542Z" hostname="DESKTOP-KREG294" time="0.127">	  <testcase name="printEnvironmentVariables()" classname="com.mathtutor.application.DebugEnvironmentTest" time="0.111"/>

### com.mathtutor.application.MathTutorApplicationTest

| Métrica | Valor |
|---------|-------|
| Tests | 4 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 13.678 |

#### Tests omitidos:
<testsuite name="com.mathtutor.application.MathTutorApplicationTest" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:49:48.908Z" hostname="DESKTOP-KREG294" time="13.678">	  <testcase name="modulesAreWiredByInterfaces()" classname="com.mathtutor.application.MathTutorApplicationTest" time="0.686"/>

### com.mathtutor.application.Rnf03NoOpenTransactionDuringLlmCallTest

| Métrica | Valor |
|---------|-------|
| Tests | 2 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 17.81 |

#### Tests omitidos:
<testsuite name="com.mathtutor.application.Rnf03NoOpenTransactionDuringLlmCallTest" tests="2" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:50:02.905Z" hostname="DESKTOP-KREG294" time="17.81">	  <testcase name="rnf03_noOpenTransactionDuringEmbeddingGeneration()" classname="com.mathtutor.application.Rnf03NoOpenTransactionDuringLlmCallTest" time="2.295"/>

### com.mathtutor.application.controller.AuthControllerIntegrationTest$CsrfProtectionTests

| Métrica | Valor |
|---------|-------|
| Tests | 8 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 3.31 |

#### Tests omitidos:
<testsuite name="Protección CSRF" tests="8" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:50:27.507Z" hostname="DESKTOP-KREG294" time="3.31">	  <testcase name="Debe permitir refresh CON token CSRF válido" classname="com.mathtutor.application.controller.AuthControllerIntegrationTest$CsrfProtectionTests" time="1.042"/>

### com.mathtutor.application.controller.AuthControllerIntegrationTest$LoginTests

| Métrica | Valor |
|---------|-------|
| Tests | 4 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.846 |

#### Tests omitidos:
<testsuite name="POST /api/auth/login" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:51:30.842Z" hostname="DESKTOP-KREG294" time="0.846">	  <testcase name="Debe fallar con contraseña incorrecta" classname="com.mathtutor.application.controller.AuthControllerIntegrationTest$LoginTests" time="0.224"/>

### com.mathtutor.application.controller.AuthControllerIntegrationTest$LogoutTests

| Métrica | Valor |
|---------|-------|
| Tests | 1 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.296 |

#### Tests omitidos:
<testsuite name="POST /api/auth/logout" tests="1" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:51:30.545Z" hostname="DESKTOP-KREG294" time="0.296">	  <testcase name="Debe cerrar sesión y limpiar cookie refresh_token" classname="com.mathtutor.application.controller.AuthControllerIntegrationTest$LogoutTests" time="0.292"/>

### com.mathtutor.application.controller.AuthControllerIntegrationTest$MeTests

| Métrica | Valor |
|---------|-------|
| Tests | 4 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 1.526 |

#### Tests omitidos:
<testsuite name="GET /api/auth/me" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:50:30.818Z" hostname="DESKTOP-KREG294" time="1.526">	  <testcase name="Debe retornar info del usuario autenticado con access token válido" classname="com.mathtutor.application.controller.AuthControllerIntegrationTest$MeTests" time="0.292"/>

### com.mathtutor.application.controller.AuthControllerIntegrationTest$RateLimitTests

| Métrica | Valor |
|---------|-------|
| Tests | 7 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 3.032 |

#### Tests omitidos:
<testsuite name="Rate Limiting (ASVS L1: 5 intentos / 15 min por IP)" tests="7" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:50:24.474Z" hostname="DESKTOP-KREG294" time="3.032">	  <testcase name="Debe permitir hasta 5 intentos de login fallidos desde la misma IP" classname="com.mathtutor.application.controller.AuthControllerIntegrationTest$RateLimitTests" time="0.828"/>

### com.mathtutor.application.controller.AuthControllerIntegrationTest$RefreshTests

| Métrica | Valor |
|---------|-------|
| Tests | 7 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 58.2 |

#### Tests omitidos:
<testsuite name="POST /api/auth/refresh" tests="7" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:50:32.345Z" hostname="DESKTOP-KREG294" time="58.2">	  <testcase name="Debe fallar con refresh token expirado" classname="com.mathtutor.application.controller.AuthControllerIntegrationTest$RefreshTests" time="7.477"/>

### com.mathtutor.application.controller.AuthControllerIntegrationTest$RegisterTests

| Métrica | Valor |
|---------|-------|
| Tests | 5 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.402 |

#### Tests omitidos:
<testsuite name="POST /api/auth/register" tests="5" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:51:31.689Z" hostname="DESKTOP-KREG294" time="0.402">	  <testcase name="Debe fallar con email duplicado" classname="com.mathtutor.application.controller.AuthControllerIntegrationTest$RegisterTests" time="0.159"/>

### com.mathtutor.application.controller.AuthControllerIntegrationTest$TrustedProxyTests

| Métrica | Valor |
|---------|-------|
| Tests | 3 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 2.275 |

#### Tests omitidos:
<testsuite name="X-Forwarded-For y Trusted Proxies (D6)" tests="3" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:50:22.198Z" hostname="DESKTOP-KREG294" time="2.275">	  <testcase name="Debe confiar en X-Forwarded-For para localhost (desarrollo)" classname="com.mathtutor.application.controller.AuthControllerIntegrationTest$TrustedProxyTests" time="0.401"/>

### com.mathtutor.application.retriever.SpringAiPgVectorRetrieverIntegrationTest

| Métrica | Valor |
|---------|-------|
| Tests | 8 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 43.133 |

#### Tests omitidos:
<testsuite name="com.mathtutor.application.retriever.SpringAiPgVectorRetrieverIntegrationTest" tests="8" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:51:32.413Z" hostname="DESKTOP-KREG294" time="43.133">	  <testcase name="retrieve_findsRelevantChunksByModuleAndTopic()" classname="com.mathtutor.application.retriever.SpringAiPgVectorRetrieverIntegrationTest" time="8.095"/>

### com.mathtutor.application.security.SecurityApiKeyNotLoggedInRealCallsTest

| Métrica | Valor |
|---------|-------|
| Tests | 2 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 5.217 |

#### Tests omitidos:
<testsuite name="com.mathtutor.application.security.SecurityApiKeyNotLoggedInRealCallsTest" tests="2" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:52:15.886Z" hostname="DESKTOP-KREG294" time="5.217">	  <testcase name="realApiCall_with4xxError_doesNotLogApiKey()" classname="com.mathtutor.application.security.SecurityApiKeyNotLoggedInRealCallsTest" time="0.237"/>

### com.mathtutor.architecture.ArchitectureTest

| Métrica | Valor |
|---------|-------|
| Tests | 10 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 1.561 |

#### Tests omitidos:
<testsuite name="com.mathtutor.architecture.ArchitectureTest" tests="10" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:55:12.381Z" hostname="DESKTOP-KREG294" time="1.561">	  <testcase name="persistence_only_depends_on_common" classname="com.mathtutor.architecture.ArchitectureTest" time="1.471"/>

### com.mathtutor.auth.adapter.AuditLogAdapterTest$SaveUserLoggedInEvent

| Métrica | Valor |
|---------|-------|
| Tests | 1 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.003 |

#### Tests omitidos:
<testsuite name="save() - UserLoggedInEvent" tests="1" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:29.155Z" hostname="DESKTOP-KREG294" time="0.003">	  <testcase name="Debe persistir evento de inicio de sesión" classname="com.mathtutor.auth.adapter.AuditLogAdapterTest$SaveUserLoggedInEvent" time="0.002"/>

### com.mathtutor.auth.adapter.AuditLogAdapterTest$SaveUserLoggedOutEvent

| Métrica | Valor |
|---------|-------|
| Tests | 1 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.004 |

#### Tests omitidos:
<testsuite name="save() - UserLoggedOutEvent" tests="1" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:29.150Z" hostname="DESKTOP-KREG294" time="0.004">	  <testcase name="Debe persistir evento de cierre de sesión" classname="com.mathtutor.auth.adapter.AuditLogAdapterTest$SaveUserLoggedOutEvent" time="0.003"/>

### com.mathtutor.auth.adapter.AuditLogAdapterTest$SaveUserLoginFailedEvent

| Métrica | Valor |
|---------|-------|
| Tests | 1 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.238 |

#### Tests omitidos:
<testsuite name="save() - UserLoginFailedEvent" tests="1" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:28.911Z" hostname="DESKTOP-KREG294" time="0.238">	  <testcase name="Debe persistir evento de fallo de inicio de sesión" classname="com.mathtutor.auth.adapter.AuditLogAdapterTest$SaveUserLoginFailedEvent" time="0.237"/>

### com.mathtutor.auth.adapter.AuditLogAdapterTest$SaveUserRegisteredEvent

| Métrica | Valor |
|---------|-------|
| Tests | 1 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.003 |

#### Tests omitidos:
<testsuite name="save() - UserRegisteredEvent" tests="1" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:29.163Z" hostname="DESKTOP-KREG294" time="0.003">	  <testcase name="Debe persistir evento de registro de usuario" classname="com.mathtutor.auth.adapter.AuditLogAdapterTest$SaveUserRegisteredEvent" time="0.002"/>

### com.mathtutor.auth.adapter.AuditLogAdapterTest$SaveUserRegistrationFailedEvent

| Métrica | Valor |
|---------|-------|
| Tests | 1 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.003 |

#### Tests omitidos:
<testsuite name="save() - UserRegistrationFailedEvent" tests="1" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:29.159Z" hostname="DESKTOP-KREG294" time="0.003">	  <testcase name="Debe persistir evento de fallo de registro" classname="com.mathtutor.auth.adapter.AuditLogAdapterTest$SaveUserRegistrationFailedEvent" time="0.003"/>

### com.mathtutor.auth.adapter.AuditLogAdapterTest$TransactionalBehavior

| Métrica | Valor |
|---------|-------|
| Tests | 1 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 1.464 |

#### Tests omitidos:
<testsuite name="Comportamiento transaccional" tests="1" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:27.440Z" hostname="DESKTOP-KREG294" time="1.464">	  <testcase name="Debe llamar a save en el repositorio para cada evento" classname="com.mathtutor.auth.adapter.AuditLogAdapterTest$TransactionalBehavior" time="1.455"/>

### com.mathtutor.auth.adapter.PasswordEncoderAdapterTest$Encode

| Métrica | Valor |
|---------|-------|
| Tests | 6 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.589 |

#### Tests omitidos:
<testsuite name="encode()" tests="6" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:30.207Z" hostname="DESKTOP-KREG294" time="0.589">	  <testcase name="Debe manejar contraseña muy larga" classname="com.mathtutor.auth.adapter.PasswordEncoderAdapterTest$Encode" time="0.131"/>

### com.mathtutor.auth.adapter.PasswordEncoderAdapterTest$Matches

| Métrica | Valor |
|---------|-------|
| Tests | 8 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.6 |

#### Tests omitidos:
<testsuite name="matches()" tests="8" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:29.606Z" hostname="DESKTOP-KREG294" time="0.6">	  <testcase name="Debe retornar false para hash null" classname="com.mathtutor.auth.adapter.PasswordEncoderAdapterTest$Matches" time="0.004"/>

### com.mathtutor.auth.adapter.PasswordEncoderAdapterTest$SecurityParameters

| Métrica | Valor |
|---------|-------|
| Tests | 4 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.436 |

#### Tests omitidos:
<testsuite name="Parámetros de seguridad Argon2id" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:29.168Z" hostname="DESKTOP-KREG294" time="0.436">	  <testcase name="Debe usar 3 iteraciones" classname="com.mathtutor.auth.adapter.PasswordEncoderAdapterTest$SecurityParameters" time="0.132"/>

### com.mathtutor.auth.adapter.RefreshTokenAdapterTest$CleanupExpiredTokens

| Métrica | Valor |
|---------|-------|
| Tests | 1 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.007 |

#### Tests omitidos:
<testsuite name="cleanupExpiredTokens()" tests="1" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:31.075Z" hostname="DESKTOP-KREG294" time="0.007">	  <testcase name="Debe llamar a repositorio para borrar tokens expirados" classname="com.mathtutor.auth.adapter.RefreshTokenAdapterTest$CleanupExpiredTokens" time="0.007"/>

### com.mathtutor.auth.adapter.RefreshTokenAdapterTest$FindActiveByUserId

| Métrica | Valor |
|---------|-------|
| Tests | 3 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.276 |

#### Tests omitidos:
<testsuite name="findActiveByUserId()" tests="3" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:30.798Z" hostname="DESKTOP-KREG294" time="0.276">	  <testcase name="Debe retornar empty cuando no hay tokens activos" classname="com.mathtutor.auth.adapter.RefreshTokenAdapterTest$FindActiveByUserId" time="0.268"/>

### com.mathtutor.auth.adapter.RefreshTokenAdapterTest$RevokeRefreshToken

| Métrica | Valor |
|---------|-------|
| Tests | 4 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.014 |

#### Tests omitidos:
<testsuite name="revokeRefreshToken()" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:31.087Z" hostname="DESKTOP-KREG294" time="0.014">	  <testcase name="Debe manejar tokenId no numérico sin llamar al repositorio" classname="com.mathtutor.auth.adapter.RefreshTokenAdapterTest$RevokeRefreshToken" time="0.003"/>

### com.mathtutor.auth.adapter.RefreshTokenAdapterTest$RevokeTokenFamily

| Métrica | Valor |
|---------|-------|
| Tests | 1 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.003 |

#### Tests omitidos:
<testsuite name="revokeTokenFamily()" tests="1" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:31.083Z" hostname="DESKTOP-KREG294" time="0.003">	  <testcase name="Debe llamar a repositorio para revocar familia" classname="com.mathtutor.auth.adapter.RefreshTokenAdapterTest$RevokeTokenFamily" time="0.002"/>

### com.mathtutor.auth.adapter.RefreshTokenAdapterTest$StoreRefreshToken

| Métrica | Valor |
|---------|-------|
| Tests | 2 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.009 |

#### Tests omitidos:
<testsuite name="storeRefreshToken()" tests="2" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:31.142Z" hostname="DESKTOP-KREG294" time="0.009">	  <testcase name="Debe almacenar refresh token con familyId nuevo y retornar token en claro" classname="com.mathtutor.auth.adapter.RefreshTokenAdapterTest$StoreRefreshToken" time="0.004"/>

### com.mathtutor.auth.adapter.RefreshTokenAdapterTest$StoreRefreshTokenWithFamily

| Métrica | Valor |
|---------|-------|
| Tests | 1 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.007 |

#### Tests omitidos:
<testsuite name="storeRefreshTokenWithFamily()" tests="1" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:31.135Z" hostname="DESKTOP-KREG294" time="0.007">	  <testcase name="Debe almacenar refresh token con familyId específico y retornar token en claro" classname="com.mathtutor.auth.adapter.RefreshTokenAdapterTest$StoreRefreshTokenWithFamily" time="0.006"/>

### com.mathtutor.auth.adapter.RefreshTokenAdapterTest$ValidateRefreshToken

| Métrica | Valor |
|---------|-------|
| Tests | 7 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.032 |

#### Tests omitidos:
<testsuite name="validateRefreshToken()" tests="7" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:31.102Z" hostname="DESKTOP-KREG294" time="0.032">	  <testcase name="Debe retornar empty cuando token ya está revocado (detección de reuso se maneja en endpoint /refresh)" classname="com.mathtutor.auth.adapter.RefreshTokenAdapterTest$ValidateRefreshToken" time="0.003"/>

### com.mathtutor.auth.adapter.TokenServiceAdapterTest$ExtractClaims

| Métrica | Valor |
|---------|-------|
| Tests | 4 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.886 |

#### Tests omitidos:
<testsuite name="extractClaims()" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:31.321Z" hostname="DESKTOP-KREG294" time="0.886">	  <testcase name="Debe extraer claims sin validar firma" classname="com.mathtutor.auth.adapter.TokenServiceAdapterTest$ExtractClaims" time="0.875"/>

### com.mathtutor.auth.adapter.TokenServiceAdapterTest$GenerateAccessToken

| Métrica | Valor |
|---------|-------|
| Tests | 2 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.005 |

#### Tests omitidos:
<testsuite name="generateAccessToken()" tests="2" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:32.519Z" hostname="DESKTOP-KREG294" time="0.005">	  <testcase name="Debe generar token con claims correctos" classname="com.mathtutor.auth.adapter.TokenServiceAdapterTest$GenerateAccessToken" time="0.003"/>

### com.mathtutor.auth.adapter.TokenServiceAdapterTest$GenerateRefreshToken

| Métrica | Valor |
|---------|-------|
| Tests | 1 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.003 |

#### Tests omitidos:
<testsuite name="generateRefreshToken()" tests="1" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:32.515Z" hostname="DESKTOP-KREG294" time="0.003">	  <testcase name="Debe generar refresh token JWT válido con tokenId" classname="com.mathtutor.auth.adapter.TokenServiceAdapterTest$GenerateRefreshToken" time="0.002"/>

### com.mathtutor.auth.adapter.TokenServiceAdapterTest$RevokeAndCleanup

| Métrica | Valor |
|---------|-------|
| Tests | 3 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.116 |

#### Tests omitidos:
<testsuite name="revokeRefreshToken() y revokeTokenFamily()" tests="3" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:32.247Z" hostname="DESKTOP-KREG294" time="0.116">	  <testcase name="Debe manejar tokenId null en revokeRefreshToken" classname="com.mathtutor.auth.adapter.TokenServiceAdapterTest$RevokeAndCleanup" time="0.002"/>

### com.mathtutor.auth.adapter.TokenServiceAdapterTest$SecretKeyValidation

| Métrica | Valor |
|---------|-------|
| Tests | 1 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.161 |

#### Tests omitidos:
<testsuite name="Validación de clave secreta" tests="1" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:31.154Z" hostname="DESKTOP-KREG294" time="0.161">	  <testcase name="Debe lanzar excepción si clave es menor a 256 bits" classname="com.mathtutor.auth.adapter.TokenServiceAdapterTest$SecretKeyValidation" time="0.16"/>

### com.mathtutor.auth.adapter.TokenServiceAdapterTest$StoreRefreshToken

| Métrica | Valor |
|---------|-------|
| Tests | 2 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.038 |

#### Tests omitidos:
<testsuite name="storeRefreshToken() y storeRefreshTokenWithFamily()" tests="2" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:32.208Z" hostname="DESKTOP-KREG294" time="0.038">	  <testcase name="Debe almacenar refresh token y retornar token JWT" classname="com.mathtutor.auth.adapter.TokenServiceAdapterTest$StoreRefreshToken" time="0.032"/>

### com.mathtutor.auth.adapter.TokenServiceAdapterTest$ValidateToken

| Métrica | Valor |
|---------|-------|
| Tests | 6 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.15 |

#### Tests omitidos:
<testsuite name="validateToken()" tests="6" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:32.364Z" hostname="DESKTOP-KREG294" time="0.15">	  <testcase name="Debe validar access token correcto y retornar claims" classname="com.mathtutor.auth.adapter.TokenServiceAdapterTest$ValidateToken" time="0.016"/>

### com.mathtutor.auth.adapter.UserRepositoryAdapterTest$ExistsByEmail

| Métrica | Valor |
|---------|-------|
| Tests | 2 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.005 |

#### Tests omitidos:
<testsuite name="existsByEmail" tests="2" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:32.564Z" hostname="DESKTOP-KREG294" time="0.005">	  <testcase name="Debe retornar true si existe" classname="com.mathtutor.auth.adapter.UserRepositoryAdapterTest$ExistsByEmail" time="0.002"/>

### com.mathtutor.auth.adapter.UserRepositoryAdapterTest$FindActiveByEmail

| Métrica | Valor |
|---------|-------|
| Tests | 2 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.005 |

#### Tests omitidos:
<testsuite name="findActiveByEmail" tests="2" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:32.559Z" hostname="DESKTOP-KREG294" time="0.005">	  <testcase name="Debe retornar usuario activo si existe" classname="com.mathtutor.auth.adapter.UserRepositoryAdapterTest$FindActiveByEmail" time="0.002"/>

### com.mathtutor.auth.adapter.UserRepositoryAdapterTest$FindByEmail

| Métrica | Valor |
|---------|-------|
| Tests | 2 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.004 |

#### Tests omitidos:
<testsuite name="findByEmail" tests="2" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:32.570Z" hostname="DESKTOP-KREG294" time="0.004">	  <testcase name="Debe retornar usuario si existe" classname="com.mathtutor.auth.adapter.UserRepositoryAdapterTest$FindByEmail" time="0.002"/>

### com.mathtutor.auth.adapter.UserRepositoryAdapterTest$RoleMapping

| Métrica | Valor |
|---------|-------|
| Tests | 3 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.008 |

#### Tests omitidos:
<testsuite name="Mapeo de roles" tests="3" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:32.545Z" hostname="DESKTOP-KREG294" time="0.008">	  <testcase name="Debe mapear ALUMNO correctamente" classname="com.mathtutor.auth.adapter.UserRepositoryAdapterTest$RoleMapping" time="0.002"/>

### com.mathtutor.auth.adapter.UserRepositoryAdapterTest$Save

| Métrica | Valor |
|---------|-------|
| Tests | 1 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.003 |

#### Tests omitidos:
<testsuite name="save" tests="1" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:32.555Z" hostname="DESKTOP-KREG294" time="0.003">	  <testcase name="Debe guardar usuario y retornar con ID asignado" classname="com.mathtutor.auth.adapter.UserRepositoryAdapterTest$Save" time="0.003"/>

### com.mathtutor.auth.adapter.UserRepositoryAdapterTest$SaveExistingUser

| Métrica | Valor |
|---------|-------|
| Tests | 3 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.017 |

#### Tests omitidos:
<testsuite name="save() - Actualización de usuario existente" tests="3" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:32.527Z" hostname="DESKTOP-KREG294" time="0.017">	  <testcase name="Debe crear nuevo usuario cuando ID existe pero no está en BD" classname="com.mathtutor.auth.adapter.UserRepositoryAdapterTest$SaveExistingUser" time="0.005"/>

### com.mathtutor.auth.config.SecurityConfigTest$CsrfConfig

| Métrica | Valor |
|---------|-------|
| Tests | 1 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.002 |

#### Tests omitidos:
<testsuite name="Configuración CSRF" tests="1" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:32.963Z" hostname="DESKTOP-KREG294" time="0.002">	  <testcase name="Debe instanciar SecurityConfig correctamente" classname="com.mathtutor.auth.config.SecurityConfigTest$CsrfConfig" time="0.001"/>

### com.mathtutor.auth.config.SecurityConfigTest

| Métrica | Valor |
|---------|-------|
| Tests | 4 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.386 |

#### Tests omitidos:
<testsuite name="SecurityConfig" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:32.575Z" hostname="DESKTOP-KREG294" time="0.386">	  <testcase name="Debe crear JwtAuthenticationFilter bean" classname="com.mathtutor.auth.config.SecurityConfigTest" time="0.065"/>

### com.mathtutor.auth.controller.AuthControllerTest$CookieValidation

| Métrica | Valor |
|---------|-------|
| Tests | 3 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.084 |

#### Tests omitidos:
<testsuite name="Validación de cookies" tests="3" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:35.114Z" hostname="DESKTOP-KREG294" time="0.084">	  <testcase name="Cookie refresh_token debe ser HttpOnly y Secure" classname="com.mathtutor.auth.controller.AuthControllerTest$CookieValidation" time="0.036"/>

### com.mathtutor.auth.controller.AuthControllerTest$Login

| Métrica | Valor |
|---------|-------|
| Tests | 5 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.142 |

#### Tests omitidos:
<testsuite name="POST /api/auth/login" tests="5" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:35.371Z" hostname="DESKTOP-KREG294" time="0.142">	  <testcase name="Debe retornar 400 para email inválido (validación)" classname="com.mathtutor.auth.controller.AuthControllerTest$Login" time="0.068"/>

### com.mathtutor.auth.controller.AuthControllerTest$Logout

| Métrica | Valor |
|---------|-------|
| Tests | 2 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.023 |

#### Tests omitidos:
<testsuite name="POST /api/auth/logout" tests="2" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:35.347Z" hostname="DESKTOP-KREG294" time="0.023">	  <testcase name="Debe limpiar cookie incluso sin usuario autenticado" classname="com.mathtutor.auth.controller.AuthControllerTest$Logout" time="0.013"/>

### com.mathtutor.auth.controller.AuthControllerTest$Me

| Métrica | Valor |
|---------|-------|
| Tests | 2 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.085 |

#### Tests omitidos:
<testsuite name="GET /api/auth/me" tests="2" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:35.199Z" hostname="DESKTOP-KREG294" time="0.085">	  <testcase name="Debe retornar info del usuario autenticado" classname="com.mathtutor.auth.controller.AuthControllerTest$Me" time="0.072"/>

### com.mathtutor.auth.controller.AuthControllerTest$Refresh

| Métrica | Valor |
|---------|-------|
| Tests | 4 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.062 |

#### Tests omitidos:
<testsuite name="POST /api/auth/refresh" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:35.284Z" hostname="DESKTOP-KREG294" time="0.062">	  <testcase name="Debe retornar 401 cuando no hay refresh token en cookie" classname="com.mathtutor.auth.controller.AuthControllerTest$Refresh" time="0.016"/>

### com.mathtutor.auth.controller.AuthControllerTest$Register

| Métrica | Valor |
|---------|-------|
| Tests | 6 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.104 |

#### Tests omitidos:
<testsuite name="POST /api/auth/register" tests="6" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:35.513Z" hostname="DESKTOP-KREG294" time="0.104">	  <testcase name="Debe retornar 400 con error genérico cuando email ya existe (no enumeración)" classname="com.mathtutor.auth.controller.AuthControllerTest$Register" time="0.018"/>

### com.mathtutor.auth.controller.AuthControllerTest$TrustedProxyAndXForwardedFor

| Métrica | Valor |
|---------|-------|
| Tests | 7 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 2.145 |

#### Tests omitidos:
<testsuite name="X-Forwarded-For y Trusted Proxy (IP Spoofing Protection)" tests="7" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:32.968Z" hostname="DESKTOP-KREG294" time="2.145">	  <testcase name="Debe usar primera IP de X-Forwarded-For cuando hay múltiples (client, proxy1, proxy2)" classname="com.mathtutor.auth.controller.AuthControllerTest$TrustedProxyAndXForwardedFor" time="1.917"/>

### com.mathtutor.auth.filter.JwtAuthenticationFilterTest$ExtractTokenBranches

| Métrica | Valor |
|---------|-------|
| Tests | 3 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.006 |

#### Tests omitidos:
<testsuite name="extractToken() - Cobertura de ramas" tests="3" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:35.843Z" hostname="DESKTOP-KREG294" time="0.006">	  <testcase name="Debe retornar token de header cuando cookie existe pero es otro nombre" classname="com.mathtutor.auth.filter.JwtAuthenticationFilterTest$ExtractTokenBranches" time="0.001"/>

### com.mathtutor.auth.filter.JwtAuthenticationFilterTest$ExtractTokenFromCookie

| Métrica | Valor |
|---------|-------|
| Tests | 4 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.008 |

#### Tests omitidos:
<testsuite name="doFilterInternal() - Extracción de token desde cookie" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:35.882Z" hostname="DESKTOP-KREG294" time="0.008">	  <testcase name="Debe manejar array de cookies null" classname="com.mathtutor.auth.filter.JwtAuthenticationFilterTest$ExtractTokenFromCookie" time="0.002"/>

### com.mathtutor.auth.filter.JwtAuthenticationFilterTest$ExtractTokenFromHeader

| Métrica | Valor |
|---------|-------|
| Tests | 4 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.008 |

#### Tests omitidos:
<testsuite name="doFilterInternal() - Extracción de token desde header Authorization" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:35.874Z" hostname="DESKTOP-KREG294" time="0.008">	  <testcase name="Debe ignorar header Authorization sin prefijo Bearer" classname="com.mathtutor.auth.filter.JwtAuthenticationFilterTest$ExtractTokenFromHeader" time="0.002"/>

### com.mathtutor.auth.filter.JwtAuthenticationFilterTest$NoToken

| Métrica | Valor |
|---------|-------|
| Tests | 2 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.005 |

#### Tests omitidos:
<testsuite name="doFilterInternal() - Sin token" tests="2" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:35.850Z" hostname="DESKTOP-KREG294" time="0.005">	  <testcase name="Debe continuar filtro sin autenticación cuando no hay token" classname="com.mathtutor.auth.filter.JwtAuthenticationFilterTest$NoToken" time="0.002"/>

### com.mathtutor.auth.filter.JwtAuthenticationFilterTest$SetAuthenticationBranches

| Métrica | Valor |
|---------|-------|
| Tests | 2 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.223 |

#### Tests omitidos:
<testsuite name="setAuthentication() - Cobertura de ramas" tests="2" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:35.619Z" hostname="DESKTOP-KREG294" time="0.223">	  <testcase name="Debe crear AuthenticatedUser con role correcto" classname="com.mathtutor.auth.filter.JwtAuthenticationFilterTest$SetAuthenticationBranches" time="0.22"/>

### com.mathtutor.auth.filter.JwtAuthenticationFilterTest$TokenValidation

| Métrica | Valor |
|---------|-------|
| Tests | 7 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.017 |

#### Tests omitidos:
<testsuite name="doFilterInternal() - Validación de token" tests="7" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:35.856Z" hostname="DESKTOP-KREG294" time="0.017">	  <testcase name="Debe NO establecer autenticación cuando email está vacío" classname="com.mathtutor.auth.filter.JwtAuthenticationFilterTest$TokenValidation" time="0.004"/>

### com.mathtutor.cas.LatexContentExtractorBlackBoxTest

| Métrica | Valor |
|---------|-------|
| Tests | 21 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.301 |

#### Tests omitidos:
<testsuite name="com.mathtutor.cas.LatexContentExtractorBlackBoxTest" tests="21" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:39.251Z" hostname="DESKTOP-KREG294" time="0.301">	  <testcase name="Extrae entorno paso" classname="com.mathtutor.cas.LatexContentExtractorBlackBoxTest" time="0.125"/>

### com.mathtutor.cas.LatexContentExtractorWhiteBoxTest

| Métrica | Valor |
|---------|-------|
| Tests | 17 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.021 |

#### Tests omitidos:
<testsuite name="com.mathtutor.cas.LatexContentExtractorWhiteBoxTest" tests="17" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:39.555Z" hostname="DESKTOP-KREG294" time="0.021">	  <testcase name="extract: fórmulas en diferentes entornos se extraen globalmente" classname="com.mathtutor.cas.LatexContentExtractorWhiteBoxTest" time="0.002"/>

### com.mathtutor.cas.LatexToSymjaConverterBlackBoxTest

| Métrica | Valor |
|---------|-------|
| Tests | 28 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 2.743 |

#### Tests omitidos:
<testsuite name="com.mathtutor.cas.LatexToSymjaConverterBlackBoxTest" tests="28" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:39.578Z" hostname="DESKTOP-KREG294" time="2.743">	  <testcase name="Expresión compleja: fracción anidada" classname="com.mathtutor.cas.LatexToSymjaConverterBlackBoxTest" time="0.125"/>

### com.mathtutor.cas.LatexToSymjaConverterTest

| Métrica | Valor |
|---------|-------|
| Tests | 57 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 1.219 |

#### Tests omitidos:
<testsuite name="com.mathtutor.cas.LatexToSymjaConverterTest" tests="57" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:42.322Z" hostname="DESKTOP-KREG294" time="1.219">	  <testcase name="areEquivalent - diferencia constante NO es equivalencia" classname="com.mathtutor.cas.LatexToSymjaConverterTest" time="0.005"/>

### com.mathtutor.cas.LatexToSymjaCorpusTest

| Métrica | Valor |
|---------|-------|
| Tests | 163 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.344 |

#### Tests omitidos:
<testsuite name="com.mathtutor.cas.LatexToSymjaCorpusTest" tests="163" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:43.541Z" hostname="DESKTOP-KREG294" time="0.344">	  <testcase name="derivatives.deriv_01: parse 'x^2'" classname="com.mathtutor.cas.LatexToSymjaCorpusTest" time="0.002"/>

### com.mathtutor.cas.SymjaCasEngineCatchBlockTest

| Métrica | Valor |
|---------|-------|
| Tests | 4 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 1.431 |

#### Tests omitidos:
<testsuite name="com.mathtutor.cas.SymjaCasEngineCatchBlockTest" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:43.901Z" hostname="DESKTOP-KREG294" time="1.431">	  <testcase name="analyzeDifference handles exception in numeric evaluation" classname="com.mathtutor.cas.SymjaCasEngineCatchBlockTest" time="1.407"/>

### com.mathtutor.cas.SymjaCasEngineTest

| Métrica | Valor |
|---------|-------|
| Tests | 31 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.195 |

#### Tests omitidos:
<testsuite name="com.mathtutor.cas.SymjaCasEngineTest" tests="31" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:45.345Z" hostname="DESKTOP-KREG294" time="0.195">	  <testcase name="verify con equivalencia numérica pero no simbólica" classname="com.mathtutor.cas.SymjaCasEngineTest" time="0.009"/>

### com.mathtutor.common.auth.domain.LoginUseCaseTest$EmailBasedRateLimiting

| Métrica | Valor |
|---------|-------|
| Tests | 7 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 1.681 |

#### Tests omitidos:
<testsuite name="Email-based Rate Limiting (Credential Stuffing Protection)" tests="7" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:48.279Z" hostname="DESKTOP-KREG294" time="1.681">	  <testcase name="Debe resetear email rate limit tras login exitoso" classname="com.mathtutor.common.auth.domain.LoginUseCaseTest$EmailBasedRateLimiting" time="1.397"/>

### com.mathtutor.common.auth.domain.LoginUseCaseTest$InputValidation

| Métrica | Valor |
|---------|-------|
| Tests | 3 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.01 |

#### Tests omitidos:
<testsuite name="Validación de entrada" tests="3" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:50.026Z" hostname="DESKTOP-KREG294" time="0.01">	  <testcase name="Debe fallar si contraseña es null" classname="com.mathtutor.common.auth.domain.LoginUseCaseTest$InputValidation" time="0.002"/>

### com.mathtutor.common.auth.domain.LoginUseCaseTest$InvalidCredentials

| Métrica | Valor |
|---------|-------|
| Tests | 3 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.032 |

#### Tests omitidos:
<testsuite name="Credenciales inválidas" tests="3" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:49.993Z" hostname="DESKTOP-KREG294" time="0.032">	  <testcase name="Debe fallar si usuario está inactivo (no encontrado por findActiveByEmail)" classname="com.mathtutor.common.auth.domain.LoginUseCaseTest$InvalidCredentials" time="0.022"/>

### com.mathtutor.common.auth.domain.LoginUseCaseTest$RateLimiting

| Métrica | Valor |
|---------|-------|
| Tests | 6 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.026 |

#### Tests omitidos:
<testsuite name="Rate Limiting (ASVS L1: 5 intentos / 15 min)" tests="6" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:49.966Z" hostname="DESKTOP-KREG294" time="0.026">	  <testcase name="Debe aplicar rate limit antes de verificar usuario (previene enumeración)" classname="com.mathtutor.common.auth.domain.LoginUseCaseTest$RateLimiting" time="0.003"/>

### com.mathtutor.common.auth.domain.LoginUseCaseTest$SuccessfulLogin

| Métrica | Valor |
|---------|-------|
| Tests | 4 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.026 |

#### Tests omitidos:
<testsuite name="Login exitoso" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:50.037Z" hostname="DESKTOP-KREG294" time="0.026">	  <testcase name="Debe autenticar PROFESOR y ADMIN" classname="com.mathtutor.common.auth.domain.LoginUseCaseTest$SuccessfulLogin" time="0.005"/>

### com.mathtutor.common.auth.domain.LogoutUseCaseTest$InputValidation

| Métrica | Valor |
|---------|-------|
| Tests | 4 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.008 |

#### Tests omitidos:
<testsuite name="Validación de entrada" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:50.067Z" hostname="DESKTOP-KREG294" time="0.008">	  <testcase name="Debe fallar si email es solo espacios" classname="com.mathtutor.common.auth.domain.LogoutUseCaseTest$InputValidation" time="0.002"/>

### com.mathtutor.common.auth.domain.LogoutUseCaseTest$SuccessfulLogout

| Métrica | Valor |
|---------|-------|
| Tests | 3 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.011 |

#### Tests omitidos:
<testsuite name="Logout exitoso" tests="3" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:50.075Z" hostname="DESKTOP-KREG294" time="0.011">	  <testcase name="Debe normalizar email a minúsculas" classname="com.mathtutor.common.auth.domain.LogoutUseCaseTest$SuccessfulLogout" time="0.004"/>

### com.mathtutor.common.auth.domain.RegisterUserUseCaseTest$EmailValidation

| Métrica | Valor |
|---------|-------|
| Tests | 5 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.015 |

#### Tests omitidos:
<testsuite name="Validación de email" tests="5" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:50.131Z" hostname="DESKTOP-KREG294" time="0.015">	  <testcase name="Debe fallar si email tiene formato inválido" classname="com.mathtutor.common.auth.domain.RegisterUserUseCaseTest$EmailValidation" time="0.002"/>

### com.mathtutor.common.auth.domain.RegisterUserUseCaseTest$ErrorHandling

| Métrica | Valor |
|---------|-------|
| Tests | 1 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.011 |

#### Tests omitidos:
<testsuite name="Manejo de errores" tests="1" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:50.090Z" hostname="DESKTOP-KREG294" time="0.011">	  <testcase name="Debe fallar si repositorio lanza excepción al guardar" classname="com.mathtutor.common.auth.domain.RegisterUserUseCaseTest$ErrorHandling" time="0.011"/>

### com.mathtutor.common.auth.domain.RegisterUserUseCaseTest$PasswordValidation

| Métrica | Valor |
|---------|-------|
| Tests | 7 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.025 |

#### Tests omitidos:
<testsuite name="Validación de contraseña" tests="7" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:50.105Z" hostname="DESKTOP-KREG294" time="0.025">	  <testcase name="Debe fallar si contraseña no tiene carácter especial" classname="com.mathtutor.common.auth.domain.RegisterUserUseCaseTest$PasswordValidation" time="0.002"/>

### com.mathtutor.common.auth.domain.RegisterUserUseCaseTest$RoleValidation

| Métrica | Valor |
|---------|-------|
| Tests | 1 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.002 |

#### Tests omitidos:
<testsuite name="Validación de rol" tests="1" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:50.102Z" hostname="DESKTOP-KREG294" time="0.002">	  <testcase name="Debe fallar si rol es null" classname="com.mathtutor.common.auth.domain.RegisterUserUseCaseTest$RoleValidation" time="0.001"/>

### com.mathtutor.common.auth.domain.RegisterUserUseCaseTest$SuccessfulRegistration

| Métrica | Valor |
|---------|-------|
| Tests | 2 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.01 |

#### Tests omitidos:
<testsuite name="Registro exitoso" tests="2" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:50.147Z" hostname="DESKTOP-KREG294" time="0.01">	  <testcase name="Debe permitir registrar PROFESOR y ADMIN" classname="com.mathtutor.common.auth.domain.RegisterUserUseCaseTest$SuccessfulRegistration" time="0.004"/>

### com.mathtutor.common.dto.CanonicalStepTest

| Métrica | Valor |
|---------|-------|
| Tests | 3 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.014 |

#### Tests omitidos:
<testsuite name="com.mathtutor.common.dto.CanonicalStepTest" tests="3" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:50.157Z" hostname="DESKTOP-KREG294" time="0.014">	  <testcase name="shouldCreateCanonicalStepWithAllFields()" classname="com.mathtutor.common.dto.CanonicalStepTest" time="0.004"/>

### com.mathtutor.common.dto.ErrorTypeTest

| Métrica | Valor |
|---------|-------|
| Tests | 4 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.039 |

#### Tests omitidos:
<testsuite name="com.mathtutor.common.dto.ErrorTypeTest" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:50.171Z" hostname="DESKTOP-KREG294" time="0.039">	  <testcase name="shouldHaveFiveTypes()" classname="com.mathtutor.common.dto.ErrorTypeTest" time="0.023"/>

### com.mathtutor.common.dto.ExerciseRunTest

| Métrica | Valor |
|---------|-------|
| Tests | 5 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.031 |

#### Tests omitidos:
<testsuite name="com.mathtutor.common.dto.ExerciseRunTest" tests="5" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:50.211Z" hostname="DESKTOP-KREG294" time="0.031">	  <testcase name="shouldHaveThreeRunStates()" classname="com.mathtutor.common.dto.ExerciseRunTest" time="0.002"/>

### com.mathtutor.common.dto.ExerciseTest

| Métrica | Valor |
|---------|-------|
| Tests | 4 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.021 |

#### Tests omitidos:
<testsuite name="com.mathtutor.common.dto.ExerciseTest" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:50.243Z" hostname="DESKTOP-KREG294" time="0.021">	  <testcase name="shouldHaveThreeExerciseTypes()" classname="com.mathtutor.common.dto.ExerciseTest" time="0.002"/>

### com.mathtutor.common.dto.GraphicSpecTest

| Métrica | Valor |
|---------|-------|
| Tests | 9 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.127 |

#### Tests omitidos:
<testsuite name="com.mathtutor.common.dto.GraphicSpecTest" tests="9" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:50.264Z" hostname="DESKTOP-KREG294" time="0.127">	  <testcase name="shouldCreateGraphicSpecWithAllFields()" classname="com.mathtutor.common.dto.GraphicSpecTest" time="0.004"/>

### com.mathtutor.common.dto.HelpLevelTest

| Métrica | Valor |
|---------|-------|
| Tests | 5 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.01 |

#### Tests omitidos:
<testsuite name="com.mathtutor.common.dto.HelpLevelTest" tests="5" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:50.392Z" hostname="DESKTOP-KREG294" time="0.01">	  <testcase name="shouldCompareCorrectly()" classname="com.mathtutor.common.dto.HelpLevelTest" time="0.006"/>

### com.mathtutor.common.dto.NoteChunkTest

| Métrica | Valor |
|---------|-------|
| Tests | 3 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.017 |

#### Tests omitidos:
<testsuite name="com.mathtutor.common.dto.NoteChunkTest" tests="3" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:50.402Z" hostname="DESKTOP-KREG294" time="0.017">	  <testcase name="shouldCreateNoteChunkWithAllFields()" classname="com.mathtutor.common.dto.NoteChunkTest" time="0.005"/>

### com.mathtutor.common.dto.PedagogicalActionTest

| Métrica | Valor |
|---------|-------|
| Tests | 10 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.029 |

#### Tests omitidos:
<testsuite name="com.mathtutor.common.dto.PedagogicalActionTest" tests="10" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:50.419Z" hostname="DESKTOP-KREG294" time="0.029">	  <testcase name="shouldCreateAnalogyAction()" classname="com.mathtutor.common.dto.PedagogicalActionTest" time="0.001"/>

### com.mathtutor.common.dto.PromptTest

| Métrica | Valor |
|---------|-------|
| Tests | 4 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.01 |

#### Tests omitidos:
<testsuite name="com.mathtutor.common.dto.PromptTest" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:50.449Z" hostname="DESKTOP-KREG294" time="0.01">	  <testcase name="shouldAllowBoundaryValues()" classname="com.mathtutor.common.dto.PromptTest" time="0.002"/>

### com.mathtutor.common.dto.RoleTest

| Métrica | Valor |
|---------|-------|
| Tests | 4 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.006 |

#### Tests omitidos:
<testsuite name="com.mathtutor.common.dto.RoleTest" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:50.460Z" hostname="DESKTOP-KREG294" time="0.006">	  <testcase name="shouldParseFromString()" classname="com.mathtutor.common.dto.RoleTest" time="0.002"/>

### com.mathtutor.common.dto.SessionStateTest

| Métrica | Valor |
|---------|-------|
| Tests | 3 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.011 |

#### Tests omitidos:
<testsuite name="com.mathtutor.common.dto.SessionStateTest" tests="3" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:50.467Z" hostname="DESKTOP-KREG294" time="0.011">	  <testcase name="shouldCreateSessionStateWithAllFields()" classname="com.mathtutor.common.dto.SessionStateTest" time="0.007"/>

### com.mathtutor.common.dto.TurnEventTest

| Métrica | Valor |
|---------|-------|
| Tests | 8 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.024 |

#### Tests omitidos:
<testsuite name="com.mathtutor.common.dto.TurnEventTest" tests="8" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:50.480Z" hostname="DESKTOP-KREG294" time="0.024">	  <testcase name="shouldCreateTokenEvent()" classname="com.mathtutor.common.dto.TurnEventTest" time="0.003"/>

### com.mathtutor.common.dto.TurnTest

| Métrica | Valor |
|---------|-------|
| Tests | 3 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.002 |

#### Tests omitidos:
<testsuite name="com.mathtutor.common.dto.TurnTest" tests="3" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:50.505Z" hostname="DESKTOP-KREG294" time="0.002">	  <testcase name="shouldCreateTurnWithoutGraphicSpec()" classname="com.mathtutor.common.dto.TurnTest" time="0.001"/>

### com.mathtutor.common.dto.TypicalErrorTest

| Métrica | Valor |
|---------|-------|
| Tests | 2 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.0 |

#### Tests omitidos:
<testsuite name="com.mathtutor.common.dto.TypicalErrorTest" tests="2" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:50.508Z" hostname="DESKTOP-KREG294" time="0.0">	  <testcase name="shouldCreateTypicalErrorWithAllFields()" classname="com.mathtutor.common.dto.TypicalErrorTest" time="0.0"/>

### com.mathtutor.graphics.GraphicSpecServiceImplTest

| Métrica | Valor |
|---------|-------|
| Tests | 3 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.189 |

#### Tests omitidos:
<testsuite name="com.mathtutor.graphics.GraphicSpecServiceImplTest" tests="3" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:52.742Z" hostname="DESKTOP-KREG294" time="0.189">	  <testcase name="buildWithNullPointsReturnsEmptyList()" classname="com.mathtutor.graphics.GraphicSpecServiceImplTest" time="0.143"/>

### com.mathtutor.llm.DeterministicEmbeddingModelTest

| Métrica | Valor |
|---------|-------|
| Tests | 25 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.217 |

#### Tests omitidos:
<testsuite name="com.mathtutor.llm.DeterministicEmbeddingModelTest" tests="25" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:56.165Z" hostname="DESKTOP-KREG294" time="0.217">	  <testcase name="Embed de lista con nulls maneja gracefully" classname="com.mathtutor.llm.DeterministicEmbeddingModelTest" time="0.124"/>

### com.mathtutor.llm.SecurityNoApiKeyInLogsTest

| Métrica | Valor |
|---------|-------|
| Tests | 5 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.035 |

#### Tests omitidos:
<testsuite name="com.mathtutor.llm.SecurityNoApiKeyInLogsTest" tests="5" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:56.384Z" hostname="DESKTOP-KREG294" time="0.035">	  <testcase name="testApiKeyUsedOnlyInHeaders()" classname="com.mathtutor.llm.SecurityNoApiKeyInLogsTest" time="0.005"/>

### com.mathtutor.llm.SpringAiPgVectorConfigTest

| Métrica | Valor |
|---------|-------|
| Tests | 2 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 3.156 |

#### Tests omitidos:
<testsuite name="com.mathtutor.llm.SpringAiPgVectorConfigTest" tests="2" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:56.420Z" hostname="DESKTOP-KREG294" time="3.156">	  <testcase name="hasEmbeddingModelWithDeterministicProfile()" classname="com.mathtutor.llm.SpringAiPgVectorConfigTest" time="3.108"/>

### com.mathtutor.llm.retriever.PgVectorRetrieverBlackBoxTest

| Métrica | Valor |
|---------|-------|
| Tests | 7 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.239 |

#### Tests omitidos:
<testsuite name="com.mathtutor.llm.retriever.PgVectorRetrieverBlackBoxTest" tests="7" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:59.591Z" hostname="DESKTOP-KREG294" time="0.239">	  <testcase name="Recupera chunks relevantes para una consulta" classname="com.mathtutor.llm.retriever.PgVectorRetrieverBlackBoxTest" time="0.204"/>

### com.mathtutor.llm.retriever.PgVectorRetrieverWhiteBoxTest

| Métrica | Valor |
|---------|-------|
| Tests | 11 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.446 |

#### Tests omitidos:
<testsuite name="com.mathtutor.llm.retriever.PgVectorRetrieverWhiteBoxTest" tests="11" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:54:59.837Z" hostname="DESKTOP-KREG294" time="0.446">	  <testcase name="retrieve sin filtro de módulo ni tema" classname="com.mathtutor.llm.retriever.PgVectorRetrieverWhiteBoxTest" time="0.121"/>

### com.mathtutor.persistence.repository.AuditLogRepositoryTest

| Métrica | Valor |
|---------|-------|
| Tests | 5 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 8.265 |

#### Tests omitidos:
<testsuite name="com.mathtutor.persistence.repository.AuditLogRepositoryTest" tests="5" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:55:03.356Z" hostname="DESKTOP-KREG294" time="8.265">	  <testcase name="shouldFindByAction()" classname="com.mathtutor.persistence.repository.AuditLogRepositoryTest" time="0.935"/>

### com.mathtutor.persistence.repository.CanonicalStepRepositoryTest

| Métrica | Valor |
|---------|-------|
| Tests | 3 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.06 |

#### Tests omitidos:
<testsuite name="com.mathtutor.persistence.repository.CanonicalStepRepositoryTest" tests="3" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:55:11.623Z" hostname="DESKTOP-KREG294" time="0.06">	  <testcase name="shouldFindByExerciseAndPosition()" classname="com.mathtutor.persistence.repository.CanonicalStepRepositoryTest" time="0.023"/>

### com.mathtutor.persistence.repository.ConsentRecordRepositoryTest

| Métrica | Valor |
|---------|-------|
| Tests | 3 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.041 |

#### Tests omitidos:
<testsuite name="com.mathtutor.persistence.repository.ConsentRecordRepositoryTest" tests="3" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:55:11.684Z" hostname="DESKTOP-KREG294" time="0.041">	  <testcase name="shouldFindByUserAndType()" classname="com.mathtutor.persistence.repository.ConsentRecordRepositoryTest" time="0.011"/>

### com.mathtutor.persistence.repository.ExerciseRepositoryTest

| Métrica | Valor |
|---------|-------|
| Tests | 6 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.087 |

#### Tests omitidos:
<testsuite name="com.mathtutor.persistence.repository.ExerciseRepositoryTest" tests="6" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:55:11.726Z" hostname="DESKTOP-KREG294" time="0.087">	  <testcase name="shouldFindByType()" classname="com.mathtutor.persistence.repository.ExerciseRepositoryTest" time="0.016"/>

### com.mathtutor.persistence.repository.ExerciseRunRepositoryTest

| Métrica | Valor |
|---------|-------|
| Tests | 5 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.095 |

#### Tests omitidos:
<testsuite name="com.mathtutor.persistence.repository.ExerciseRunRepositoryTest" tests="5" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:55:11.814Z" hostname="DESKTOP-KREG294" time="0.095">	  <testcase name="shouldFindByExercise()" classname="com.mathtutor.persistence.repository.ExerciseRunRepositoryTest" time="0.02"/>

### com.mathtutor.persistence.repository.KeyConceptRepositoryTest

| Métrica | Valor |
|---------|-------|
| Tests | 2 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.026 |

#### Tests omitidos:
<testsuite name="com.mathtutor.persistence.repository.KeyConceptRepositoryTest" tests="2" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:55:11.910Z" hostname="DESKTOP-KREG294" time="0.026">	  <testcase name="shouldReturnEmptyWhenNoConcepts()" classname="com.mathtutor.persistence.repository.KeyConceptRepositoryTest" time="0.008"/>

### com.mathtutor.persistence.repository.ModuleRepositoryTest

| Métrica | Valor |
|---------|-------|
| Tests | 5 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.044 |

#### Tests omitidos:
<testsuite name="com.mathtutor.persistence.repository.ModuleRepositoryTest" tests="5" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:55:11.937Z" hostname="DESKTOP-KREG294" time="0.044">	  <testcase name="shouldSaveAndFindModuleByCode()" classname="com.mathtutor.persistence.repository.ModuleRepositoryTest" time="0.007"/>

### com.mathtutor.persistence.repository.NoteChunkRepositoryTest

| Métrica | Valor |
|---------|-------|
| Tests | 5 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.058 |

#### Tests omitidos:
<testsuite name="com.mathtutor.persistence.repository.NoteChunkRepositoryTest" tests="5" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:55:11.982Z" hostname="DESKTOP-KREG294" time="0.058">	  <testcase name="shouldFindSimilarByModuleAndTopic()" classname="com.mathtutor.persistence.repository.NoteChunkRepositoryTest" time="0.016"/>

### com.mathtutor.persistence.repository.PasswordResetTokenRepositoryTest

| Métrica | Valor |
|---------|-------|
| Tests | 5 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.051 |

#### Tests omitidos:
<testsuite name="com.mathtutor.persistence.repository.PasswordResetTokenRepositoryTest" tests="5" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:55:12.040Z" hostname="DESKTOP-KREG294" time="0.051">	  <testcase name="shouldSaveAndFindTokenByHash()" classname="com.mathtutor.persistence.repository.PasswordResetTokenRepositoryTest" time="0.01"/>

### com.mathtutor.persistence.repository.TopicRepositoryTest

| Métrica | Valor |
|---------|-------|
| Tests | 4 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.04 |

#### Tests omitidos:
<testsuite name="com.mathtutor.persistence.repository.TopicRepositoryTest" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:55:12.092Z" hostname="DESKTOP-KREG294" time="0.04">	  <testcase name="shouldFindTopicsByModuleOrderedByDifficulty()" classname="com.mathtutor.persistence.repository.TopicRepositoryTest" time="0.01"/>

### com.mathtutor.persistence.repository.TutorSessionRepositoryTest

| Métrica | Valor |
|---------|-------|
| Tests | 5 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.07 |

#### Tests omitidos:
<testsuite name="com.mathtutor.persistence.repository.TutorSessionRepositoryTest" tests="5" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:55:12.132Z" hostname="DESKTOP-KREG294" time="0.07">	  <testcase name="shouldFindByUserAndStatusOrderByLastActivityDesc()" classname="com.mathtutor.persistence.repository.TutorSessionRepositoryTest" time="0.008"/>

### com.mathtutor.persistence.repository.TypicalErrorRepositoryTest

| Métrica | Valor |
|---------|-------|
| Tests | 3 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.054 |

#### Tests omitidos:
<testsuite name="com.mathtutor.persistence.repository.TypicalErrorRepositoryTest" tests="3" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:55:12.203Z" hostname="DESKTOP-KREG294" time="0.054">	  <testcase name="shouldFindByCanonicalStepAndCode()" classname="com.mathtutor.persistence.repository.TypicalErrorRepositoryTest" time="0.025"/>

### com.mathtutor.persistence.repository.UsageQuotaRepositoryTest

| Métrica | Valor |
|---------|-------|
| Tests | 4 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.049 |

#### Tests omitidos:
<testsuite name="com.mathtutor.persistence.repository.UsageQuotaRepositoryTest" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:55:12.258Z" hostname="DESKTOP-KREG294" time="0.049">	  <testcase name="shouldEnforceUniqueConstraintUserAndDay()" classname="com.mathtutor.persistence.repository.UsageQuotaRepositoryTest" time="0.018"/>

### com.mathtutor.persistence.repository.UserRepositoryTest

| Métrica | Valor |
|---------|-------|
| Tests | 8 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.07 |

#### Tests omitidos:
<testsuite name="com.mathtutor.persistence.repository.UserRepositoryTest" tests="8" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:55:12.308Z" hostname="DESKTOP-KREG294" time="0.07">	  <testcase name="shouldCascadeDeletePasswordResetTokens()" classname="com.mathtutor.persistence.repository.UserRepositoryTest" time="0.009"/>

### com.mathtutor.tutor.PedagogicalPolicyImplTest

| Métrica | Valor |
|---------|-------|
| Tests | 4 |
| Fallos | 0 |
| Errores | 0 |
| Omitidos | 0 |
| Tiempo (s) | 0.175 |

#### Tests omitidos:
<testsuite name="com.mathtutor.tutor.PedagogicalPolicyImplTest" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-11-13T00:55:16.404Z" hostname="DESKTOP-KREG294" time="0.175">	  <testcase name="secondAttemptReturnsProceduralGuidance()" classname="com.mathtutor.tutor.PedagogicalPolicyImplTest" time="0.149"/>

