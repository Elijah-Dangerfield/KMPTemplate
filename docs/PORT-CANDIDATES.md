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

## A bottom sheet whose top rests inside the status bar bounces instead of closing

**What it is.** `BottomSheet` (`libraries/ui/.../components/dialog/bottomsheet/BottomSheet.kt`) hands Material3's `ModalBottomSheet` its default `contentWindowInsets`, so Material pads the content by the top inset minus the sheet's current offset. That padding changes the sheet's measured height at every offset while the sheet's top is inside the status bar, and Material snaps the sheet to the recomputed Expanded anchor whenever the height changes. The fix, landed in Opson (commit `fix(ui): stop a sheet resting in the status bar from bouncing instead of closing`): pass `contentWindowInsets = { WindowInsets(0, 0, 0, 0) }` and `dragHandle = null`, then in the content apply the pinned height first, the insets inside it (`windowInsetsPadding(BottomSheetDefaults.windowInsets)`), and draw the handle inside that (with a `dismiss` semantics action, since Material's handle wrapper carried one). For a sheet nothing pins, cap the height at the available height minus the top inset (a `layout` modifier reading `insets.getTop(this)`, with `maxOf(minHeight, ...)` so a fixed height passes through), so it never rests where the padding varies.

**How it looks from the outside.** Drag a tall sheet down starting on its scrolling content: it does not move, the content rubber-bands on iOS, and once let go the sheet bounces in place and never settles or closes. Dragging by the handle still works. It depends on the phone: a 93% sheet bounces on a 393 by 852 point iPhone (it rests about 57 points down, inside the 59 point inset) and is fine on a Pro Max.

**Why it is hard to spot.** It looks like the content deciding the sheet's height, and the template's own `scrollableContent` (Sodogku's fix) reads as the cure: pin the height. That is only true for full height, where the padding is inside the fill. A fractional pin measured inside Material's padding, or content-sized content that reaches the status bar (at large text sizes, or a list in a child composable a lint rule cannot see), still bounces. The mechanism is that a drag starting on scrolling content moves the sheet through nested scroll (`dispatchRawDelta`), which does not take the drag lock, so `updateAnchors`' snap succeeds every frame. A test that injects a whole swipe between two frames never re-measures mid-drag and passes on the broken sheet; drive one move per frame.

**Where it hooks in.** The `ModalBottomSheet(...)` call and the `Column` inside it in `BottomSheet.kt`. Opson's version also takes `heightFraction` and an `internal fun SheetLayout(..., windowInsets)` seam so a Robolectric test can inject a status bar; copy `BottomSheetDragTest` from `libraries/ui/src/androidUnitTest/.../dialog/bottomsheet/` with it (qualifiers `w393dp-h852dp-xxhdpi`, `WindowInsets(top = 59.dp, bottom = 34.dp)`). Update the AGENTS.md landmine about sheets that snap back mid-drag, which names the wrong cause.

**Provenance.** Opson, 2026-10-29. Reproduced and verified in Opson's Robolectric test (red before, green after); not reproduced in this repo, and not yet confirmed on a device.


## A Kotlin crash that passes through UIKit reaches Sentry as a bare C++ exception

**What it is.** iOS crash reporting runs both Kotlin's `std::terminate` handler (installed when the Kotlin runtime starts) and Sentry's (installed when `AppTelemetry` initializes Sentry). Sentry's goes in second, so it runs first. For a Kotlin exception that unwinds through Objective-C or Swift (Kotlin code UIKit called while Kotlin was calling into UIKit, which is common with sheets, gestures and anything presenting), the Kotlin call into UIKit ends it with `std::terminate`. Sentry's handler records a bare C++ exception and aborts, and Kotlin's handler, whose `setUnhandledExceptionHook` is where sentry-kotlin-multiplatform would have stored the real Kotlin exception, never runs. The fix in Opson (commit `fix(ios): fatal Kotlin exceptions reach Sentry with their type, message and stack`): a small `KotlinCrashes.mm` remembers Kotlin's handler right after the first Kotlin call, then, once Sentry has started, installs a handler that calls Kotlin's first when the in-flight exception's type name contains `ExceptionObjHolder` and falls back to Sentry's for everything else. It needs a bridging header (`iosApp-Bridging-Header.h`, `SWIFT_OBJC_BRIDGING_HEADER`). Swift calls `RememberKotlinHandler()` before `telemetry.initialize()` and `PutKotlinFirst()` after it.

**How it looks from the outside.** A crash in Sentry titled `C++ Exception: N12_GLOBAL__N_122ExceptionObjHolderImplE: (null)`, `mechanism: cpp_exception`, no stack trace, the main thread missing from the payload, and nothing in the logs. Several different crashes group into that one issue. In Opson it hid "performMeasureAndLayout called during measure layout" (a native sheet resized from Compose's measure pass) for an afternoon; lldb had to find it.

**Why it is hard to spot.** sentry-kotlin-multiplatform says it installs the unhandled-exception hook, and it does, so "Kotlin crashes are covered" looks settled. Crashes thrown purely in Kotlin do reach the hook and arrive fine; only the ones that cross UIKit don't, and those are the hard ones. The obvious fix, calling `setUnhandledExceptionHook` yourself, changes nothing, because the hook never runs on this path.

**Where it hooks in.** The iOS app's startup where `iOSAppComponent.telemetry.initialize()` is called (Opson: `apps/ios/iosApp/iOSApp.swift`, around the component creation), plus `apps/ios/iosApp/Platform/KotlinCrashes.{h,mm}` and the bridging header. Generic version: rename the `Opson` prefix. Opson also logs the exception as a breadcrumb first (`KotlinCrashes.logBeforeReporting()`), optional.

**Provenance.** Opson, 2026-11-05. Verified in Opson on the iOS simulator: a forced Kotlin crash arrived as an event with the Kotlin type, message and about 120 frames, and no new bare C++ event. The template's Sentry setup (`sentryKmp` plugin, `AppTelemetry`) matches Opson's, so it most likely has the same gap; not reproduced here.
