# Task List: UC-01 Implementation

## Phase 1: Foundation - Domain & Ports (mathtutor-common)

- [x] **Task 1**: Create Auth Domain Package Structure & Domain Events
- [x] **Task 2**: Create RegisterUserUseCase (TDD - Red Phase)
- [x] **Task 3**: Create LoginUseCase (TDD - Red Phase)
- [x] **Task 4**: Create LogoutUseCase (TDD - Red Phase)
- [x] **Task 5**: Create Auth Ports (UserRepositoryPort, TokenServicePort, AuditLogPort)

### Checkpoint: Foundation Complete
- [x] All mathtutor-common tests pass
- [x] Coverage thresholds met for mathtutor-common
- [x] ArchUnit passes

## Phase 2: Persistence Adapters (mathtutor-persistence)

- [ ] **Task 6**: Add Argon2id Dependency & Create PasswordEncoderAdapter (TDD)
- [ ] **Task 7**: Create UserRepositoryAdapter Implementing UserRepositoryPort
- [ ] **Task 8**: Create Flyway Migrations for Auth Tables
- [ ] **Task 9**: Create AuditLogRepositoryAdapter for Domain Event Persistence

### Checkpoint: Persistence Adapters Complete
- [ ] All mathtutor-persistence tests pass
- [ ] Coverage thresholds met
- [ ] Flyway migrations work
- [ ] ArchUnit passes

## Phase 3: Web Layer - REST API & Security (mathtutor-web)

- [ ] **Task 10**: Add mathtutor-web to settings.gradle.kts
- [ ] **Task 11**: Create JWT TokenServiceAdapter (TDD)
- [ ] **Task 12**: Create Spring Security Configuration (TDD)
- [ ] **Task 13**: Create AuthController with Register/Login/Logout Endpoints (TDD)
- [ ] **Task 14**: Create Cookie-Based JWT Filter & Token Refresh Endpoint

### Checkpoint: Web Layer Complete
- [ ] All mathtutor-web tests pass
- [ ] Coverage thresholds met
- [ ] ArchUnit passes (no forbidden dependencies)

## Phase 4: Integration & Application Wiring

- [ ] **Task 15**: Wire Dependencies in mathtutor-application
- [ ] **Task 16**: Create Testcontainers Integration Tests (Black-box)

### Checkpoint: Integration Complete
- [ ] Full build passes: `./gradlew clean check --no-daemon --no-scan`
- [ ] All modules meet coverage thresholds
- [ ] ArchUnit passes
- [ ] Integration tests pass with Docker

## Phase 5: Documentation & Compliance

- [ ] **Task 17**: Update Sprint 01 Checklist & Progress Documents
- [ ] **Task 18**: Verify Security Requirements (RNF-05, RNF-06)