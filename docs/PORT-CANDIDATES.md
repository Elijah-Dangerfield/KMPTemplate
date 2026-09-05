# Port candidates

Work for this repo, found by the apps built from it. Two kinds: something a downstream app built and proved in production that a brand-new app would want, and a bug or rough edge in code this template already ships. The second kind matters more, because every generated app already has it.

See AGENTS.md → "This template is fed by the apps built from it" for how entries get here and what qualifies.

**This is a queue, not a changelog.** Delete an entry when you land it. Nothing here is a commitment — an item that still looks wrong after a closer look should be deleted with a line saying why, in the section at the bottom.

Paths are relative to the source repo named on the entry.

**Candidates 1–6 and 9–10 now have a sequenced implementation plan:** [docs/plans/port-from-cards-2026-09.md](plans/port-from-cards-2026-09.md). Delete an entry here as its phase there lands.

---

## From Cards (`~/Workspace/Cards`), captured 2026-09-05

### 2. A Baseline Profile module and the workflow that keeps it fresh

Cold start is the one cost every user pays on every launch, and a Baseline Profile is the single biggest lever on it. There is no reason for each new app to rebuild the module, the Gradle Managed Device config, and the generator split from scratch.

Two things here are earned rather than obvious. **The startup profile and the journey profile must come from different generators** — sharing one deep journey produces byte-identical files, and an oversized startup profile spills into later DEX files and slows the launch it exists to speed up. And **the generator runs a *release* variant**, so a benchmark hook gated on `BuildConfig.DEBUG` is dead exactly where it is needed; gate on the backend environment instead, which is also what stops a generation run creating accounts in production.

**Copy:** `apps/baselineprofile/` and `.github/workflows/baseline-profile.yml` (monthly, opens a PR rather than pushing, with sanity checks on rule count and per-package coverage).

### 6. Install facts as telemetry attributes

`genuine_install`, `is_emulator`, `is_sideloaded`, `is_rooted`, `installer_package`, `device_class`, `os_version`, stamped on every record.

Every app hits the same problem the first time it looks at its own dashboards: developer emulators and sideloaded debug builds are counted as users, and crash-free rate is measured against a population that includes the developer. Cards found **98 client-side 429s that were 100% sideloaded dev builds and zero retail** — a number that reads as an incident without this attribution and as noise with it.

**Copy:** `libraries/telemetry/impl/.../InstallFacts.kt`, `AndroidInstallFactsProvider.kt`, `IosInstallFactsProvider.kt`.

### 7. `rememberLoopingFloat` — infinite animations that don't hang capture

A small `:libraries:ui` primitive that returns a fixed value under `LocalInspectionMode`. Any looping animation left running under a preview or screenshot test means the harness waits for an idle state that never arrives, so the test hangs rather than failing — the worst kind of failure to diagnose. Cheap to include now, annoying to retrofit once a screenshot suite exists.

**Copy:** `libraries/ui/src/commonMain/.../components/LoopingAnimation.kt`.

---

## From Moving Eyes (`~/Workspace/MovingEyes`), captured 2026-09-05

Generated before the September 2026 update, so it inherited a template several
months old and hit its edges in order.

### 11. An opt-in switch for the Compose compiler's own metrics

`-Pmovingeyes.composeMetrics=true` writes the stability and skippability
reports to `build/compose-reports`. Off by default, because it slows every
compile and the output is only useful while someone is reading it.

The reason it earns a place in the template rather than in each app: "is this
composable skippable" has no answer from reading the source. A
restartable-but-not-skippable composable recomposes whenever its parent does,
however unchanged its arguments, and nothing about the code says so. Without
the report the honest answer to "is our Compose performance good" is a shrug,
and with it Moving Eyes could say **341 restartable composables, 0
non-skippable** and move on.

**Copy:** `build-logic/src/main/java/.../util/ComposeMetrics.kt` plus its call
in `ComposeMultiplatformConventionPlugin` and the `compose-compiler-gradle-plugin`
entry in the version catalog.

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

### 12. `moduleConfig.serialization()` adds a dependency commonMain cannot see

`ConfigurationExtension.serialization()` does
`project.dependencies { add("implementation", ...) }`. In a Kotlin Multiplatform
module `implementation` is the Android/JVM configuration, so `commonMain` never
sees it — a module that calls the helper and uses `@Serializable` in `commonMain`
**compiles on Android and fails to link on iOS**. Since everyone builds Android
far more often, that surfaces at a milestone rather than at the edit that caused
it, and the helper's name sends you looking at the iOS target instead of at the
source set.

Two neighbours compound it: `compose()` and `networking()` in the same class are
**empty**, so a module can call them, look configured, and have nothing happen.

Moving Eyes deleted all three — the Compose compiler plugin already arrives via
`KotlinMultiplatformConventionPlugin`, and the serialization dependency belongs
in the module's own `commonMain` block where it is visible.

Four template modules call `serialization()` today, so landing this means an iOS
link of each to confirm. Worth doing; not worth doing blind.

**Reference:** `libraries/scene/build.gradle.kts` in Moving Eyes for the shape
after the change.

### 13. `Modifier.composed` is still all over `:libraries:ui`

`composed` is deprecated, allocates a fresh modifier on every composition, and
opts its chain out of skipping. It survives here in `fadingEdges` (now fixed),
`Pulsate`, `BounceClick`, `ScrollBar` and `Header` — all of them attached to
things that recompose with the screen behind them.

Most are mechanical to convert: a `@Composable fun Modifier.x()` where the body
needs composition, or a plain factory where it turns out it never did. Both
`fadingEdge` overloads needed `composed` only to read a theme colour that
`BlendMode.DstIn` then ignored, so they became plain factories.

### 14. Never conclude a custom detekt rule is clean from a clean run

Cards found rules registered, configured active, compiled into the jar, and
never dispatched on detekt `2.0.0-alpha.5`; `alpha.6` fixed it. That story is in
circulation, so here is the correction: **Moving Eyes checked and its rules
dispatched fine on `alpha.5`.** An unconditional-report probe returned an
identical 8078 findings on both versions.

So the version is not a universal cause, and there is at least one other worth
checking first — also seen in Moving Eyes: **the Gradle daemon caches detekt's
worker classloader**, so an edited rule keeps running its previous jar until
`./gradlew --stop`.

The method is the lesson, not the version. A silently-undispatched rule and a
working rule that finds nothing are identical from the build output, so prove
dispatch by making the rule report unconditionally, confirm the flood, then
revert.

---

## Considered and rejected

Kept so they don't get re-proposed.

- **Macrobenchmark `FrameTimingMetric` in CI.** The right tool for catching jank regressions, and it needs a real device: emulator frame timing on a shared CI runner varies more run-to-run than the regressions worth catching, so a threshold produces flaky red and gets disabled. Only worth it for a project with a device farm.
- **The Grafana dashboards themselves.** The queries encode a specific app's events. The *conventions* are portable and already live in the downstream app's observability wiki; the boards are not.
- **The observability routine and its skills.** Genuinely useful, and shaped entirely around one project's dashboards, alert ids and inbox. Revisit only if a second app wants the same thing, which is the point at which the generic shape becomes visible.
