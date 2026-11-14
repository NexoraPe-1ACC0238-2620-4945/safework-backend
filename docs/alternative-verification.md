# Alternative verification — 13 November 2026

## Completed

- Fresh independent clone of the current-course repository at base commit
  5badac5ade6c5996af81d5f1038a790ac5321842. Original repositories and production data
  were preserved. No commit, push, PR, merge or deployment was performed.
- Maven verify completed with **40 tests, zero failures/errors/skips** against an
  independently provisioned **MySQL 9.5.0** instance on loopback port 33319.
  JBR/JDK 25.0.3 was used. Reports: .local/maven-verify.log and target/surefire-reports.
- Real HTTP integration tests cover account signup without codes; company creation;
  minimal paginated discovery; pending/rejected/accepted/revoked memberships; one
  permanent company binding; unauthorized/foreign access; sessions; incidents,
  responsible assignment/start/close; notifications; password recovery.
- Concurrency tests cover duplicate signup, competing pending requests, simultaneous
  approvals, company creation versus approval, login versus revocation and incident
  assignment. The initial run exposed two stale-entity races. Refreshing the managed
  user/request under pessimistic locks and adding optimistic request versioning
  corrected them. The final full suite passed.
- A legacy-schema migration test checks the actual local server data directory before
  resetting its expendable fixture. It demonstrates explicit baselining, archived ADMIN
  mappings, preserved password hashes and business records, inactive roleless accounts,
  revoked old sessions and idempotent re-execution.
- A separate packaged-JAR run passed **26 real HTTP checks**. The real operator tool
  ran non-web password recovery with private input supplied by the synthetic test
  harness. The previous password/session stopped working, the replacement authenticated,
  and an incorrect database UUID was rejected without revoking the fresh session.
- The final packaged migration mode was executed against the already migrated isolated
  database and completed idempotently with exit 0.
- PowerShell helpers passed syntax parsing. Git diff whitespace checks passed.
- The Render-compatible entrypoint passed four additional container checks: plain
  Java startup, base64 truststore content and owner-only file permissions, rejection
  of invalid encoding and rejection of conflicting truststore configuration.
  The final image then passed the 16 HTTP/MySQL 8.4 checks again.
- OpenAPI explicitly documents 201 for signup, company creation and membership requests.
- Local demonstration access is in .local/DEMO-ACCESS.private.txt, ignored and excluded
  from the source archive. The alternative local server uses port 18086.

## Remaining release checks

- Docker Desktop was started locally. The image build succeeded, and a fresh isolated
  mysql:8.4 container applied V1, V2 and V3 successfully. The application container
  passed 16 additional HTTP checks, including code-free signup, company creation,
  admission and session invalidation. Both test containers, their anonymous volumes
  and their private network were removed afterward. Source/runtime smoke helpers do
  not publish or deploy images. Container packaging skips integration tests; the
  complete 40-test Maven verify remains the release gate.
- No Aiven connection, TLS handshake against production, deployed-SHA verification,
  production-schema inspection, restored-production-data rehearsal or production HTTP
  session was executed. The actual Aiven MySQL version must be confirmed and matched in
  a staging/restored-copy rehearsal before migration approval.
- Production deployment, backup and migration require the service owner's reviewed
  configuration and destination approval. MySQL DDL is not fully transactional.
- Android/Flutter work is pending. Existing invitation-based mobile signup is not
  compatible with the alternative signup contract.
- Professor sample files were not available. The user explicitly requested retaining
  the existing architecture, which this implementation does.

## Reproduce on this laptop

From this repository, run scripts/start-test-mysql.ps1, then scripts/verify-local.ps1
in a local Windows PowerShell with process-scoped script execution enabled. The helpers
accept MySqlBin or JavaHome where applicable. Do not reprovision an existing fixture.

After verification, scripts/run-local.ps1 serves the packaged JAR on 127.0.0.1:18086.
scripts/check-packaged-runtime.ps1 provisions fresh synthetic demo fixtures once; an
explicit -Resume can continue inspected fixture evidence without re-creating accounts.
scripts/operator.ps1 defaults to preparation mode and requires -Execute plus interactive
destination confirmation for recovery or migration.

Diagnostic evidence and credentials remain under .local and must not be published.

## TLS correction verified locally

The production datasource file now contains only the five standard properties. No
unresolved truststore placeholder is passed to Connector/J. Render can supply the
Aiven PEM certificate as Secret File ca.pem; the entrypoint imports it automatically.
Legacy explicit PKCS12 input remains supported.

- Full clean Maven verify: 40 tests, zero failures/errors/skips. Reports are in
  .local/verified-build/surefire-reports, log in .local/maven-verify.log.
- Docker rebuild succeeded. All four existing entrypoint checks passed again.
- scripts/check-production-tls.ps1 passed five checks with a new isolated mysql:8.4
  instance and a synthetic CA: prod-profile V1-V3 migration, application startup with
  only /etc/secrets/ca.pem, signup, login/logout, and certificate-hostname rejection.
  Evidence: .local/production-tls-verification.json. No Aiven connection was opened.
- Windows operator PEM preparation passed using a local test CA without altering it.
- The TLS fixture containers, network, volume and synthetic credentials were removed.

Publish the reviewed correction and redeploy the selected branch to update Render.
The original deployment and migration requirements above still apply to real data.
