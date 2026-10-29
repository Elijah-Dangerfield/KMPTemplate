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

