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

---

## A failed App Store submission takes the Android release down with it

**What it is.** In `template/ci/.github/workflows/release.yml` the Android job
is `needs: [resolve-tag, build-number, ios]` and runs only when iOS succeeded
or was skipped. The intent is right: shipping Android alone leaves the two
stores on different versions, and nobody wants to discover that from a review
queue.

The problem is what the iOS job *is*. It builds, uploads to TestFlight, and
submits to the App Store, all in one job, so any of those three failing looks
identical from the Android job's point of view. When Drop2048's submission step
failed on 2026-10-02, the iOS binary had already uploaded and was sitting on
TestFlight. Android was skipped anyway. The release shipped nothing, and the
recovery was a hand-dispatched run with `skip_ios: true`.

**How it looks from the outside.** A red release run, one platform's binary
quietly on TestFlight, nothing on Play, and a workflow summary that says the
Android job was skipped — which reads like a deliberate choice rather than
collateral.

**Why it is hard to spot.** The gate is correct for the failure everyone
pictures, a broken iOS build, and wrong for the one that actually happens,
which is a submission that trips over App Store Connect state. Both arrive as
`needs.ios.result == 'failure'`.

The fix that looks obvious and is worse: dropping the dependency so Android
always runs. That reintroduces exactly the version skew the gate exists to
prevent, and it would ship Android against an iOS build that never compiled.

**Where it hooks in.** Split the iOS job in two: build-and-upload, then a
separate submit job that `needs` it. Android then depends on the *upload* job
rather than the whole iOS lane, so a submission failure leaves Android free to
ship while a build failure still stops everything. The `skip_ios` input stays
useful for the manual re-run path either way.

Worth doing alongside it: `release.yml`'s Android job is also where the
`skip_play_store` escape hatch lives, so the two inputs should end up
symmetrical once the jobs are.

**Provenance.** Drop2048, 2026-10-02 and 2026-10-04. The failure and the
`skip_ios: true` recovery both happened; the proposed job split is a design
recommendation and has not been implemented or tested anywhere yet.
