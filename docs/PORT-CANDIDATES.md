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

## A build-time release channel does not survive promotion to the App Store

**What it is.** `beta.yml` stamps `RELEASE_CHANNEL_OVERRIDE: beta` and
`release.yml` stamps `store`, and `Versioning.kt` bakes that into
`BuildInfo.releaseChannel`. The value is correct when the binary is built and
wrong forever after, because promoting a TestFlight build to the App Store is
an ordinary action in App Store Connect that rewrites nothing. The binary that
goes on sale is the one that was built for TestFlight, still saying `beta`.

**How it looks from the outside.** Sentry cannot tell App Store customers from
TestFlight testers: every event from the live build arrives tagged
`beta-ios-release`. Moving Eyes shipped that way on 2026-09-22 and found it by
reading production events from real devices whose `build_type` was
`"app store"` while `release_channel` said `"beta"`.

Anything conditioned on the channel then behaves in production the way it was
meant to behave for testers. Moving Eyes had invented an `isQaBuild` that was
true for debug plus `beta`, and used it to gate a QA config menu reachable by
deep link. The result was a live App Store build one link and one toggle away
from granting its paid unlock for free.

**Why it is hard to spot.** The channel is right at every moment you would
think to check it. CI sets it correctly, the build reports it correctly, and
TestFlight shows exactly what you expect. Nothing is wrong until a human
promotes the build weeks later, and promotion produces no artefact, no log line
and no new binary. The tag only becomes a lie in retrospect.

The fix that looks obvious and is worse: tighten the channel gate, or add a
`store` check next to the `beta` one. That keeps a security decision resting on
a value the App Store is free to invalidate. **Do not gate anything that
protects money or data on the release channel.** Gate it on `BuildInfo.isDebug`,
which cannot survive a release build at all, and let the channel do what it is
good for, which is labelling telemetry.

**Where it hooks in.** `RELEASE_CHANNEL_OVERRIDE` in
`template/ci/.github/workflows/{beta,release}.yml` and the resolve chain in
`build-logic/.../Versioning.kt`. This template ships the channel but does not
yet gate behaviour on it, so there is nothing to fix here today. The entry
exists so the next app that invents channel-based gating, as Moving Eyes did,
does not have to learn this from a production incident.

Two things worth carrying with it. A local build script that stamps `beta` for
TestFlight will eventually produce a binary someone promotes, so the safer
default is `store` with `beta` as the explicit opt-in. And a debug-only
affordance reached by deep link needs its route registration gated, not just
whatever UI normally opens it: Moving Eyes gated the shake gesture and left the
deep link open, with a comment asserting the opposite.

**Provenance.** Moving Eyes, 2026-09-25. Verified against production Sentry
events and by installing a signed `store` channel APK to confirm the gate. The
claims about this repo's files were checked by reading them.

