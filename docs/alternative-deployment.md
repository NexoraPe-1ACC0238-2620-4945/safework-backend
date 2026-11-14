# Render / Aiven deployment and rollback

This repository is prepared for deployment review. Nothing here has been applied to
Render or Aiven. Confirm the active source SHA, effective datasource and database
identity with the service owner before scheduling a change.

## Release gate and runtime

Run Maven verify against isolated local MySQL with zero skipped tests. Preserve its
reports and hash the resulting target/service-1.0.0-SNAPSHOT.jar. The Dockerfile uses
JDK/JRE 25 and a non-root runtime. It packages with tests skipped inside the container;
that packaging step cannot replace the complete MySQL-backed verification gate.

Render uses the repository Dockerfile. Production binds 0.0.0.0 and accepts Render's
PORT, with optional SERVER_PORT override. Set health check path /health.
No automatic deployment, paid resource or Render job is provisioned by this delivery.

Configure privately:
- APP_PROFILE=prod
- DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD
- JWT_SECRET: at least 64 UTF-8 bytes; use the reviewed service secret
- JWT_EXPIRATION: 1–365 days, default 7
- DB_TRUSTSTORE_URL: file URL of a dedicated PKCS12 truststore containing the Aiven CA
- Or DB_TRUSTSTORE_BASE64: base64 text of that PKCS12 file for the Render environment
- DB_TRUSTSTORE_PASSWORD: private truststore password
- DDL_AUTO=validate; MIGRATIONS_ENABLED=false

Production forces Connector/J sslMode=VERIFY_IDENTITY and disables public-key retrieval.
There is no insecure TLS fallback. Make the truststore accessible to the container's
non-root user through a reviewed file mount, or set DB_TRUSTSTORE_BASE64 instead of
DB_TRUSTSTORE_URL. The entrypoint decodes that text to an owner-only temporary file
and exports its file URL before Java starts. It rejects supplying both forms.
A CA certificate is public;
database passwords, JWT keys and truststore passwords are not.

Spring does not automatically load .env. .env.example has names and empty secret values.
Remove legacy bootstrap flags from the web service. Recovery/migration flags must never
be left true in the normal web environment. Do not bake secrets into a Docker image.

## Existing database preflight

Stop writes during the maintenance window. Take a recoverable backup through the
existing authorized database service; do not create paid resources automatically.

From the same database used by the active service, inspect:

```sql
SELECT DATABASE(), @@server_uuid;
SELECT name, COUNT(*) FROM companies GROUP BY name HAVING COUNT(*) > 1;
SELECT u.id,u.email,u.fk_company_id,r.name
FROM users u LEFT JOIN user_roles ur ON ur.user_id=u.id
LEFT JOIN roles r ON r.id=ur.role_id ORDER BY u.id;
SELECT * FROM flyway_schema_history;
```

The final query may report that no history table exists; record that explicitly.
Compare the complete actual schema to V1__legacy_schema.sql before baselining.
Resolve duplicate company names explicitly with the owner before migration: the new
model adds a unique company-name index. Do not rename or remove companies automatically.

The migration test fixture used MySQL 9.5.0. Verify the actual Aiven MySQL version and
rehearse the migration on a restored isolated copy matching that version before approval.
The source's original stated database version was MySQL 8.4.

## Versioned migration

V1 describes the original schema from source commit 5badac5.
V2 allows nullable user/session company IDs and introduces memberships and audit records.
V3 adds optimistic versioning to membership requests.

For a new empty database, apply all versions. For a reviewed existing V1-compatible
schema without Flyway history, explicitly baseline at version 1, then apply V2 and V3.
Automatic baselining is disabled by default.

An existing ADMIN user is mapped to EMPLOYER of its existing company. ADMIN without a
company becomes WORKER without active membership. Previous ADMIN assignments and role
rows are archived in legacy_admin_assignments and legacy_admin_roles. A roleless
legacy account receives WORKER but remains inactive until approval. Existing worker
and employer memberships remain active. Existing passwords are not changed.

Users, company IDs, business records, prior administration audits and legacy invitation
data remain. Legacy invitation data is not used to register new accounts. Every user's
security version increases and all existing sessions are revoked. Everyone must log in
again after migration.

From an operator laptop, with private variables already loaded:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\operator.ps1 -Action Migrate
# Review the plan first. Only after destination/backup/schema approval:
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\operator.ps1 -Action Migrate -Execute
# Only for a reviewed V1 legacy schema without Flyway history, add -BaselineLegacy.
```

The operator process is non-web and exits. MySQL DDL commits implicitly: failure does
not promise a complete rollback of prior DDL statements. Stop after an error, preserve
the database and operator log, inspect the partial state, and use the reviewed restore
procedure. Do not edit applied migration checksums or retry blindly.

## Forgotten password

The recovery tool has no public HTTP endpoint and does not require an existing ADMIN.
Only a trusted infrastructure operator with the database and service configuration can
run it. Preflight the exact database UUID, target ID and email first.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\operator.ps1 -Action ResetPassword -Email TARGET_EMAIL -UserId TARGET_ID -ServerUuid VERIFIED_UUID
# After reviewing the destination and account, repeat with -Execute.
```

The tool prompts twice with hidden input, uses BCrypt, checks database name/UUID and
target ID/email, increments security_version and revokes sessions in one transaction.
It records the actual operator's identity separately from application users. Candidate
credentials and diagnostic evidence are private under .local. Passwords are not in
arguments or printed output. Check login and profile by HTTPS after exit 0.

## Deploy and verify

1. Approve the exact source/build, backup, data mapping and maintenance window.
2. Stop writes and apply the reviewed migration once.
3. Start/redeploy the normal web service with migration/recovery/bootstrap flags disabled.
4. Check /health and /v3/api-docs, existing account login, company/profile state,
   new signup, company creation, requests, scoped approval/revocation, incident lifecycle,
   notifications and logout. Store method/status results without tokens.
5. Deploy the adapted mobile client only after its new onboarding contract is verified.

## Rollback

Before production writes resume, restore the verified pre-migration backup and deploy
the previous application build with its matching configuration. The old build cannot
authenticate former ADMIN accounts after the role migration without restoring their
prior role data. Returning to the old JAR alone is not a complete rollback.

Once writes resume, preserve new data and plan reconciliation with the owner before
restoring. This delivery does not execute restore, discard new data or downgrade a live
schema. Preserve the original backend copy and its deployment identity for comparison.
