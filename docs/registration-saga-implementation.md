# Registration saga: problem and implementation

## Portfolio summary

SkillForge is split into an authentication service and a user-profile service.
Registering a user writes data to both services: credentials belong to the
authentication service, while the public-facing profile belongs to the
user service.

The engineering problem is that these writes are in separate services and
databases. A local database transaction cannot make both writes atomic. If the
account is saved but profile creation fails, the system can be left with an
account that has no profile. The implementation uses a persisted,
orchestrated saga to coordinate the work and to attempt cleanup when a step
fails.

## Registration flow

1. `AuthController` accepts `POST /api/v1/auth/register` and validates the
   request.
2. `AuthUserCommandService` starts a saga and creates the authentication user.
   The password is stored as a BCrypt hash, not as plain text.
3. `RegistrationSagaOrchestrator` asks `UserProfileClient` to provision the
   profile in the user service.
4. The client sends a `PUT /api/users/me` request with the profile fields and
   an internally generated, short-lived JWT in its `Authorization` header.
   The user service validates the JWT and maps its subject to a deterministic
   profile UUID.
5. On success, the saga records profile creation and transitions to
   `COMPLETED`. The API returns `201 Created`.

The profile request uses `PUT` so provisioning is repeatable for the same
authenticated identity. The username uniqueness constraint is still enforced
by the user service; a conflict is translated into an API `409 Conflict`.

## Failure handling and compensation

If provisioning or a later registration step throws a runtime exception, the
orchestrator asks `RegistrationSagaCompensationService` to undo the work:

1. The saga moves into `COMPENSATING`.
2. The authentication service requests deletion of the profile using a
   service-generated JWT.
3. After successful profile deletion, the authentication user is deleted and
   the saga is marked `COMPENSATED`.
4. The original registration failure is returned to the caller.

If compensation itself fails, the saga is marked `COMPENSATION_FAILED` when
possible, and registration returns `503 Service Unavailable` with a message
that cleanup is pending. A scheduled recovery job retries `COMPENSATION_FAILED`
and `COMPENSATING` sagas. It processes up to 20 oldest records per status on
each run; the default fixed delay is 30 seconds and can be configured with
`registration-saga.recovery-delay`.

## Durable saga state

Saga state is persisted in the authentication service database, rather than
being held only in memory. The state machine currently includes:

```text
STARTED
  -> AUTH_USER_CREATED
  -> PROFILE_CREATED
  -> COMPLETED

AUTH_USER_CREATED or PROFILE_CREATED
  -> COMPENSATING
  -> COMPENSATED

COMPENSATING
  -> COMPENSATION_FAILED
  -> COMPENSATING (retry)
```

The `registration_sagas` table stores a saga UUID, associated authentication
user ID, status, failure reason, timestamps, and an optimistic-lock version.
The status/timestamp index supports recovery-job queries.

## Why use a saga?

A distributed transaction would couple both services to shared transaction
coordination and is not provided by the current architecture. The saga keeps
each database operation local to its owning service and makes the cross-service
workflow explicit. Compensation does not provide strict atomicity: for some
period, and potentially until a retry succeeds, an incomplete account or
profile may exist. Persisted state and retryable cleanup make that failure
visible and recoverable rather than silently abandoning it.

## Important implementation details

- **Authentication and profile IDs differ.** Registration returns the
  authentication service's numeric ID. The profile service derives its UUID
  from the JWT subject using the `skillforge-user:` namespace. The profile
  UUID is returned by profile endpoints, not by registration.
- **Internal service calls are authenticated.** The auth service signs a JWT
  for calls to the user service. The user service validates the signature
  using the configured shared JWT secret and treats the token subject as the
  authenticated account identity.
- **Compensation treats a missing profile as already cleaned up.** A `404`
  from profile deletion is considered success, which makes this cleanup step
  safe to retry when the profile was already removed.
- **Email and username conflicts are distinct.** Duplicate email is rejected
  before account creation; a username conflict can occur during profile
  provisioning and triggers compensation for the newly created auth account.
- **The saga is an internal implementation detail.** The registration
  response does not expose a saga ID or intermediate states. A `503` indicates
  provisioning failure or pending cleanup, not a successfully completed
  registration.

## Current limitations and possible next steps

- Compensation retries use a fixed delay and a bounded batch, but do not
  currently implement exponential backoff, dead-letter handling, or an
  operator-facing view of stuck sagas.
- Registration returns the numeric auth ID but not the profile UUID. If
  clients need to construct public profile links immediately, return both
  identifiers or provide a dedicated lookup.
- API documentation is maintained as Markdown; the project does not currently
  generate an OpenAPI specification from the controllers.
- The saga orchestrator has unit coverage for successful registration,
  compensation after profile failure, and failed compensation. Integration
  coverage across both live services and their databases would further verify
  the end-to-end recovery behavior.

## How to explain this project in a portfolio

> I implemented user registration across separate authentication and profile
> services without assuming a shared database transaction. The auth service
> orchestrates a persisted saga: it creates the credential record, provisions
> a profile through an authenticated service-to-service request, and records
> each state transition. If provisioning fails, it compensates by deleting the
> partial work. Failed cleanup remains durable and is retried by a scheduled
> recovery process. This makes cross-service failure behavior explicit and
> recoverable while acknowledging that a saga provides eventual consistency,
> not instantaneous atomicity.
