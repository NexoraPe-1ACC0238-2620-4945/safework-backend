# Deploy this backend

This checkout implements self-service registration and company membership.
A global ADMIN account and invitation codes are not required.

## First use after deployment

1. Register with POST /api/v1/authentication/sign-up using fullName, emailAddress and
   a privately chosen password.
2. Sign in with POST /api/v1/authentication/sign-in using email and password.
3. The future employer creates a company with POST /api/v1/companies and a name.
   Its creator becomes that company's EMPLOYER. Sign in again after creation.
4. Each worker registers and signs in, searches GET /api/v1/companies and sends an
   empty-body POST /api/v1/companies/{companyId}/membership-requests.
5. That company's EMPLOYER accepts or rejects the request with the corresponding
   /membership-requests/{requestId}/accept or /reject endpoint.
6. Accepted workers sign in again. Only then can they use incidents and notifications.

Accounts can exist without a company. One account can belong to only one company in
this version. A company ID does not grant membership. Company creation and approvals
are authorized by the backend, not by a public role selector.

Use Swagger first to validate this flow. The existing Android client still needs its
new onboarding screens and request DTOs; that mobile change is a separate phase.

## Before your friend deploys

The implementation is on feature/self-service-companies. Publish the reviewed TLS fix
on the branch selected in Render before redeploying; a local edit does not update the
running service. Confirm the deployed source SHA with the service owner.

Check Render's auto-deploy settings before publishing. Use an unwatched review branch
or arrange the maintenance window so a push cannot activate the new code before the
schema migration is ready. No automatic deployment is authorized by this guide.

## Existing Aiven database

Follow [the full deployment procedure](docs/alternative-deployment.md):
- Confirm the active service, effective database, schema and MySQL version.
- Take a recoverable backup and check duplicate company names.
- Rehearse the migrations on a restored isolated copy.
- For a reviewed V1 schema without Flyway history, explicitly baseline at V1 and then
  migrate. For a fresh database apply all versions. Never force schema compatibility.
- During the approved window, stop writes, run the non-web migration, then deploy the
  normal web service with one-shot flags disabled.

Existing ADMIN assignments are archived and mapped to company-scoped EMPLOYER access.
Existing users do not need to be deleted. Their passwords remain unchanged; forgotten
passwords can be reset by the private operator tool, which revokes old sessions.

Do not leave SAFEWORK_BOOTSTRAP_ENABLED, SAFEWORK_MIGRATE_ENABLED or
SAFEWORK_PASSWORD_RESET_ENABLED true in the normal web service.

## Render settings

Use the Dockerfile in this repository, APP_PROFILE=prod, health path /health.
PORT is supported and the production listener binds to 0.0.0.0.
Configure DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD, JWT_SECRET privately.
Download the CA certificate from the same Aiven service. In Render Environment >
Secret Files create ca.pem and paste the full PEM certificate contents. The container
reads /etc/secrets/ca.pem and prepares its private PKCS12 store automatically.
DB_TRUSTSTORE_URL, DB_TRUSTSTORE_BASE64 and DB_TRUSTSTORE_PASSWORD are not needed for
this approach; remove legacy values if switching to ca.pem. Keep sslMode=VERIFY_IDENTITY.
DDL_AUTO=validate and MIGRATIONS_ENABLED=false are the normal web settings.
For an operator laptop set DB_CA_CERT_PATH to the downloaded ca.pem file.

No secret belongs in Git or build arguments. See .env.example for variable names;
Spring does not automatically load that file.

## Verification in this checkout

A clean build and all 41 tests passed after the member directory addition, using the existing
isolated MySQL test instance. Reports are in .local/verified-build/surefire-reports;
the build log is .local/maven-verify.log.

Windows held directories from the previous IDE build open. The verified clean output
therefore uses .local/verified-build, and its JAR was copied to
target/service-1.0.0-SNAPSHOT.jar after checking no previous JAR was running.
Use that packaged JAR or a fresh Docker build, rather than stale IDE class output.

For the already provisioned fixture on this laptop, from this repository:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-local.ps1 -TestRuntimeRoot ..\safework-mobile\.tools\backend-alternative\.local -MavenUserHome ..\safework-mobile\.tools\local-run\maven-home -BuildDirectory .local\verified-build
```

On another machine provision a separate isolated MySQL fixture with the supplied helper
and configure Java/MySQL paths. No test may target Aiven.

The same production Java implementation previously passed a Docker image build,
16 HTTP checks with isolated mysql:8.4, 26 packaged-JAR HTTP checks and four truststore
entrypoint checks. Actual Aiven migration and public Render HTTP validation are
still pending. Five production-profile TLS checks passed with a synthetic CA and isolated MySQL8.4, including certificate-hostname rejection. This guide does not claim that the running Render service was changed.

The mobile employer screen also requires GET /api/v1/companies/{companyId}/members. This directory addition needs a backend redeploy but no new migration or database settings.
