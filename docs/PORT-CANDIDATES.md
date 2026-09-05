# Port candidates

Things a downstream app built and proved in production that this template should probably have. See AGENTS.md → "This template is fed by the apps built from it" for how entries get here and what qualifies.

**This is a queue, not a changelog.** Delete an entry when you land it. Nothing here is a commitment — an item that still looks wrong after a closer look should be deleted with a line saying why, in the section at the bottom.

Paths are relative to the source repo named on the entry.

---

## From Cards (`~/Workspace/Cards`), captured 2026-09-05

### 1. R8 is off in this template, and the keep rules are non-obvious

`ApplicationConventionPlugin` sets `isMinifyEnabled = false`, so every app generated from here ships unminified until someone notices. Play flags apps under 25% obfuscation with a **February 2027 deadline**, so this is a dated obligation for every downstream app, not a nice-to-have. Shrinking and startup gains come with it.

The reason this belongs in a template rather than in each app: R8 breaks what is resolved **by name at runtime**, and in a KMP app of this shape that is always the same three things — `kotlinx.serialization` models (the generated `Companion.serializer()` looks dead to R8), type-safe navigation routes (renaming them breaks nav with an argument error rather than a missing-class error), and the DI entry point. Every app built from here will need the identical rules and will lose the same day rediscovering them.

**Copy:** `apps/compose/proguard-rules.pro`, and the `release { }` block in `build-logic/.../ApplicationConventionPlugin.kt`.

**Bring the comments.** The rules are unremarkable; the explanations of what breaks and how it presents are the valuable part. Note also `-keepnames class * extends java.lang.Throwable`, without which every crash report arrives titled `a.b.c`.

**Caveat:** rules for third-party libraries only apply if the template has those libraries. Take the serialization / nav / DI / Throwable core; drop anything app-specific.

### 2. A Baseline Profile module and the workflow that keeps it fresh

Cold start is the one cost every user pays on every launch, and a Baseline Profile is the single biggest lever on it. There is no reason for each new app to rebuild the module, the Gradle Managed Device config, and the generator split from scratch.

Two things here are earned rather than obvious. **The startup profile and the journey profile must come from different generators** — sharing one deep journey produces byte-identical files, and an oversized startup profile spills into later DEX files and slows the launch it exists to speed up. And **the generator runs a *release* variant**, so a benchmark hook gated on `BuildConfig.DEBUG` is dead exactly where it is needed; gate on the backend environment instead, which is also what stops a generation run creating accounts in production.

**Copy:** `apps/baselineprofile/` and `.github/workflows/baseline-profile.yml` (monthly, opens a PR rather than pushing, with sanity checks on rule count and per-package coverage).

### 3. The `AnimatedStateReadInComposition` detekt rule

This template already has a `detekt-rules` module with one rule in it, so this is a drop-in.

Reading an animated value during composition (`val x by animateFloatAsState(...)` in a composable body) recomposes the whole subtree every frame. It is the most common Compose performance bug there is, it is invisible in review, and when the subtree contains text it thrashes Skia's glyph cache and can wedge the RenderThread into an ANR. **The rule found 19 instances on its first run in Cards, seven of them in files that had just been swept by hand for exactly this pattern.** A human reviewer does not reliably catch it; a linter does, on every PR, for free.

**Copy:** `detekt-rules/src/main/kotlin/.../AnimatedStateReadInComposition.kt` plus its registration in the rule-set provider.

**Gotcha worth carrying:** the rule silently did not dispatch on detekt `2.0.0-alpha.5` and started working on `alpha.6`, with no change to the rule itself. If a new rule appears to do nothing, suspect the detekt version before the rule.

### 4. Cold-start timing (`app.startup`)

Measures OS process creation → first usable frame, reported once per process.

Measuring from process creation rather than from the first line of Kotlin is the whole point: process fork, DEX loading and Application init all happen before any app code can start a timer, and that is precisely the part a Baseline Profile improves. A timer started later reports "no change" after the change that mattered most. Pairs with candidate 2 — without this, a template ships profile generation and no way to tell whether it helped.

Also calls `reportFullyDrawn()` at the same instant, which is what Play Console grades "fully drawn" startup on. Without it Play measures to the splash frame, a number no user experiences.

**Copy:** `libraries/telemetry/impl/.../StartupReporter.kt`, `AndroidProcessStartTimeProvider.kt`, `IosProcessStartTimeProvider.kt`, and the `reportStartupWhenReady()` hook in `MainActivity`.

**Carry the two exclusions**, both of which are load-bearing: drop "startups" over 30s (the system started the process in the background hours before anyone opened the app) and report only once per process (an Activity recreation draws a fresh first frame that is not a startup). Without them the percentiles are meaningless.

**iOS reports nothing, deliberately.** There is no readable process-start clock without `sysctl(KERN_PROC)`, which Kotlin/Native does not expose for Apple targets and which is a required-reason API. The right iOS source is MetricKit's `MXAppLaunchMetric`, under its own event name. The `IosProcessStartTimeProvider` KDoc explains this so nobody re-attempts it.

### 5. Real-user frame timing (`app.jank`)

One event per screen visit from AndroidX JankStats: frames rendered, how many missed their deadline, worst single frame. Answers "is the app smooth, and which screen isn't" continuously and for everyone, rather than from the one user who bothered to report it.

Aggregated per screen visit, never per frame — a frame callback fires 60 times a second per user, and logging that would cost more than it tells you and swamp the pipe.

**Copy:** `libraries/telemetry/impl/.../JankMonitor.kt`, `JankTally.kt`, `androidMain/AndroidJankMonitor.kt`, `iosMain/IosJankMonitor.kt` (a deliberate no-op — iOS has no equivalent API, and a lookalike number that meant something different per platform would be worse than an absence).

### 6. Install facts as telemetry attributes

`genuine_install`, `is_emulator`, `is_sideloaded`, `is_rooted`, `installer_package`, `device_class`, `os_version`, stamped on every record.

Every app hits the same problem the first time it looks at its own dashboards: developer emulators and sideloaded debug builds are counted as users, and crash-free rate is measured against a population that includes the developer. Cards found **98 client-side 429s that were 100% sideloaded dev builds and zero retail** — a number that reads as an incident without this attribution and as noise with it.

**Copy:** `libraries/telemetry/impl/.../InstallFacts.kt`, `AndroidInstallFactsProvider.kt`, `IosInstallFactsProvider.kt`.

### 7. `rememberLoopingFloat` — infinite animations that don't hang capture

A small `:libraries:ui` primitive that returns a fixed value under `LocalInspectionMode`. Any looping animation left running under a preview or screenshot test means the harness waits for an idle state that never arrives, so the test hangs rather than failing — the worst kind of failure to diagnose. Cheap to include now, annoying to retrofit once a screenshot suite exists.

**Copy:** `libraries/ui/src/commonMain/.../components/LoopingAnimation.kt`.

### 8. Sentry ProGuard mapping upload

Only matters once candidate 1 lands, and it matters immediately then: obfuscated crash reports are unreadable. Uses the Sentry Gradle plugin with `autoInstallation` **off**, because the default adds `sentry-android` on top of the KMP Sentry SDK the template already uses.

**Copy:** the Sentry plugin block in `apps/compose/build.gradle.kts`.

**Warning worth carrying:** Cards had a hand-rolled `upload-proguard` CI step that looked healthy for months and could never have worked — it named a manifest path AGP no longer writes, and there was no ProGuard UUID to match, so it associated with nothing and reported success. Verify by confirming `assets/sentry-debug-meta.properties` is in the built APK, not by trusting a green step.

---

## Lessons for code this template already has

No new module — these are fixes or comments for what is already here.

### 9. OTel trace-root poisoning on a long-lived WebSocket

This template already has `withSpan` and a Ktor telemetry plugin, so it is very likely to have this bug already.

`withSpan` parents to the *current* OTel context, and two things poison it: the Ktor telemetry plugin's WebSocket-upgrade span stays current for the life of the socket, and any shared `Dispatchers.Default` scope leaves a context on a pool thread that the next unrelated unit of work picks up. In Cards this produced **one trace id covering six rooms, several users and hours of wall time**, permanently stuck at "root span not yet received" — effectively one trace, so trace-level debugging was impossible.

The fix is to root a new trace at the originator of each unit of work and let everything below nest. Be exhaustive about what counts as an originator: rooting only the obvious handlers left the trace unbounded, just smaller.

**Reference:** commit `ac58b1ba` in Cards.

### 10. If you add release-please, the version markers are not optional

Cards listed `versions.properties` and `Config.xcconfig` in `extra-files` but neither carried `x-release-please` markers, so release-please updated neither **and said nothing about it**. A release cut a tag and a changelog while both files stayed a version behind, and the binaries that reached App Store Connect and the Play production track were the new code labelled with the old version.

Use the block form, not a trailing comment: in a properties file `#` only starts a comment at the start of a line, so a trailing `# x-release-please-version` becomes part of the value.

**Reference:** commit `7f91f507` in Cards.

---

## Considered and rejected

Kept so they don't get re-proposed.

- **Macrobenchmark `FrameTimingMetric` in CI.** The right tool for catching jank regressions, and it needs a real device: emulator frame timing on a shared CI runner varies more run-to-run than the regressions worth catching, so a threshold produces flaky red and gets disabled. Only worth it for a project with a device farm.
- **The Grafana dashboards themselves.** The queries encode a specific app's events. The *conventions* are portable and already live in the downstream app's observability wiki; the boards are not.
- **The observability routine and its skills.** Genuinely useful, and shaped entirely around one project's dashboards, alert ids and inbox. Revisit only if a second app wants the same thing, which is the point at which the generic shape becomes visible.
