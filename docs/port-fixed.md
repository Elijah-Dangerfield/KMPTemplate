# Problems found downstream

Every app generated from this template inherits its bugs, and the apps find them
before the template does — they are the ones that actually ship the code. This
file is where those findings come back.

**Add an entry when you fix something here that a generated app hit.** Say what
broke, how it looked from the outside, and why it was hard to spot, because the
next person will be looking at a symptom, not a cause. An entry is worth writing
even when the fix is one line; the value is the diagnosis, not the diff.

**Read it when you generate a new app.** Anything dated after the template
commit you branched from is probably still live in your copy.

---

## fadingEdge(ScrollState) erased the content instead of its edge

*Found in Moving Eyes, 2026-09-05. Fixed here.*

`Modifier.fadingEdge(scrollState)` anchored its gradient at
`size.height - scrollState.maxValue + scrollState.value`. For any content taller
than the viewport that is a coordinate **above the top of the screen**, so every
visible pixel fell past the gradient's transparent end and `BlendMode.DstIn`
erased almost all of it.

From the outside it looked like the fade covering most of the panel on arrival
and then *retreating* as you scrolled down — the opposite of what a fading edge
does. Easy to read as a styling problem rather than arithmetic.

The `LazyListState` overload immediately below it always did this correctly,
which is a large part of why the bug survived: the file appeared to contain a
working implementation of the thing that was broken.

Fixed by anchoring the band to the bottom of the viewport and shrinking it as
the end comes into reach, so it reaches zero exactly when there is nothing left
to scroll. Both overloads also lost `Modifier.composed` — the only thing it was
there for was a theme colour, and `DstIn` reads nothing but the source's alpha,
so the colour never mattered.

---

## `moduleConfig.serialization()` adds a dependency commonMain cannot see

*Found in Moving Eyes, 2026-09-05. **Not fixed here** — see below.*

`ConfigurationExtension.serialization()` does:

```kotlin
project.dependencies { add("implementation", libs.kotlinx.serialization.json) }
```

In a Kotlin Multiplatform module, `implementation` is the Android/JVM
configuration. `commonMain` never sees it. So a module that calls
`moduleConfig.serialization()` and uses `@Serializable` in `commonMain`
**compiles on Android and fails to link on iOS** — and since most people build
Android far more often, the failure surfaces at a milestone rather than at the
edit that caused it.

The name makes it worse: it reads as "this module does serialization", so the
natural conclusion when iOS breaks is that something is wrong with the iOS
target, not that the helper is pointed at the wrong source set.

Two neighbours compound it. `compose()` and `networking()` in the same class are
**empty**, so a module can call them, appear configured, and have nothing
happen.

Moving Eyes deleted all three. The Compose compiler plugin already comes from
`KotlinMultiplatformConventionPlugin`, and the serialization dependency belongs
in the module's own `commonMain` source set where it can be seen.

Left unfixed here only because four template modules call `serialization()` and
changing it needs an iOS link of each to confirm — worth doing, not worth doing
blind. If you are touching build-logic anyway, do it then.

---

## Custom detekt rules registered but never dispatched

*Found in Cards, 2026. Fixed here — template is on `2.0.0-alpha.6`.*

On detekt `2.0.0-alpha.5`, a custom rule could be registered in the rule-set
provider, marked active in `detekt.yml`, compiled into the ruleset jar, and
never called. The build passed and detekt reported success, which is
indistinguishable from a working rule that finds nothing.

Cards ruled out rule order, config-cache staleness, jar freshness, YAML shape
and the baseline before finding the version was the whole cause. `alpha.6`
dispatched the same rule unchanged, which then found 19 real violations.

**Note for anyone re-deriving this:** Moving Eyes checked and its rules
dispatched fine on `alpha.5` — an unconditional-report probe returned an
identical 8078 findings on both versions. So the failure is not universal to
that version, and a silent rule has at least one other cause worth checking
first.

That other cause, also seen in Moving Eyes: **the Gradle daemon caches detekt's
worker classloader**, so an edited rule keeps running its previous jar until you
`./gradlew --stop`.

Either way, the method is the same and it is the actual lesson here: **never
conclude a custom rule is clean from a clean run.** Make it report
unconditionally, confirm the flood, then revert.

---

## Animated state read during composition

*Found in Cards, 2026. Rule ported here as `AnimatedStateReadInComposition`.*

`val x by animateFloatAsState(...)` unwraps the `State` **during composition**,
subscribing the enclosing composable to a value that changes every frame. Its
whole subtree then recomposes at 60fps for as long as the animation runs.

In Cards this caused four production ANRs — a recomposing subtree containing
text thrashes Skia's glyph cache and wedges the RenderThread. One component went
from 471 recompositions to 16 once fixed.

It is invisible in review; it looks like completely normal Compose. Cards found
19 instances on the rule's first run, seven of them in files that had just been
swept by hand for exactly this pattern. Moving Eyes, generated before the rule
existed, found 10 more.

The fix is to keep the `State` and read `.value` inside a lambda that runs in
draw or layout — `graphicsLayer`, `drawBehind`, `layout`, `offset` — or to
derive the narrower thing composition actually needs with `derivedStateOf`.
`Modifier.shadow` has no lambda form and is the one known-good suppression.

**Related, and worth checking at the same time:** any infinite animation must
return a fixed value under `LocalInspectionMode`, or previews and screenshot
tests never reach idle and **hang rather than fail**. See `LoopingAnimation.kt`.
