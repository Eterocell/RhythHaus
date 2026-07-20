## 1. Behavioral playlist-row RED coverage and row-mode contract

- [ ] 1.1 Add the first RED JVM Compose interaction test at `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistEditModeSemanticsJvmTest.kt`, using the existing `@OptIn(ExperimentalTestApi::class)`, `runComposeUiTest`, `setContent`, `SemanticsMatcher`, `performClick`, `performTouchInput`, and `waitForIdle` pattern from `SearchSelectionPoliciesJvmTest.kt`, `TrackSelectionSemanticsJvmTest.kt`, and `Task3ReviewSemanticsJvmTest.kt`.
- [ ] 1.2 In that test, render the actual internal `PlaylistDetailScreen` with two `PlaylistEntry` rows (including two entries sharing one `trackId`) and counters for `onPlayEntry`, `onReorder`, and `onRemoveEntry`. RED assertions must cover: default row exposes artwork/title/artist/album/duration content semantics, default row has no move/drag/remove descriptions, default row click invokes playback once for the exact `PlaylistEntry.id`, and duplicate track references remain distinct.
- [ ] 1.3 Add common pure assertions to the existing `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreensTest.kt` only for policy projections not expressible through Compose semantics: `PlaylistDetailRowMode`, action availability, `playlistMoveAvailability`, `movedPlaylistEntryIds`, `playlistDragTargetIndex`, and exact occurrence selection. Preserve the already-GREEN duplicate playback and boundary tests.
- [ ] 1.4 Add the smallest pure policy in `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistPresentationPolicy.kt` (or package-local `PlaylistScreens.kt` symbols if that is the existing convention): `PlaylistDetailRowMode`, `PlaylistDetailRowAction`, `PlaylistDetailRowPresentation`, and a click activation projection. Default mode returns no mutation actions; edit mode returns drag/remove plus only applicable move actions.
- [ ] 1.5 Implement the default row using the existing `LazyTrackArtworkImage`, title/artist/album metadata, and the repository's existing duration formatter. Put one stable row semantics description on the playback surface containing title, artist/album metadata, and formatted duration; do not expose mutation semantics outside edit mode.
- [ ] 1.6 Keep `items(model.rows, key = { it.entry.id })`, `savedPlaylistPlaybackRequest(entries, tracksById, row.entry.id)`, `PlaylistDragPresentation(entryIds, row.entry.id)`, `movedPlaylistEntryIds`, remove confirmation, and all mutation callbacks entry-ID-based. Never replace an occurrence ID with `trackId` or a mutable row index.
- [ ] 1.7 Run RED before implementation and GREEN after implementation:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --configuration-cache
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --configuration-cache
```

Expected RED: missing mode/interaction behavior, not a guessed policy-only failure. Expected GREEN: actual row semantics and playback callback pass, including duplicate entry selection.
- [ ] 1.8 Commit only this group as `feat: add playlist playback-first row policy`.

## 2. Single shell-owned Back decision and modal/edit precedence

- [ ] 2.1 Extend the existing `LibraryBackDecision` in `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt`; do not create `PlaylistEditBackDecision`. Add explicit branches in this order: `DismissPlaylistModal`, `ExitPlaylistEditMode`, `CancelSelection`, `HideNowPlaying`, `PopRoute`, `None`.
- [ ] 2.2 Change `libraryBackDecision(...)` to accept authoritative booleans/callback state for `hasPlaylistModal`, `isPlaylistEditModeActive`, selection, Now Playing, and route-pop eligibility. Add common tests in `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt` for all six precedence steps, especially modal -> edit -> selection -> Now Playing -> route pop.
- [ ] 2.3 Make `LibraryAppShell.kt` the sole owner/registrant. Do not mirror edit state with a Boolean. Store only the current owner's clear callback and derive active state from callback presence:

```kotlin
var playlistEditModeClear: (() -> Unit)? by remember { mutableStateOf(null) }
var playlistEditModeOwner: Any? by remember { mutableStateOf(null) }
var playlistModalDismiss by remember { mutableStateOf<(() -> Unit)?>(null) }
val hasPlaylistModal = playlistModalDismiss != null
fun registerPlaylistEditMode(owner: Any, clear: () -> Unit): () -> Unit {
    playlistEditModeOwner = owner
    playlistEditModeClear = clear
    return {
        if (playlistEditModeOwner === owner && playlistEditModeClear === clear) {
            playlistEditModeOwner = null
            playlistEditModeClear = null
        }
    }
}
fun registerPlaylistModalDismiss(dismiss: (() -> Unit)?) {
    playlistModalDismiss = dismiss
}
```

Pass registration callbacks through the existing `LibraryRouteContent` parameters in `LibraryRoutes.kt` to `PlaylistDetailScreen`; do not add a second BackHandler or a route-local navigation decision. Use a stable `remember(playlist.id) { Any() }` owner token. Registration is installed only while edit mode is active, and its disposer runs when edit mode clears and on `DisposableEffect` disposal. The identity guard prevents an outgoing `AnimatedContent`/predictive composition from unregistering a newer route's callback.
- [ ] 2.4 In `PlaylistDetailScreen`, register the current modal dismiss action whenever `renameDraft != null`, `deleteConfirmation`, or `removeConfirmation != null`; unregister with `DisposableEffect` when no modal is active or the route leaves. The registered action must close/reset that dialog state before returning. The same registration is used for rename, playlist delete, and local remove confirmation.
- [ ] 2.5 Register edit state changes through the shell callback. The detail screen's state remains transient and local to the screen, but the shell's authoritative active state is `playlistEditModeClear != null`. Use:

```kotlin
val editOwner = remember(playlist.id) { Any() }
var unregisterEdit by remember(editOwner) { mutableStateOf<(() -> Unit)?>(null) }
LaunchedEffect(editMode, editOwner) {
    unregisterEdit?.invoke()
    unregisterEdit = null
    if (editMode) {
        unregisterEdit = registerPlaylistEditMode(editOwner) { editMode = false }
    }
}
DisposableEffect(editOwner) { onDispose { unregisterEdit?.invoke() } }
```

Use `rememberUpdatedState` or a stable callback object if needed so disposer identity remains valid. The clear callback must synchronously set `editMode = false`; the effect then unregisters it. The shell does not separately mutate or mirror detail state.
- [ ] 2.6 Route the visual playlist toolbar/back button through the same shell-owned request, not directly to `popRoute`. The callback must invoke `libraryBackDecision` and dispatch exactly one branch. This covers `PlaylistScreenFrame`'s back button for both hub and detail; hub passes no active modal/edit registration.
- [ ] 2.7 Update `LibraryAppShell`'s `NavigationBackHandler`:

```kotlin
val backDecision = libraryBackDecision(
    hasPlaylistModal = hasPlaylistModal,
    isPlaylistEditModeActive = playlistEditModeClear != null,
    selectionState = trackSelectionState,
    isNowPlayingExpanded = appState.showNowPlaying,
    canPopRoute = appState.navigation.canPop,
)
NavigationBackHandler(
    isBackEnabled = backDecision != LibraryBackDecision.None,
    onBackCompleted = {
        when (backDecision) {
            LibraryBackDecision.DismissPlaylistModal -> playlistModalDismiss?.invoke()
            LibraryBackDecision.ExitPlaylistEditMode -> playlistEditModeClear?.invoke()
            LibraryBackDecision.CancelSelection -> dispatchTrackSelection(TrackSelectionAction.Cancel)
            LibraryBackDecision.HideNowPlaying -> appState.hideNowPlaying()
            LibraryBackDecision.PopRoute -> popRouteWithPredictiveCompletion()
            LibraryBackDecision.None -> Unit
        }
    },
)
```

Use the actual existing handler parameters, but preserve this single branch order and one owner.
- [ ] 2.8 Predictive Back must not preview or pop for modal dismissal or edit clearing. Gate the existing `predictiveBackProgress`/`predictiveBackOffset` and completion bookkeeping on `backDecision == LibraryBackDecision.PopRoute`; for all other enabled decisions keep offset zero and do not call `appState.navigation.pop()` or `completePredictivePop`.
- [ ] 2.9 Add first RED JVM Compose interaction tests for modal-before-edit, top visual Back, and system Back. Assert the shell invokes the current owner callback exactly once, stale disposal from owner A cannot clear newer owner B, and route behavior progresses only after the screen state actually clears. First Back closes the local rename/delete/remove dialog; next Back clears edit; later Back cancels selection/hides Now Playing/pops route. Assert no playback/navigation callback occurs during state-only branches.
- [ ] 2.10 Run focused navigation and semantics tests; expected GREEN proves one authoritative decision and no predictive route animation during state-only clearing:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --configuration-cache
```
- [ ] 2.11 Commit as `feat: add playlist edit back precedence`.

## 3. Page-wide long press, explicit outside boundary, and edit controls

- [ ] 3.1 Add RED JVM Compose interaction tests before wiring: invoke the row's `SemanticsActions.OnLongClick` (matching `SearchSelectionPoliciesJvmTest`), assert every visible row now exposes exactly one drag, move-up, move-down, and remove action set, click an edit row and assert `onPlayEntry` remains zero, and invoke one move/remove button to assert its callback fires exactly once.
- [ ] 3.2 Split `PlaylistScreenFrame` without breaking its two current callers at `PlaylistScreens.kt:530` (`PlaylistHubScreen`) and `PlaylistScreens.kt:915` (`PlaylistDetailScreen`). Prefer a caller-provided optional `LazyListState` plus a separate `beforeList`/`content` slot; if that makes the hit-test boundary ambiguous, create `PlaylistHubFrame` and `PlaylistDetailFrame` wrappers over one shared chrome function. The detail caller must own/provide the observable `LazyListState`; the hub caller keeps its current independent list behavior.
- [ ] 3.3 Define the exact detail layout: toolbar/back is outside the editable list; the action header is outside the editable row list but remains normally tappable; the `LazyColumn` is the editable list hit region; blank space inside the list viewport is part of the editable-list region; row controls are descendants inside that region. A first tap on toolbar, action header, or any other region outside the list while edit mode is active clears/consumes and must not invoke the back/action callback. A toolbar/back invocation is an explicit semantic Back request and uses the shell decision from Task 2, not the outside pointer path.
- [ ] 3.4 Implement that boundary with explicit sibling layout, not an unsafe parent `pointerInput` that competes with children:

```kotlin
Column(Modifier.fillMaxSize()) {
    PlaylistToolbar(onBack = requestLibraryBack)
    if (beforeList != null) {
        Box(
            Modifier
                .fillMaxWidth()
                .then(if (editMode) Modifier.clearAndConsumeOutsideTap { clearEditMode() } else Modifier),
        ) { beforeList() }
    }
    Box(Modifier.weight(1f).fillMaxWidth()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), content = listContent)
    }
}
```

`clearAndConsumeOutsideTap` is a small dedicated modifier/test seam implemented with the project's existing pointer APIs and must be attached only to the toolbar/header sibling, never the parent of the `LazyColumn`; controls therefore remain clickable. If the actual modifier requires a transparent sibling overlay, it must cover only the toolbar/header bounds and be placed above those siblings while edit mode is active.
- [ ] 3.5 Add `var editMode by remember(playlist.id) { mutableStateOf(false) }` in `PlaylistDetailScreen`; `hausCombinedClickable` on the playback row uses default click -> play, long click -> register active/enter edit, edit click -> consume. Do not put the edit-mode clear detector on the row/list parent.
- [ ] 3.6 Render edit controls only when active. Keep localized `playlist_drag_format`, `playlist_move_up_format`, `playlist_move_down_format`, and `playlist_remove_track_format` descriptions. Increase move/remove `IconButton` targets from the current 40dp to at least 44dp (`minWidth = 44.dp`, `minHeight = 44.dp`); preserve 44dp drag target. Do not claim an existing shared component covers the 40dp controls unless the source/test proves its effective semantics bounds are at least 44dp.
- [ ] 3.7 Keep disabled first/last move buttons in the semantics tree with `enabled = availability.canMoveUp/canMoveDown`; drag remains an enhancement and buttons independently reorder/remove. Keep confirmation keyed by the exact `PlaylistEntry.id`.
- [ ] 3.8 Add interaction tests for outside toolbar/header taps, blank list-region taps, control taps, and normal action-header taps. Expected: outside tap exits/consumes once; action is not invoked on the consuming tap; controls remain usable; the next tap after exit can invoke the action.
- [ ] 3.9 Run RED/GREEN:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```
- [ ] 3.10 Commit as `feat: add playlist long-press edit controls`.

## 4. Real playlist LazyListState reporting and measured shell spacer

- [ ] 4.1 Add RED interaction coverage in `PlaylistEditModeSemanticsJvmTest.kt` with a callback list for `onScrollPositionChanged`; render a detail screen with the test list state, scroll it, `waitForIdle`, and assert the callback receives the real `LibraryScrollPosition` from `firstVisibleItemIndex`/`firstVisibleItemScrollOffset`.
- [ ] 4.2 Add a RED interaction/semantics assertion that the final spacer is present with the supplied measured `bottomContentPadding` and that no playlist-local fixed footer/bar is rendered. Use the existing `BottomBarModeTest.kt` pure measurements as the expected `activeBottomBarClearancePx` source, not a new threshold.
- [ ] 4.3 Extend `PlaylistDetailScreen` with `onScrollPositionChanged: (LibraryScrollPosition) -> Unit = {}` and `val listState = rememberLazyListState()` (or receive the caller-provided state from the split frame). Use the existing detail pattern exactly:

```kotlin
LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
    onScrollPositionChanged(listState.toLibraryScrollPosition())
}
```

- [ ] 4.4 Pass that state through the detail frame to its real `LazyColumn`; retain `item(key = "spacer") { Spacer(Modifier.height(bottomContentPadding)) }` as the final item. Forward the existing shell `activeBottomBarClearance` from `LibraryRouteContent` and `LibraryAppShell` unchanged.
- [ ] 4.5 Extend `BottomBarModeTest.kt` only for missing stale/zero/matching measurement cases and keep `LibraryNavigationTest.kt` focused on shared scroll decision behavior. Do not add a playlist-specific reducer, footer, or duplicate bottom bar.
- [ ] 4.6 Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --tests 'com.eterocell.rhythhaus.BottomBarModeTest' --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
./gradlew :desktopApp:compileKotlin --configuration-cache
```

Expected GREEN: real scroll callback, measured final clearance, shared down-hide/up-reveal policy, and both hub/detail frame call sites compile.
- [ ] 4.7 Commit as `feat: connect playlist scroll to bottom bar chrome`.

## 5. Shared genuinely-dark dialog exterior for both variants

- [ ] 5.1 Update the existing common test at `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/HausDialogTest.kt`: replace `darkDialogPresentationUsesSolidPanelAndLightScrim`, which currently asserts the defective light-looking result via luminance, with an exact policy assertion. Do not use RGB luminance as the acceptance condition.
- [ ] 5.2 The RED test must assert:

```kotlin
val dark = hausDialogPresentation(DarkHausPalette)
assertEquals(DarkHausPalette.panel, dark.panelColor)
assertEquals(DarkHausPalette.paper.copy(alpha = 0.72f), dark.scrimColor)
assertEquals(LightHausPalette.ink.copy(alpha = 0.36f), hausDialogPresentation(LightHausPalette).scrimColor)
```

The exact dark alpha may be a separately approved constant, but it must be an explicit dark `DarkHausPalette`-derived value (not `DarkHausPalette.ink.copy(alpha = .20f)`) and the test must compare that exact value. Do not rely on `luminance()` because the old test falsely classified light `DarkHausPalette.ink` as desirable.
- [ ] 5.3 Fix production `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/HausDialog.kt` in `hausDialogPresentation` once, using the active palette's dark `paper`/dark surface derivation for dark mode. Both `HausDialog` at lines 54-111 and `HausLazyDialog` at lines 114-159 must continue to consume the same returned `panelColor` and `scrimColor`; no variant-specific branch is allowed.
- [ ] 5.4 Add/retain JVM semantics coverage in `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/ui/HausDialogSemanticsJvmTest.kt` only for actual composable dismissal; the common exact policy test is the color contract. Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ui.HausDialogTest' --tests 'com.eterocell.rhythhaus.ui.HausDialogSemanticsJvmTest' --configuration-cache
```

Expected RED before policy fix and GREEN after both dialog variants resolve the same genuinely dark active-palette exterior.
- [ ] 5.5 Commit as `fix: apply theme-aware dialog exterior policy`.

## 6. Full selected verification and evidence handoff

- [ ] 6.1 Run strict validation: `openspec validate playlist-edit-mode-bottom-bar-dialog-theme --strict`. Expected: `Change 'playlist-edit-mode-bottom-bar-dialog-theme' is valid`.
- [ ] 6.2 Run the complete focused suite, including the required Bottom Bar test:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' \
  --tests 'com.eterocell.rhythhaus.BottomBarModeTest' \
  --tests 'com.eterocell.rhythhaus.ui.HausDialogTest' \
  --tests 'com.eterocell.rhythhaus.ui.HausDialogSemanticsJvmTest' \
  --configuration-cache
```

Expected: every selected test passes with zero failures/errors/skips.
- [ ] 6.3 Run selected full verification: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`; expected `BUILD SUCCESSFUL`.
- [ ] 6.4 Run `/usr/bin/xcrun xcodebuild -version`; record the reported version. Run `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`; claim pass only on success, otherwise record the exact blocker/output.
- [ ] 6.5 Run `git diff --check`, inspect `git status --short`, and review only the two planning artifacts in this lane. Implementation must remain limited to shared source/common-JVM tests and must not add dependencies, schema/migrations, platform media, route definitions, fixed footers, or duplicate bars.
- [ ] 6.6 In the later implementation lane only, update `openspec/.../tasks.md`, `progress.md`, and `roadmap.md` with exact commands/outcomes, modal/edit/back acceptance, duplicate-ID evidence, runtime/manual gaps, and next owner. This planning revision must not edit `progress.md` or `roadmap.md`.
- [ ] 6.7 Use task-scoped semantic commits after each completed group; inspect `git status`, `git diff`, and `git log --oneline -10` before staging. Do not commit these planning artifacts in this lane.
