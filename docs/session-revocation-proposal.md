# Approved session revocation policy

Approved and implemented on 2026-10-08. The filename is retained from the original proposal so existing references continue to work; this is the implemented policy.

## Token and durable session

Login creates an independent UserSession inside the login transaction, while holding the user row lock. The JWT contains jti (the session UUID), sub (normalized email), userId, companyId, iat and exp. HS512 signature, issuer safework-backend, audience safework-mobile and required claims are validated.

Lifetime defaults to seven days. JWT_EXPIRATION is configurable from 1 through 365 whole days; invalid settings fail startup. UTC creation/expiration instants are shared by the JWT and session. No refresh token or automatic renewal exists.

A UserSession stores its UUID, user ID, company ID, security version, role snapshot, createdAt, expiresAt and optional revokedAt. It stores neither a complete JWT nor a password. Session metadata survives application restart.

Every authenticated request checks the signed token and a persisted active, unexpired session against current account/company/roles/security version. Missing, unknown, expired, altered, revoked or mismatched sessions return 401 SESSION_INVALID. Business services recheck the session after acquiring the user lock inside write transactions.

## Logout

POST /api/v1/authentication/sign-out requires a valid bearer session and accepts no body or selectors. It revokes only that session and returns 204 with no body. Other sessions of the same or another user remain active. A previously revoked session returns 401, including after server restart.

Android must clear local identity/token/cache state even if a logout request fails. Local clearing alone cannot revoke a stolen token. Prevent in-flight responses from restoring a signed-out account; no refresh request should follow 401.

## Administrative changes

ADMIN-only backend operations replace WORKER/EMPLOYER roles, move company membership or enable/disable ordinary users. A real change increments the user's security version, revokes every existing session and writes an AdministrationAudit in the same transaction. A no-op does not invalidate sessions or add a change audit.

Login and administrative writes serialize on the target user record. Current account snapshots additionally reject stale role/company state. A valid WORKER/EMPLOYER session without administrative permission returns 403 ROLE_FORBIDDEN; revocation returns 401 rather than 403.

Public registration cannot grant roles or select an existing company arbitrarily. Administrative routes cannot grant ADMIN, target the actor itself or modify another ADMIN. ADMIN bootstrap is an explicit one-shot local non-web process for an empty user database; no public bootstrap route exists.

Company transfers do not move incidents, assignments or notifications. Old-company records remain invisible to the new membership. An operational procedure for unresolved responsibilities after transfer/role removal/disablement is a future business decision; there is no mobile manager override or reassignment endpoint.

## Evidence and limits

[Verification](verification.md) records real MySQL/HTTP tests for two sessions, selective logout, role/company/disable revocation, unrelated-user isolation, expired/altered/unknown sessions and concurrent login/role change. Two actual packaged-server restarts verified persistent revocation and surviving active sessions.

Expired/revoked row retention and cleanup, signing-key rotation, database retention, shared deployment rate limits and additional ADMIN lifecycle tooling remain deployment/operations tasks. Do not bypass administrative services with unaudited direct SQL updates; manual data maintenance must invalidate security versions and sessions transactionally. No production deployment or database migration was performed.
