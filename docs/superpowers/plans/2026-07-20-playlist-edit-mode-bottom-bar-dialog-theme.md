# Playlist Edit Mode, Bottom Bar, and Dialog Theme Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Change Saved playlist detail from always-editable rows to playback-first rows, add page-wide long-press editing with accessible duplicate-safe mutations, connect the real playlist list to the shell-owned Bottom Bar policy, and fix both Haus dialog variants to use a genuinely dark active-palette exterior in dark mode.

**Architecture:** Keep implementation in shared common Compose code and common/JVM tests. Extend the existing `LibraryBackDecision` rather than introducing a playlist-specific back reducer. `LibraryAppShell` owns modal dismissal/edit-mode registration and executes all visual/system/predictive Back decisions. It stores only `playlistEditModeClear: (() -> Unit)?` plus owner identity; active state is `playlistEditModeClear != null`, never a mirrored Boolean. `PlaylistDetailScreen` owns transient edit/modal state but registers an owner-safe clear callback with the shell. Split or parameterize the existing `PlaylistScreenFrame` so the hub and detail callers remain covered while detail supplies the observable `LazyListState`. Reuse `PlaylistEntry.id`, `savedPlaylistPlaybackRequest`, `LibraryScrollPosition`, `activeBottomBarClearancePx`, and one `hausDialogPresentation` policy.

## Global constraints

- This revision owns only `openspec/changes/playlist-edit-mode-bottom-bar-dialog-theme/tasks.md` and this plan. Do not edit source, tests, proposal/design/specs, `progress.md`, or `roadmap.md` while drafting.
- One active change: `playlist-edit-mode-bottom-bar-dialog-theme`; no dependency, schema/migration, platform-media, route-definition, or toolchain changes.
- Real behavioral TDD is mandatory. Pure common policies supplement but do not replace first RED JVM Compose interaction tests using the existing `v2.runComposeUiTest` harness and current semantics/interaction patterns.
- Default playlist rows must expose artwork, title, artist/album metadata, duration, and click-to-play; mutation controls are absent while edit mode is inactive.
- Long press enters one page-wide transient edit mode. Edit-mode row taps do not play. Move/remove controls are labeled and independently usable; every move/remove target is at least 44dp unless an existing shared component is proven to provide that effective semantics bounds.
- `PlaylistEntry.id` is the identity for Compose keys, playback occurrence selection, drag targets, reorder/remove callbacks, confirmations, and stale-result handling. Duplicate `trackId` values must remain independent.
- Back precedence is exactly modal overlay -> playlist edit -> selection -> Now Playing -> route pop -> none. One shell-owned `LibraryBackDecision` is used by system Back, predictive Back, and the visual playlist toolbar/back button.
- Local playlist rename/delete/remove dialogs dismiss before edit mode. The shell must know the active modal dismissal callback through the same authoritative registration path.
- Outside-tap behavior has an explicit hit-test layout: toolbar/back and action header are outside the editable list; blank space within the list viewport and row controls are inside it. A tap outside exits and consumes without invoking the underlying action; controls remain interactive.
- Playlist detail reports the real `LazyListState` through the existing shell callback and appends the shell's measured active Bottom Bar clearance. No fixed footer, duplicate bar, copied threshold, or playlist-only visibility policy.
- Dark dialog exterior must not use `DarkHausPalette.ink.copy(alpha = .20f)`: that is the defect because dark `ink` is light. Use and test an exact dark active-palette-derived surface value (for example `DarkHausPalette.paper.copy(alpha = .72f)`), while retaining light `ink.copy(alpha = .36f)` if the current light contract remains correct. Do not use luminance as the acceptance test.

## Exact files and current symbols

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt`: `PlaylistHubScreen` line 514, `PlaylistDetailScreen` line 890, `PlaylistScreenFrame` line 1115, `PlaylistEntryRow` line 1173, local `PlaylistNameDialog`, `ConfirmationDialog`, and current 40dp move/remove controls.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistPresentationPolicy.kt`: current `PlaylistScreenLayoutPolicy` and tab policy; add pure row-mode policy here if appropriate.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt`: current `LibraryBackDecision` and `libraryBackDecision` at lines 136-152; current shared scroll positions and `decideNowPlayingBarVisibilityForLibraryScroll` at lines 216-235; current `activeBottomBarClearancePx` at lines 97-104.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`: `LibraryRouteContent` parameters and `PlaylistDetailScreen` call at lines 295-349; add only shell registration/Back callback forwarding.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`: shell `NavigationBackHandler` at lines 169-190, predictive offset at lines 192-205, measured clearance at lines 131-140, route callback wiring at lines 268-303, and fixed single Bottom Bar at lines 445-499.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/HausDialog.kt`: `hausDialogPresentation` lines 43-51, `HausDialog` lines 54-111, `HausLazyDialog` lines 114-159.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreensTest.kt`: existing duplicate-safe movement/playback tests at lines 87-120 and boundary availability at lines 95-102.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt`: existing navigation/back/scroll policy tests.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/BottomBarModeTest.kt`: existing measured-clearance/stale-measurement tests at lines 97-119; include this class in final focused verification.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/HausDialogTest.kt`: current dark test incorrectly expects a light-looking dark scrim at lines 13-21; replace that assertion.
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/ui/HausDialogSemanticsJvmTest.kt`: actual `v2.runComposeUiTest`/`SemanticsActions.Dismiss` pattern to follow.
- New JVM interaction test: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistEditModeSemanticsJvmTest.kt`.

---

### Task 1: First RED JVM Compose behavior and playback-first rows

**Interfaces:** `PlaylistDetailScreen` renders actual semantics and calls `onPlayEntry`; common policy remains a projection only. Existing `PlaylistEntry.id` helpers remain unchanged.

- [ ] **Step 1: Write the first failing JVM Compose test before production policy changes.** Use the existing imports/pattern from `SearchSelectionPoliciesJvmTest.kt`:

```kotlin
@OptIn(ExperimentalTestApi::class)
@Test
fun defaultPlaylistRowMatchesTrackContentAndHidesMutationActions() = runComposeUiTest {
    var playCount = 0
    var selectedOccurrence: String? = null
    setContent {
        PlaylistDetailScreen(
            playlist = playlist("playlist-1", "Saved"),
            entries = listOf(entry("entry-a", "track-a", 0)),
            libraryTracks = listOf(libraryTrack("track-a", title = "Song A", artist = "Artist A", album = "Album A")),
            state = PlaylistState(),
            onBack = {}, onRetry = {}, onRename = { _, _ -> }, onDelete = {}, onOpenBrowser = {},
            onPlayEntry = { playCount++; selectedOccurrence = it.selectedOccurrenceId },
            onRemoveEntry = {}, onReorder = {}, bottomContentPadding = 0.dp,
        )
    }
    onNode(hasContentDescription("Song A, Artist A, Album A, 03:12")).assertExists().performClick()
    assertEquals(1, playCount)
    assertEquals("entry-a", selectedOccurrence)
    onNode(hasContentDescription("Move up Song A")).assertDoesNotExist()
    onNode(hasContentDescription("Move down Song A")).assertDoesNotExist()
    onNode(hasContentDescription("Remove Song A")).assertDoesNotExist()
}
```

Use the project's actual fixture constructors/resources and duration formatter; the required assertions are exact content/hidden mutation semantics, not the illustrative labels. RED must fail against current always-editable rows.
- [ ] **Step 2: Add common policy RED tests** to `PlaylistScreensTest.kt` for default empty actions, edit action availability, duplicate exact occurrence selection, first/middle/last boundaries, and drag target clamping. Do not duplicate the already-GREEN tests unnecessarily.
- [ ] **Step 3: Run RED:**

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --configuration-cache
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --configuration-cache
```

- [ ] **Step 4: Implement `PlaylistDetailRowMode`/action policy and refactor `PlaylistEntryRow` to default playback content. Preserve `items(..., key = { it.entry.id })` and `savedPlaylistPlaybackRequest(..., row.entry.id)`. Add a stable content description containing the exact rendered duration.
- [ ] **Step 5: Run GREEN and commit:** `feat: add playlist playback-first row policy`.

---

### Task 2: Extend the one shell-owned Back decision

**Interfaces:** Extend `LibraryBackDecision`; `LibraryAppShell` owns registration/dispatch; `LibraryRoutes` forwards callbacks; detail registers local modal/edit state. No parallel `PlaylistEditBackDecision` or route-local BackHandler.

- [ ] **Step 1: Add RED common tests** in `LibraryNavigationTest.kt` for:

```kotlin
assertEquals(LibraryBackDecision.DismissPlaylistModal, libraryBackDecision(true, true, selected, true, true))
assertEquals(LibraryBackDecision.ExitPlaylistEditMode, libraryBackDecision(false, true, selected, true, true))
assertEquals(LibraryBackDecision.CancelSelection, libraryBackDecision(false, false, selected, true, true))
assertEquals(LibraryBackDecision.HideNowPlaying, libraryBackDecision(false, false, emptySelection, true, true))
assertEquals(LibraryBackDecision.PopRoute, libraryBackDecision(false, false, emptySelection, false, true))
```

Use the actual `TrackSelectionState` fixture and final parameter order; include `None`.
- [ ] **Step 2: Implement the existing enum/function extension** in `LibraryNavigation.kt` with precedence modal, edit, selection, Now Playing, pop. Keep all existing non-playlist callers passing explicit false values or use named/default arguments without changing their behavior.
- [ ] **Step 3: Add authoritative owner-safe shell registration** in `LibraryAppShell.kt`; do not use a mirrored Boolean or an unregistered route-local clear function:

```kotlin
var playlistEditModeClear: (() -> Unit)? by remember { mutableStateOf(null) }
var playlistEditModeOwner: Any? by remember { mutableStateOf(null) }
var playlistModalDismiss by remember { mutableStateOf<(() -> Unit)?>(null) }
val requestLibraryBack = {
    when (val decision = libraryBackDecision(
        hasPlaylistModal = playlistModalDismiss != null,
        isPlaylistEditModeActive = playlistEditModeClear != null,
        selectionState = trackSelectionState,
        isNowPlayingExpanded = appState.showNowPlaying,
        canPopRoute = appState.navigation.canPop,
    )) {
        LibraryBackDecision.DismissPlaylistModal -> playlistModalDismiss?.invoke()
        LibraryBackDecision.ExitPlaylistEditMode -> playlistEditModeClear?.invoke()
        LibraryBackDecision.CancelSelection -> dispatchTrackSelection(TrackSelectionAction.Cancel)
        LibraryBackDecision.HideNowPlaying -> appState.hideNowPlaying()
        LibraryBackDecision.PopRoute -> popRoute()
        LibraryBackDecision.None -> Unit
    }
}

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
```

Pass `requestLibraryBack`, `registerPlaylistEditMode`, and `registerPlaylistModalDismiss` through `LibraryRouteContent` to `PlaylistDetailScreen`. The detail creates `val editOwner = remember(playlist.id) { Any() }`, registers only while edit mode is active, and unregisters when clear changes `editMode` to false and from `DisposableEffect` disposal:

```kotlin
var unregisterEdit by remember(editOwner) { mutableStateOf<(() -> Unit)?>(null) }
LaunchedEffect(editMode, editOwner) {
    unregisterEdit?.invoke()
    unregisterEdit = null
    if (editMode) unregisterEdit = registerPlaylistEditMode(editOwner) { editMode = false }
}
DisposableEffect(editOwner) { onDispose { unregisterEdit?.invoke() } }
```

Use `rememberUpdatedState` or a stable callback object if recomposition would otherwise change disposer identity. The owner/disposer identity guard is mandatory because an outgoing `AnimatedContent` or predictive composition can dispose after a newer route registers; stale disposal must not clear the newer callback.
- [ ] **Step 4: Replace direct playlist frame `onBack` with `requestLibraryBack`; hub uses the same callback with no active registrations. Local rename/delete/remove dialogs register dismiss closures and therefore dismiss before edit mode.
- [ ] **Step 5: Wire `NavigationBackHandler` to the same decision. `isBackEnabled` includes modal/edit/selection/Now Playing/pop. Predictive progress/offset and `completePredictivePop` are active only when the current decision is `PopRoute`; modal dismissal/edit clearing leaves offset zero and never calls route pop.
- [ ] **Step 6: Add JVM RED interaction tests for actual modal -> edit -> system Back and toolbar Back. Assert first closes the local dialog, second invokes the current shell callback exactly once, and route behavior progresses only after the screen state actually clears. Add a registration race test: owner A registers, owner B replaces it, A's stale disposer runs, B remains active; shell Back invokes B once. Assert no route/pop/play callback during state-only branches. Run focused RED/GREEN and commit `feat: add playlist edit back precedence`.

---

### Task 3: Explicit outside boundary, page-wide long press, and accessible controls

- [ ] **Step 1: Split/parameterize `PlaylistScreenFrame` for both callers. Current hub call is `PlaylistScreens.kt:530`; current detail call is `PlaylistScreens.kt:915`. Use optional caller-provided `LazyListState` and separate toolbar/header/list slots, or create `PlaylistHubFrame` and `PlaylistDetailFrame` wrappers over shared chrome. Do not silently change hub list behavior while making detail observable.
- [ ] **Step 2: Adopt this exact hit-test policy in the detail layout:** toolbar/back and action header are outside the editable `LazyColumn`; blank viewport space inside the `LazyColumn` and all row controls are inside; outside toolbar/header taps clear and consume while edit mode is active; action-header callbacks must not run on the consuming tap; after edit exits, the next action-header tap works normally.
- [ ] **Step 3: Implement sibling-region consumption, not a parent pointer detector. Put an edit-active transparent consuming layer only over toolbar/header sibling bounds, behind no list controls; leave the `LazyColumn` unobstructed. The visual toolbar Back remains a semantic Back request and calls the shell-owned decision, not the outside pointer callback.
- [ ] **Step 4: Add JVM interaction RED tests with `performTouchInput { click() }` for toolbar, action header, blank list area, row controls, and normal header action. Assert outside consumes without invoking action/navigation, controls invoke once, and blank list taps do not accidentally dismiss through a competing parent detector.
- [ ] **Step 5: Add `var editMode by remember(playlist.id) { mutableStateOf(false) }`; attach `hausCombinedClickable` to the playback surface with default click -> play, long click -> `editMode = true`, edit click -> consume. Use Task 2's owner-token `LaunchedEffect(editMode, editOwner)` registration to install/remove the shell clear callback; the callback itself sets `editMode = false`.
- [ ] **Step 6: Render drag/move-up/move-down/remove only in edit mode. Keep localized descriptions and exact entry IDs. Raise current move/remove `IconButton` `minWidth`/`minHeight` from 40dp to 44dp; retain disabled boundary semantics and confirmation.
- [ ] **Step 7: Add JVM interaction RED/GREEN coverage for long-click page-wide controls, edit click no-play, controls exactly once, duplicate move/remove targeting, and outside consumption. Run `PlaylistEditModeSemanticsJvmTest` plus `PlaylistScreensTest`; commit `feat: add playlist long-press edit controls`.

---

### Task 4: Real playlist scroll reporting and measured clearance

- [ ] **Step 1: Add JVM RED interaction test** that supplies/observes the detail `LazyListState`, scrolls the actual `LazyColumn`, waits idle, and asserts `onScrollPositionChanged` receives `listState.toLibraryScrollPosition()`.
- [ ] **Step 2: Add JVM RED semantics/layout test** proving the final item uses supplied `bottomContentPadding` and no local footer/bar exists. Use `BottomBarModeTest.kt`'s `activeBottomBarClearancePx` as the expected shell value.
- [ ] **Step 3: Add `onScrollPositionChanged` and real `rememberLazyListState()` to detail, or pass the state through the detail frame. Use:

```kotlin
LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
    onScrollPositionChanged(listState.toLibraryScrollPosition())
}
```

- [ ] **Step 4: Pass the state to the detail `LazyColumn`; preserve final `item(key = "spacer") { Spacer(Modifier.height(bottomContentPadding)) }`. Forward `LibraryAppShell`'s existing measured clearance through `LibraryRoutes.kt`; do not change Bottom Bar ownership.
- [ ] **Step 5: Cover stale/matching/zero measurement in existing `BottomBarModeTest.kt`; use `LibraryNavigationTest.kt` for shared reducer jitter/down/up behavior. Run focused tests and `:desktopApp:compileKotlin`; commit `feat: connect playlist scroll to bottom bar chrome`.

---

### Task 5: Fix the shared dark dialog exterior policy

- [ ] **Step 1: Change the existing common test `HausDialogTest.darkDialogPresentationUsesSolidPanelAndLightScrim` into an exact dark-policy test. The current assertion `presentation.scrimColor.luminance() > DarkHausPalette.paper.luminance()` is invalid and must be removed.
- [ ] **Step 2: Assert the active dark exterior exactly, for example:

```kotlin
assertEquals(DarkHausPalette.panel, dark.panelColor)
assertEquals(DarkHausPalette.paper.copy(alpha = 0.72f), dark.scrimColor)
assertEquals(LightHausPalette.ink.copy(alpha = 0.36f), light.scrimColor)
```

The chosen dark value must be a named production policy constant derived from dark `paper`/surface, not dark `ink`; test both palette variants by exact equality. Do not use RGB luminance.
- [ ] **Step 3: Fix `hausDialogPresentation` in `HausDialog.kt` once. Both `HausDialog` and `HausLazyDialog` must consume that same returned `panelColor`/`scrimColor`; do not add a variant-specific conditional. Run `HausDialogTest` and existing `HausDialogSemanticsJvmTest`; commit `fix: apply theme-aware dialog exterior policy`.

---

### Task 6: Selected verification, review, and implementation-lane evidence

- [ ] **Step 1:** `openspec validate playlist-edit-mode-bottom-bar-dialog-theme --strict`; expected exact valid output.
- [ ] **Step 2:** Run focused tests including `BottomBarModeTest`:

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

Expected: all selected tests pass with zero failures/errors/skips.
- [ ] **Step 3:** `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`; expected `BUILD SUCCESSFUL`.
- [ ] **Step 4:** `/usr/bin/xcrun xcodebuild -version`; then `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`; record exact blocker if iOS fails and never claim a blocked pass.
- [ ] **Step 5:** Run `git diff --check`, inspect status and final diff, and confirm only the requested implementation/test/evidence files changed in the later lane; no fixed footer/duplicate bar, dependency, schema, platform media, or route-definition changes.
- [ ] **Step 6:** Later implementation owner updates OpenSpec checkboxes, `progress.md`, and `roadmap.md` with exact command output, modal/edit/system/visual Back evidence, interaction-test coverage, duplicate-ID coverage, runtime gaps, and next owner. This plan revision does not edit those files.
- [ ] **Step 7:** Inspect status/diff/log before each task-scoped semantic commit; do not commit these planning artifacts in this lane.
