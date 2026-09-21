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
