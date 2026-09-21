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

## Remove the camera entirely

**What it is.** The template ships a camera nothing uses, and it costs every
generated app something on both stores. Delete the whole surface rather than
declaring permissions for it:

- `libraries/ui/src/commonMain/.../CameraPreview.kt` and the `androidMain` /
  `iosMain` actuals
- `libraries/ui/src/commonMain/.../PermissionLauncher.kt` and both actuals.
  Its entire public surface is `rememberCameraPermissionLauncher`, so it goes
  with the camera rather than surviving as a general permission helper
- the six camera members and `CameraGuidanceState` on
  `libraries/ui/src/iosMain/.../nativeviews/NativeViewFactory.kt`
- the camera half of `apps/ios/iosApp/Platform/IOSNativeViewFactory.swift`
  (544 lines, `import AVFoundation`)
- `NSCameraUsageDescription` from `apps/ios/iosApp/Info.plist`
- `android.permission.CAMERA` and the `android.hardware.camera` `uses-feature`
  from `apps/compose/src/androidMain/AndroidManifest.xml`
- whatever falls out of `config/detekt/baseline.xml`

The Apple Sign In button factory is the other half of `NativeViewFactory` and
is a separate question: it is live while the template ships Supabase auth. If
both halves go, the interface, `LocalNativeViewFactory`, the `nativeViewFactory`
parameter on `IosAppComponent` and its plumbing through `iOSApp.swift` and
`MainViewController.kt` all go with them.

**How it looks from the outside.** Two symptoms, and neither names a camera at
the moment you meet it.

On iOS the first upload is accepted and then rejected by email twenty minutes
later: `ITMS-90683: Missing purpose string in Info.plist`, asking for
`NSCameraUsageDescription` on an app with no camera screen. On Android nobody
emails at all. The Play listing simply shows a **Camera** permission, which a
reviewer reads as an app asking for a sensor it never uses, and which a player
reads as worse than that.

**Why it is hard to spot.** Every instinct points at the wrong fix.

Apple's own error text tells you to add the purpose string, and adding it works:
the build goes through. That is the trap. A purpose string declares access to a
sensor the app never touches, which contradicts the App Privacy answers and
`PrivacyInfo.xcprivacy`, and leaves the reviewer a question you cannot answer
well. The template did exactly this on 2026-09-21 before this entry was written.

It also looks like dead code that cannot matter, because it is unreachable:
nothing in the generated app calls `CameraPreview`. App Store delivery scans the
binary for **API references**, not call sites, so unreachable is not the same as
absent. `grep` for a call site finds nothing and tells you the wrong thing.

**Where it hooks in.** Nothing consumes any of it. `CameraPreview` has no call
site outside its own actuals, and `rememberCameraPermissionLauncher` has none at
all, so this is a deletion rather than a refactor. Prove it on the built
artifact rather than the diff: `otool -L` on the `.app` should report no
AVFoundation linkage and `nm -u` no `AVCapture` symbols, and
`./gradlew :apps:compose:assembleRelease` plus `aapt dump permissions` should
show no camera permission. `scripts/verify_template.sh --fast` is the gate for
the generator still working afterwards.

**Provenance.** Drop2048, 2026-09-21. It inherited the iOS half, deleted the
Kotlin side in an earlier chunk, and left the Swift bridge behind; the first
TestFlight upload was rejected with ITMS-90683 and the fix was to delete the
bridge, verified with `otool`. Its release checklist separately carried the
Android half as a blocker: "The Play listing shows a Camera permission for a
falling-block puzzle." Both claims verified in this repo: the files above exist
here, `Info.plist` had no purpose string until 2026-09-21, and
`AndroidManifest.xml` still declares `android.permission.CAMERA`.

**Confirmed a second time the same day, by Sodogku**, whose first upload of
0.2.0 was rejected with the identical ITMS-90683 thirty-six minutes before
Doublestack's. Same inheritance, same deletion, same `strings` check on the
binary afterwards. Two apps failing the same way within an hour is the argument
for fixing it here rather than app by app.

Sodogku adds one diagnostic the entry above does not have, and it is the
expensive part. Its privacy documentation had **twice** examined this exact
scaffolding and twice concluded it was harmless: a 2026-09-08 pass claimed
`android.permission.CAMERA` was declared, a 2026-09-10 pass correctly found it
was not, withdrew the finding, and wrote down that the leftover Kotlin "has no
call sites and no manifest entry of its own, so it changes nothing about the
listing". Both passes asked *is a permission declared*. Apple asks *is an API
referenced*. Those are different questions with different answers, and the
second one is the only one App Store delivery evaluates. Anyone auditing a
generated app for this should grep for `AVCaptureDevice`, not for
`uses-permission`.

Sodogku's copy also carried two things this entry does not list, and **the
template still ships both**, verified here:
`libraries/ui/src/{common,android,ios}Main/.../AudioRecorder*.kt` and the same
three `PhotoSaver*.kt`. `AudioRecorder.ios.kt` asks `AVAudioSession` for the
record permission, which makes `NSMicrophoneUsageDescription` the next ITMS-90683
waiting to happen; the only reason it has not fired is that Apple names one
missing purpose string per rejection and the camera came first. `PhotoSaver`
touches Foundation only and is harmless, but it is equally dead. Delete all six
in the same pass.

---

## The navigation queue can wedge, and only a touch can prove it

**What it is.** `DelegatingRouter` queues navigation commands and drains them
under the Compose host's lifecycle, which is correct: applying a command to a
controller whose host is genuinely gone loses the back stack or crashes. The
whole mechanism rests on the host telling the truth about whether its view is on
screen, and on iOS it sometimes does not.

Two pieces port together, and the second is worthless without the first:

- a watchdog at the Compose root that reports a press landing on a host that has
  claimed to be off screen for more than about two seconds
- a `NavigationRecovery` seam, separate from `Router`, whose only member drains
  the queue past the shut gate, called only by that watchdog

**How it looks from the outside.** The app takes taps and does nothing with
them. Not a freeze: the screen holds its last frame, touches are received,
logging carries on, and the shake-to-report gesture still works. Buttons simply
have no effect, and it clears on its own eventually or not at all. Every bug
report for it says "the app froze", which sends you to the main thread, where
there is nothing wrong.

**Why it is hard to spot.** Nothing in the app is broken. Every component is
behaving correctly given a lifecycle that is below STARTED, and a host below
STARTED is completely ordinary: it is what happens whenever the app is
backgrounded or a full-screen ad is up. Logging the state would be one error per
ad and would train everyone to ignore it.

The signal that is never ordinary is a **touch**. A covered view is not
touchable, so a press reaching the root while the host says the view is off
screen is a contradiction, and the host is the half that is wrong. That single
observation is what turns an unfalsifiable "sometimes it wedges" into a
detector, and then into a repair.

The obvious fix, ungating the drain, is worse: it removes the protection in the
ordinary case to fix the rare one. Gate the repair on the proof instead.

**Where it hooks in.** `libraries/navigation/impl/.../DelegatingRouter.kt`
already has the drain and the lifecycle reference. The generic version is the
`NavigationRecovery` interface next to `Router`, a `drainInto` extension on the
command channel so the loop is testable without a `NavHostController`, and a
root-level `Modifier` in `:apps:compose` that reads `LocalLifecycleOwner` and
watches `PointerEventPass.Initial`. The template has none of these: no
`HostLifecycleWatchdog`, no `NavigationQueueWatchdog`, no recovery seam.

**Provenance.** Sodogku, 2026-09-21, and reported here rather than verified
here. Sodogku hit it as `SD-26` in the field, could not reproduce it for weeks,
and eventually caught it in Sentry as `SODOGKU-R`: host lifecycle CREATED,
`GADFullScreenAdViewController` in `view_names`, five commands queued, four
seconds elapsed. The trigger there was a rewarded ad presented from the window
root rather than the top of the presentation chain, which is an ads-module
problem this template does not have. **The wedge is not ads-specific.** Any
full-screen UIKit presentation over the Compose host is a candidate, which is
why the recovery is worth having even in a generated app with no ads.
