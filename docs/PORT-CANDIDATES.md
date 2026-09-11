# Port candidates

Work for this repo, found by the apps built from it — something a downstream app proved in production that a brand-new app would want, or a bug in code this template already ships. See AGENTS.md → "This template is fed by the apps built from it" for what qualifies and how to write an entry. This is a queue, not a changelog: delete an entry when you land it.

Closed questions live in [decisions.md](decisions.md); traps that can't be fixed,
only avoided, live in AGENTS.md → "Known landmines".

---

## A tall `BottomSheet` snaps back to the top mid-drag

**Found by:** Sodogku, 2026-09-09. Fixed there in `430cc88`.

**Symptom, as reported by a player:** with sheet content tall enough to fill the
screen, scrolling works but dragging the sheet back down to close jumps, and can
be made to bounce near the top indefinitely without ever closing. Worse on iOS,
present on both.

**Cause.** Material3 derives the sheet's Expanded anchor from the sheet's
*measured* height (`Expanded at fullHeight - sheetSize.height`), recomputes the
anchors on every measure pass, and snaps to the recomputed target whenever the
new anchors differ from the old. A sheet whose height is decided by its content
can therefore be re-measured mid-drag and yanked back to Expanded. Frame-by-frame
logging on a device showed the offset climbing 2 → 22 → 42 → 72 → 86 and then
going to exactly 0.0 in two frames, eight times in a row, with `targetValue`
never leaving Expanded.

It only bites once content is tall enough to be clamped against the available
height. Short content gives a stable measured height and identical anchors on
every pass, so there is nothing to snap to — which is why this can sit unnoticed
until the first long sheet.

**Fix.** Add a `scrollableContent: Boolean` parameter to `BottomSheet` that both
owns the scroll and pins the sheet to the full height. Identical anchors on every
recompute means `updateAnchors` has nothing to do. Callers stop hand-rolling
`Column(verticalScroll)` inside the content lambda, so the next long sheet
inherits the fix instead of rediscovering the bug. Sodogku also added a detekt
rule for the hand-rolled case.

**Two things that look like the cause and are not**, recorded so they are not
retried: it is not overscroll (providing `LocalOverscrollFactory = null` changed
nothing), and it is not navigation (the router logged one navigate and one go
back per reproduction).

`libraries/ui/.../components/dialog/bottomsheet/BottomSheet.kt` here is the same
file.

---

## Shake-to-report fires on resume, then never works again

**Found by:** Sodogku, 2026-09-09. Fixed there in `90a4c6c`.

**Symptom:** the report dialog appears by itself when the app returns from the
background, and after that shaking does nothing for the rest of the process.

**Three defects, all in code this template ships.**

1. `ShakeHandler.onDialogDismissed()` — the only thing that clears
   `isShowingDialog` — **has zero callers**. The flag latches on the first shake
   and the dialog can never appear again. Check `ShakeHandler.kt:45` here; it is
   still uncalled.
2. `stop()` never cancels the collector `start()` launched, and the detector's
   `shakeEvents` was a single-consumer `Channel.receiveAsFlow()`, so each
   foreground cycle leaked another collector onto a stream where every event goes
   to exactly one of them.
3. **The one that produces the phantom.** `App.kt` binds the detector with a
   `DisposableEffect`, which only tears down when the composition goes away —
   backgrounding does not. So the accelerometer keeps running in a pocket, while
   `DelegatingRouter` drains its navigation queue under
   `repeatOnLifecycle(STARTED)`. A jostle queues a navigate that is held until the
   app comes back. Proven on a device: two queued navigations while backgrounded
   with the old effect, none with `LifecycleStartEffect`.

**Also worth carrying over:** the two platform detectors had silently diverged —
Android measured m/s² divided by elapsed time, iOS measured raw g and never
divided, leaving iOS roughly twice as hard to trigger. Sodogku moved the gesture
logic into a shared, testable recognizer.

**And a latent iOS crash next door:** `ShakeDialogRoute` was a `data object`,
which AGENTS.md already names as a SIGSEGV at navigate time on iOS.

The generalisable lesson is defect 3: a sensor scoped to the composition feeding
a queue gated on the lifecycle. The symptom appears on resume and looks nothing
like the cause.

---

## The custom detekt rules are enforced only by a hook, and their own tests never run

**Found by:** Sodogku, 2026-09-09. Fixed there by adding one CI step.

Two gaps, and the second is the one that rots quietly.

**1. Nothing on the server runs detekt.** `.github/workflows/template-ci.yml` runs
`testDebugUnitTest`, the server tests and the iOS compile. The only thing that
runs `./gradlew detekt` is `.githooks/pre-push`, which is per-machine, requires
`scripts/install_hooks.sh` to have been run, and is skippable with
`SKIP_DETEKT=1`. So the `kmptemplate` ruleset is enforced by whoever remembered
to install the hook.

These are not style rules. `VerifyStrings` guards a crash on a missing key and
`AnimatedStateReadInComposition` guards a per-frame recomposition. A gate only
the author runs is not a gate.

**2. `:detekt-rules:test` runs nowhere at all.** `detekt-rules` is a JVM module,
so `testDebugUnitTest` never touches it. A rule that silently stops matching
leaves the build green, which is strictly worse than having no rule: the team
believes something is guarded when it is not.

Sodogku hit exactly that. A newly added rule compiled, was in the jar, was listed
in the provider bytecode and was enabled in config, and detekt ran it **zero
times** — the Gradle daemon caches the ruleset ClassLoader by classpath *path*,
not by jar contents, so it kept serving a classloader built from the previous
jar. `--rerun-tasks` does not help; `./gradlew --stop` does. The pre-existing
rules keep working throughout, which is what makes it so misleading.
`build/reports/detekt/detekt.sarif` lists what actually loaded and is the way to
check.

**Fix.** One step in the CI job:

```yaml
      - name: Lint with the custom ruleset
        run: ./gradlew detekt :detekt-rules:test
```

Worth doing here rather than only downstream, so a project generated from this
template has the rules enforced on day one instead of discovering years later
that a hook nobody installed was the only thing holding the line.

**Also worth taking:** Sodogku's `NoRawDesignValues` (rejects raw colours and
dimensions so the palette stays the palette) and `ScrollInsideBottomSheet` (see
the bottom sheet entry above) are both general to any app built on this
template, not Sodogku-specific.

---

## `SEAViewModel` banks events forever, and a stalled screen replays them into a crash

**Found by:** Sodogku, 2026-09-09. Fixed there in `6de22f9`.

**Symptom, as reported:** the app stops navigating while still registering taps.
Marks land, view models log `Sending event ...`, and nothing happens. Later,
something unrelated wakes the screen and the app crashes:

```
IllegalStateException: Attempted to pop Destination route=AchievementsRoute,
which is not the top of the back stack (Destination route=AchievementsRoute)
```

**Cause.** `SEAViewModel` holds events in `Channel<E>(Channel.UNLIMITED)` and
`ObserveEvents` collects through `repeatOnLifecycle(STARTED)`. Below STARTED the
collector detaches and the channel keeps accepting, so a stopped screen
*accumulates* side effects rather than losing them. When collection resumes they
all arrive at once. In the report, twelve `OpenAchievements` events built up over
eight minutes and replayed 1.5ms apart; twelve navigations to the same route is
more than NavController will tolerate.

`sendEvent` has no lifecycle gate, which is why the logs look healthy the whole
time and only navigation goes quiet.

Worth noting `ShakeDetector` in this template already carries the same lesson in
its own doc comment — "a shake is only meaningful in the moment it happens",
written after events sat in a buffer and arrived minutes stale as a dialog nobody
asked for. It is the same mistake one layer up, in the base class every screen
uses.

**Fix.** Wrap each event with a `TimeMark` at send and drop it on collection if
it is older than a few seconds. The threshold only bites when nothing is
collecting: with a collector attached, delivery is immediate and every event is
microseconds old, so it cannot drop an event from a slow view model. Keep it
comfortably longer than the gap between a view model sending in `init` and the
screen composing its collector — that is the case that must not break, and a
zero threshold strands it.

Take a constructor-injected `TimeSource` defaulting to `Monotonic` while you are
there; `runTest`'s virtual clock does not advance a monotonic source, so without
it the expiry can only be tested with a real sleep.

**This does not fix the underlying stall**, which is still open in Sodogku as
SD-26. It stops the stall ending in a crash, which is the difference between an
annoyance and a crash report.

`libraries/flowroutines/.../SEAViewModel.kt` here is the same file, with the same
`Channel(UNLIMITED)` and the same `receiveAsFlow()`.

---

## Lifecycle-gated collection stops silently, and nothing in the logs says so

**Found by:** Sodogku, 2026-09-09. Fixed there in `6de22f9`.

Every `repeatOnLifecycle(STARTED)` in this template is a place the app can go
quiet without going wrong: the collector detaches, the producer keeps producing,
and there is no trace of it. Chasing the stall above, the only way to tell
whether the lifecycle had dropped was to infer it from what had *stopped*
appearing in the logs, which is a slow way to learn something the framework
already knows.

**Fix.** Log on both edges inside the `repeatOnLifecycle` block — entry, and a
`finally` for the exit — tagged with what is being collected. Route
`ObserveWithLifecycle`, `observeWithLifecycle` and `observeWithLifecycleIn`
through one helper so there is a single place doing it, and give `ObserveEvents`
the view model's simple name as the tag. `DelegatingRouter`'s queue drain wants a
tag too; it is the other collector that goes quiet, and the pair of them
disappearing together is the signature of the bug.

`libraries/flowroutines/.../Compose.kt` here has all three call sites.

---

## Feedback reaches Sentry and is unreadable when it gets there

**Found by:** Sodogku, 2026-09-09. Fixed there in `26fcba1`.

Three separate things, all found in one triage session on real reports.

**The typed message was invisible.** The log attachment and the screenshot both
showed on the issue; the words the user actually wrote did not. The KMP SDK only
has the *legacy* User Feedback API, where the comment is attached to an event and
rendered wherever the org's feedback settings decide. Checked against the
published sources: 0.26.0 has no `captureFeedback` or `SentryFeedback` either, so
upgrading does not help. Fix: also put the payload on the carrier event, as an
extra and as a `feedback.txt` attachment. Attachments and extras render on the
issue page unconditionally, which is the page where the rest of the evidence
already is.

**It sorted as an error.** The carrier `captureMessage` took the default level,
so feedback sat alongside crashes and read like one. Set `scope.level` to INFO
explicitly.

**`onCleared` was filing Sentry issues for documented behaviour.**
`SavedStateHandle` accepts primitives and Parcelables; a Kotlin data class is
neither, so saving state throws on nearly every screen on every teardown, exactly
as the class doc says it may. In Sodogku that went through `logOnFailure`, which
logs at Error, and `SentryLogTree` turns Error into an event — so the single most
routine thing the base view model does was generating issues.

This template is **already better here**: its `onCleared` is debug-gated with a
message that explains itself. Worth keeping that way, and worth knowing why: the
fix for a screen that genuinely needs to survive process death is to make its
state Bundle-able, never to raise this back to a warning.

---

## The shake detector is duplicated per platform, untestable, and fires when you put the phone down

**Found by:** Sodogku, 2026-09-09. Fixed there across `90a4c6c` and `1d78cd6`.

This template still has the shape Sodogku started from: `AndroidShakeDetector`
and `IosShakeDetector` each implementing the gesture themselves, with their own
thresholds and their own `ShakeIntensity`. The two disagreed — Android divided by
elapsed time and iOS did not, Android measured m/s² and iOS measured multiples of
gravity — and neither could be unit tested, because constructing a `SensorEvent`
or a `CMAccelerometerData` is not something a test does. That is why the
baseline-on-resume bug survived for as long as it did.

**Fix, in two parts.**

Pull the gesture into a `ShakeRecognizer` in `commonMain` that takes
`(x, y, z, atMs)` and returns a boolean. The platform detectors shrink to
register/unregister plus a unit conversion, and every rule becomes testable.

Then retune it, because the numbers are too loose. Sodogku's inherited values
were two qualifying samples anywhere inside a full second, over a bar of 8.0 m/s²
per 100ms. The bar is the lesser problem — 8.0 is under 1g of *change*, which a
firm set-down clears — but the real looseness is that two of those had a whole
second to find each other, so any two unrelated bumps in the same second read as
a shake. Four inside 700ms asks for roughly 6Hz of sustained direction reversal:
what a hand shaking a phone does, and what nothing else in normal handling does.

Also drop `ShakeIntensity` unless something reads it. In Sodogku it existed only
to flavour randomly generated dialog copy that had already been deleted, so both
platforms were classifying a magnitude with no reader.

**One trap when testing the retune.** A burst-window test whose gap is longer
than any plausible window passes with the window widened back, because the burst
resets under either value. Pin the window with three tight reversals and a
straggler just outside it.

---

## `animatePlacement`: a modifier that makes anything slide when its parent moves it

**Found by:** Sodogku, 2026-09-09. Added there in `ef0f6e8`.

Not a bug, and small, but it is the kind of thing every app rewrites badly once.

Anything positioned relative to something else — a tooltip, a coach mark, a
popover — eventually needs to travel when its anchor changes rather than
teleport. The tempting implementation animates inside the positioning component,
and it goes wrong the same way every time: animation needs a coroutine, a layout
pass cannot start one, so the placement maths gets dragged into composition where
the content's measured size is not known yet. What follows is a sentinel for "not
measured", a hidden first frame, and a measure-to-state-to-layout loop.

Split it. A `Layout` measures the content and places it — one pass, no unknown
size, and the placement rule falls out as a pure function you can unit test. A
separate `Modifier.animatePlacement()` watches where it was actually placed via
`onPlaced`, and if that is somewhere new, springs the delta back to zero. It
knows nothing about anchors and works on anything.

Two details worth copying: snap the first placement, since there is nothing to
travel from and springing in from the origin is an unasked-for entrance; and read
the spring inside `Modifier.offset { }` rather than in composition, or the whole
subtree recomposes every frame of the travel.

---

## Analytics attributes spelled with a class name are renamed by R8, and no test can see it

**Found by:** Sodogku, 2026-09-10. Fixed there in `2a4cad1`.

**Symptom.** A dashboard panel reads zero on Android while the store console
shows the sales it is meant to be counting. Nothing errors, nothing is logged,
and the panel is not empty, because the other values in its filter still match.
iOS is unaffected, which makes it look like a platform bug rather than a build
one.

**Cause.** The event was emitted as `logEvent("iap.purchase_result", "outcome" to
result::class.simpleName)`. `PurchaseOutcome.Success` and its siblings are plain
sealed objects: not `@Serializable`, not `Throwable`. Release minification is on,
and the only name-keeping rule in `proguard-rules.pro` is `-keepnames class *
extends java.lang.Throwable`, which does not cover them. So the release build
sends whatever R8 renamed the class to, while the dashboard filters on
`outcome="Success"`.

Confirmed rather than reasoned: a real `minifyRelease` build's `mapping.txt` has
`com.sodogku.libraries.billing.PurchaseOutcome$Success -> ta.l`, so the attribute
arrived as the letter `l`.

**This template does not have the bug and does have the setup that produces it.**
Minification is on, the same Throwable-only keep rule is the only one, and
several `::class.simpleName` reads exist. They are all inside log *messages*,
which a human reads, so an obfuscated name there is ugly and not wrong. The
moment somebody feeds one to an analytics attribute, this happens.

**Two things checked on the way that are worth not re-deriving.** Throwable names
really are kept, so `simpleName` on an exception is safe. And enum `.name`
survives even though R8 renames the constant fields, because the string is baked
into the class initializer: disassembling `<clinit>` shows `const-string
v1, "Rewarded"` intact.

**Fix.** Give each type an explicit `val name` literal and emit that.
A `-keepnames` rule also works and is worse: the dashboards are keyed on these
strings, and nobody renaming a class will think to open a ProGuard file.

**What to port is the guard, not the fix.**
`noAttributeIsSpelledWithAClassNameR8CanRename` in `DashboardQueryContractTest`
fails when any `logEvent` argument contains `::class.simpleName`,
`::class.qualifiedName` or the `::class.java` forms. It is honest about its limit
in its own KDoc: a class name laundered through a helper is invisible to it.

It is worth porting even though this template has no dashboards, because the
cost of adding it now is a few lines and the cost of finding this in a shipped
app is a quarter of missing revenue data.

---

## Grafana dashboards as files in the repo, with a test that holds them to the code

**Found by:** Sodogku, 2026-09-08 onward. Six dashboards in `ops/grafana/`.

**What it is.** Grafana exports a dashboard as JSON and imports it back, so
keeping the JSON in git makes a dashboard reviewable and versioned like code
instead of clicked together in a browser and lost when somebody leaves. One file
per dashboard, plus a README with a table of file, uid, and the question the
board answers.

**Do not port the dashboards.** Every panel in Sodogku's six asks a question
about that specific game. What is portable is the convention and, much more
importantly, the thing that keeps it from rotting.

**The part that earns its keep is `DashboardQueryContractTest`.** Dashboards in a
repo are a liability without it: a query drifts from the code that feeds it, the
panel quietly reads zero or reads less than it claims, and nobody finds out,
because a wrong number looks exactly like a real one. It parses the LogQL out of
every panel and holds four things:

- Every attribute a dashboard queries is emitted on the event it is queried
  against. Renaming at either end goes red, and the emit site can be in a
  different module.
- Every emitted event has a row in the events registry markdown, and every row
  names an event something emits. This catches an event deleted with its feature
  while its row and its panel live on.
- Every value a dashboard filters on is one some emit site can produce. This is
  the one Sodogku needed twice: once for the R8 bug above, and once for a panel
  filtering `outcome=~"Rewarded|Completed"` where `Completed` had been deleted
  with the interstitial two days earlier and nothing looked wrong because
  `Rewarded` still matched.
- A guard against the guard: every check that could silently stop checking, for
  example by failing to extract a vocabulary, has to say why in an exemption list,
  and a stale exemption fails too.

**The trap to port with it.** A Gradle test task cannot see files a test reads at
runtime, so the dashboards, the registry markdown and the scanned source tree all
have to be declared with `inputs.files(...)`. Without that the task stays
UP-TO-DATE and the whole thing is green while checking nothing. Sodogku has four
recorded instances of exactly that hole, one of which passed while reading five
files because the exclusion matched an absolute path.

**Which dashboards could ship, and the order to do it in.** More of Sodogku's
event surface is app-shaped rather than game-shaped than it first looks:

```
app.launched  app.startup  app.jank  app.foregrounded  app.backgrounded
net.backend_unreachable   net.offline_banner   conn.reconnecting
iap.paywall_shown   iap.purchase_result   iap.restore_result
```

That supports two dashboards a generated app would keep on day one, and a third
with a caveat:

- **App health.** Cold start, jank by screen, crash-free sessions, backend
  unreachable rate. Answers "is it working" for any app at all.
- **Purchase funnel.** Paywall shown, purchase result by outcome, restore result.
  The most valuable of the three, because it is the one most likely to be wrong
  in a way nobody notices. See the R8 entry above: Sodogku's version of exactly
  this board would have read zero on Android through its whole first release.
- **Active users**, with a caveat. Sessions per install, returning against new,
  foreground time. Least portable of the three, because what counts as active
  depends on what the app is for, and a daily puzzle and a tax filing app do not
  share an answer. Ship it as a starting point that expects editing, or not at
  all.

The ad funnel is halfway generic. It ports if the template assumes AdMob, which
is worth deciding out loud rather than inheriting by accident.

**Do it in this order, because the reverse does not work.** The template has the
telemetry plumbing and no `ops/grafana` directory and no event registry. So:

1. **Decide which events the template guarantees every generated app emits.**
   This is the actual work and it is a design decision about the template, not a
   copy job. A dashboard querying an event a generated app might not send is
   worse than no dashboard.
2. Write the registry markdown for that set.
3. Then the dashboards, which are mechanical once 1 and 2 exist.
4. Then the contract test, which holds all three to each other.

Starting at 3 gives you boards that are wrong for most apps generated from this
template, and no way to find out.

`libraries/telemetry/impl/src/androidUnitTest/.../DashboardQueryContractTest.kt`
is about 1,000 lines in Sodogku, most of it the LogQL parser and its self-tests.
Porting it means porting the parser; the four assertions on top are short.
