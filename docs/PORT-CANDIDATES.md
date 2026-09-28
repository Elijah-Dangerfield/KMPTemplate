# Port candidates

Work for this repo, found by the apps built from it. Two kinds qualify: something a downstream app proved in production that a brand-new app would want, and a bug in code this template already ships. The second matters more, because every generated app already has it. See AGENTS.md → "This template is fed by the apps built from it" for the full rules.

This is a queue, not a changelog. Delete an entry when you land it. Empty is the correct state after a working session. If you are reading this and it is empty, there is nothing queued, not nothing to find.

Closed questions live in [decisions.md](decisions.md); traps that can only be avoided rather than fixed live in AGENTS.md → "Known landmines".

This file lives in the template only. A generated project does not carry a copy, so writing an entry means writing across repos on purpose.

## How to write one

One `##` section per candidate. The next person meets a symptom, not a cause, so the diagnosis is the valuable part. Worth writing even when the fix is one line. If you can fix it in the template yourself, do that and skip the entry.

- **What it is.** The change, in one paragraph. Name the files.
- **How it looks from the outside.** The symptom someone hits before they know the cause. This is what makes the entry findable.
- **Why it is hard to spot.** What makes the wrong diagnosis the natural one. Include the fix that looks obvious but is worse, if there is one.
- **Where it hooks in.** The existing pattern to extend, with file and line. Ports arrive shaped like the app they came from; say what the generic version looks like.
- **Provenance.** Which app, what date, and whether you verified the claims in this repo or are reporting them.

Entries are in priority order.

---

## `FloatingWindowHost` pins an entry for the life of the process

**What it is.** `FloatingWindowHost` is a copy of AndroidX's `DialogHost` that
predates the fix for this. It completes the navigator transition from
`onDispose`, so an entry pushed and popped inside a single frame never composes,
never disposes, and stays in `transitionsInProgress` forever. `NavController`
will not move an entry it believes is transitioning past `CREATED` and will not
clear its `ViewModelStore`, so that entry and everything it holds is pinned for
the life of the process. Port AndroidX's `LaunchedEffect` sweep, `awaitingDispose`
guard included.

**How it looks from the outside.** A slow memory climb with no obvious owner, and
a ViewModel that is never cleared for a dialog the user never saw.

**Why it is hard to spot.** The leak needs a push and pop inside one frame, which
is a programmatic navigation rather than anything a person does by hand, so it
does not appear in manual testing.

**Where it hooks in.**
`libraries/navigation/src/commonMain/.../floatingwindow/FloatingWindowHost.kt:38-39`.

**Provenance.** Sodogku, commit `83eff68`. Verified here by reading: the bare
`onDispose` is unchanged.

## Exported schemas from a project this template is not

**What it is.**
`libraries/storage/impl/schemas/com.dangerfield.goodtimes.libraries.storage.impl.db.AppDatabase/`
holds `2.json` and `3.json` beside the real `com.kmptemplate.…AppDatabase/`
directory. Room keys the schema directory on the fully-qualified database class,
so these are exports from a class that no longer exists, under a package this
repo does not use. Delete them.

**How it looks from the outside.** Two schema directories where there should be
one, and the dead one disagrees with the live one at the same version numbers:
its `3.json` carries `tasks`, `task_progress` and `task_results`, which the live
`3.json` does not have. Anyone reading schema history to work out what a
migration has to do can read the wrong file and get a confident wrong answer.

**Where it hooks in.** Delete the directory, then confirm
`./gradlew :libraries:storage:impl:kspDebugKotlinAndroid` still exports only into
the `com.kmptemplate` directory.

**Provenance.** Found in this repo on 2026-09-28 while fixing the destructive
migration above, which needed the schema history to pick a safe drop range. Not
reported by any downstream app. Same class as the entry below: a name from an
earlier lineage that the rename pass does not touch, so every generated app
inherits it.

## A source directory named for a project this template is not

**What it is.** `libraries/kmptemplate/src/androidMain/kotlin/com/kmptemplate/libraries/merizo/`
and its `iosMain` twin hold `AndroidAppLifecycle.kt` and `IosAppLifecycle.kt`,
both of which declare `package com.kmptemplate.libraries.kmptemplate`. The
directory disagrees with the package and carries a name from some earlier
lineage. Kotlin does not require the two to match, so it compiles and nothing
complains.

**How it looks from the outside.** Every generated app inherits a directory named
after a project that is not the template and not itself, because the rename pass
rewrites the template's name and leaves this one alone. Anyone navigating by path
does not find these files where the package says they are.

**Where it hooks in.** Move both files to match their package and delete the
directories. Nothing references the path: `grep` for `libraries.merizo` and
`libraries/merizo` returns zero hits across the repo.

**Provenance.** Found in this repo on 2026-09-28 while verifying the launch-crash
entry above. Not reported by any downstream app.

## The `VerifyStrings` detekt rule ships active and fully baselined

**What it is.** The template ships `VerifyStrings` on, then baselines every screen
that predates it. So nothing in the repo follows the rule, and a generated app
inherits a rule that is enabled and unenforced: the baseline is what it actually
obeys. Either honour it in the template's own screens and shrink the baseline to
nothing, or turn the rule off until someone is ready to.

**Why it is hard to spot.** A green detekt run and a rule that is not being
enforced look identical from the build output. Same shape as the stale detekt
worker classloader already in AGENTS.md → Known landmines.

**Provenance.** Sodogku, `docs/decisions.md`, which honours the rule from day one
with `OnboardingScreen` as the worked example.

## `AnimatedStateReadInComposition` only catches one of the two spellings

**What it is.** The template's rule matches `by animate*AsState(...)` read in a
composable body. It misses the second and more common spelling:
`val progress = remember { Animatable(0f) }` followed by a `progress.value` read
outside a draw, layout, effect or derivation lambda. Sodogku widened the rule to
track composable locals built from animations and report those reads; on first
run it found four live sites in that app.

**Where it hooks in.**
`detekt-rules/.../AnimatedStateReadInComposition.kt`. Diff it against Sodogku's
copy rather than rewriting. Expect the widened rule to find sites in this repo
too, and fix them rather than baselining them, given the entry above.

**Provenance.** Sodogku, commit `31660bf`.

## Three leftover assets that are wrong in different ways

**What it is.** Small, independent, one commit between them.

- `apps/compose/src/androidMain/res/values/ic_launcher_background.xml` declares
  `<color name="ic_launcher_background">` while
  `res/drawable-v24/ic_launcher_foreground.xml` is a dead green-robot drawable.
  Two different resources share one name; the adaptive icon asks for the
  drawable, so the colour is never read. Change an adaptive XML to
  `@color/ic_launcher_background` and the icon silently goes dark grey.
- `apps/ios/iosApp/Info.plist:27` declares `NSUserNotificationsUsageDescription`
  as "We'd like to send you notifications." in an app that posts none. Harmless
  while unused, a reviewer question the moment a notification is requested and
  the string is still that. Same shape as the camera purpose string this repo
  already removed.
- The adaptive icon has no `<monochrome>` layer, so Android 13+ themed icons fall
  back to the full-colour icon. Not broken, just not themed.

**Provenance.** Sodogku, `docs/store/icons.md`, under a heading called "template
leftovers that are still in the tree". All three verified here by reading.

---

The rest are things a downstream app built that a brand-new app would want. They
are larger than the entries above and none of them is urgent.

## A composition test tier: Robolectric under `androidUnitTest`, run offline

**What it is.** The ability to assert against a real composition, which this repo
has never had. Deliberately *not* a `jvm()` target, which is the
Compose-Multiplatform-idiomatic answer, because that cascades `actual` stubs
through six library modules plus a third value on a two-case `Platform` enum that
several exhaustive `when`s read. Robolectric's framework jar is resolved through
Gradle and staged so tests run offline; left to itself it fetches ~100MB into
`~/.m2` at test time, and a network call inside a test is how a tier earns a
reputation for flaking. Runs under the `testDebugUnitTest` sweep CI already
executes, with no workflow change.

**Provenance.** Sodogku, commits `83eff68` and `854b727`.

## Grafana dashboards in the repo, held to the code by a test

**What it is.** Dashboards committed as JSON under `ops/grafana/`, plus a test
that parses every committed query and every `logEvent(...)` in the source tree
and fails when a dashboard names an event or attribute nothing emits.

The argument is the port-worthy part: a panel that filters on `strikes_used`
against an app that emits `strikes` is not an error anywhere. Loki accepts the
query, the panel renders, and it renders empty, which is exactly what a healthy
panel looks like before launch. The LogQL reader deliberately throws on
constructs it does not understand rather than shrugging, because a looser regex
reader is how a check like this passes while proving nothing.

This repo has a documented shared Grafana stack and no committed dashboards.

**Provenance.** Sodogku, `docs/decisions.md`.

## A config registry drift guard, and the Gradle wiring that makes it real

**What it is.** `apps/admin/config-manifest-registry.json` is a hand-written
transcription of the `ConfiguredValue` classes and nothing checks it. The test
belongs in `:apps:integration` rather than `:libraries:config`, because only an
`:apps:*` module can see both halves of the declared key set.

The non-obvious half is the Gradle wiring, and it generalises to any repo with a
test that reads a file: **a file read at test runtime is invisible to the
up-to-date check.** Editing the registry alone left the test task `UP-TO-DATE`
and the drift shipped. Observed rather than theorised, on a run that reported
BUILD SUCCESSFUL against a registry with a key deleted and three values wrong.
`inputs.file(registry)` on the `Test` tasks is what makes it real.

**Provenance.** Sodogku, `docs/decisions.md`.

## An agent-facing device driver, and the `monkey` trap in writing one

**What it is.** A `scripts/dev/drive.py` that launches, screenshots and drives the
app on a device, which this repo has no equivalent of.

Port the gotcha with it, because it is the reason not to write the tool the
obvious way. `monkey -p <pkg> -c LAUNCHER 1` is the usual launch incantation, and
monkey's trailing count is the number of *random events* it injects after
starting the app. So every launch fired a stray tap into the first frame: twice
it closed a launch-gate banner while an agent was verifying that banner, wrote a
persisted dismissal, and made the feature look broken. Measured at roughly 6%
misfire over 16 clean-install runs. `am start -W -n <pkg>/<activity>` is the
replacement.

Tooling that fails outright gets fixed. Tooling that acts on the app occasionally
makes every screenshot after it one interaction ahead of where you think you are,
and the app takes the blame.

**Provenance.** Sodogku, `docs/decisions.md`.
