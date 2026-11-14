# Alternative API and mobile handoff

All routes below start with /api/v1 and use JSON. Only signup and signin are public.
All other API routes require a signed token and a current persisted session.
GET /health is an unauthenticated liveness check.

## Accounts

POST /authentication/sign-up: fullName, emailAddress, password. Returns 201 UserResource.
New passwords require 12–64 Unicode code points and at most 64 UTF-8 bytes.
Roles, companyId, registrationCode and invitationToken are not accepted.

POST /authentication/sign-in: email, password. Returns 200 with id, username, token.
An existing correct short password can authenticate: new-password policy is not applied
to login. Wrong credentials or a disabled account return 401. Rate limiting returns 429.
Company membership is checked for business operations, not as a prerequisite to login.

GET /users/me and PATCH /users/me retain their routes. Editable fields remain fullName
and phoneNumber. UserResource retains its existing fields and adds membershipStatus:
UNASSIGNED, ACTIVE or REVOKED. companyId may be null. A pending request is obtained from
GET /users/me/membership-requests; it does not grant an active company membership.

POST /authentication/sign-out accepts no body and returns 204. Its token then returns
401. Different sessions remain independent until a security-context change revokes all.

## Discover and create a company

GET /companies?search=&page=0&size=20 returns a list of only {id,name} for active companies.
Page is 0–10000; size is 1–50; search is at most 120 characters. Results are ordered by
name then ID. GET /companies/{companyId} returns the same minimal shape. Neither route
returns members, invitation codes, incidents or notification information.

POST /companies with {name} returns 201 {id,name}. The caller must never have belonged
to a company. The caller becomes EMPLOYER of the new company and its previous sessions
are revoked. The mobile app must prompt for a new login. A second company returns 409.

## Join a company

POST /companies/{companyId}/membership-requests, without a body, returns 201:
{id,companyId,userId,fullName,email,status,createdAt,decidedAt,decidedBy}.

Knowing a company ID only allows this request. At most one PENDING request per account
is allowed globally; duplicate or competing pending requests return 409. The database
also enforces this rule. Rejection permits a new request. Creating a company cancels
the creator's pending requests.

GET /users/me/membership-requests?page=0&size=20 returns the caller's request history.
GET /companies/{companyId}/membership-requests?page=0&size=20 is limited to that company's
active EMPLOYER. Applicant details are never included in public company discovery.

POST /companies/{companyId}/membership-requests/{requestId}/accept or /reject takes no
body and returns 200 with the decided request. Only the company's EMPLOYER may decide it.
Foreign-company access returns 404; a WORKER receives 403. A repeated decision returns
409. Approval assigns WORKER membership and revokes the applicant's previous sessions.

DELETE /companies/{companyId}/members/{userId} revokes a WORKER membership and returns
200 UserResource. It preserves the account, roles and permanent company binding but
revokes all previous sessions. A fresh login works for profile and membership requests;
incidents and notifications return 403. Repeating a revocation is a no-op.
Self-revocation and revoking another EMPLOYER return 403.

## Business operations

Incident reporting/list/detail/document/analytics, assignment, start/close and recipient
notifications retain their current routes and DTOs. They now require active membership
in an active company. EMPLOYER and responsible-user checks remain. Foreign records are
hidden. Pending/unassigned/revoked members receive 403, even when their token is valid.

Legacy invitation and global administration routes are removed. They return 404 with
an authenticated valid session. Authentication still runs before route handling.

## Error and session rules

400 invalid JSON/fields/types/limits; 401 invalid or revoked session/credentials;
403 insufficient membership or role; 404 absent or foreign resource;
409 duplicate/conflicting/concurrent operation; 429 authentication rate limit.
The standard {code,message,fieldErrors,requestId} envelope is retained.

A JWT may omit companyId for an unassigned account. Its user, email, role snapshot,
security version and persisted session are checked. Admission/revocation does not keep
a stale token usable: sign in again after a membership change.

## Mobile phase

Implement signup without codes; company search/creation; own pending/rejected requests;
EMPLOYER decisions and member revocation; clear onboarding states and re-login after
security-context changes. Preserve the existing incident and notification flows after
membership becomes active. This backend delivery does not modify Android or Flutter.

## Employer member directory

GET /companies/{companyId}/members?page=0&size=20 returns UserResource records for
that company only, including ACTIVE/REVOKED membership status. Page is 0-10000;
size is 1-50. Only its active EMPLOYER can read the directory. WORKER/unassigned
accounts receive 403; a foreign employer receives 404. Passwords and password hashes
are never returned. This supplements /users, which deliberately returns only the
caller. No database migration or connection-setting change is required.
