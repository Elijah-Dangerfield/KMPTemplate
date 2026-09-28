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

Entries are in priority order. The first four are live defects every generated app ships today.

---

## A launch crash every generated app ships: lifecycle registration off the main thread, with nothing to catch it

**What it is.** Two inherited defects that are only fatal together, so port them together.

`AndroidAppLifecycle.addObserver` calls `lifecycle.addObserver(this)` on whatever
thread the caller arrived on. `LifecycleRegistry` throws rather than synchronize,
so off the main thread that is an `IllegalStateException`. Nothing in the code
chooses the thread: callers reach it through the object graph, so registration
runs on whichever thread first touched a dependency that constructs an
`AppEventDispatcher`. Fix: post to main rather than running inline, keeping add
and remove in call order.

`AppCoroutineScope` is `SupervisorJob() + dispatcherProvider.default` with no
`CoroutineExceptionHandler`. A supervisor stops one failed child cancelling its
siblings and does nothing about an exception nobody catches; that goes to the
thread's default handler, which on Android kills the process. So every
`appScope.launch` anywhere in the app is one uncaught throw from a crash. Fix:
add a handler that does `KLog.e(throwable)`, which reaches Sentry through
`SentryLogTree`, and rethrows a `DebugException` only in debug builds, mirroring
the existing `Catching {}.logOnFailure().throwIfDebug()` shape.

**How it looks from the outside.** The app dies on launch, before any frame, on
some installs and not others. Downstream it was a fatal on a real release build
when a Play Games sign-in coroutine won the race on `Dispatchers.Default`.
Whether it crashes depends on which dependency the object graph happens to touch
first, so it reproduces on a device and not on the next one.

**Why it is hard to spot.** Each half looks fine alone. Thread-unsafe
registration usually produces a caught exception somewhere; a missing exception
handler usually produces a logged warning. The scope's `SupervisorJob` reads as
the error-handling decision, and it is the thing that makes people stop looking.
The fix that looks obvious and is worse: wrap the `addObserver` call site in a
`Catching {}`. That fixes the one caller you found and leaves every other
`appScope.launch` in the app one throw from the same crash.

**Where it hooks in.**
`libraries/kmptemplate/src/androidMain/.../AndroidAppLifecycle.kt:32` and
`libraries/flowroutines/src/commonMain/.../DispatcherProvider.kt:52-60`.

**Provenance.** Sodogku, commit `d4c446f`, 2026-09-28, found as a fatal launch
crash on a shipped release build. Both claims verified by reading this repo:
the `addObserver` call is unguarded and `AppCoroutineScope` has no handler.

## `Cache.update` is a read-then-write, and several writers share one record

**What it is.** `Cache.update`'s interface default is `set(transform(get()))`.
Two callers that overlap each transform a snapshot the other has already
replaced, so the second write silently reverts the first. Neither implementation
overrides it, even though `DataStore.updateData` already serialises the read and
the write and `MutableStateFlow.updateAndGet` does the same in memory. Override
in both, and put the reason on the interface default so the next implementation
does not quietly inherit it.

Use `updateAndGet` rather than update-then-read in the in-memory case: a second
read can see a later writer's value, so the caller would be handed a result its
own transform never produced.

**How it looks from the outside.** Flip a setting on a fresh install, send a
piece of feedback, relaunch, and the toggle is off again with the counters at
zero. Three unrelated features losing a write at once, which reads like the file
never saved rather than like a race.

**Why it is hard to spot.** Every unit test for a toggle uses a single-writer
in-memory fake, where the interleaving cannot occur. The tests are correct and
stay green straight through the bug. Downstream it stayed latent until the app
navigated to its first `TrackableRoute`, which made the navigation tracker's
write fire against a live screen for the first time.

**Where it hooks in.** `libraries/storage/src/commonMain/.../Cache.kt:12` is the
default. In this repo four writers share `AppData`
(`CachedInstallIdProvider.kt:58`, `AppNavigationTracker.kt:27`,
`UserScopedAppDataReset.kt:27`, `SessionExpiredViewModel.kt:62`) and two share
`ConfigCacheSnapshot` (`OfflineFirstAppConfigRepository.kt:171` and
`ConfigOverrideRepositoryImpl.kt:52`). That second pair writes *different fields
of the same record* and one of them runs on every boot, which makes it the more
likely of the two to bite here.

**Provenance.** Sodogku, `docs/decisions.md`, 2026-09-07. Its author wrote "this
is a template bug, not a Sodogku one" and filed it locally, where it sat until
2026-09-28. Verified here by reading: the default is unchanged and neither
implementation overrides it.

## `Cache.clear()` deletes the wrong path, and would not work if it deleted the right one

**What it is.** `DataStoreCache.clear()` calls `deleteFile(name)` while the file
is created as `"$name.json"`, so it removes nothing. Correcting the name is not
enough: `DataStore` holds the value in memory and serves readers from there, so
even a correctly-named delete leaves every reader on the old value until the
process dies. `updateData` is the only write path `DataStore` observes, which
makes it the only one `clear` can use. Write the serializer's default back
through it.

**How it looks from the outside.** Data that is supposed to be gone is still
there. In this repo `clear()` is the user-isolation mechanism:
`UserScopedProfileCacheCleaner` calls `profileCache.clear()` on a user change,
and `ProfileCache` is a `cacheFactory.persistent(...)`, so **the previous
account's profile record survives sign-out and account switch on a shared
device.**

**Why it is hard to spot.** It is silent twice over. `deleteRecursively()` on a
path that does not exist returns false without throwing, so the
`Catching {}.logOnFailure` wrapper around the call never fires. And the reader
keeps serving the in-memory value, so the data looks present for the ordinary
reason that data looks present.

**Where it hooks in.**
`libraries/storage/impl/src/commonMain/.../CacheFactoryImpl.kt:117` is the
`clear`, and `:63` is the `"$name.json"` that it disagrees with. Sodogku's fix
changes the `DataStoreCache` constructor from `deleteFile: () -> Unit` to
`defaultValue: suspend () -> T`.

**Provenance.** Sodogku, `docs/decisions.md`, found while fixing `update` above.
It went unnoticed there because the account-switch clearer it was written for had
been deleted. This repo still has accounts, so the method has live callers.
Verified here by reading both lines.

## The database ships `fallbackToDestructiveMigration(dropAllTables = true)`

**What it is.** `RealAppDatabaseProvider` builds the database with destructive
fallback across all versions. Harmless while the only table is the template's
example; a silent unrecoverable wipe the moment a real table lands and a release
adds a column. Replace with `autoMigrations`, and narrow the destructive fallback
to the version range that is template history no install has ever run.

**How it looks from the outside.** Nothing, until it is unrecoverable. The next
release after a schema change launches normally, having deleted everything the
user had. With no account, a player's records exist in exactly one place.

**Why it is hard to spot.** The line is correct for the state the template is in
and stops being correct on the day someone adds a column, which is a different
day from the one where anyone reads this file. Nothing fails, nothing logs, and
the app launches fine.

**Where it hooks in.**
`libraries/storage/impl/src/commonMain/.../db/RealAppDatabaseProvider.kt:22`.

**Provenance.** Sodogku, `docs/decisions.md`. Two of its agents flagged it
independently on the same afternoon from opposite ends of the schema, which
suggests it is discoverable and also easy to ship past. Verified here by reading.

## A mistyped boolean in remote config turns the feature off, everywhere, silently

**What it is.** `getValueRecursive` resolves booleans with
`rawValue.toString().toBoolean()`, and `"banana".toBoolean()` is `false`. So a
string typed into a boolean key in the admin console does not fall back to the
shipped default and does not log. It turns that feature off on every device that
fetches the config. Resolve booleans only from `"true"`/`"false"`
case-insensitively, with anything else null so the declared default wins.

**How it looks from the outside.** A feature is off for everyone and the config
that turned it off looks fine in the console, because it is the value someone
meant to type with a typo in it.

**Why it is hard to spot.** Every numeric branch on the same `when` is already
safe, because `toDoubleOrNull` returns null and falls back. Booleans are the sole
outlier, sitting in a list of lines that all look alike. And the fail-open test
reads against an *empty* map, where every key falls back correctly: the hole is
in resolving a value that is present and malformed, which no test covers. This is
the one path that can make a kill switch or a monetization key fail closed.

**Where it hooks in.**
`libraries/config/src/commonMain/.../MapExt.kt:32`. The generic rule it is worth
carrying with the fix: a parse that invents an answer is worse than no answer.

**Provenance.** Sodogku, `docs/decisions.md`, 2026-09-07. Verified here by
reading the line.

## The first frame waits ten seconds for a config server that is not deployed

**What it is.** `EnsureAppConfigLoaded` awaits `configStream().first()`, and that
stream is built with `mapNotNull` over the cached snapshot. With no cached config
(a fresh install, or a corrupt cache) it emits nothing, so `first()` sits through
the entire retry chain against the unconfigured base URL until the boot timeout.
Measured downstream at 10s cold, 3.3s after the fix. `configStream` should start
from the bundled fallback and re-emit when a cached or fetched snapshot
supersedes it.

**How it looks from the outside.** A brand-new generated app takes ten seconds to
show its first frame, once, on the very first launch after install. This is what
someone evaluating the template sees first.

**Why it is hard to spot.** It is true from the second launch onwards, and on a
dev machine you launch the app twice. The second one is fine, so the first gets
written off as cold-start noise.

**Where it hooks in.**
`libraries/config/impl/src/commonMain/.../OfflineFirstAppConfigRepository.kt:90`.

**Provenance.** Sodogku, `docs/decisions.md`. Verified here by reading the line.

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

## Three desktop source sets that nothing compiles

**What it is.** `libraries/ui/src/jvmMain` and
`libraries/navigation/impl/src/jvmMain` hold a `JvmWebLinkLauncher`, a desktop
`FontFamily` and a `NativeButton` actual. No module in the repo declares a
`jvm()` target, so none of it has ever been compiled. Delete them, or add the
target deliberately.

**How it looks from the outside.** It reads as live platform support, which is
worse than absent. Someone fixing a link-opening bug on desktop edits a file that
does not run, and the build agrees with them by staying green.

**Where it hooks in.** Both directories. Verified here: `grep "jvm()"` across
every build file in the repo returns nothing.

**Provenance.** Sodogku, `docs/decisions.md`.

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
