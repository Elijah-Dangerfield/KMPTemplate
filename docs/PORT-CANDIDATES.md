# Port candidates

Things a downstream app built and proved in production that this template should probably have. See AGENTS.md → "This template is fed by the apps built from it" for how entries get here and what qualifies.

**This is a queue, not a changelog.** Delete an entry when you land it. Nothing here is a commitment — an item that still looks wrong after a closer look should be deleted with a line saying why, in the section at the bottom.

Paths are relative to the source repo named on the entry.

**Candidates 1–6 and 9–10 now have a sequenced implementation plan:** [docs/plans/port-from-cards-2026-09.md](plans/port-from-cards-2026-09.md). Delete an entry here as its phase there lands.

---

## From Cards (`~/Workspace/Cards`), captured 2026-09-05

### 2. A Baseline Profile module and the workflow that keeps it fresh

Cold start is the one cost every user pays on every launch, and a Baseline Profile is the single biggest lever on it. There is no reason for each new app to rebuild the module, the Gradle Managed Device config, and the generator split from scratch.

Two things here are earned rather than obvious. **The startup profile and the journey profile must come from different generators** — sharing one deep journey produces byte-identical files, and an oversized startup profile spills into later DEX files and slows the launch it exists to speed up. And **the generator runs a *release* variant**, so a benchmark hook gated on `BuildConfig.DEBUG` is dead exactly where it is needed; gate on the backend environment instead, which is also what stops a generation run creating accounts in production.

**Copy:** `apps/baselineprofile/` and `.github/workflows/baseline-profile.yml` (monthly, opens a PR rather than pushing, with sanity checks on rule count and per-package coverage).

### 4. Cold-start timing (`app.startup`)

Measures OS process creation → first usable frame, reported once per process.

Measuring from process creation rather than from the first line of Kotlin is the whole point: process fork, DEX loading and Application init all happen before any app code can start a timer, and that is precisely the part a Baseline Profile improves. A timer started later reports "no change" after the change that mattered most. Pairs with candidate 2 — without this, a template ships profile generation and no way to tell whether it helped.

Also calls `reportFullyDrawn()` at the same instant, which is what Play Console grades "fully drawn" startup on. Without it Play measures to the splash frame, a number no user experiences.

**Copy:** `libraries/telemetry/impl/.../StartupReporter.kt`, `AndroidProcessStartTimeProvider.kt`, `IosProcessStartTimeProvider.kt`, and the `reportStartupWhenReady()` hook in `MainActivity`.

**Carry the two exclusions**, both of which are load-bearing: drop "startups" over 30s (the system started the process in the background hours before anyone opened the app) and report only once per process (an Activity recreation draws a fresh first frame that is not a startup). Without them the percentiles are meaningless.

**iOS reports nothing, deliberately.** There is no readable process-start clock without `sysctl(KERN_PROC)`, which Kotlin/Native does not expose for Apple targets and which is a required-reason API. The right iOS source is MetricKit's `MXAppLaunchMetric`, under its own event name. The `IosProcessStartTimeProvider` KDoc explains this so nobody re-attempts it.

### 6. Install facts as telemetry attributes

`genuine_install`, `is_emulator`, `is_sideloaded`, `is_rooted`, `installer_package`, `device_class`, `os_version`, stamped on every record.

Every app hits the same problem the first time it looks at its own dashboards: developer emulators and sideloaded debug builds are counted as users, and crash-free rate is measured against a population that includes the developer. Cards found **98 client-side 429s that were 100% sideloaded dev builds and zero retail** — a number that reads as an incident without this attribution and as noise with it.

**Copy:** `libraries/telemetry/impl/.../InstallFacts.kt`, `AndroidInstallFactsProvider.kt`, `IosInstallFactsProvider.kt`.

### 7. `rememberLoopingFloat` — infinite animations that don't hang capture

A small `:libraries:ui` primitive that returns a fixed value under `LocalInspectionMode`. Any looping animation left running under a preview or screenshot test means the harness waits for an idle state that never arrives, so the test hangs rather than failing — the worst kind of failure to diagnose. Cheap to include now, annoying to retrofit once a screenshot suite exists.

**Copy:** `libraries/ui/src/commonMain/.../components/LoopingAnimation.kt`.

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
