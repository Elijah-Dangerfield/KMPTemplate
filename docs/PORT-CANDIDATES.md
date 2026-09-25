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

## A build that cannot sell anything says nothing

**What it is.** Whatever a downstream app uses to fetch its catalog should
report at error level, not debug, when the store answers and the product is not
in the answer. Moving Eyes added roughly fifteen lines to `EntitlementsImpl`:
an error log plus a `purchase_unavailable` event when the product query comes
back without the product, the same for an outright query failure, and a
`purchase_failed` event when someone taps buy and does not get the thing.
This template has no billing module, so there is nothing to fix here. The
pattern is what travels, and it applies to any remote fetch whose empty result
is indistinguishable from its failure.

**How it looks from the outside.** It does not look like anything. That is the
entry. A paywall with no price renders a buy button that errors on tap, the app
does not crash, no log passes the release-build severity filter, and the first
anyone hears is a store rejection weeks later. Moving Eyes was rejected by App
Review on 2026-09-08 for exactly this and spent two weeks unable to reproduce
it, because the failure was in the reviewer's environment and produced no
signal anywhere.

Once the telemetry existed it took one read of Sentry. Five events tagged
`store returned no matching product`, from a device reporting model
`iPhone99,7` with a `VMAPPLE` kernel in San Jose, running the exact build under
review. Apple's review infrastructure, caught in the act, with timestamps
straddling the submission.

**Why it is hard to spot.** Empty is a legitimate answer. A store with no
matching product returns success and an empty list, so every `when` branch is
handled, nothing throws, and the code reads as correct. The natural severity
for "we got a response we did not like" is debug, and debug is exactly the
level a release build drops. The bug is not in the branch, it is in the
severity of the branch, which no review catches and no test asserts.

The fix that looks obvious and is worse: log it at debug and move on, or put it
behind the existing "store unreachable" message. Unreachable and
answered-but-empty need different words, because one sends someone to check
their wifi and the other is a configuration fault on your side.

**Where it hooks in.** `SentryLogTree` already raises an event at error and
above, so the whole change is choosing the level deliberately and adding an
analytics event beside it. Report once per process: the query runs on every
foreground and a store that is unhappy stays unhappy, so reporting each time
turns one broken build into thousands of identical events.

Generic version, for any fetch that gates something the user paid for: if the
response parsed, succeeded, and still cannot produce the thing the screen
exists to offer, that is an error, not a debug line.

**Provenance.** Moving Eyes, 2026-09-25. Verified: the telemetry was added on
2026-09-11 and the App Review events it captured were read out of Sentry on
2026-09-25. The claims about this repo were checked by reading it; there is no
billing module here.

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

