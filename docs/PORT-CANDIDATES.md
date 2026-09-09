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
