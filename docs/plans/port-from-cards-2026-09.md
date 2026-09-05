# Plan: port the Cards production hardening into the template

**Source repo:** `~/Workspace/Cards` (github.com/Elijah-Dangerfield/Cards) — generated from this template, live on Play and the App Store. Every item below was built and proven there first.

**Status:** written 2026-09-05, executed 2026-09-05. **All phases landed**, one commit each. Phase 7 was deferred on a false premise (this repo does have a release pipeline, staged in `template/ci/`), and its marker half turned out to be a live bug; that is now fixed. The dynamic release-PR probe is deliberately not built — see Phase 7.

Three things did not match the plan's assumptions, recorded here because the plan was the thing that was wrong:

- **Phase 0's premise was false.** `VerifyStrings` was never inert — a deliberate violation fails the build on `alpha.5` as well as `alpha.6`, checked both ways. There was no backlog. The bump still landed, as a prerequisite for Phase 4's rule, which genuinely does not dispatch on `alpha.5`.
- **Phase 1 uncovered a pre-existing boot crash.** The app did not launch at all, on any build type, because Room rejected an unrequired `@ProvidedTypeConverter`. R8 changed the symptom (it merged the converter into an unrelated class) but not the cause. Fixed separately, ahead of the R8 commit.
- **Phase 1's acceptance is only partly met.** Navigation and serializer round-trips were exercised on a minified build; a completed network round trip was not, because this template has no live backend to reach — the configured Supabase host is NXDOMAIN and `NetworkConfig.baseUrl` defaults to empty. Re-check the first time a backend is wired up.

Read [AGENTS.md](../../AGENTS.md) → "This template is fed by the apps built from it" for why this exists, and [PORT-CANDIDATES.md](../PORT-CANDIDATES.md) for the wider queue this plan is drawn from. Delete an entry from that queue as its phase here lands.

## What this is

Seven phases. Phases 0–3 are the ones that matter most: one fixes a bug that is almost certainly live in this repo right now, and three close gaps that every app generated from here inherits.

Do them in order — the dependencies are real, not stylistic. Each phase is independently shippable; land them as separate commits, not one.

**Generalize on the way in.** These arrive shaped like a poker app. Strip the domain, rename for what a thing does rather than what it did, and **keep the comments explaining why a rule exists** — that is what stops the next person deleting it. Where Cards uses `com.dangerfield.cards`, this repo uses `com.kmptemplate`.

---

## Phase 0 — Fix detekt, because your custom rules are probably not running

**Do this first.** It is the smallest change here and it may invalidate an assumption the repo is built on.

This template pins `detekt = "2.0.0-alpha.5"` in `gradle/libs.versions.toml`. On that exact version, **custom rules silently fail to dispatch** — the build passes, detekt reports success, and your rule never executes. Cards lost time to this: its `AnimatedStateReadInComposition` rule appeared completely broken and was rewritten repeatedly, after provider ordering, config cache, jar freshness, YAML and baselines had all been ruled out. The fix turned out to be the version alone. Bumping to `alpha.6` made it dispatch with no change to the rule itself, and it immediately found 19 real violations.

So the live question for this repo: **is `VerifyStrings` actually running, or has it been quietly inert?**

**Do:**
1. Bump `detekt` to `2.0.0-alpha.6` (or later, if newer is stable).
2. Prove `VerifyStrings` dispatches — deliberately introduce a violation it should catch, run detekt, confirm it fails, then revert.
3. Fix whatever it finds. If it was inert, there may be a backlog of real violations.

**Acceptance:** a deliberately-introduced violation fails the build. Not "detekt runs clean" — that is the symptom being investigated, and a clean run is exactly what a broken rule looks like.

**Reference:** ENG-54 in `~/Workspace/Cards/docs/todo.md`.

---

## Phase 1 — Turn on R8, with the keep rules that make it survive

**The highest-value phase, and the only one with a deadline.** `build-logic/src/main/java/com/kmptemplate/plugin/ApplicationConventionPlugin.kt` sets `isMinifyEnabled = false`, so every app generated from this template ships unminified. Play flags apps under 25% obfuscation with a **February 2027 deadline**, so this is a dated obligation inherited by every downstream project. Shrinking and startup gains come free with it.

The keep rules are the actual deliverable. R8 breaks what is resolved **by name at runtime**, and in a KMP app of this shape that is always the same three things:

- **`kotlinx.serialization` models.** Serializers resolve through a generated `Companion.serializer()` that nothing calls directly, so R8 sees them as dead. Losing one does not fail the build — it throws the first time that model crosses the wire.
- **Type-safe navigation routes.** These are `@Serializable` classes resolved by type. Renaming them breaks navigation with an *argument* error rather than a missing-class error, which is the hardest failure here to attribute.
- **The DI entry point.** Generated at compile time, referenced through `::class.create`.

Plus `-keepnames class * extends java.lang.Throwable`. Without it every crash report and ANR arrives titled `a.b.c` and has to be un-mangled before it can even be triaged. Throwable names are a rounding error in APK size.

**Do:**
1. Copy `apps/compose/proguard-rules.pro` from Cards, **with its comments**. Drop rules for libraries this template does not have; keep the serialization / nav / DI / Throwable core.
2. Set `isMinifyEnabled = true` and `isShrinkResources = true` in the release block of `ApplicationConventionPlugin`, with `proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")`.
3. Check for a `-dontwarn` needed by any debug-only dependency that is swapped for a no-op in release.

**Acceptance:** a release build installs and reaches the main screen on a device or emulator, with navigation and at least one network round trip exercised. **A release build that merely compiles proves nothing** — every failure mode here is at runtime. Phase 3 automates this check; until then, do it by hand.

**Note:** R8 will log "An error occurred when parsing kotlin metadata" repeatedly. That is an AGP/Kotlin version skew in R8's metadata parser, not a correctness problem, though it can reduce obfuscation quality — re-check Play's reported percentage after the first minified release.

---

## Phase 2 — Let Sentry deobfuscate (depends on Phase 1)

Pointless before Phase 1 and mandatory immediately after: minified crash reports are unreadable without a mapping file.

**Do:**
1. Add the Sentry Android Gradle plugin. Copy the `sentry { }` block from Cards' `apps/compose/build.gradle.kts`.
2. **Set `autoInstallation { enabled.set(false) }`.** The plugin's default adds `sentry-android` on top of the KMP Sentry SDK this template already uses, and two SDKs initialising in one process is its own problem.
3. Make mapping upload conditional on `SENTRY_AUTH_TOKEN` being present, so a contributor without one can still build a release.
4. **If any hand-rolled mapping upload exists in a workflow, delete it.** Cards had a `sentry-cli upload-proguard` step that looked healthy for months and could never have worked: it named a manifest path AGP no longer writes, and the app had no ProGuard UUID to match, so it associated with nothing and reported success. It was dormant behind a minification guard, which is why nobody noticed. This template has no release workflow yet, so there is likely nothing to remove — check anyway.

**Acceptance:** unzip the release APK and confirm `assets/sentry-debug-meta.properties` exists and contains `io.sentry.ProguardUuids`. That file is what makes a mapping associable. **Do not accept a green upload step as proof** — that is exactly how the broken one looked for months. Full confirmation needs frames read on a real issue from a shipped minified build.

---

## Phase 3 — Baseline profiles (depends on Phase 1)

Cold start is the one cost every user pays on every launch, and a Baseline Profile is the biggest single lever on it. No app generated from here should have to rebuild the module, the Gradle Managed Device config and the generator split from scratch.

**Copy from Cards:** `apps/baselineprofile/` (four files) and `.github/workflows/baseline-profile.yml`.

Three things here are earned rather than obvious, and all three should survive the port:

- **Two generators, not one.** `StartupProfileGenerator` sets `includeInStartupProfile = true` over a shallow launch path; `JourneyProfileGenerator` sets it false over a deep journey. Cards first used one generator for both and produced **byte-identical files** (verified by matching MD5). An oversized startup profile spills into later DEX files and slows the very launch it exists to speed up.
- **Generation runs a *release* variant.** So a benchmark hook gated on `BuildConfig.DEBUG` is dead exactly where it is needed. Gate on the backend environment instead — which is also what stops a generation run creating real accounts in production, a mistake Cards made once.
- **Nothing network-bound on the launch path.** An earlier Cards version awaited sign-in inside `onCreate`, which put a network round trip on the main thread: the activity never finished launching and the benchmark failed with a blank screen. Split the local cache write (blocking, short timeout) from anything remote (fire-and-forget).

`MinifiedReleaseSmokeTest` is worth the port on its own: `benchmarkRelease` **is** minified, so the journey doubles as the runtime R8 check Phase 1 needs. Note it is a plain `AndroidJUnit4` test on purpose — `BaselineProfileRule` *skips* on minified variants, and a skip reads like a pass.

The workflow opens a PR rather than pushing, with sanity checks on rule count and per-package coverage, because a profile that silently lost half its rules means the journey broke while every assertion still passed.

**Generalize:** Cards' `BenchmarkJourney` taps through onboarding to a poker table. Replace with this template's own shortest meaningful path. Keep the adaptive structure (it walks whatever is on screen rather than a fixed tap sequence) and keep `describeScreen()` — a failure that only says "the thing I wanted was not there" costs a full emulator run per guess.

**Acceptance:** `./gradlew :apps:compose:generateBaselineProfile` produces two files of visibly different sizes, both non-empty, and the smoke test **runs** rather than skipping — check the XML for `RAN`, not just a green build.

---

## Phase 4 — The `AnimatedStateReadInComposition` detekt rule (depends on Phase 0)

Drop-in: this repo already has a `detekt-rules` module.

Reading an animated value during composition (`val x by animateFloatAsState(...)` in a composable body) recomposes the whole subtree every frame. It is the most common Compose performance bug, it is invisible in review, and when the subtree contains text it thrashes Skia's glyph cache and can wedge the RenderThread into an ANR — Cards traced four production ANRs to exactly this.

The rule found **19 instances on its first run, seven of them in files that had just been swept by hand for this precise pattern.** A human reviewer does not reliably catch it. A linter does, on every PR, for free.

**Copy:** `detekt-rules/src/main/kotlin/.../AnimatedStateReadInComposition.kt` plus its registration in the rule-set provider and its config entry.

**Acceptance:** it fires on a deliberate violation and the repo is clean of real ones. Expect to fix what it finds — the fix is reading the value inside `graphicsLayer` / `drawBehind` / `Modifier.layout` so the animation runs in the draw phase instead.

**Known good suppression:** `Modifier.shadow` has no lambda form, so a shadow driven by an animated value has no phase-deferred equivalent. Suppress with a reason rather than contorting the code.

---

## Phase 5 — Real-user frame timing (`app.jank`)

This template has `libraries/telemetry` but no frame data. One event per screen visit from AndroidX JankStats: frames rendered, how many missed their deadline, worst single frame.

The gap it closes: crash and exit telemetry tells you a session died and nothing about what was slow in the minutes before. Cards diagnosed four production ANRs by asking a user to capture a Perfetto trace on their own device, which does not scale and only works for a bug someone already reported.

**Copy:** `libraries/telemetry/impl/.../JankMonitor.kt`, `JankTally.kt`, `androidMain/AndroidJankMonitor.kt`, `iosMain/IosJankMonitor.kt`.

**Keep two design decisions:**
- **Aggregated per screen visit, never per frame.** A frame callback fires 60 times a second per user; logging that would cost more than it tells you and swamp the pipe.
- **The iOS binding is a deliberate no-op.** There is no equivalent frame-timing API, and a lookalike number meaning something different per platform is worse than an obvious absence. The KDoc says so, so nobody "fixes" it later.

JankStats needs a `Window`, so arm it in `onCreate` *before* `setContent` — attaching after misses the first frames, which are the ones most likely to be janky. Flush on background so a session that never returns still reports its last screen.

**Acceptance:** `JankTally` unit tests pass and a debug run emits one event per screen visit with plausible counts.

---

## Phase 6 — Cold-start timing (`app.startup`)

Pairs with Phase 3. Without it, this template ships profile generation and no way to tell whether it helped.

Measures OS process creation → first usable frame, once per process. **Measuring from process creation is the whole point**: fork, DEX loading and Application init all happen before any app code could start a timer, and that is precisely the part a Baseline Profile improves. A timer started later reports "no change" after the change that mattered most.

**Copy:** `libraries/telemetry/impl/.../StartupReporter.kt`, `AndroidProcessStartTimeProvider.kt`, `IosProcessStartTimeProvider.kt`, and the `reportStartupWhenReady()` hook in `MainActivity`.

**Carry all of this — each part is load-bearing:**
- **Drop "startups" over 30s.** That is the system having started the process in the background hours before anyone opened the app. Real elapsed time, not a launch, and it drags every percentile somewhere meaningless.
- **Report once per process.** An Activity recreation (rotation, theme change) draws a fresh first frame that is not a startup.
- **Close the measurement one frame *after* ready**, via `onPreDraw` + `post`. At the moment the splash condition lets go, the frame behind it has not been drawn, and that first real composition is the slowest frame of the launch.
- **Call `reportFullyDrawn()` at the same instant.** Play Console grades "fully drawn" startup on that call; without it Play measures to the splash frame, a number no user experiences.

**iOS reports nothing, deliberately.** There is no readable process-start clock short of `sysctl(KERN_PROC)`, which Kotlin/Native does not expose for Apple targets and which is a required-reason API needing a privacy-manifest declaration. The right iOS source is MetricKit's `MXAppLaunchMetric`, under its own event name since it is a daily histogram rather than one launch. The `IosProcessStartTimeProvider` KDoc explains this so nobody re-attempts the sysctl route — Cards did, and it does not compile.

**Acceptance:** six `StartupReporter` unit tests pass (they cover the exclusions, which are the whole logic) and a debug launch emits one plausible `startup_ms`.

---

## Phase 7 — Release PR context (still open; the reason for deferring it was wrong)

**Deferred on the grounds that there was nowhere for it to land. That is not true.** The template repo's *own* CI is only `template-ci.yml`, which is what the paragraph below was written from — but generated projects get a full release pipeline staged in `template/ci/`: `release.yml`, `beta.yml`, `release-please.yml`, `retag-release.yml` and `release-please-config.json`. Everything in this phase has somewhere to land, one directory over.

Worse, the second lesson below is **already a live bug there**. `template/ci/release-please-config.json` lists `versions.properties` and `apps/ios/Configuration/Config.xcconfig` in `extra-files`, and neither file carries an `x-release-please` marker. That is the exact configuration that cut a tag and a changelog downstream while both version files stayed a release behind, shipping the new code labelled with the old version to the Play production track and App Store Connect. Every project generated from this template inherits it today.

The first lesson is also directly applicable: that config's `pull-request-header` is a fixed string asserting four things about every release, which is the "asserts rather than asks" shape the note below describes.

**Landed 2026-09-05.** Both files now carry `x-release-please-start-version` / `x-release-please-end` markers, and `verify_template.sh` asserts every `extra-files` entry exists and is marked — proven to fail on a stripped marker, since a silent skip is invisible to every other check in the pipeline. `pull-request-header` no longer asserts store state it cannot see; it says so and asks the reader to check the cases where merging is wrong.

**Still not built, on purpose:** the dynamic probe. Cards' `release_pr_context.py` queries Play and App Store Connect on every PR update — is there a production release at all, is a staged rollout still running, is a version already `WAITING_FOR_REVIEW`. That needs credentials and a script this repo does not have, and the checklist covers the same cases at the cost of a human reading it. Build it when someone has actually merged over a build in review.

When this template does grow a release pipeline, two Cards lessons should come with it:

**A release PR body should ask, not assert.** Cards' was a fixed string in `release-please-config.json` claiming the same four things every release. It once read "submitted to App Store review" while a build was *already sitting in review* — precisely the case where merging is the wrong move, and the body said nothing about it. `.github/scripts/release_pr_context.py` (523 lines) now queries Play and App Store Connect on every update: is there a production release at all, is a staged rollout still in progress, is a version already `WAITING_FOR_REVIEW` or `IN_REVIEW`. Related: make a probe report *what it found*, not a count — "matched 2 apps" was wrong twice over and gave nobody anything to act on.

**If you use release-please, `extra-files` needs markers.** Files listed there without `x-release-please` markers are skipped **silently**. Cards cut a tag and a changelog while its version files stayed a release behind, and the binaries that reached App Store Connect and the Play production track were the new code labelled with the old version. Use the block form: in a properties file `#` only starts a comment at the start of a line, so a trailing `# x-release-please-version` becomes part of the value.

---

## Sequencing summary

| Phase | Depends on | Rough size |
|---|---|---|
| 0 · Fix detekt dispatch | — | Tiny, do first |
| 1 · R8 + keep rules | — | Medium |
| 2 · Sentry mapping upload | 1 | Small |
| 3 · Baseline profiles | 1 | Large |
| 4 · Animated-state detekt rule | 0 | Small |
| 5 · JankStats | — | Medium |
| 6 · Cold-start timing | — (pairs with 3) | Medium |
| 7 · Release PR context | — (one already exists in `template/ci/`) | Markers fixed; probe not built |

Phases 0, 1, 4, 5 and 6 are independent of each other and can be done in any order once 0 is out of the way. Only 2 and 3 genuinely need 1 first.

Executed in the order 0, 1, 2, 4, 5, 6, 3 — the dependencies were respected and the two largest phases were left until last.
