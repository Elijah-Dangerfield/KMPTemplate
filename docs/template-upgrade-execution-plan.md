# Template upgrade execution plan (Cards backport)

Written 2026-07-23. This is the HOW that pairs with `docs/cards-backport-plan.md` (the
WHAT — phases 0–9 with exact source/destination file paths). An executing agent should
read both fully before touching anything, follow the milestone order and gates here, and
use the backport plan for file-level content.

Scope decision (owner-approved): **everything ships wired-up**, including the optional
extras — onboarding auth UI screens, guest self-heal cluster, two-stage boot gate,
Wiretap inspector (renamed env var), MetricKit exit reports, and the VM scenario-harness
example. Only the items the backport plan marks app-specific stay out (poker logic,
SingleWriterGuard, chips outbox impl, Cards ops workflows, the Astro website).

## Ordering principle

Build the verification harness FIRST (M0); every later milestone commits against it.
Work happens directly on `main` with incremental conventional commits per logical chunk;
the tree stays green between commits; each new-module milestone must pass the
generated-project smoke test before its closing commit. End every session with a clean
working tree.

## Milestones

| # | Milestone | Backport-plan phases | Size | Depends on |
|---|---|---|---|---|
| M0 | Verification harness + rot fixes | (new) | M | — |
| M1 | Correctness fixes | Phase 0 items 1–5 + merizo leftover test | S | M0 |
| M2 | Foundations (AutoInit, events, runWhen, retry, connectivity, nav, testing module) | Phase 1 | L | M1 |
| M3 | Auth overhaul (incl. profile layer replacing demo scaffolding, Apple sign-in, auth UI screens, guest self-heal cluster) | Phase 2 + Phase 0 item 6 | XL | M2, M4 (wire contract) |
| M4 | Server: moderation, admin client, ClientContext, WebSockets, two-env deploy | Phase 3 | M | M0 (parallelizable; run BEFORE M3 so the 403 AccessDenied wire envelope exists first) |
| M5 | Remote config end-to-end + admin console (`apps/admin`, Kotlin/JS) | Phase 4 + Phase 0 items 7–8 | L | M2, M4 |
| M6 | Telemetry (server correlation, client Sentry upgrades, `:libraries:telemetry` Grafana pipe, MetricKit exit reports, build-time secret injection) | Phase 5 + Phase 0 item 9 | L | M2 (client half); server half only needs M0 |
| M7 | Triggered sync (SyncTriggers, UserScopedSyncer, coordinator, NetworkCall wrappers, outbox doc) | Phase 6 | M | M2 + M3 |
| M8 | Integration harness (`apps/integration`, IntegrationAuth, smoke test, testing docs, VM scenario example) | Phase 8 | M | M4 |
| M9 | CI/CD staging upgrades (ci.yml with final module set, release-please, detekt + pre-push, Versioning overrides, beta/release + Fastfile, server-deploy dev/prod, gitignore security block) | Phase 7 | M | M5, M8 |
| M10 | Client shell polish (DS catalog, button model, review module, boot gate, Wiretap inspector, splash mechanism) | Phase 9 | M/L | M2 |
| M11 | Docs consolidation + final full gate | (docs) | S/M | all |

Suggested session split if needed: [M0–M2], [M4, M3, M5], [M6–M11]. Natural hand-off
points: after M2, after M3, after M6, after M9. Never hand off mid-M3.

Key sequencing rules:
- Do NOT delete the `User`/`Session` demo scaffolding in M1 — `features/home` consumes
  it; it dies in M3's profile-layer commit.
- M3 commit order: (a) sealed AuthState + outcomes + gateway seam + migrate home
  atomically, (b) orchestrator + token provider + buses, (c) secure session storage,
  (d) Apple sign-in, (e) profile layer + demo-scaffolding deletion, (f) guest self-heal
  cluster, (g) onboarding auth UI + AccessDenied/SessionExpired routing. Never land the
  new AuthState without migrating all consumers in the same commit.
- M9 runs last among code milestones because ci.yml/release.yml/settings includes/init
  allowlists reference the final module set.
- Preserve two template-is-better things: graceful limited-mode server boot (keep
  null-safe wiring when adding server pieces) and the `docker-compose.yml` local stack.

## M0 in detail (the centerpiece)

1. **Green baseline gate** (run before any commit; if anything is red, fixing it is
   commit #1):
   - `docker info` (testcontainers precondition)
   - `./gradlew :apps:compose:compileDebugKotlinAndroid :apps:compose:compileKotlinIosSimulatorArm64 :apps:compose:assembleDebug`
   - `./gradlew testDebugUnitTest`
   - `./gradlew :apps:server:test` (testcontainers — NOT covered by staged CI today)
   - `actionlint template/ci/.github/workflows/*.yml`
   - `xcodebuild -project apps/ios/*.xcodeproj -scheme <scheme> -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO`
2. **Non-interactive init**: add a CLI-flag mode to `scripts/init_project.main.kts`
   (`--name --package --email --dir --ci=yes|no --yes`; all-or-nothing — any flag
   present requires all; interactive remains the default). Flags, not env vars or piped
   stdin — piped stdin silently breaks when prompts change, which is exactly the drift
   this harness exists to catch.
3. **`scripts/verify_template.sh`**: mktemp dir → non-interactive init with CI enabled →
   static assertions on the generated project:
   - zero residual `kmptemplate|KMPTemplate|com.kmptemplate|kmp-template|kmp_template|kmp.template` strings
   - zero `cards|Downcard|warm-felt` strings (case-insensitive, with a small
     false-positive allowlist)
   - no unsubstituted `{{` placeholders
   - executable bits on gradlew/scripts/hooks; init + rename scripts deleted;
     `./scripts/install_hooks.sh` runs clean; exactly one git commit
   then build the generated project (`--fast`: Android compile + `testDebugUnitTest`;
   `--full` adds iOS Kotlin compile, `:apps:server:test`, and — once they exist —
   `:apps:admin:jsBrowserDistribution` and a `docker build` of the server) →
   `actionlint` its workflows → cleanup. The script must be deleted from generated
   projects by `cleanupTemplateArtifacts`.
4. **Template-repo CI** at root `.github/workflows/template-ci.yml` (requires teaching
   `copyTemplate` in the init script to skip a root `.github` dir, same pattern as the
   `template/` staging skip). Three jobs:
   - `jvm-tests` (ubuntu): `testDebugUnitTest` + `:apps:server:test` + actionlint on
     both workflow sets + the de-branding grep
   - `ios-compile` (macos): iOS Kotlin compile + xcodebuild no-codesign
   - `generated-smoke` (ubuntu): `scripts/verify_template.sh --fast` + generated-project
     server tests
   Triggers: every push + PR, no path filters (this repo IS the artifact).
5. **Rot fixes**: delete `libraries/kmptemplate/**/merizo/ReceiptTextParserTest.kt`;
   split `scripts/README.md` (template-only content moves to
   `docs/template-maintenance.md` so generated projects stop referencing the deleted
   init script); replace `maybeEnableCi`'s false "re-run later" promise with a real
   `scripts/enable_ci.sh` shipped into generated projects when CI is declined.
6. **`docs/template-maintenance.md`** (new): how init works, the manual-update checklist
   when adding modules/files (`settings.gradle.kts` includes, staged
   ci.yml/release.yml task lists, `ensureExecutableBits` paths,
   `TEXT_FILE_EXTENSIONS`/extensionless allowlist), how verify_template.sh and template
   CI work.

## Per-milestone gates

- Every commit: `:apps:compose:compileDebugKotlinAndroid` +
  `:apps:compose:compileKotlinIosSimulatorArm64` + `testDebugUnitTest`.
- Server-touching: add `:apps:server:test`.
- Swift-touching (`apps/ios/`): add the xcodebuild simulator build.
- New module / init-allowlist-touching: `scripts/verify_template.sh --fast` before the
  milestone's closing commit.
- CI-staging-touching: `actionlint` + `ruby -c` on the Fastfile.
- Milestone close: full baseline (minus xcodebuild unless Swift was touched).
- Final gate (M11): full baseline + `verify_template.sh --full` + clean `git status`.

## Not verifiable locally — de-risking

- Fastlane/ASC uploads: `ruby -c`; add a `validate` lane (everything up to `pilot`,
  `skip_codesigning`); SETUP.md first-release checklist with expected outputs.
- Fly deploys: actionlint; a `dry_run` workflow input running `fly deploy --build-only`;
  the in-workflow `/_health` smoke step is the post-deploy check.
- Grafana/Sentry ingestion: unit tests on exporter serialization; SETUP.md "day 1
  verification" (launch app → find your session_id in Loki; trigger a test crash → find
  it in Sentry with the trace link).
- release-please/prod environments: `jq` parse of the config in CI; SETUP.md documents
  the first-release walkthrough and GitHub Environment creation.

## Docs architecture (docs land in the milestone that ships the system)

- `README.md` — pitch, feature inventory, quickstart, link map (M11).
- `docs/template-maintenance.md` — maintainer doc (M0).
- `template/SETUP.md` — the generated project's "hour 1 / day 1" runbook, expanded
  across M3–M9: Supabase project + auth providers + `SUPABASE_SERVICE_ROLE_KEY`; Fly
  dev/prod apps + secrets; Sentry DSN + Grafana OTLP tokens (local.properties keys and
  CI secret names); `ADMIN_API_TOKEN` + admin console access; GitHub secrets tables;
  first server deploy; first TestFlight/Play build; first release; the manual
  verification checklist. Each step: command + expected output.
- `AGENTS.md` — new sections as systems land: AutoInit (M2); nav landmines — routes
  must be `class` not `data object`, enum args `@Serializable` + registered in the type
  map (M2); auth model + `UserScopedDataReset` seam (M3); `ConfiguredValue` convention
  (M5); telemetry conventions + session_id pivot + `logEvent` discipline (M6); syncer
  registration (M7); testing pointer (M8).
- `docs/practices/` — `testing.md` (M8), `outbox.md` (M7), `observability.md` +
  `app-events.md` (M6). These ship into generated projects.
- Module READMEs: rewrite `libraries/config/README.md` (kills the Virtu/`Experiment<T>`
  rot) and genericize `apps/admin/README.md` (M5).

## Risks

1. Swift wiring invisible to Gradle (M2/M3/M10) → xcodebuild gate per Swift-touching
   commit + the macOS CI job.
2. Init-allowlist drift on new modules (M2/M5/M6/M8) → smoke test enforces; maintenance
   doc is the checklist.
3. Auth red-tree window (M3) → atomic consumer migration per commit; no old/new
   coexistence across commits.
4. Kotlin/JS admin target (M5) → land compiling first, de-brand second, deploy wiring
   third; `jsBrowserDistribution` in the gate.
5. Incomplete de-branding → `cards|Downcard|warm-felt` grep in template CI + the smoke
   test.
6. Docker absent locally → `docker info` preflight with explicit skip messaging.

## Session hand-off protocol

If a session must stop, stop only at a hand-off point (after M2, M3, M6, or M9) with a
clean tree, and record in this file exactly which milestone/commit was last completed
and what is next.

## Status

- [x] M0  - [x] M1  - [x] M2  - [x] M3  - [x] M4  - [x] M5
- [x] M6  - [x] M7  - [x] M8  - [x] M9  - [x] M10 - [x] M11
