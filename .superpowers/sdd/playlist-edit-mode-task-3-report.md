# Playlist edit mode Task 3 report

## Reproduced RED and root cause

Focused reproduction command before the corrective changes:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest.realToolbarBackDismissesModalThenLeavesEditModeAndLaterBranchesUseShippingCallbacks' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest.productionSystemBackCallbackDismissesRealModalBeforeRoutePop' --configuration-cache --rerun-tasks
```

Exact result: `2 tests completed, 2 failed`; `BUILD FAILED in 12s`.

- `realToolbarBackDismissesModalThenLeavesEditModeAndLaterBranchesUseShippingCallbacks`: `assertExists` failed because no `×` edit control existed after modal dismissal (`PlaylistEditModeSemanticsJvmTest.kt:184`).
- `productionSystemBackCallbackDismissesRealModalBeforeRoutePop`: the same `assertExists` failure occurred after system Back (`PlaylistEditModeSemanticsJvmTest.kt:257`).

Root cause: Task 3 moved the action header outside the editable `LazyColumn` and correctly placed an edit-active consuming overlay over that sibling. The old Task 2 tests started in edit mode and tried to open Rename through that now-outside header. Their first Rename tap therefore cleared edit mode and was consumed. The prior uncommitted test change added a second Rename click, which opened the modal only after edit had already ended; after Back dismissed the modal, expecting edit controls to remain was impossible. This was a test setup error, not a Back dispatcher failure. A real modal that can open while edit remains active must come from an editable-list control (Remove), while the action-header behavior needs its own physical-pointer test proving first-tap clear/consume and second-tap action.

## GREEN evidence

Minimum root-cause correction:

- Removed the duplicate Rename clicks from the prior tests rather than making header actions clickable through edit mode.
- Reworked both Back precedence integrations to open the real Remove confirmation from the visible editable-row control. Toolbar/system Back dismisses that modal while edit controls remain visible; the next Back clears edit mode, and the existing later precedence assertions remain unchanged.
- Kept the consuming overlay exactly bounded by `Modifier.matchParentSize()` inside the `beforeList` action-header `Box`. It is a sibling before the `LazyColumn`; it never wraps or overlays the list viewport. The hub caller continues to use the default frame arguments.
- Added production semantics coverage for semantic long-click entry, default action absence, no row playback while editing, exact-once move/remove, disabled boundaries, 44dp controls, first action-header physical tap clear/consume followed by action on the second tap, blank list-viewport taps preserving edit mode, and toolbar Back remaining the semantic shell action.

Focused GREEN command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --configuration-cache --rerun-tasks
```

Exact result: `BUILD SUCCESSFUL in 9s`; `52 tests` passed with zero failures, errors, or skips (`10` Compose interaction tests and `42` common playlist screen tests); `26 actionable tasks` executed.

Production compilation command:

```text
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Exact result: `BUILD SUCCESSFUL in 11s`; `26 actionable tasks`: 4 executed, 1 from cache, 21 up-to-date.

Whitespace validation:

```text
git diff --check
```

Exact result: exit status 0 with no output.

## UI/UX review repairs (post-commit `dae162a`)

### RED evidence

Two assertion-level Compose regressions were added first:

- `compactEditRowKeepsMetadataWideWhileMovingMutationControlsToASeparateRail` renders a 360dp-wide detail screen and requires at least 100dp metadata width, a 132dp control rail, and a 44dp remove target.
- `editHeaderExposesOnlyExitEditingSemanticsUntilItsFirstActivationIsConsumed` requires edit mode to expose no Add tracks action, exposes one Exit playlist editing action, consumes its first semantic activation without opening the browser, then restores Add tracks so its next activation opens the browser.

Before the repair, the focused new-test command compiled after minor test setup correction but failed both behavioral assertions: compact edit rows had no metadata/rail test nodes, and the action header exposed underlying actions rather than a dedicated accessible exit action.

### GREEN implementation and verification

- `PlaylistEntryRow` now measures its own available width with `BoxWithConstraints`. At less than 520dp it keeps the drag handle, artwork, title/artist/album, and duration on the information row, then places the three 44dp mutation controls in a right-aligned second control rail. At 520dp and above, the previous desktop-quality inline controls remain.
- The edit-mode header now replaces Add tracks/Rename/Delete with a single full-width, 44dp **Exit playlist editing** action. This removes underlying action semantics entirely in edit mode, gives assistive technology a truthful action, and consumes its first semantic or pointer activation by exiting edit mode. The actual header actions return on recomposition.
- Physical-pointer coverage was retained by tapping the stable action-header container for the edit exit, then tapping Add tracks after it returns.

Verification command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --tests 'com.eterocell.rhythhaus.library.ui.Task3ReviewSemanticsJvmTest' --configuration-cache --rerun-tasks
```

Exact result: `BUILD SUCCESSFUL in 10s`; `64 tests completed` with zero failures.

```text
./gradlew :shared:compileKotlinJvm --configuration-cache
git diff --check
```

Exact result: compilation `BUILD SUCCESSFUL in 355ms`; whitespace validation exited 0 with no output.
