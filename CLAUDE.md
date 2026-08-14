# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

kbuddy — a Spring Boot 4.1.0 / Java 21 REST API backend ("외국인 유학생의 한국 생활 정착과 적응을 돕는 AI 기반 생활 지원 서비스", per `OpenApiConfig`). Built with Gradle.

## Commands

Use the Gradle wrapper (`gradlew.bat` on Windows / `./gradlew` in bash).

```bash
./gradlew build          # compile + test + assemble
./gradlew test           # run all tests (JUnit 5 / useJUnitPlatform)
./gradlew test --tests "com.example.kbuddy.user.service.UserServiceTest"          # single test class
./gradlew test --tests "com.example.kbuddy.user.service.UserServiceTest.methodName"  # single test method
./gradlew bootRun        # run the app locally (needs a local MySQL + application-local.yaml, see below)
```

There is no separate lint task configured; `build` is the standard verification command.

### Local run requirements

- `spring.profiles.active` defaults to `local`, which loads `src/main/resources/application-local.yaml`. That file is git-ignored and must be created locally — it supplies the datasource password, Google/Kakao OAuth2 client id/secret, and the RSA `jwt.private-key` / `jwt.public-key` pair used to sign JWTs (RS256).
- Requires a local MySQL instance with a `kbuddy` schema (`ddl-auto: update`, see `application.yaml`).

## Architecture

### Package-by-feature layout

Code under `com.example.kbuddy` is organized by domain feature, not by layer. Each feature package (`auth`, `user`, `calendar`, `guide`) internally splits into `controller` / `service` / `dto` / `entity` / `repository` as needed. `global` holds cross-cutting concerns (`global.response`, `global.exception`, `global.security`, `global.config`). Follow this layout for new features — a new domain gets its own top-level package, not files added to an existing one.

### Auth flow: OAuth2 login → two JWT token types

Login is OAuth2-only (Google, Kakao — see `auth.oauth`), and the app issues its own JWTs afterward rather than relying on Spring's session:

1. `OAuth2UserService` (`auth/service`) loads the OAuth2 profile and determines `OAuth2LoginStatus` (`NEW_USER` / `EXISTING_USER` / `EMAIL_DUPLICATED`) by looking up `UserRepository` by provider+providerId, then by email.
2. `OAuth2LoginSuccessHandler` redirects to the frontend (`app.oauth2.redirect-uri`) with the result encoded in the URL **fragment** (not a JSON body, since this is a redirect from the OAuth provider): either a `signupToken` (new user) or `accessToken` (existing user), plus `status`.
3. `JwtProvider` issues RS256 JWTs of two kinds via `TokenType`: `SIGNUP` (short-lived, `jwt.signup-token-expiration`, carries provider/providerId/email/name claims, used only to complete `POST /api/users/me`) and `ACCESS` (`jwt.access-token-expiration`, subject = user id, used for all other authenticated endpoints).
4. `JwtAuthorityConverter` maps the JWT's `type` claim to a Spring `GrantedAuthority` of `TOKEN_SIGNUP` or `TOKEN_ACCESS`. `SecurityConfig` then authorizes individual endpoints by requiring one or the other — e.g. `POST /api/users/me` requires `TOKEN_SIGNUP`, everything else requires `TOKEN_ACCESS`. When adding a new authenticated endpoint, add an explicit `requestMatchers(...).hasAuthority(...)` rule in `SecurityConfig`; don't rely on `anyRequest().authenticated()` alone for endpoints that should be signup-token-blocked.

### Response and error conventions

- Controllers return `ResponseEntity<ApiResponse<T>>`. `ApiResponse` (`global.response`) is a simple envelope: `success(code, message, data)` / `fail(code, message)` — every endpoint returns a business `code` string in addition to the HTTP status (documented per-endpoint in the `@ApiResponses` Swagger annotations, e.g. `USER_CREATED`, `USER_INFO_FETCHED`).
- Business-rule failures are thrown as `BusinessException(ErrorCode)` from services; `ErrorCode` (`global.exception`) is the single enum mapping each error to an `HttpStatus` + code string + Korean message. Add new failure cases there rather than throwing ad hoc exceptions.
- `GlobalExceptionHandler` (`@RestControllerAdvice`) converts `BusinessException` and `MethodArgumentNotValidException` into `ApiResponse.fail(...)`. Controller-slice tests (`@WebMvcTest`) explicitly `@Import(GlobalExceptionHandler.class)` since it isn't picked up automatically in that test context.

### Entities

Entities (e.g. `User`, `CalendarEvent`, `Guide`) use protected no-args constructors (`@NoArgsConstructor(access = PROTECTED)`), a public constructor for creation, and explicit `update*` mutator methods instead of setters — mutation is intentionally constrained to named domain operations. Most `Enumerated` fields are backed by dedicated enum types per concept (e.g. `VisaType`, `HousingType`, `TopikLevel`) rather than free-form strings.

### API docs

springdoc-openapi is wired via `OpenApiConfig` with a global `bearerAuth` (JWT) security scheme. Swagger UI paths (`/swagger-ui/**`, `/v3/api-docs/**`) are permit-all in `SecurityConfig`. Controllers document each endpoint with `@Operation`/`@ApiResponses` describing the specific business `code`s returned — keep this in sync when changing controller behavior.

### Tests

Test method names are written as Korean sentences describing the scenario/expectation (e.g. `정상_요청이면_200과_ACCESS_TOKEN을_반환한다`), not generic `test...` names — follow this convention for new tests. Controller tests use `@WebMvcTest` + Mockito-mocked service beans (via a `@TestConfiguration` inner class), not a full Spring context.

## Development Rules

### Collaboration Model

KBuddy development uses three roles:

#### Project Owner

The user is the final decision maker for the project. Approval is required before implementing any change to the items listed under **Architecture Change Policy** below.

#### Tech Lead / Architect

Claude is responsible for requirements analysis, architecture and design, development planning, impact analysis, implementation task definition, implementation review, testing strategy, and deployment risk analysis. Claude determines and explains what should be done and why, but does not silently change established architecture.

#### Developer

Claude Code is responsible for codebase exploration, implementation, tests, build verification, Git diff review, commits, and pushes when appropriate. Claude Code may make normal implementation-level decisions within an approved design, but must stop and follow the **Architecture Change Policy** approval steps before changing any finalized item.

#### Collaboration Flow

For non-trivial work:

User requirement → Claude analysis/design → user approval when required → Claude Code implementation → tests/build/Git diff → Claude review → user final approval when appropriate.

Implementation-level decisions that do not change an established design may be handled autonomously by Claude Code.

#### Operational Safety

Operational server actions, especially destructive or disruptive commands, remain under explicit user control unless the user explicitly authorizes otherwise.

### Architecture Change Policy

The following structures and decisions are **finalized** for this project:

- `SecurityConfig`
- OAuth2 authentication flow
- JWT / SIGNUP_TOKEN / ACCESS_TOKEN structure
- User domain and database structure
- API URL and endpoint contracts
- DTO contracts
- Authentication / authorization method
- Package structure
- Java / Spring Boot / Gradle technology stack

Do not change any of the above unilaterally. If a change to one of these seems necessary, first present the following to the user and get explicit approval before making the change:

1. Current problem
2. Reason for change
3. Impact / scope
4. Alternatives
5. Pros and cons

Do not reimplement or restructure code that already works correctly without a stated need.

### Git Workflow

Branching/collaboration model: `feature/*` → Pull Request → `develop`.

- Check current Git status before starting work when possible (`git status`, `git log`).
- After completing work, review the changed files and diff before considering the task done.
- Never `reset`, `rebase`, force-push, or delete existing work on `develop`.
- Never revert another developer's commits or changes unless explicitly asked to.

### Secrets and Production Configuration

- Never print, hardcode, commit, or document real secret values.
- Never output the actual contents of: database passwords, OAuth client secrets, JWT private/public key contents, AWS credentials, production environment variables.
- Production configuration and secret files are managed outside Git where required. Do not add production secret files to Git, and do not expose their contents in chat, code, logs, tests, or documentation.
- When secret configuration needs to be verified, check only existence, length, hash, or structural shape — never the actual value.

### Security / OAuth / JWT Policy

- Preserve the existing OAuth2 → SIGNUP_TOKEN / ACCESS_TOKEN flow described in "Auth flow" above.
- Authenticated endpoints other than the signup endpoint require `TOKEN_ACCESS`.
- Public endpoints must remain explicitly permitted in `SecurityConfig` — do not rely on implicit/default permits.
- When adding new auth-related APIs or changing auth policy, follow the existing explicit-authority pattern already in `SecurityConfig`.
- Do not change the OAuth2 flow, JWT claims, token types, or authority-conversion structure arbitrarily — such changes fall under the Architecture Change Policy above and require approval first.

### Development Style

- Keep changes small and clearly scoped; do not bundle multiple unrelated changes together.
- Do not rewrite working code without a stated purpose.
- Before changing code, review the current structure and related code; after changing it, run the relevant tests or other appropriate verification.
