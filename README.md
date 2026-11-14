# SafeWork backend - self-service companies

A self-service alternative for NexoraPe, UPC 1ACC0238, period 202620, NRC 4945.
Based on commit 5badac5ade6c5996af81d5f1038a790ac5321842 of the current-course backend.
This checkout now implements the self-service version. The previous source is preserved
in Git at the base commit and in the integration backup. Deployed databases are unchanged.

Start with [DEPLOY-NOW.md](DEPLOY-NOW.md) for the deployment handoff and first-use flow.

## What changed

There is no global ADMIN or invitation-based signup. An account starts without a company.
It can sign in, edit its own profile, discover company names, create a company, or submit
one pending membership request. It cannot read incidents or notifications before approval.

Creating a company makes its creator that company's EMPLOYER. The account cannot create
or join another company afterward. An EMPLOYER accepts or rejects requests for its own
company and can revoke WORKER memberships. Revocation preserves the account's company
binding and allows a new request to that same company. It does not allow switching
companies. Employers cannot revoke themselves or other employers in this version.

Approval, company creation, revocation and password recovery invalidate affected sessions.
Sign in again after a successful company creation or membership approval.

The existing layered architecture, Spring Boot 4.0.5, JDK 25, JPA, BCrypt and signed,
persisted sessions are retained. No additional framework was introduced. No professor
example files were supplied; the user asked to continue with the existing structure.
Flutter guidance belongs to the later mobile phase. Android has not been changed.

## Local verification

Requires JDK 25 and isolated MySQL. Windows helpers use the installed MySQL 9.5 binaries
by default. Integration tests use a **new instance** on 127.0.0.1:33319 and
safework_alternative_test, never Aiven. The separate safework_legacy_schema database is
an expendable migration fixture; its actual server data directory is checked before
fixture cleanup. Do not point these tests at a tunnel or a shared database.

From this repository, in your own Windows terminal:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-test-mysql.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-local.ps1
```

The helper runs Maven verify without skipping tests. Reports and local configuration are
ignored under .local. The local test secrets are encrypted for the Windows user who
provisioned them; another Windows user cannot decrypt them. On another machine, configure
the helper's Java/MySQL paths first.

## Contracts and deployment

- [API and mobile handoff](docs/alternative-api.md)
- [Render/Aiven deployment, migration and rollback](docs/alternative-deployment.md)
- [Actual verification and remaining limits](docs/alternative-verification.md)

Normal startup uses schema validation, not schema updates. Migration is an explicit,
non-web operator action. A populated legacy schema is not automatically baselined.
Review the deployment procedure and backup first.

Render's Docker build packages the application without integration tests because that
build has no isolated MySQL fixture. The complete local Maven verify is the release
gate. A successful Docker packaging build alone is not test evidence.

Current mobile registration must be updated before using this alternative: signup no
longer accepts invitationToken, profiles may have null companyId, and onboarding has
new request/approval states. Do not replace the public backend until that handoff and
the data migration have been reviewed.
