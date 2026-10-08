# Tyde — Android Client

## What Tyde is

Tyde is an AI-powered job application assistant. End-to-end flow (most of this does not exist
yet — see Roadmap):

```
Upload Resume → AI builds Professional Profile → Find Relevant Jobs
→ Explain Why Each Job Matches → Generate Application Answers
→ Prepare Application → User Reviews → User Submits
```

## Backend boundary

A separate Spring Boot + Kotlin + PostgreSQL backend is the source of truth for resume
processing, profile generation, job discovery/matching, AI-generated answers, application
preparation, and application tracking. **The Android app is a thin client.** It never embeds AI
logic, LLM calls, or credentials — those only ever reach the backend's REST API.

Identity is handled by **Firebase Authentication**, not the Spring Boot backend. Firebase owns
sign-up/sign-in/session; the backend's job is to verify the Firebase ID token (via the Firebase
Admin SDK, backend-side only) and map the verified Firebase UID to a Tyde user row. The Android
app never generates, signs, or validates its own tokens — it only requests a Firebase ID token
and attaches it to backend requests.

This repo is the Android client only.

## Tech stack and why

- **Kotlin + Jetpack Compose + Material 3** — standard modern Android UI stack.
- **MVVM** with explicit `UiState` — ViewModels own state, Composables render it.
- **Koin (4.2.1)**, not Hilt — simpler DSL, no annotation processing, explicit module wiring.
- **Firebase Authentication** (`firebase-auth`, BoM 35.0.0) — email/password now; Google Sign-In
  via Credential Manager also implemented (see Authentication below). No custom auth, no
  password hashing, no JWT issuance anywhere in this codebase.
- **Retrofit 3.0.0 / OkHttp 5.5.0 / kotlinx.serialization 1.7.3** — networking; Retrofit's
  first-party `converter-kotlinx-serialization` avoids pulling in Moshi/Gson.
- **Navigation Compose 2.10.2** with `kotlinx.serialization`-backed type-safe routes.
- **Room is intentionally not used yet** — nothing in the app needs structured local persistence.
  Add it when a real feature needs to query local data.
- **MockK, Turbine, kotlinx-coroutines-test** for testing ViewModels/use cases/repositories.
- AGP 9.4.1 has built-in Kotlin support, so Android modules apply only `com.android.library` /
  `com.android.application` — no separate `org.jetbrains.kotlin.android` plugin, **except**
  `:core:auth`, which needs it (see Known gaps / toolchain note below).

## Module structure

```
:app                 composition root — Koin startup, MainActivity, auth-gated NavHost, HomeScreen
:core:common          AppResult/AppError, DispatcherProvider, AuthTokenProvider (pure kotlin("jvm"))
:core:designsystem    TydeTheme, typography, shapes, spacing, TydeButton/Card/Loading/ErrorState
:core:network         Retrofit/OkHttp setup, AuthInterceptor, safeApiCall error mapping
:core:navigation      TydeDestination route contracts, TydeNavHost wrapper
:core:auth            AuthRepository, AuthState, TydeUser, FirebaseAuthRepository (only module
                      that imports FirebaseAuth), FirebaseAuthTokenProvider
:features:auth        Login/Register Composables + ViewModels, Google Sign-In launcher
:features:resume      Resume screen, upload via SAF picker, Resume domain model + repository
```

Dependency direction: `:app` depends on all `:core:*` and `:features:*` modules. `:core:network`
and `:core:auth` depend on `:core:common` only (for the shared `AppError`/`AppResult`/
`AuthTokenProvider` contracts) — `:core:network` never depends on `:core:auth` or vice versa.
`:features:auth` depends on `:core:auth`, `:core:common`, `:core:designsystem`.
`:features:resume` depends on `:core:network`, `:core:common`, `:core:designsystem` — it owns its
own Retrofit `ResumeApi` (feature-owned API contracts, not centralized in `:core:network`, same
pattern as the rest of the module map) and does **not** depend on `:core:auth` — it doesn't need
to know anything about how the bearer token gets attached, that's `:core:network`'s
`AuthInterceptor`'s job, already wired. No `:core:database` exists yet — see Roadmap.

`AuthTokenProvider` lives in `:core:common` specifically so `:core:network`'s `AuthInterceptor`
never has to depend on `:core:auth` directly as a Gradle edge; the binding happens through Koin
at the `:app` composition root (`FirebaseAuthTokenProvider` implements the `:core:common`
interface, bound in `:core:auth`'s own Koin module).

**`:core:datastore` was deleted in Phase 1.** It held a DataStore-backed `SessionTokenStorage`
built in Phase 0 for a custom backend-issued-token model. Once Firebase became the identity
provider, it was redundant: Firebase persists its own session securely and restores it via
`FirebaseAuth.addAuthStateListener`, and `FirebaseUser.getIdToken(false)` already refreshes an
expired token automatically. Keeping a second, weaker (plaintext) token store alongside Firebase's
own would have been actively worse than not having one — so it was removed rather than adapted.

## Authentication architecture

```
Composable → ViewModel → AuthRepository (interface) → FirebaseAuthRepository → FirebaseAuth
```

- `AuthState` (`:core:auth`): `Loading | Unauthenticated | Authenticated(TydeUser)`. Backed by a
  single `MutableStateFlow` inside `FirebaseAuthRepository`, updated from Firebase's own
  `AuthStateListener`. This is the one app-wide source of truth for "is anyone signed in" —
  nothing else maintains auth state.
- `TydeUser` (`:core:auth`): uid/email/displayName/photoUrl/isEmailVerified — the mapped,
  Firebase-agnostic shape the rest of the app sees. `FirebaseUser` never leaves `:core:auth`.
- `AuthRepository.login/register/signInWithGoogle` return `AppResult<TydeUser>`; `AuthErrorMapper`
  maps `FirebaseAuthException.errorCode` to `AppError` (`InvalidEmail`, `WeakPassword`,
  `InvalidCredentials`, `EmailAlreadyInUse`, `TooManyRequests`, plus the existing `Network`/
  `Unknown`). ViewModels expose `AppError?` in their UI state, not pre-rendered strings — the
  Composable resolves `AppError` → user-facing copy via `stringResource` (`authErrorMessage` in
  `:features:auth`), keeping `:core:common` free of hardcoded English and localizable later.
- **Google Sign-In** uses Credential Manager (`androidx.credentials` + `googleid`), not the
  deprecated `GoogleSignInClient`. `GoogleIdTokenRequester` (`:features:auth`) takes `Context` as
  a per-call parameter, never a stored field — Activity context must never be held by a
  ViewModel or a Koin singleton. The resulting ID token is exchanged for a Firebase credential in
  `FirebaseAuthRepository.signInWithGoogle`. The Google button lives on the Login screen only —
  Google sign-in creates-or-signs-in in one action, so a separate Register-with-Google path adds
  nothing.
- **Navigation gating** (`TydeApp.kt`, `:app`): while `AuthState.Loading`, shows a loading
  indicator (no separate splash screen/activity). Once resolved, mounts `TydeNavHost` with a
  startDestination captured once from that first resolved state, then a `LaunchedEffect(authState)`
  reacts to *transitions only* (Unauthenticated→Authenticated navigates to `Home`;
  Authenticated→Unauthenticated navigates to `Login`; both with `popUpTo(0) { inclusive = true }`).
  It deliberately does **not** compare against `AuthState.Loading` again after the first
  resolution — Loading must be a one-way door, or a stray re-emission would tear down the entire
  NavHost and back stack.
- Backend integration (Spring Boot verifying the Firebase ID token via the Admin SDK, loading/
  creating the Tyde `User` row keyed on `firebaseUid`) is out of scope for this repo — that's
  backend work. The Android side's only job is attaching `Authorization: Bearer <idToken>`, which
  `:core:network`'s existing `AuthInterceptor` already does unchanged from Phase 0, now backed by
  `FirebaseAuthTokenProvider` instead of a stored token.

## Resume upload architecture

```
ResumeScreen (SAF picker) → ResumeViewModel → ResumeRepository → ResumeApi (Retrofit) → backend
```

- **The Spring Boot backend does not exist in this repo or anywhere on this machine as of Phase
  2.** It's being built separately in IntelliJ. `BACKEND_CONTRACT.md` at the repo root is the
  full API/DB/storage specification the Android side already assumes — implement against it.
  Nothing describing the backend in this file or that one has been run or verified; it's a
  specification, not a confirmed integration.
- **Picker:** `ActivityResultContracts.GetContent()` with MIME type `application/pdf` — modern
  Storage Access Framework, no custom file browser, no storage permission requested.
- **Client-side validation** (`ResumeFileReader`, `:features:resume`): checks declared MIME type
  (falling back to a `.pdf` filename check if the provider reports none), reads the file size via
  `OpenableColumns.SIZE` where available to reject oversized files *before* reading them, then
  reads the actual bytes and re-checks both emptiness and size as a safety net. Limits live in one
  place (`ResumeUploadLimits`, 10 MB default), never scattered as literals. **This is defense in
  depth only — the backend independently re-validates everything per `BACKEND_CONTRACT.md`,
  since client-side checks are trivially bypassable.**
- **In-memory read, not streamed:** the full file is read into a `ByteArray` (bounded by the 10 MB
  cap) before upload, rather than streaming directly from the `ContentResolver` `InputStream`.
  Simpler and correct for an MVP at this size cap; revisit with a streaming `RequestBody` if the
  size limit ever grows substantially.
- **Upload progress:** `ProgressRequestBody` (`:core:network`, reusable by any future feature that
  uploads files) wraps the multipart body's `writeTo`, throttled to only call back on integer
  percentage changes — a naive per-chunk callback would fire hundreds of times for a 10 MB file
  and flood recomposition.
- **Retry keeps the already-read file.** `ResumeViewModel` holds the last successfully-read
  `SelectedResumeFile` privately (not in UI state) so "Try Again" after a failed upload re-sends
  the same bytes without re-opening the system picker.
- **Duplicate-upload guard:** `onFileSelected`/`onRetry` both check `uploadState.isBusy`
  (Validating or Uploading) synchronously before launching, so a second tap during either phase is
  a no-op rather than a second in-flight request.
- Domain model (`Resume`, `ResumeStatus`) is mapped from `ResumeDto` in the data layer — the DTO
  never reaches the ViewModel or UI, same DTO/domain separation as everywhere else in this codebase.

## Known gaps / manual setup required

- **`app/google-services.json` does not exist in this repo and cannot be generated by me.**
  `:app` will not build until you add it — see "Firebase Console setup" below. Every other
  module (`:core:auth`, `:features:auth`, etc.) builds and tests standalone without it.
- **Toolchain note:** `:core:auth`'s `build.gradle.kts` adds
  `-Xskip-metadata-version-check` to its Kotlin compiler args. AGP 9.4.1's built-in Kotlin
  compiler is pinned to 2.2.10, but the current Firebase Auth SDK and `googleid` artifacts are
  compiled with Kotlin 2.4.0 metadata — without this flag, `compileDebugKotlin` fails with
  "Module was compiled with an incompatible version of Kotlin." This is a known AGP-built-in-
  Kotlin vs. newer-library ecosystem gap, not a Tyde-specific issue; revisit removing the flag
  once AGP's built-in Kotlin baseline catches up.
- **No backend exists anywhere, on this machine or otherwise, as of Phase 2.** `ResumeApi`
  (`:features:resume`) defines the `/api/resumes` contract Android expects — see
  `BACKEND_CONTRACT.md`. Until a real backend exists, resume upload cannot be exercised
  end-to-end; only client-side behavior (picker, validation, UI states, unit tests) is verified.
- **`HomeScreen` is still a placeholder**, now with a functional Logout button, signed-in email
  display, and a working link into the Resume screen, but no ViewModel and no real feature logic
  of its own.
- **Backend-driven validation errors (400/413/415) currently surface as a generic `AppError.Server(code)`
  on Android**, not as the specific `InvalidFile`/`FileTooLarge` cases (those are currently only
  produced by Android's own client-side `ResumeFileReader` checks). Flagged in
  `BACKEND_CONTRACT.md` as a known gap, not fixed this phase.

## Firebase Console setup (manual — do this before `:app` will build)

1. Create (or use an existing) Firebase project at the Firebase Console.
2. Add an Android app to it with package name `com.cephcoding.tyde`. Add your debug keystore's
   SHA-1 fingerprint (`./gradlew signingReport` finds it) — required for Google Sign-In.
3. Download the generated `google-services.json` and place it at `app/google-services.json`
   (not committed — add it to `.gitignore` if it isn't already covered).
4. In **Authentication → Sign-in method**, enable **Email/Password**.
5. In the same screen, enable **Google** as a sign-in provider. This is supposed to generate a
   **Web client ID** (OAuth client type 3) inside `google-services.json`, which the google-services
   plugin would read automatically into `R.string.default_web_client_id`. **In practice, enabling
   the provider without also registering a verified SHA-1 fingerprint can leave `oauth_client: []`
   empty in the downloaded JSON** — if that happens, `R.string.default_web_client_id` won't exist
   and `:app` won't compile. The fallback actually in use: add `WEB_CLIENT=<your-oauth-client-id>`
   to `local.properties` (gitignored, machine-local) — `:app/build.gradle.kts` reads it into
   `BuildConfig.GOOGLE_WEB_CLIENT_ID`, and `AppModule` sources the Koin-provided web client ID from
   there instead of the resource. Get the Web client ID from Google Cloud Console → APIs &
   Services → Credentials (OAuth 2.0 Client IDs), under the project Firebase created.
6. Nothing else needs manual wiring — no API keys go in the Android app.

## Roadmap

- **Phase 0 (done)** — foundation: multi-module structure, DI, networking, persistence boundary,
  navigation contracts, design system.
- **Phase 1 (done, verified working)** — Authentication: Firebase email/password + Google
  Sign-In, `AuthState` observation, session restoration, authenticated Retrofit requests,
  login/register UI, logout.
- **Phase 2 (Android side done; backend not started)** — Resume upload: picker, client-side
  validation, multipart upload with progress, metadata retrieval, Resume screen UI states. See
  `BACKEND_CONTRACT.md` for the backend half of this phase, which hasn't been built yet.
- **Phase 3 (next)** — AI Resume Analysis → structured professional profile, with a strict truth
  boundary so the AI doesn't invent experience not present in the resume.
- **Later** — Jobs discovery + match explanations, Application generation/review/submission,
  Settings. Each becomes a `:features:<name>` Gradle module, following the same dependency
  pattern as `:features:auth`/`:features:resume` (depends on the `:core:*` modules it needs;
  `:app` depends on it).

## Code style

No comments in generated Kotlin source — names, types, and structure carry intent. Constructor
injection everywhere; no field injection (the one deliberate exception: `Context` passed as a
per-call parameter to `GoogleIdTokenRequester`, never stored). No business logic in Composables.
DTOs and Room entities (once either exists) never cross into the UI layer unmapped.
