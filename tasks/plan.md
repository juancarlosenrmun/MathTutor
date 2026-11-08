# Implementation Plan: UC-01 Registrarse, iniciar y cerrar sesión

## Overview
Implement secure user registration, login, and logout (UC-01) following hexagonal architecture, TDD, and security requirements (RNF-05, RNF-06, RNF-11, RNF-13). This includes:
- Argon2id password hashing (preferred per RNF-05)
- Spring Security configuration with CSRF, secure HttpOnly SameSite cookies
- REST endpoints: POST /api/auth/register, POST /api/auth/login, POST /api/auth/logout
- Domain use cases in mathtutor-common
- Adapters in mathtutor-persistence (password encoder) and mathtutor-web (REST controllers, security config)
- Comprehensive tests (white-box unit, black-box MockMvc, Testcontainers integration)
- Audit logging for authentication events (RNF-13)

## Architecture Decisions

1. **Session Management**: JWT tokens stored in secure HttpOnly SameSite=Strict cookies (not localStorage per RNF-06). Refresh token rotation with short-lived access tokens.

2. **Password Encoding**: Argon2id implementation of PasswordEncoderPort in mathtutor-persistence (preferred over BCrypt per RNF-05).

3. **Module Structure**:
   - `mathtutor-common`: Domain use cases (RegisterUserUseCase, LoginUseCase, LogoutUseCase), ports, domain events
   - `mathtutor-persistence`: PasswordEncoderAdapter (Argon2id), UserRepositoryAdapter, Flyway migrations for users, password_reset_tokens, audit_logs, consent_records
   - `mathtutor-web`: REST controllers, Spring Security config, JWT token service, cookie-based auth filter (re-added to settings.gradle.kts)

4. **Security**:
   - CSRF protection via Spring Security (double-submit cookie pattern for stateless JWT)
   - Secure; HttpOnly; SameSite=Strict cookies
   - TLS 1.2+ enforced in production
   - No secrets in repo (config via environment variables)

5. **Audit**: Domain events published on register/login/logout, persisted via AuditLog entity

## Task List

### Phase 1: Foundation - Domain & Ports (mathtutor-common)

#### Task 1: Create Auth Domain Package Structure & Domain Events
**Description:** Create package structure for auth domain and define domain events for audit trail.

**Acceptance criteria:**
- [ ] Package `com.mathtutor.common.auth.domain` exists
- [ ] Domain event `UserRegisteredEvent` with userId, email, timestamp
- [ ] Domain event `UserLoggedInEvent` with userId, email, timestamp, ipAddress
- [ ] Domain event `UserLoggedOutEvent` with userId, email, timestamp
- [ ] Domain event `UserRegistrationFailedEvent` with email, reason, timestamp
- [ ] Domain event `UserLoginFailedEvent` with email, reason, timestamp
- [ ] All events implement `DomainEvent` interface with `occurredAt()`

**Verification:**
- [ ] Tests pass: `./gradlew :mathtutor-common:test --no-daemon --no-scan`
- [ ] Build succeeds: `./gradlew :mathtutor-common:build --no-daemon --no-scan`

**Dependencies:** None

**Files likely touched:**
- `mathtutor-common/src/main/java/com/mathtutor/common/auth/domain/DomainEvent.java`
- `mathtutor-common/src/main/java/com/mathtutor/common/auth/domain/UserRegisteredEvent.java`
- `mathtutor-common/src/main/java/com/mathtutor/common/auth/domain/UserLoggedInEvent.java`
- `mathtutor-common/src/main/java/com/mathtutor/common/auth/domain/UserLoggedOutEvent.java`
- `mathtutor-common/src/main/java/com/mathtutor/common/auth/domain/UserRegistrationFailedEvent.java`
- `mathtutor-common/src/main/java/com/mathtutor/common/auth/domain/UserLoginFailedEvent.java`

**Estimated scope:** S (1-2 files for events, 1 for interface)

#### Task 2: Create RegisterUserUseCase (TDD - Red Phase)
**Description:** Write failing test for RegisterUserUseCase, then implement the use case.

**Acceptance criteria:**
- [ ] Test `RegisterUserUseCaseTest` written first (failing)
- [ ] Use case validates email format, password strength (min 8 chars, 1 upper, 1 lower, 1 digit, 1 special)
- [ ] Use case checks email uniqueness via UserRepositoryPort
- [ ] Use case encodes password via PasswordEncoderPort
- [ ] Use case saves User via UserRepositoryPort
- [ ] Use case publishes UserRegisteredEvent on success
- [ ] Use case publishes UserRegistrationFailedEvent on failure
- [ ] Returns Result with userId or error

**Verification:**
- [ ] Tests pass: `./gradlew :mathtutor-common:test --no-daemon --no-scan`
- [ ] Coverage ≥ 80% lines, ≥ 70% branches for use case

**Dependencies:** Task 1

**Files likely touched:**
- `mathtutor-common/src/main/java/com/mathtutor/common/auth/domain/RegisterUserUseCase.java`
- `mathtutor-common/src/main/java/com/mathtutor/common/auth/port/UserRepositoryPort.java`
- `mathtutor-common/src/test/java/com/mathtutor/common/auth/domain/RegisterUserUseCaseTest.java`

**Estimated scope:** M (3-5 files)

#### Task 3: Create LoginUseCase (TDD - Red Phase)
**Description:** Write failing test for LoginUseCase, then implement the use case.

**Acceptance criteria:**
- [ ] Test `LoginUseCaseTest` written first (failing)
- [ ] Use case finds active user by email via UserRepositoryPort
- [ ] Use case verifies password via PasswordEncoderPort
- [ ] Use case publishes UserLoggedInEvent on success (with IP address)
- [ ] Use case publishes UserLoginFailedEvent on failure
- [ ] Returns Result with userId, role, or error

**Verification:**
- [ ] Tests pass: `./gradlew :mathtutor-common:test --no-daemon --no-scan`
- [ ] Coverage ≥ 80% lines, ≥ 70% branches for use case

**Dependencies:** Task 1

**Files likely touched:**
- `mathtutor-common/src/main/java/com/mathtutor/common/auth/domain/LoginUseCase.java`
- `mathtutor-common/src/test/java/com/mathtutor/common/auth/domain/LoginUseCaseTest.java`

**Estimated scope:** M (2-3 files)

#### Task 4: Create LogoutUseCase (TDD - Red Phase)
**Description:** Write failing test for LogoutUseCase, then implement the use case.

**Acceptance criteria:**
- [ ] Test `LogoutUseCaseTest` written first (failing)
- [ ] Use case publishes UserLoggedOutEvent
- [ ] Returns Result indicating success

**Verification:**
- [ ] Tests pass: `./gradlew :mathtutor-common:test --no-daemon --no-scan`
- [ ] Coverage ≥ 80% lines, ≥ 70% branches for use case

**Dependencies:** Task 1

**Files likely touched:**
- `mathtutor-common/src/main/java/com/mathtutor/common/auth/domain/LogoutUseCase.java`
- `mathtutor-common/src/test/java/com/mathtutor/common/auth/domain/LogoutUseCaseTest.java`

**Estimated scope:** S (2 files)

#### Task 5: Create Auth Ports (UserRepositoryPort, TokenServicePort)
**Description:** Define ports for UserRepository and TokenService (JWT).

**Acceptance criteria:**
- [ ] `UserRepositoryPort` interface with findByEmail, existsByEmail, save, findActiveByEmail
- [ ] `TokenServicePort` interface with generateAccessToken, generateRefreshToken, validateToken, extractClaims
- [ ] Ports in `com.mathtutor.common.auth.port` package

**Verification:**
- [ ] Tests pass: `./gradlew :mathtutor-common:test --no-daemon --no-scan`
- [ ] Build succeeds

**Dependencies:** Task 1

**Files likely touched:**
- `mathtutor-common/src/main/java/com/mathtutor/common/auth/port/UserRepositoryPort.java`
- `mathtutor-common/src/main/java/com/mathtutor/common/auth/port/TokenServicePort.java`

**Estimated scope:** S (2 files)

### Checkpoint: Foundation Complete
- [ ] All mathtutor-common tests pass
- [ ] Coverage thresholds met for mathtutor-common
- [ ] ArchUnit passes

---

### Phase 2: Persistence Adapters (mathtutor-persistence)

#### Task 6: Add Argon2id Dependency & Create PasswordEncoderAdapter (TDD)
**Description:** Add Argon2id library dependency and implement PasswordEncoderPort with Argon2id.

**Acceptance criteria:**
- [ ] Dependency `org.bouncycastle:bcprov-jdk18on:1.78` or `com.lambdaworks:scrypt:1.4.0` added (Argon2id)
- [ ] Test `PasswordEncoderAdapterTest` written first (failing)
- [ ] Implementation uses Argon2id with secure params (memory=65536, iterations=3, parallelism=4)
- [ ] `encode()` returns Argon2id hash
- [ ] `matches()` verifies password against hash
- [ ] Handles null/empty inputs gracefully

**Verification:**
- [ ] Tests pass: `./gradlew :mathtutor-persistence:test --no-daemon --no-scan`
- [ ] Coverage ≥ 80% lines, ≥ 70% branches for adapter

**Dependencies:** Task 5

**Files likely touched:**
- `mathtutor-persistence/build.gradle.kts` (add dependency)
- `mathtutor-persistence/src/main/java/com/mathtutor/persistence/adapter/PasswordEncoderAdapter.java`
- `mathtutor-persistence/src/test/java/com/mathtutor/persistence/adapter/PasswordEncoderAdapterTest.java`

**Estimated scope:** M (3 files)

#### Task 7: Create UserRepositoryAdapter Implementing UserRepositoryPort
**Description:** Implement UserRepositoryPort using Spring Data JPA UserRepository.

**Acceptance criteria:**
- [ ] Test `UserRepositoryAdapterTest` written first (failing)
- [ ] Implements all methods from UserRepositoryPort
- [ ] Maps between User entity and domain User (if domain User exists) or uses entity directly
- [ ] Uses existing UserRepository from persistence

**Verification:**
- [ ] Tests pass: `./gradlew :mathtutor-persistence:test --no-daemon --no-scan`
- [ ] Coverage ≥ 80% lines, ≥ 70% branches for adapter

**Dependencies:** Task 5

**Files likely touched:**
- `mathtutor-persistence/src/main/java/com/mathtutor/persistence/adapter/UserRepositoryAdapter.java`
- `mathtutor-persistence/src/test/java/com/mathtutor/persistence/adapter/UserRepositoryAdapterTest.java`

**Estimated scope:** M (2 files)

#### Task 8: Create Flyway Migrations for Auth Tables
**Description:** Create Flyway migration scripts for users, password_reset_tokens, audit_logs, consent_records tables.

**Acceptance criteria:**
- [ ] Migration `V1__create_auth_tables.sql` creates all four tables with proper indexes, constraints
- [ ] Migration `V2__add_auth_indexes.sql` adds performance indexes
- [ ] Tables match JPA entities exactly
- [ ] Compatible with PostgreSQL and H2 (for tests)

**Verification:**
- [ ] `./gradlew :mathtutor-application:flywayMigrate --no-daemon --no-scan` works with test profile
- [ ] Tables created correctly in test DB

**Dependencies:** None (can run in parallel with Phase 1)

**Files likely touched:**
- `mathtutor-application/src/main/resources/db/migration/V1__create_auth_tables.sql`
- `mathtutor-application/src/main/resources/db/migration/V2__add_auth_indexes.sql`

**Estimated scope:** M (2 SQL files)

#### Task 9: Create AuditLogRepositoryAdapter for Domain Event Persistence
**Description:** Adapter to persist domain events as AuditLog entries.

**Acceptance criteria:**
- [ ] Test `AuditLogRepositoryAdapterTest` written first (failing)
- [ ] Implements `AuditLogPort` (to be created in common)
- [ ] Saves AuditLog entities for each domain event
- [ ] Includes actor (user), action, targetType, targetId, timestamp

**Verification:**
- [ ] Tests pass: `./gradlew :mathtutor-persistence:test --no-daemon --no-scan`
- [ ] Coverage ≥ 80% lines, ≥ 70% branches

**Dependencies:** Task 1 (DomainEvent interface), Task 8 (migrations)

**Files likely touched:**
- `mathtutor-common/src/main/java/com/mathtutor/common/auth/port/AuditLogPort.java`
- `mathtutor-persistence/src/main/java/com/mathtutor/persistence/adapter/AuditLogRepositoryAdapter.java`
- `mathtutor-persistence/src/test/java/com/mathtutor/persistence/adapter/AuditLogRepositoryAdapterTest.java`

**Estimated scope:** M (3 files)

### Checkpoint: Persistence Adapters Complete
- [ ] All mathtutor-persistence tests pass
- [ ] Coverage thresholds met
- [ ] Flyway migrations work
- [ ] ArchUnit passes

---

### Phase 3: Web Layer - REST API & Security (mathtutor-web)

#### Task 10: Add mathtutor-web to settings.gradle.kts
**Description:** Re-add mathtutor-web module to the build.

**Acceptance criteria:**
- [ ] `include("mathtutor-web")` in settings.gradle.kts
- [ ] Module builds successfully

**Verification:**
- [ ] `./gradlew :mathtutor-web:build --no-daemon --no-scan` succeeds

**Dependencies:** None

**Files likely touched:**
- `settings.gradle.kts`

**Estimated scope:** XS (1 line)

#### Task 11: Create JWT TokenServiceAdapter (TDD)
**Description:** Implement TokenServicePort with JWT tokens (access + refresh).

**Acceptance criteria:**
- [ ] Test `JwtTokenServiceAdapterTest` written first (failing)
- [ ] Access token: 15 min expiry, contains userId, email, role
- [ ] Refresh token: 7 days expiry, contains userId, tokenId (for rotation)
- [ ] RS256 signing (keys from config, not hardcoded)
- [ ] `validateToken()` verifies signature, expiry, claims
- [ ] `extractClaims()` returns parsed claims

**Verification:**
- [ ] Tests pass: `./gradlew :mathtutor-web:test --no-daemon --no-scan`
- [ ] Coverage ≥ 80% lines, ≥ 70% branches

**Dependencies:** Task 5 (TokenServicePort), Task 10

**Files likely touched:**
- `mathtutor-web/src/main/java/com/mathtutor/web/security/JwtTokenServiceAdapter.java`
- `mathtutor-web/src/test/java/com/mathtutor/web/security/JwtTokenServiceAdapterTest.java`
- `mathtutor-web/src/main/resources/application.yml` (JWT config)

**Estimated scope:** M (3 files)

#### Task 12: Create Spring Security Configuration (TDD)
**Description:** Configure Spring Security with CSRF, secure cookies, JWT filter, endpoint authorization.

**Acceptance criteria:**
- [ ] Test `SecurityConfigTest` written first (failing - MockMvc tests)
- [ ] CSRF enabled with cookie-based double-submit for stateless JWT
- [ ] Cookie config: Secure=true, HttpOnly=true, SameSite=Strict
- [ ] JWT authentication filter for `/api/**` (except `/api/auth/register`, `/api/auth/login`)
- [ ] `/api/auth/**` permitAll
- [ ] Role-based authorization: ADMIN, PROFESOR, ALUMNO
- [ ] Session management: STATELESS
- [ ] Security headers (HSTS, X-Frame-Options, etc.)

**Verification:**
- [ ] Tests pass: `./gradlew :mathtutor-web:test --no-daemon --no-scan`
- [ ] Coverage ≥ 80% lines, ≥ 70% branches for config

**Dependencies:** Task 11, Task 10

**Files likely touched:**
- `mathtutor-web/src/main/java/com/mathtutor/web/security/SecurityConfig.java`
- `mathtutor-web/src/main/java/com/mathtutor/web/security/JwtAuthenticationFilter.java`
- `mathtutor-web/src/test/java/com/mathtutor/web/security/SecurityConfigTest.java`

**Estimated scope:** L (4-5 files) - Consider splitting

#### Task 13: Create AuthController with Register/Login/Logout Endpoints (TDD)
**Description:** REST controller for authentication endpoints.

**Acceptance criteria:**
- [ ] Test `AuthControllerTest` written first (failing - MockMvc)
- [ ] POST `/api/auth/register` - validates input, calls RegisterUserUseCase, sets auth cookies on success
- [ ] POST `/api/auth/login` - validates input, calls LoginUseCase, sets auth cookies on success
- [ ] POST `/api/auth/logout` - calls LogoutUseCase, clears auth cookies
- [ ] Request/response DTOs with validation annotations
- [ ] Proper HTTP status codes (201, 200, 400, 401, 409)
- [ ] CSRF token endpoint: GET `/api/auth/csrf`

**Verification:**
- [ ] Tests pass: `./gradlew :mathtutor-web:test --no-daemon --no-scan`
- [ ] Coverage ≥ 80% lines, ≥ 70% branches for controller

**Dependencies:** Task 2, Task 3, Task 4, Task 12, Task 10

**Files likely touched:**
- `mathtutor-web/src/main/java/com/mathtutor/web/auth/AuthController.java`
- `mathtutor-web/src/main/java/com/mathtutor/web/auth/dto/RegisterRequest.java`
- `mathtutor-web/src/main/java/com/mathtutor/web/auth/dto/LoginRequest.java`
- `mathtutor-web/src/main/java/com/mathtutor/web/auth/dto/AuthResponse.java`
- `mathtutor-web/src/test/java/com/mathtutor/web/auth/AuthControllerTest.java`

**Estimated scope:** M (5 files)

#### Task 14: Create Cookie-Based JWT Filter & Token Refresh Endpoint
**Description:** Filter to extract JWT from cookie, validate, set authentication. Refresh endpoint.

**Acceptance criteria:**
- [ ] Test `JwtAuthenticationFilterTest` written first (failing)
- [ ] Filter reads access token from `access_token` cookie
- [ ] On expiry, attempts refresh using `refresh_token` cookie
- [ ] POST `/api/auth/refresh` - rotates refresh token, issues new access token
- [ ] Secure cookie handling (no token in localStorage per RNF-06)

**Verification:**
- [ ] Tests pass: `./gradlew :mathtutor-web:test --no-daemon --no-scan`
- [ ] Coverage ≥ 80% lines, ≥ 70% branches

**Dependencies:** Task 11, Task 12

**Files likely touched:**
- `mathtutor-web/src/main/java/com/mathtutor/web/security/JwtAuthenticationFilter.java`
- `mathtutor-web/src/main/java/com/mathtutor/web/auth/AuthController.java` (add refresh endpoint)
- `mathtutor-web/src/test/java/com/mathtutor/web/security/JwtAuthenticationFilterTest.java`

**Estimated scope:** M (3 files)

### Checkpoint: Web Layer Complete
- [ ] All mathtutor-web tests pass
- [ ] Coverage thresholds met
- [ ] ArchUnit passes (no forbidden dependencies)

---

### Phase 4: Integration & Application Wiring

#### Task 15: Wire Dependencies in mathtutor-application
**Description:** Configure component scanning, bean definitions to wire use cases with adapters.

**Acceptance criteria:**
- [ ] `@ComponentScan` includes auth packages
- [ ] Use cases get adapter implementations injected
- [ ] Application starts successfully with `prod` and `test` profiles

**Verification:**
- [ ] `./gradlew :mathtutor-application:bootRun --no-daemon --no-scan` starts
- [ ] `./gradlew :mathtutor-application:test --no-daemon --no-scan` passes

**Dependencies:** All previous phases

**Files likely touched:**
- `mathtutor-application/src/main/java/com/mathtutor/application/MathTutorApplication.java`
- `mathtutor-application/src/main/resources/application.yml`

**Estimated scope:** S (2 files)

#### Task 16: Create Testcontainers Integration Tests (Black-box)
**Description:** Full integration tests with PostgreSQL + Testcontainers for auth flow.

**Acceptance criteria:**
- [ ] Test `AuthIntegrationTest` with `@Testcontainers(disabledWithoutDocker=true)`
- [ ] Tests register → login → access protected endpoint → logout → refresh token
- [ ] Tests CSRF protection, cookie security flags
- [ ] Tests password hashing with Argon2id
- [ ] Tests audit logs created for each event
- [ ] Uses `spring-ai-pgvector` profile or dedicated `test` profile

**Verification:**
- [ ] Tests pass with Docker: `./gradlew :mathtutor-application:test --no-daemon --no-scan`
- [ ] Tests SKIPPED without Docker (disabledWithoutDocker=true)

**Dependencies:** Task 8, Task 15

**Files likely touched:**
- `mathtutor-application/src/test/java/com/mathtutor/application/auth/AuthIntegrationTest.java`
- `mathtutor-application/src/test/resources/application-test.yml`

**Estimated scope:** M (2 files)

### Checkpoint: Integration Complete
- [ ] Full build passes: `./gradlew clean check --no-daemon --no-scan`
- [ ] All modules meet coverage thresholds
- [ ] ArchUnit passes
- [ ] Integration tests pass with Docker

---

### Phase 5: Documentation & Compliance

#### Task 17: Update Sprint 01 Checklist & Progress Documents
**Description:** Mark UC-01 tasks as complete in brain documents.

**Acceptance criteria:**
- [ ] `brain/04 Sprints/Sprint 01.md` - UC-01 checklist all checked
- [ ] `brain/05 Seguimiento/Progreso global.md` updated
- [ ] `brain/05 Seguimiento/Registro de iteraciones.md` updated with iteration entry

**Verification:**
- [ ] Documents updated and committed

**Dependencies:** All implementation tasks complete

**Files likely touched:**
- `brain/04 Sprints/Sprint 01.md`
- `brain/05 Seguimiento/Progreso global.md`
- `brain/05 Seguimiento/Registro de iteraciones.md`

**Estimated scope:** XS (3 files)

#### Task 18: Verify Security Requirements (RNF-05, RNF-06)
**Description:** Manual verification of security requirements.

**Acceptance criteria:**
- [ ] Argon2id used (not BCrypt) - verify in PasswordEncoderAdapter
- [ ] TLS 1.2+ enforced in production config
- [ ] CSRF protection active
- [ ] Cookies: Secure, HttpOnly, SameSite=Strict
- [ ] No secrets in repo (JWT keys from env vars)
- [ ] RGPD basics: consent records, audit logs, data minimization
- [ ] OWASP ASVS Level 1 baseline met

**Verification:**
- [ ] Manual review checklist completed
- [ ] Security scan (if available) passes

**Dependencies:** All implementation tasks

**Files likely touched:** None (verification only)

**Estimated scope:** XS (verification)

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Argon2id library compatibility with JDK 21 | High | Test early; fallback to BCrypt if Argon2id fails (but RNF-05 prefers Argon2id) |
| JWT key management in CI/CD | High | Use environment variables; document in deployment guide |
| CSRF with stateless JWT complexity | Medium | Use Spring Security's CsrfTokenRepository with cookie; test thoroughly |
| Testcontainers requiring Docker in CI | Medium | Use `disabledWithoutDocker=true`; ensure CI has Docker |
| Coverage thresholds for new modules | Medium | Write tests first (TDD); exclude config/dto from verification |
| ArchUnit violations (web depending on persistence directly) | High | Ensure web only depends on common ports; persistence adapters implement common ports |

## Open Questions

1. **JWT Signing Algorithm**: RS256 (asymmetric) vs HS256 (symmetric)? RS256 preferred for production but needs key pair management. Decision: RS256 with keys from config.

2. **Refresh Token Storage**: Store refresh token hash in DB (for revocation) or stateless with tokenId? Decision: Store hash in DB (new column in User or separate table) for revocation capability.

3. **Rate Limiting on Auth Endpoints**: Not in UC-01 scope but needed for production. Decision: Add basic rate limiting in SecurityConfig (Spring Boot Actuator + Bucket4j or Spring Cloud Gateway).

4. **Email Verification**: Not in UC-01 (UC-02 is password reset). Registration should probably require email verification. Decision: Keep simple for UC-01; email verification can be UC-02 related.

---

## Implementation Order Summary

1. **Phase 1** (Foundation): Tasks 1-5 → mathtutor-common domain & ports
2. **Phase 2** (Persistence): Tasks 6-9 → mathtutor-persistence adapters & migrations
3. **Phase 3** (Web): Tasks 10-14 → mathtutor-web REST API & security
4. **Phase 4** (Integration): Tasks 15-16 → wiring & integration tests
5. **Phase 5** (Docs): Tasks 17-18 → documentation & security verification

Each phase has a checkpoint. Do not proceed to next phase until checkpoint passes.