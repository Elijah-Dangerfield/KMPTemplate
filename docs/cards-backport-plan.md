# Backport plan: Cards → KMPTemplate

Written 2026-07-23 from a full comparison of `~/Workspace/Cards` against this template
(8 deep-dive passes: auth, networking/sync, telemetry, config/admin, CI/tooling,
integration tests, server, client shell). Cards was generated from this template and has
since shipped V1; this plan pulls the reusable improvements back so the next project
starts from the good version.

Conventions for every item below:
- Package rename `com.dangerfield.cards.*` / `com.cards.*` → `com.kmptemplate.*`
  (module `libraries/cards` maps to `libraries/kmptemplate`).
- Strip Cards branding: "Downcard"/"Cards" strings, warm-felt tokens, `cards://` scheme
  (make configurable), `cards-server-dev/prod.fly.dev` URLs, `CARDS_*` env/secret names.
- Preserve two things the template does BETTER than Cards (do not overwrite):
  - `apps/server` graceful "limited mode" boot (null DB / null Supabase still serves
    `/_health`). Keep the null-safe branching when wiring new server pieces.
  - `apps/server/docker-compose.yml` + `docker/init-auth.sql` local-dev stack (Cards
    lacks it — candidate to backport *into Cards* separately).
- CI lands in `template/ci/.github/workflows/` (the init-script staging dir), not a live
  `.github/` in the template repo itself, matching the existing release/ci staging setup.

---

## Phase 0 — Small correctness fixes and removals (do first, independent)

Fixes:
1. **Exposed transactions off the Netty loop**: `Database.transaction` →
   `newSuspendedTransaction(context = Dispatchers.IO, db = exposed)` + port Cards' kdoc
   rationale. Source: `Cards/apps/server/.../data/Database.kt`.
2. **CORS defaults**: add `allowMethod(Put)`, `allowMethod(Delete)`,
   `allowHeader("X-Admin-Token")`, `allowHeader("X-Admin-Actor")` with the explanatory
   comments. Source: `Cards/apps/server/.../plugins/Cors.kt`. (This was a real prod bug:
   admin writes 403'd at preflight.)
3. **Remember the NavHost graph builder** in `App.kt` (`remember(featureEntryPoints,
   router) { ... }`) to stop recompositions re-pushing the start destination. Source
   pattern: `Cards/apps/compose/.../App.kt` (`AppNavHost`).
4. **`android:configChanges`** on the activity (orientation|screenSize|uiMode|fontScale|
   locale|density|…) + comment — prevents nav back-stack loss. Source:
   `Cards/apps/compose/src/androidMain/AndroidManifest.xml`.
5. **`@ContributesBinding` boundType**: template `SupabaseAuthRepository` should declare
   `boundType = AuthRepository::class`.

Removals (stale template code):
6. `libraries/kmptemplate` demo scaffolding: `User`, `Session`, `UserRepository`,
   `SessionRepository` + `UserDao/UserEntity/SessionDao/SessionEntity` — superseded by
   the real profile layer in Phase 2.
7. `libraries/config` `FeatureConfig.kt` delegated-property DSL — Cards abandoned it;
   the injectable `ConfiguredValue` subclass convention won. One convention only.
8. Stale `libraries/config/README.md` (mentions "Virtu" and a nonexistent
   `Experiment<T>`) — rewrite during Phase 4.
9. The 4 hardcoded placeholder Sentry DSNs in `AppTelemetry.kt` — replaced by single-DSN
   + build-time injection in Phase 5.

## Phase 1 — Foundations (prerequisites for everything after)

1. **`AutoInit`** marker + boot loop resolving `Set<AutoInit>` at app start (replace the
   hardcoded eager `appEventDispatcher` init in AppComponent / iOSApp.swift /
   Application). Source: `Cards/libraries/core/.../AutoInit.kt` + AppComponent wiring.
   Port the AGENTS.md "Boot-time construction" section too.
2. **`AppEventBus` upgrade**: two-flow dispatcher (replay=1 `eventStream()` + replay-free
   `liveEventStream()`), injectable `AppEvents`, new events `ConnectivityRegained` and
   `UserChanged(previous, current)` (keep UserChanged — Phase 2 auth uses it). Source:
   `Cards/libraries/cards/.../AppEvent.kt`, `AppEvents.kt`, impl `AppEventDispatcher.kt`.
3. **`runWhen` primitive** + `RunWhenRetry` (+ tests) into `libraries/flowroutines` —
   verbatim, fully generic. Source: `Cards/libraries/flowroutines/.../RunWhen.kt`.
4. **`RetryPolicy` / `withRetry` / `Backoff` / `Jitter`** (+ tests) into
   `libraries/networking/retry/` — verbatim. Source: `Cards/libraries/networking/src/.../retry/`.
5. **`OfflineErrors`** (`isOfflineError()` + android/ios actuals) — verbatim.
6. **Connectivity + reachability** (fixes the template's dead offline banner —
   `AppState.isOffline` is currently a hardcoded `PreviewAppState` stub in prod):
   - `NetworkReachability` (consecutive-failure counter, threshold 2)
   - `ConnectivityObserver` + `AndroidConnectivityObserver` (NET_CAPABILITY_VALIDATED) +
     `IosConnectivityObserver` (NWPath)
   - `AppStateImpl` = combine(platform, reachability); offline when either says so
   - Witnessed reachability in `NetworkClientImpl` via `HttpResponseValidator`
     (any response → reachable; non-ResponseException failure → unreachable)
   - `ConnectivityEdgeDispatcher` (AutoInit; offline→online edge, debounce 750ms →
     `AppEvent.ConnectivityRegained`)
   Sources: `Cards/libraries/networking[/impl]` + `Cards/libraries/cards/impl`.
7. **Navigation hardening**: centralize `baseRouteTypeMap` (de-dupe the 3 inline maps),
   `routeDeepLink()`, `navigation<T>` nested-graph helper. Document the two iOS
   landmines in AGENTS.md: routes must be `class` not `data object` (SIGSEGV at
   navigate), and enum route args must be `@Serializable` + registered in the type map.
   Source: `Cards/libraries/navigation/src/.../NavGraphBuilderExt.kt`.
8. **`libraries/flowroutines/testing` module**: `CoroutineTest`, `TestDispatcherProvider`,
   `runUnitTest`; re-exports coroutines-test + turbine (add turbine to the catalog).
   Add `include(":libraries:flowroutines:testing")`. Wire one `HomeViewModelTest` as the
   reference example.
9. Small utilities: `DispatcherProvider.mainImmediate`, `Catching.withBackoffRetry`.

## Phase 2 — Auth overhaul (the biggest single delta)

The template's `AuthRepository` is a 4-method placeholder with an `AuthState.Loading`
enum. Replace it with Cards' model.

1. **State + outcomes**: sealed `AuthState.Authenticated(userId, isAnonymous, email)` /
   `Unauthenticated(cause, reason, wasAnonymous)` with `Reason {None, SessionExpired,
   SignedOut}`; no Loading sentinel (`current()`/`observe()` suspend). Sealed per-op
   outcomes (`SignInOutcome`, `SignUpOutcome`, `RefreshOutcome`, `DeleteAccountOutcome`,
   `LinkIdentityOutcome`, …) + `AuthOutcomeClassifier`. Sources:
   `Cards/libraries/identity/src/.../auth/{AuthRepository,AuthOutcomes,AuthOutcome,AuthOutcomeClassifier}.kt`.
   Breaking change: migrate `features/home` off `authState`/`Loading`.
2. **Gateway seam**: `SupabaseAuthGateway` + `RealSupabaseAuthGateway` + `AuthClaims`
   (JWT `is_anonymous` derivation) — the testable seam over supabase-kt. Make the OAuth
   redirect scheme configurable (Cards hardcodes `cards://login-callback`).
3. **Orchestrator**: `SupabaseAuthRepositoryImpl` generalized (mutex-serialized state,
   bootstrap resolve loop, `emitLocked` choke point). Keep the user-change choke point as
   a seam (`UserScopedDataReset` can be a no-op-friendly multibinding).
4. **Buses** (in `libraries/networking`): `SessionRejectionBus` (server-confirmed 401
   after refresh → routed sign-out, no 401 loops) + `AccessDeniedBus` (403 ban envelope
   `Denial(reason, until, appealUrl)`) + `AuthTokenInvalidator`; the 403-envelope
   detection + rejection wiring in `NetworkClientImpl`.
5. **Token provider**: replace `SupabaseAuthTokenProvider` with `GatewayAuthTokenProvider`
   + `RefreshFailureClassifier` (AuthRejected vs Transient); add `awaitReady()` to the
   `AuthTokenProvider` interface.
6. **Encrypted session storage**: `SecureSessionStorage` API + `SecureSessionManager` +
   `SessionMirrorStore` (file mirror for anon sessions — survives TestFlight Keychain
   wipes) + `EncryptedSessionStorage` (Android) + documented iOS Keychain wiring point in
   `iOSApp.swift`. Wire into `SupabaseModule` (`install(Auth) { sessionManager = … }`).
   Today's template stores sessions in plaintext settings — real security upgrade.
7. **Apple sign-in**: `AppleSignInCoordinator` + `AppleSignInCredential` +
   `awaitCredential()` + Android no-op + documented Swift `ASAuthorizationController`
   conformance.
8. **Profile layer** (replaces the Phase-0 removed demo scaffolding):
   `ProfileRepository` (`Profile.Authenticated`/`Fallback`, offline edit queue +
   `ProfileEditFlusher`, `resolveIsNewAccount`), `DisplayNameRules` (shared client/server
   validation), impl (`ProfileApi` /v1/me, `ProfileCache`, `PendingProfileEditStore`).
   Strip avatar packs / premium inventory.
9. **Optional, later**: deferred guest creation + self-heal cluster
   (`GuestAccountCreator`, `GuestSessionHealer`, `StrandedIdentityDetector`,
   `AuthReResolver`) — port only if the template keeps the anonymous-first story;
   otherwise document as an advanced pattern.
10. **Optional UI**: genericized onboarding auth screens (SignIn/SignUp/VerifyEmail/
    ForgotPassword VMs + screens) and the AccessDenied/SessionExpired routing skeleton
    (`AppViewModel` collectors + blocking screens). Largest chunk; do last.

## Phase 3 — Server: moderation, admin client, deploy

1. **Ban gate**: `plugins/AccessControl.kt` (`AccessDeniedResponse` wire body, `BanGate`)
   + the `banGate` fold into `Authentication.kt` (`validateAndChallengeForUserId`; banned
   → 403 envelope, fail-open on lookup error) + `ModerationRepository` /
   `PostgresModerationRepository` (reads native Supabase `auth.users.banned_until` — no
   migration needed) + `APPEAL_URL` config. Ship with `banGate` optional/null by default.
2. **Player reports**: `PlayerReportRoutes` + DTO + repository + ONE merged migration
   (`player_reports` with `reason_categories` from the start; Cards' V85+V87). Rename
   `roomCode` → generic nullable `context` field. Add the `PLAYER_REPORT_LIMIT` rate
   bucket. (Google Play UGC policy requires a report path for any app with user content —
   this belongs in the template.)
3. **`SupabaseAdminClient`** (`domain` + `HttpSupabaseAdminClient`): `deleteUser` (both
   stores require in-app account deletion), `updateUserDisplayName` (auth-metadata
   mirror), `listAnonymousUsersOlderThan`. Add `serviceRoleKey` to `SupabaseConfig` +
   `SUPABASE_SERVICE_ROLE_KEY` in `.env.example`. Never-log-key guard, sealed results,
   `NotConfigured` handling.
4. **MeRoutes upgrades**: `DELETE /v1/me` (admin-delete-first ordering, documented
   extension point instead of Cards' game-table cascade), `fireDisplayNameMirror`
   off-path best-effort, adopt `DisplayNameRules` validation server-side.
5. **`http/ClientContext.kt`** — header parsing (platform/version/build/country/locale/
   session/install), verbatim. Wire `install_id`/`session_id` into MDC (drop
   `room_code`).
6. **`plugins/WebSockets.kt`** — ping 15s / timeout 30s / permessage-deflate, verbatim.
   Add a one-paragraph doc on the wire-envelope lesson (sealed `@SerialName` polymorphic
   events + `ignoreUnknownKeys` for forward compat) without porting poker events.
7. **Two-environment deploy**: add `fly.prod.toml` beside `fly.toml`; document the
   dev-auto / prod-approval split in DEPLOY.md; generalize env derivation to a
   `FLY_APP_NAME` convention.
8. Skip: `SingleWriterGuard` (advisory-lock single-writer is an anti-pattern for a
   generic template — document as "if you hold authoritative state in RAM, see Cards").

## Phase 4 — Remote config end-to-end + admin console

Client:
1. Richer `ConfiguredValue` (`group`, `allowedValues`, `QaConfigValue` marker,
   `showInQADashboard=true`) + `TypedConfiguredValues` scalar bases + `JsonConfigValue`.
2. Replace the dummy `RemoteConfigRemoteDataSource` (returns random data!) with the real
   HTTP source against `GET /v1/app-config`; adopt Cards'
   `OfflineFirstAppConfigRepository` (throttled foreground refresh via
   `ConfigRefreshThrottleMs`, K/N-deadlock-safe `LazyAppConfigMap`, 5s timeout, bundled
   fallback only when no cache).

Server:
3. `PostgresAppConfigSource` + `AppConfigTargetingEngine` (platform/version/semver/
   country/locale/allow-deny/FNV-1a deterministic rollout) + `AppConfigTree` +
   `ConfigValidation` + `AppConfigRoutes` + migrations (renumber V75–V77 → next free).
   Seed ONLY the generic kill-switch trio: `upgrade.maintenanceMode`,
   `upgrade.maintenanceMessage`, `upgrade.minSupportedVersionCode`. Strip Cards flags
   (`social.enabled`, `identity.*SignInEnabled`, `onboarding.*`, `billing.*`).
4. Admin API: `ConfigAdminRoutes` (flags/rules/audit/manifest/resolve), `AdminAuth`
   (constant-time `X-Admin-Token`), `ADMIN_API_TOKEN` config, admin repositories,
   `WebhookConfigChangeNotifier` (Slack, no-op unconfigured), `plugins/AdminWeb.kt`
   (serve bundle at `/admin`).

Console:
5. Copy `apps/admin` wholesale (Compose HTML / Kotlin-JS, tabs: Flags/Versions/Audit/
   Dev↔Prod, kill-switch panel, rule editor, target-lens resolve, revert, prod
   confirm-by-typing) then de-brand: `AdminEnv` URLs → placeholders, output JS name,
   wordmark, manifest registry stripped to the `upgrade.*` trio. Keep the
   `exportConfigManifest` task + drift-test pattern. Add `:apps:admin` to settings.
6. Deploy wiring: Dockerfile `admin-web` COPY + `ADMIN_WEB_DIR`, deploy-workflow steps
   building the JS bundle + PUTting the manifest (generalize secret names).
7. Rewrite both config READMEs (`libraries/config`, `apps/admin` — Cards' admin README
   is excellent; genericize it).

## Phase 5 — Telemetry and observability

1. **Server session correlation** (highest value, fully generic):
   - `Tracing.kt`: `attributesExtractor` pinning `session_id`/`install_id` on root span
     + Baggage interceptor; keep `SpanAttrs` only for session/install/user.id.
   - `Telemetry.kt`: `BaggageAttributeSpanProcessor` + `CORRELATION_BAGGAGE_KEYS` +
     `parseOtlpHeaders` percent-decode (Grafana needs it).
   - `Observability.kt`: `session_id`/`install_id` MDC from headers (no room_code).
   - `Sentry.kt`: `captureToSentry` stamping trace_id/span_id + MDC (links Sentry issue →
     Tempo trace).
   - `logback.xml`: `captureMdcAttributes=*`.
2. **Client session plumbing**: `Session`/`SessionTracker` (15-min background rollover),
   `SessionTelemetryBinder`, `SessionIdProvider`, `InstallIdProvider`, `ClientHeaders`
   (`X-Session-Id`, `X-Install-Id`, platform/version/build/country) +
   `DefaultClientHeadersProvider` in the default request. This is what makes one
   session_id pull Sentry + Tempo + Loki.
3. **SentryLogTree upgrades**: `shouldCaptureEvent()` dropping `isOfflineError()` and
   `isExpectedControlFlow` throwables (ENG-34), `LogRingBuffer` + `minBufferLevel` +
   `snapshot()`.
4. **Widen `Telemetry` interface**: `setSession`, `setInstallId`, `setCurrentRoute`, and
   a generic `setContext(key, value?)` instead of Cards' setRoom/setSeat/setHand.
   Feedback enrichment: carrier-event `beforeSend` fingerprinting, session-log
   attachment, screenshots + email, `commit_sha`/`commit_branch` tags.
5. **New `:libraries:telemetry` module**: `GrafanaLogTree` (OTLP logs, per-record
   session/install/is_offline attrs, per-session hash sampling),
   `OtlpJsonLogRecordExporter` (custom, replaces crash-prone library exporter),
   `FailSafeLogRecordExporter`, `DurableLogExport` (disk-buffered, survives process
   death), `GrafanaAppEvents`/`AppLaunchedEmitter`, `TelemetryConfigValues` (remote
   kill switches), `TelemetryBackgroundFlusher`. Optional: `MetricKitExitReport` /
   `PreviousExitProvider` crash-free-rate mechanism.
6. **Build-time secret injection**: `loadTelemetryMetadata`/`writeTelemetryMetadata`
   (`GRAFANA_OTLP_BASE_URL`/`INSTANCE_ID`/`LOGS_WRITE_TOKEN` from CI env or
   `local.properties`); single Sentry DSN pattern replacing the 4 placeholders.
7. **Docs skeletons**: genericized `docs/wiki/observability.md` (session_id pivot, Loki
   label conventions) + `app-events.md` registry seeded with the generic events only
   (`app.launched/foregrounded/backgrounded`, `net.backend_unreachable`, `conn.*`).
8. `AppEvents.kt` (`logEvent(name, attrs)` single blessed emitter) into core logging if
   missing.

## Phase 6 — Triggered sync system

Depends on Phases 1 (AutoInit, events, reachability, runWhen) and 2 (auth level).
1. `SyncTriggers`: `activeAccount` (level flow from `AuthRepository.observe()`,
   data-class equality is the refire contract), `warmForeground`, `cameOnline`,
   `isOffline`.
2. `UserScopedSyncer` interface — features implement one idempotent `sync()` and
   contribute via `@ContributesBinding(multibinding = true)`. Ship zero impls + one stub
   example + the registration doc comment.
3. `UserScopedSyncCoordinator` (AutoInit): one `runWhen(key=activeAccount,
   refireOn=merge(warmForeground, cameOnline), retry=exponential)` loop per syncer;
   parks as success while offline (re-armed by cameOnline).
4. `UserScopedWorkRegistry` + `UserScopedWorkStopper` (cancel+await in-flight syncs on
   user switch before data wipe) — include since UserChanged is kept.
5. `NetworkCall` wrappers: `authedCall`/`unauthedCall` (description-keyed structured
   logging, RetryPolicy integration, offline log downgrade, Catching return,
   session-rejection 401→SessionExpired remap). Include the auth-gate short-circuit only
   if Phase 2 ported `AuthGate`.
6. Outbox: do NOT port Cards' chips outbox; write `docs/practices/outbox.md` describing
   the pattern (Room event table + idempotency keys + per-event reconciliation +
   flush-on-foreground/reconnect), paraphrasing `ProfileEditFlusher` as the ~50-line
   reference.

## Phase 7 — CI/CD, release, quality gates

All into `template/ci/` staging unless noted. Placeholders: `{{APP_ID}}`,
`{{FLY_APP_DEV}}`, `{{FLY_APP_PROD}}`, TestFlight group, framework/bundle names,
`{{APP}}_WIRETAP_IOS`.
1. **`ci.yml`** three-job structure (build-and-test macOS / server-test Ubuntu+Docker /
   integration-test Ubuntu) + dorny/test-reporter + artifact uploads.
2. **release-please**: config + manifest + `release-please.yml` + `commitlint.yml` +
   starter CHANGELOG. (Template already stages some of this — diff against Cards' and
   take the evolved versions, esp. the explicit `gh workflow run` tag-cascade fix.)
3. **detekt**: `detekt-rules/` module with `VerifyStrings` (no inline user-facing string
   literals in DS text composables; templated callee set), `config/detekt/`, root
   detekt task wiring, `.githooks/pre-push` (runs detekt, `SKIP_DETEKT=1` bypass) — the
   template has commit-msg/post-commit hooks but no pre-push.
4. **`Versioning.kt` CI overrides**: `VERSION_NAME_OVERRIDE` / `VERSION_CODE_OVERRIDE` /
   `BUILD_NUMBER_OVERRIDE` / `RELEASE_CHANNEL_OVERRIDE` + config-cache-safe commit
   metadata (`COMMIT_SHA`/`COMMIT_BRANCH` BuildConfig).
5. **beta/release pipeline**: `beta.yml` + `release.yml` + Cards' evolved `Fastfile`
   (ephemeral keychain .p12 import, ASC API key, timestamp build number, Sentry release
   + dSYM upload, staged/phased rollout) — heaviest templating; diff against the staged
   template Fastfile and take Cards' improvements.
6. **server-deploy**: `server-deploy.yml` (dev, on push to main touching server paths) +
   `server-deploy-prod.yml` (prod approval gate via GitHub Environment, job-level
   concurrency, `confirm: "prod"` input, health smoke test). The template HAS a Fly Ktor
   server, so these apply directly.
7. **`.gitignore` security block** (public-repo posture): `*.jks`, `*.keystore`,
   `*.p12`, `apps/server/.env*`, fastlane `.env`, admin tokens.
8. **`auto-merge.yml`** (label-gated squash auto-merge) — as-is.
9. `gradle.properties` heap bumps (`-Xmx10g` / K/N `-Xmx5g`) — needed for iOS release
   linking.
10. Skip: admin ops workflows (grant-chips, send-message, sweep), `retag-release.yml`
    (optional nice-to-have), sentry-triage prompts.

## Phase 8 — Integration test harness

1. **`apps/integration` skeleton** (Android-library module, tests as host-JVM
   `testDebugUnitTest`, empty commonMain so iOS never links the server):
   - `IntegrationAuth.kt` near-verbatim — mints Supabase-shaped HS256 JWTs against the
     `JwtVerification.Static` seam the template already has.
   - `IntegrationTest.kt` base — real Main dispatcher, `integration {}` runBlocking
     helper, `awaitUntil`/`awaitState` polling (no sleeps).
   - Slim `InProcessServer` — `embeddedServer(Netty, port = 0)` booting the template's
     real `installApp(component, JwtVerification.Static)` over testcontainers Postgres,
     exposing `baseUrl`.
   - Slim `TestClient` — real `NetworkClientImpl` + real `HomeViewModel` driving
     `GET /v1/me` as the worked example: real client → real TCP → real server → real DB.
   - One `HarnessSmokeTest`.
   Strip all poker content (wallets, decks, snapshots, chaos transports — though the
   fault-injection decorator pattern deserves a doc mention).
2. **CI job** (Phase 7 item 1 includes it).
3. **`docs/practices/testing.md`** genericized: hand-rolled fakes only, `runCatching`
   not `assertFailsWith` for suspend, dispatcher-choice rules, per-file KDoc convention,
   which-layer-catches-which-bug table.
4. Optional: a thin VM scenario-harness example (builder wiring a real VM with fakes +
   `assertState {}` DSL) against HomeViewModel — the poker harness itself stays.

## Phase 9 — Client shell polish and dev tooling

1. **Design-system catalog**: port `catalog/` scaffolding (CatalogPage/Section,
   ColorRow, SpecRow, swatches — token-agnostic) + `DesignSystemPreview` structure;
   repoint content at template tokens. Gives the template a living DS spec.
2. **Button model**: adopt `ButtonType {Primary, Secondary, Ghost, Danger}` + orthogonal
   `ButtonAccent` axis + `onDisabledTap`; keep template color tokens; make the 3D-lip
   (`deep`) opt-in.
3. **`libraries/review` module**: `ReviewLauncher` (move template's existing
   `ReviewPrompter` in) + `ReviewPromptCoordinator` eligibility gate (install-age ≥3d,
   cooldown ≥30d); neutralize the `ReviewTrigger` enum values.
4. **Two-stage boot gate** (optional): async start-destination `StateFlow<Route?>`,
   `isReady` (releases platform splash via `setKeepOnScreenCondition`) vs
   `isBootComplete` (releases Compose `BootLoadingScreen`); `CyclingLoadingMessage`
   (generic — port regardless). Strip CardsFan visuals.
5. **Wiretap network inspector** (optional dev tool): `NetworkInspector` API +
   Wiretap impl + shake → QA/debug dialog wiring + the build.gradle variant logic
   (`{{APP}}_WIRETAP_IOS` env var — env not `-P` because two Gradle invocations must
   agree on the release framework; noop artifact for store builds).
6. **Splash fixes**: Android `installSplashScreen()` + keep-on-screen condition +
   dark theme values; iOS Compose `SplashOverlay` pattern (no launch storyboard) —
   port the mechanism with a neutral visual.

## Explicitly NOT ported (app-specific)

- `SingleWriterGuard` advisory lock + rolling-only deploys (documented as a pattern).
- Poker anything: game engine, room sockets/events, scenario harness content, deck
  scripting, chaos transports (pattern documented), bots, chips/wallet/economy outbox
  impl, achievements/progression/shop/billing.
- Cards ops: admin chips/message workflows, `rotate_admin_tokens`, sentry-triage
  prompt, dashboards/uids, `website/` (Astro marketing site; separate decision if the
  template ever wants a site scaffold — note the template's `template/ci/pages/`
  static pages already cover legal/landing basics).
- Warm-felt branding, CardsFan, poker UI components, `ReportPlayerSheet` (pattern only).

## Reverse-backports worth doing in Cards (separate follow-ups)

- Bring the template's `docker-compose.yml` + `docker/init-auth.sql` local stack into
  Cards.
- Consider the template's graceful limited-mode server boot for Cards.
