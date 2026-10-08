# Current-course API contract

This describes corrected source on feature/backend-foundation and the validated local runtime. It is not the historical backend contract or a claim that main contains a deployed service. No public deployment URL is supplied. Android must not default to the historical API.

Historical source provenance is in [provenance](provenance.md), session semantics in [session policy](session-revocation-proposal.md), and executed evidence in [verification](verification.md). Runtime OpenAPI is generated at /v3/api-docs; Swagger UI is /swagger-ui/index.html.

## Transport, shapes and errors

JSON, application/json. Published environments require HTTPS. Explicit local testing uses loopback HTTP only. All authenticated requests require a signed JWT and an active server session. All dates are ISO 8601 instants with UTC/explicit offset. IDs are positive Long integers; notification/session IDs are UUIDs. No query selectors are supported in this increment. Unexpected JSON fields/types are rejected with 400.

Error: {code, message, fieldErrors, requestId}. fieldErrors maps field names to arrays of safe text. Codes: 400 VALIDATION_ERROR, 401 INVALID_CREDENTIALS for bad login or SESSION_INVALID for unusable bearer sessions, 403 ROLE_FORBIDDEN/NOT_RESPONSIBLE with a valid session, 404 RESOURCE_NOT_FOUND for absent/invisible resources, 409 EMAIL_UNAVAILABLE/STATE_CONFLICT, 422 INVITATION_INVALID, 429 RATE_LIMITED, 500 INTERNAL_ERROR. No stack trace, credential, invitation proof or token is included in an error.

Success shapes retained from history:

| Resource | Fields |
| --- | --- |
| AuthenticatedUserResource | id, username, token |
| UserResource (8 fields) | id, companyId, fullName, email, phoneNumber, createdAt, updatedAt, roles |
| IncidentResource (10 fields) | id, userId, companyId, title, description, location, status, documentUrl, reporterName, assigneeName |
| AssignmentResource | id, incidentId, userId, incidentTitle, status, assignedAt, priority, completionDate |
| NotificationResponse | id, subject, body, createdAt, isRead |

Incident userId identifies the reporter. Assignment userId identifies the responsible EMPLOYER. Incident assignmentId and assigneeUserId are absent; they remain optional future proposals. Unassigned assigneeName and absent documentUrl/completionDate/phoneNumber are null. Lists return arrays. isRead is the disclosed false placeholder; no mark-read or push feature exists.

## Mobile operations

All paths below start with /api/v1. Status tables list operation outcomes in addition to common validation/internal/session errors.

| Method/path | Request | Success | Role and authorization | Relevant errors |
| --- | --- | --- | --- | --- |
| POST /authentication/sign-in | email, password | 200 AuthenticatedUserResource | Public credentials; enabled user and active company; opens a new independent session | 401 INVALID_CREDENTIALS; 429 |
| POST /authentication/sign-up | fullName, emailAddress, password, invitationToken | 201 UserResource, WORKER only; no login token | Public; server-issued company/email-bound invitation, active company, one-time atomic consumption | 400 role/company override; 409 duplicate email; 422 invalid/expired/used/mismatched proof; 429 |
| GET /users/me | No body | 200 UserResource | Any valid identity session, own user only | 401 |
| PATCH /users/me | Optional fullName, phoneNumber | 200 UserResource | Own user; no email/role/company/password override | 400; 401 |
| POST /authentication/sign-out | No body | 204, empty body | Valid session; revokes only presented jti | 400 body; 401 |
| GET /incidents | No body | 200 IncidentResource[] | WORKER or EMPLOYER, current company only | 403 |
| GET /incidents/{incidentId} | No body | 200 IncidentResource | WORKER or EMPLOYER, current company | 403; 404 |
| POST /incidents | title, description, location | 201 IncidentResource | WORKER or EMPLOYER; reporter/company from principal; OPEN | 400; 403 |
| POST /assignments | incidentId only | 201 AssignmentResource | EMPLOYER, same company, OPEN and unassigned; caller takes responsibility | 400 selected user/override; 403; 404; 409 |
| GET /assignments | No body | 200 AssignmentResource[] | WORKER or EMPLOYER; own responsible identity and current company | 403 |
| GET /assignments/{assignmentId} | No body | 200 AssignmentResource | Own responsible identity and company | 403; 404 |
| POST /incidents/{incidentId}/start | No body | 200 IncidentResource | Responsible EMPLOYER, same company; ASSIGNED -> IN_PROGRESS | 400 body; 403 role/other responsible; 404 foreign/missing; 409 state |
| POST /incidents/{incidentId}/close | No body | 200 IncidentResource | Responsible EMPLOYER, same company; IN_PROGRESS -> CLOSED; completionDate stored | 400 body; 403; 404; 409 |
| GET /notifications/my-notifications | No body | 200 NotificationResponse[] | WORKER or EMPLOYER; recipient from principal and current company; newest first | 400 recipient selector; 403 |

ADMIN alone has no global mobile incident/assignment/notification access. Own generic IAM profile/logout are supported for backend operators; Android has no ADMIN functionality.

Names/title: 1-120 nonblank characters. Description: 1-4000; location: 1-500 nonblank characters. Name/title/description/location are measured in Unicode code points after stripping whitespace. Email: at most 254, normalized by trimming and lowercasing. Password: 12-64 Unicode code points AND at most 64 UTF-8 bytes. Phone: optional, 7-32 characters, digits and () space dot hyphen, with optional leading + and at least one digit. Omitted/null profile fields keep their values; clearing phone is deferred. Invitation proof max 2048 characters; actual proof is random URL-safe 32-byte entropy, stored as SHA-256 digest only.

location stays text. GPS/manual entry can compose this text in Android; no new coordinate fields or server geocoding are required.

## Backend-only incorporation and administration

| Method/path | Request | Success | Protection |
| --- | --- | --- | --- |
| POST /companies/{companyId}/invitations | emailAddress | 201 {invitationToken, expiresAt} | ADMIN for an active company, or EMPLOYER for its own company. WORKER403, foreign EMPLOYER404. 24-hour, email-bound, single-use proof. |
| PATCH /administration/users/{userId}/roles | roles: nonempty set of WORKER and/or EMPLOYER | 200 UserResource | ADMIN only; ADMIN grant400; self/ADMIN target403. Change revokes all target sessions and audits atomically. |
| PATCH /administration/users/{userId}/membership | companyId | 200 UserResource | ADMIN only; active company404 if absent/inactive; no arbitrary public affiliation. Change revokes all target sessions and audits. |
| PATCH /administration/users/{userId}/enabled | enabled: boolean | 200 UserResource | ADMIN only; disable/enable change revokes all target sessions and audits. |

These are new current-course operations, not historical routes. Invitation tokens are returned only to the authorized issuer for private delivery; automated email delivery is not implemented. No selector of a different incident responsible user is introduced.

## Retained related routes

Legacy GET /users returns the current user only; GET /users/{userId} and /users/email/{email} permit self lookup only. GET /companies and company ID/registration-code lookups expose only the current company. POST /companies is ADMIN-only.

PATCH /assignments/{assignmentId}/priority accepts LOW/MEDIUM/HIGH, requires same-company responsible EMPLOYER. PATCH /incidents/{incidentId}/document accepts HTTPS documentUrl up to 2048 characters, same company and reporter or responsible EMPLOYER. GET /incidents/analytics is current-company only. These routes cannot bypass the mobile authorization rules.

## Synthetic examples

The following are deliberately invalid placeholders, not credentials or usable invitation/token values.

Login request:
```json
{"email":"synthetic.worker@example.test","password":"SYNTHETIC_PASSWORD_NOT_VALID"}
```

Registration request:
```json
{"fullName":"Synthetic Worker","emailAddress":"synthetic.worker@example.test","password":"SYNTHETIC_PASSWORD_NOT_VALID","invitationToken":"SYNTHETIC_INVITATION_NOT_VALID"}
```

Self-assignment:
```json
{"incidentId":501}
```

Incident creation:
```json
{"title":"Synthetic hazard","description":"Fictional obstruction","location":"Synthetic gate; -12.000000, -77.000000"}
```

Role grant (authorized backend operator only):
```json
{"roles":["WORKER","EMPLOYER"]}
```

Session rejection:
```json
{"code":"SESSION_INVALID","message":"Session invalid.","fieldErrors":{},"requestId":"synthetic-request-id"}
```

## Historical compatibility and remaining decisions

Historical login/profile/incident/assignment/notification success fields remain recognizable. Historical public companyId/roles signup must be replaced by invitationToken. jti, durable sessions/logout, administrative APIs/audit and company-safe authorization are new corrections. Historical signing-key fallback and raw-token logging are removed.

No deployed API, refresh, mark-read, reassignment, reopening, role selector in Android, structured coordinate DTO or proposed incident assignment IDs are claimed. Operational handling of responsibilities after disabling/transferring a responsible user, expiration cleanup, key rotation and deployment migrations/TLS are follow-up tasks. Validated local integration may use this current-course commit while its PR is under review, as authorized; never use a historical fallback.
