# Backend foundation verification

2026-10-08 (America/Lima). Corrected current-course foundation, not the historical runtime and not a deployment. Application revision: `183fc4ffde12e960d26d7d19f393da67fd8ecca3`. Regression revision: `12a189fc1b30bdf6c2270ab7365536137d3f4e2b`. These revisions differ from the successful verified files only by removal of empty final lines; documentation changes do not change executable behavior.

## Executed checks

| Check | Actual result |
| --- | --- |
| Maven wrapper verify, no skipped tests | BUILD SUCCESS; 28 tests, failures0/errors0/skips0 |
| Original context / domain / JWT / security-error tests | 1 / 5 / 2 / 2 passed |
| Incident/IAM HTTP regression suite | 9 tests; 128 real HTTP requests |
| Session HTTP regression suite | 9 tests; 54 real HTTP requests |
| Packaged JAR restart checks | 30 recorded HTTP requests and two actual process restarts; all expectations passed |
| Fresh-database operator workflow | Initial non-web bootstrap exit0, repeat rejected; 10 HTTP requests passed |
| Recorded HTTP total | 222, including 39 method probes405 and one rate-limit probe429; startup polling excluded |
| Final packaged startup | DDL_AUTO=validate; OpenAPI200; session/error/ten-field incident schema checked |
| Application logs | No complete JWT pattern detected in new-backend application logs |
| Secret/publication scan | Candidate files and outgoing commits checked for private configured/historical values, complete JWTs and private keys |
| Historical snapshot | Preserved checkout clean at 778fe1ec8e999b27e8d0340eb26fef50d1a49683 |

The main initialization alone contains no application API. Foundation publication is feature/backend-foundation -> PR to main. No merge, test branch creation, production database access or automatic deployment occurred.

## Required session evidence

| Requirement | Actual HTTP/persistence evidence |
| --- | --- |
| Two independent sessions for one user | Two successful logins; different signed tokens/jti; both profiles200 |
| Logout affects only the current session | Logout204; first profile401 SESSION_INVALID, second profile200 |
| Roles/company/disable revoke both | ADMIN PATCH200; both prior sessions401; persisted revokedAt non-null; audit added |
| Revoked session remains invalid after restart | Two actual JAR restarts: revoked profiles401; active independent/unrelated profiles200 |
| Expired, altered or unknown sessions rejected | Expired JWT401, altered signature401, signed unknown jti401, expired stored session with a live signed JWT401 |
| Unrelated users remain valid | Their profile200 after each target change, logout and restart |
| Valid session without permission | WORKER administrative attempt403 ROLE_FORBIDDEN; its profile remains200 |
| Concurrent login/role change | No stale active session: the raced token is current-role200 or401; old token401; unrevoked records match the current security version |
| Unchanged administrative values |200, session stays valid and no change audit added |
| No credential persistence in sessions | Entity has metadata only, no token/password fields; clients retain runtime tokens only in memory |

No automatic refresh exists. JWT_EXPIRATION defaults to seven days and accepts 1-365; unit tests cover invalid and valid configuration. Every protected request validates JWT and persisted active session; writes recheck transactionally under the user lock.

## Other executed HTTP outcomes

- Correct login/profile200; wrong credentials/unknown account401. Missing/invalid token401.
- Valid invitation signup201, WORKER only. Public role/company overrides400. Invalid, expired, email-mismatched or replayed proof422.
- Concurrent invitation consumption: one201, one422, one new user and no duplicate role. Duplicate email409 rolls back consumption.
- Own incident create/list/detail201/200. Company-invisible detail/start/close/document/assignment404. ADMIN alone cannot globally read business data403.
- EMPLOYER self-assignment201. WORKER taking403. Other same-company responsible start/close/priority403. Foreign actor404. Invalid transitions/reassignment409.
- Accepted chain is OPEN -> ASSIGNED -> IN_PROGRESS -> CLOSED; completionDate persists with zone. Concurrent taking has one201 and one409.
- Rejected operations preserve incident/assignment state and notification counts. Selected responsible inputs, body actor overrides and query selectors400.
- Text boundaries120/4000/500 persist. Supplementary Unicode code-point boundaries also201; overflow400. Phone whitespace/misplaced plus400. Negative IDs and noninteger body IDs400.
- Notifications200 contain only authenticated recipient and current company, zoned dates and disclosed isRead=false.
- OpenAPI has logout204/401, administrative403 and shared ApiError references. IncidentResource has exactly ten historical fields; proposed assignmentId/assigneeUserId are absent.
- Bootstrap against a separate fresh synthetic schema created the first ADMIN without a web listener; repeat execution failed. The operator issued an invitation, registered WORKER, granted EMPLOYER and invalidated the earlier WORKER session.

## Environment and evidence

Java25, Spring Boot4.0.5, MySQL8.4 and inherited library versions retained. MySQL bound to 127.0.0.1:33317; normal test API18082, separate bootstrap verification API18084. Only generated synthetic identities/data were used. Existing historical evidence/database/cache/source were untouched. New test data was preserved.

Private ignored evidence: .local/reports/maven-verify.log, http-regression-results.tsv, session-http-results.tsv, session-restart-results.json, operator-bootstrap-results.json and openapi.json. Secrets/fixture credentials/runtime logs remain in private ignored .local/runtime. No secret, password, token or invitation proof appears in this report.

Earlier checks caught a Jackson3 date-property mismatch, loading incident display data after transaction closure, a notification test route mismatch and test-order interference from rate limiting; corrected and reverified. Windows JAR locking was resolved by stopping the owned server before packaging. The restart helper's subprocess pipe handling was corrected before its successful final run.

## Real limitations and pending work

- There is no deployed HTTPS API or production schema migration; review deployment/key management/TLS separately.
- Expired-session cleanup/retention, signing-key rotation and coordinated multi-instance rate limits are operational follow-ups.
- Audit stores actor ID, target ID, action and UTC time; it is not an external immutable audit export.
- Administrative transfers do not move incidents/assignments/notifications. Resolve still-open responsibilities operationally; no manager override/reassignment is implemented.
- Additional ADMIN lifecycle tooling is deferred: administrative HTTP routes cannot grant ADMIN or modify ADMIN/self accounts. First-admin bootstrap requires an empty user database.
- No refresh, mark-read/push, pagination/filter, phone clearing or proposed incident assignment IDs.
- Android IAM, business/HTTP mobile checks, build/lint, phone flows and teammate packages are subsequent work, not completed by these server tests.
- Inherited Lombok sun.misc.Unsafe and RegistrationCode deprecated API warnings remain. No framework upgrade was made.
