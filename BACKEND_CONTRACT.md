# Tyde Backend Contract

This repo is the Android client only. The Spring Boot + Kotlin + PostgreSQL backend does not
exist in this repo and is being built separately in IntelliJ. This document is the contract the
Android client already assumes — implement the backend to match it so the two sides interoperate
without either side needing to change later.

Nothing in this file has been executed or verified against a real backend. Treat every detail
below as a specification to implement against, not a confirmed-working integration.

## Scope: Phase 2 — Resume Upload

Firebase Authentication (Phase 1) is already working on the Android side. This phase adds resume
upload + metadata retrieval. No AI parsing, no profile generation — that's Phase 3.

## Authentication (already assumed, not new)

Every request below requires:

```
Authorization: Bearer <Firebase ID token>
```

The Android `AuthInterceptor` (`:core:network`) attaches this automatically via
`FirebaseAuthTokenProvider` — nothing backend-specific needed on the Android side. On the
backend: verify the token with the Firebase Admin SDK (`FirebaseAuth.getInstance().verifyIdToken(token)`),
extract `uid`, and resolve/create the Tyde `User` row keyed on `firebaseUid` (unique). **The
authenticated Firebase UID — never a client-supplied ID — is the only valid source of resume
ownership.**

**Assumption flagged:** this contract assumes a `users` table already exists with at least a
UUID `id` and a unique `firebase_uid` column, since `resumes.user_id` references it below. I
have not seen the actual schema — adjust the FK type/name to match whatever you build.

## Endpoints

### `POST /api/resumes`

Multipart upload. The Android client sends exactly one part:

| Part name | Content | Notes |
|---|---|---|
| `file` | the PDF bytes | `MultipartBody.Part.createFormData("file", fileName, body)` — controller should bind with `@RequestParam("file") file: MultipartFile` |

Responses:

| Status | When |
|---|---|
| `201 Created` | success — body is `ResumeResponse` |
| `400 Bad Request` | empty file, or content isn't actually a PDF (magic-byte check) |
| `413 Payload Too Large` | exceeds the configured max size |
| `415 Unsupported Media Type` | declared content type isn't `application/pdf` |
| `401 Unauthorized` | missing/invalid/expired Firebase token |
| `500 Internal Server Error` | storage failure |

**Container-level size limit caveat:** Spring's embedded servlet container can itself reject
oversized multipart requests via `spring.servlet.multipart.max-file-size` / `max-request-size`
*before your controller ever runs* — producing a generic container error body, not your
`ResumeResponse`-shaped error. Set that property to match (or exceed) your own
`tyde.resume.max-size-bytes` check so your handler is the one actually rejecting the request.

### `GET /api/resumes`

Returns only the authenticated user's resumes, **ordered by `uploadedAt` descending** (most
recent first) — the Android client takes `.firstOrNull()` from this list as "the" resume for
this phase. The list shape is intentional: the product only shows one active resume right now,
but the schema/API already support many, so this doesn't need to change later when that evolves.

Response: `200 OK`, body is `ResumeResponse[]`.

## `ResumeResponse` (JSON)

```json
{
  "id": "3f6a1e2c-...-uuid",
  "originalFileName": "resume.pdf",
  "status": "UPLOADED",
  "uploadedAt": "2026-10-08T15:30:00Z"
}
```

- Field names are camelCase (matches Jackson's default serialization of Kotlin data classes —
  no `@JsonProperty` renaming needed if your DTO field names match exactly).
- `status` is one of `UPLOADED | PROCESSING | READY | FAILED`, sent as the raw enum name string.
  **Keep this in sync with Android's `ResumeStatus` enum** (`features/resume/domain/Resume.kt`)
  — Android falls back to an `UNKNOWN` status for anything it doesn't recognize rather than
  crashing, but new values should still mirror this set deliberately.
- `uploadedAt` is an ISO-8601 UTC instant — `java.time.Instant.toString()` in Kotlin produces
  exactly this format. Android does not parse it into a date type; it only displays the first 10
  characters (the date portion), so exact sub-second precision doesn't matter.

**Never include** in the response: storage path, stored/physical filename, raw file bytes, or
any other internal detail.

## Database

Suggested Flyway migration (adjust the `users` FK to your actual schema):

```sql
CREATE TABLE resumes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_resumes_user_id ON resumes(user_id);
```

`status` as `VARCHAR` + application-level enum (not a Postgres `ENUM` type) keeps adding future
statuses a zero-migration change — reserve `PROCESSING`, `READY`, `FAILED` for Phase 3; this
phase only ever writes `UPLOADED`.

## File validation (backend, independent of Android's client-side checks)

Android already validates before sending (MIME type, size ≤ 10 MB, stream opens, non-empty) —
**the backend must not trust any of that** and re-validate independently:

1. Reject empty file (0 bytes) → `400`.
2. Reject size over the configured max → `413`. Make the max a config property
   (`tyde.resume.max-size-bytes`, default `10485760` to match Android's default), not a literal.
3. Reject if the declared `Content-Type` on the multipart part isn't `application/pdf` → `415`.
4. Reject if the actual bytes aren't a PDF — checking the first 5 bytes equal `%PDF-` is a
   reliable, zero-dependency MVP check. (Apache Tika is a heavier option if deeper validation is
   ever needed — not required for this phase.)

## Storage

Define a `ResumeStorage` abstraction (interface) rather than coupling the service directly to
`java.io.File`. MVP implementation: local filesystem.

- Storage directory must be **outside** `src/main/resources` and **not committed to git** —
  configurable via a property (e.g. `tyde.resume.storage-dir`) backed by an environment variable,
  not a hardcoded machine-specific path.
- **Never trust the original filename as the physical storage filename** (path traversal risk).
  Generate it: `"${UUID.randomUUID()}.pdf"`. The original filename is metadata only
  (`original_file_name` column), never touches the filesystem path.
- Store the generated name as `stored_file_name` in the DB — this is never returned to the
  client.

## Error handling

Use whatever consistent error-response shape your existing Spring error-handling convention
already defines (a `@ControllerAdvice`/`@ExceptionHandler` returning a problem-detail-style body
is the normal Spring Boot approach). Whatever the shape, the Android client currently only reads
the **HTTP status code** to classify errors (via `:core:network`'s `safeApiCall` → `AppError`),
not the response body — so getting status codes right per the table above matters more than the
exact error JSON shape. Never leak stack traces or filesystem paths in any error response.

## Security checklist

- Verify the Firebase ID token on every request (Admin SDK) — never trust a client-supplied user
  ID in any form (header, body, query param).
- Resume ownership is enforced at the query level: `GET /api/resumes` filters by the resolved
  `user_id` from the verified token, full stop. A user must never be able to fetch or reference
  another user's resume row.
- Firebase Admin SDK service-account credentials must never be committed to git or embedded in
  the Android app — load them from an environment variable or a secrets file excluded via
  `.gitignore`.
- Don't log resume contents or other PII (email, filename contents beyond what's needed for
  debugging) at anything above debug level, and avoid logging full file paths in production logs.

## What Android already does (for backend-side context)

- `AuthInterceptor` attaches the bearer token to every request automatically — nothing to wire
  per-call.
- `ResumeRepository` (`:features:resume/data/remote/ResumeRepositoryImpl.kt`) wraps both calls in
  `safeApiCall`, mapping `HttpException` by status code into `AppError` (`Unauthorized` for 401,
  `Server(code)` for everything else, `Network` for `IOException`). Validation-specific `AppError`
  cases (`InvalidFile`, `FileTooLarge`) are currently only produced by Android's own client-side
  checks (`ResumeFileReader`) — a `400`/`413`/`415` from the backend currently surfaces to the
  user as a generic `Server(code)` error, not a specific one. If you want sharper backend-driven
  error messages later, that'd need either distinct Android-side status-code branches or a
  structured error body Android parses — out of scope for this phase, flagging it as a known gap.
