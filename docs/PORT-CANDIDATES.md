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

One entry left, and it is a capability rather than a defect: nothing queued is a bug a generated app is currently shipping.

---

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
