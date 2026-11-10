# SkillForge API reference

This reference describes the HTTP API exposed by the API Gateway. The gateway
routes requests to the authentication and user-profile services using service
discovery.

## Base URL

```text
http://localhost:8080
```

The examples use `curl` and JSON. Successful registration and login are public;
the authenticated profile endpoints require a bearer access token returned by
login.

## Authentication

### Register

Creates an authentication account and its user profile.

```http
POST /api/v1/auth/register
Content-Type: application/json
```

Request:

```json
{
  "email": "alex@example.com",
  "password": "a-strong-password",
  "username": "alex_dev",
  "displayName": "Alex Morgan",
  "bio": "Building useful software.",
  "avatarUrl": "https://example.com/avatar.png"
}
```

| Field | Required | Constraints |
|---|---|---|
| `email` | Yes | Must be a valid email address |
| `password` | Yes | 8–100 characters |
| `username` | Yes | 3–30 characters; letters, numbers, and underscores only |
| `displayName` | Yes | At most 80 characters |
| `bio` | No | At most 500 characters |
| `avatarUrl` | No | At most 500 characters |

New accounts receive the `STUDENT` role by default. Registration does not
accept a role field; elevated roles must be assigned through a trusted
administrative process.

Created response (`201 Created`):

```json
{
  "message": "Registration successful",
  "id": 42,
  "email": "alex@example.com"
}
```

The numeric `id` identifies the authentication account. The profile service
uses a separate UUID identifier.

Possible errors include `400 Bad Request` for invalid input, `409 Conflict`
when the email or username is already in use, and `503 Service Unavailable`
when profile provisioning fails or registration cleanup is still pending.

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alex@example.com",
    "password": "a-strong-password",
    "username": "alex_dev",
    "displayName": "Alex Morgan",
    "bio": "Building useful software.",
    "avatarUrl": "https://example.com/avatar.png"
  }'
```

### Log in

Validates the account credentials and returns a JWT access token.

```http
POST /api/v1/auth/login
Content-Type: application/json
```

Request:

```json
{
  "email": "alex@example.com",
  "password": "a-strong-password"
}
```

Both fields are required; `email` must be a valid email address.

Successful response (`200 OK`):

```json
{
  "message": "Login successful",
  "id": 42,
  "email": "alex@example.com",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "accessToken": "<JWT>"
}
```

The configured token lifetime is 900 seconds (15 minutes). Invalid credentials
return `401 Unauthorized`; invalid request data returns `400 Bad Request`.
The signed JWT includes the account role in its `role` claim (for example,
`"role": "STUDENT"`). Protected services expose that claim as the
`ROLE_STUDENT`, `ROLE_INSTRUCTOR`, or `ROLE_ADMIN` Spring Security authority.

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alex@example.com","password":"a-strong-password"}'
```

Use the returned token on protected requests:

```http
Authorization: Bearer <JWT>
```

## User profiles

### Get my profile

Returns the profile belonging to the authenticated account.

```http
GET /api/users/me
Authorization: Bearer <JWT>
```

Successful response (`200 OK`):

```json
{
  "id": "09a1e54e-2378-3f0d-9e97-e3a963641229",
  "username": "alex_dev",
  "displayName": "Alex Morgan",
  "bio": "Building useful software.",
  "avatarUrl": "https://example.com/avatar.png",
  "createdAt": "2026-01-15T10:30:00Z",
  "updatedAt": "2026-01-15T10:30:00Z"
}
```

The profile ID is a UUID. The endpoint returns `401 Unauthorized` without a
valid bearer token and `404 Not Found` if no profile exists for the account.

### Create or update my profile

Creates the profile if it does not exist, or replaces its editable fields if
it does. This endpoint is also used internally by the registration workflow.

```http
PUT /api/users/me
Authorization: Bearer <JWT>
Content-Type: application/json
```

Request:

```json
{
  "username": "alex_dev",
  "displayName": "Alex Morgan",
  "bio": "Building useful software.",
  "avatarUrl": "https://example.com/avatar.png"
}
```

`username` and `displayName` are required. Username constraints match
registration; `displayName` is at most 80 characters; `bio` and `avatarUrl`
are optional and at most 500 characters each.

Successful response (`200 OK`) has the same shape as **Get my profile**.
Invalid input returns `400 Bad Request`; an already-used username returns
`409 Conflict`.

### Delete my profile

Deletes the authenticated account's profile.

```http
DELETE /api/users/me
Authorization: Bearer <JWT>
```

Successful response: `204 No Content`.

### Get a public profile

Returns a profile by its profile UUID. This endpoint does not require
authentication.

```http
GET /api/users/public/{id}
```

Example:

```bash
curl http://localhost:8080/api/users/public/09a1e54e-2378-3f0d-9e97-e3a963641229
```

Returns the same profile shape as **Get my profile**, or `404 Not Found` if the
UUID does not exist. The `{id}` is the profile UUID, not the numeric account
ID in the registration/login response. An authenticated client can read its
profile UUID from `GET /api/users/me`.

## Error responses

Authentication-service errors use this JSON shape:

```json
{
  "timestamp": "2026-01-15T10:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password",
  "path": "/api/v1/auth/login"
}
```

The `status`, `error`, `message`, and `path` values vary by request and error.
Profile-service errors are produced by that service and may use Spring's
default error response format.

## Service endpoints

When running locally, the gateway is on port `8080`, the authentication
service on `8081`, and the user service on `8083`. For normal client requests,
use the gateway URL above rather than calling individual services directly.
