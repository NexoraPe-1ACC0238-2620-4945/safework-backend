# SafeWork backend

Backend for UPC 1ACC0238, period 202620, NRC 4945, NexoraPe. Corrected source is proposed through feature/backend-foundation -> main. No deployed runtime URL or production deployment is provided.

Read [provenance](docs/provenance.md), [API contract](docs/api-contract.md), [approved session policy](docs/session-revocation-proposal.md) and [executed verification](docs/verification.md). Historical source and evidence were preserved separately; historical Git history was not imported.

## Build and local execution

Requires JDK 25, the included Maven wrapper (Maven 3.9.14), and MySQL 8.4. Spring Boot 4.0.5 and inherited library versions are retained.

[.env.example](.env.example) lists configuration names; its credential values are empty. Supply DB_PASSWORD and JWT_SECRET privately through process environment or a secret manager. Spring does not automatically load .env. JWT_SECRET must contain at least 64 UTF-8 bytes. JWT_EXPIRATION defaults to 7 days and accepts 1-365 whole days; no automatic renewal exists.

Database-backed tests refuse any datasource except jdbc:mysql://127.0.0.1:33317/safework_course_test. Configure DB_HOST=127.0.0.1, DB_PORT=33317, DB_NAME=safework_course_test, DB_USERNAME and private DB_PASSWORD/JWT_SECRET. Use only synthetic data. For this fresh local test database set APP_PROFILE=dev, DB_TLS=false, DB_ALLOW_PUBLIC_KEY=true and DDL_AUTO=update.

Windows PowerShell:
```powershell
$env:JAVA_HOME = '<path to JDK 25>'
.\mvnw.cmd verify
$env:DDL_AUTO = 'validate'
$env:SERVER_ADDRESS = '127.0.0.1'
$env:SERVER_PORT = '18082'
& "$env:JAVA_HOME\bin\java.exe" -jar .\target\service-1.0.0-SNAPSHOT.jar
```

Linux/macOS:
```sh
./mvnw verify
DDL_AUTO=validate SERVER_ADDRESS=127.0.0.1 SERVER_PORT=18082 java -jar target/service-1.0.0-SNAPSHOT.jar
```

verify runs tests without skipping. HTTP regressions use real MySQL and an actual embedded loopback server. Safe method/path/status reports are written to ignored .local/reports. Stop a running JAR before repackaging on Windows to avoid a file lock.

Local OpenAPI: http://127.0.0.1:18082/v3/api-docs. Swagger UI: http://127.0.0.1:18082/swagger-ui/index.html. Published environments require HTTPS; no historical API fallback is acceptable.

docker-compose.yml can provision a fresh loopback-only database after its private variables are configured. Actual verification used portable MySQL, not Docker. DDL_AUTO defaults to validate; schema update and non-TLS database flags are local-test settings only. Deployment needs a reviewed schema/migration and TLS/secret configuration.

## First operator and incorporation

There is no public ADMIN registration route. For an empty user database only, run the JAR once with SAFEWORK_BOOTSTRAP_ENABLED=true and privately configured BOOTSTRAP_COMPANY_NAME, BOOTSTRAP_FULL_NAME, BOOTSTRAP_EMAIL and BOOTSTRAP_PASSWORD. It is a non-web transactional process, creates the first company/ADMIN and exits. Repeating it with an existing user database fails. Remove these variables afterward; never place the bootstrap password in a command argument, Git or a log.

The operator logs in and issues a company/email-bound invitation through the backend-only route. Public signup consumes that proof and creates WORKER. Only ADMIN can subsequently grant EMPLOYER through the audited administration API. Android has no ADMIN screen or privilege-selection input.

## Sessions and scope

Every successful login opens an independent persisted session. JWT jti identifies it; raw JWTs are neither stored in the session table nor logged. Each protected request checks signature/expiry and current active/unexpired session, account/company/roles/security version. Logout revokes only the current session. Role/company/enablement changes revoke every target session transactionally. Invalid sessions401; valid sessions without permission403.

EMPLOYER takes an OPEN incident itself. Only that responsible EMPLOYER, in the same company, can start and close in order. Notifications are current-recipient/current-company only. Read tracking, push, reassignment and structured GPS DTOs are not implemented. Administrative company transfer does not move business records.

The single-process authentication limit is 60 requests per remote IP per minute; a multi-instance deployment needs coordinated limits. Key rotation, session cleanup, migration/deployment and handling outstanding responsibilities after administrative changes are documented follow-up tasks.

## Git and team workflow

Foundation -> PR to main, no automatic merge. Create test only after the foundation is reviewed and merged. Later feature PRs target test, followed by validation and PR to main. Use Conventional Commits and contributors' actual configured identities.

.local, .tools, credentials, logs, local data and target outputs stay out of commits. No mobile functionality or teammate feature packages are included in this backend foundation.
