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

Entries are in priority order. Everything queued right now is something a downstream app built that a brand-new app would want, rather than a defect: no entry below is a bug a generated app is currently shipping. Each is larger than a fix, and none is urgent.

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
