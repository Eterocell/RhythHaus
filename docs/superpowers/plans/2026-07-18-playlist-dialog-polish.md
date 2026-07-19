# Playlist Dialog Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Align playlist-hub spacing and controls with Library home, then migrate every existing modal to one solid, accessible shared dialog shell.

**Architecture:** Add a common-main `HausDialog` plus pure presentation policies for panel/scrim and playlist layout/control metrics. Individual dialog callers retain their state, copy, validation, selections, callbacks, and destructive coloring; settings and playlist screens only replace their duplicated shell/layout wiring with those shared primitives.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Miuix KMP, Kotlin test, Gradle.

## Global Constraints

- Keep all production UI shared under `shared/src/commonMain/kotlin/com/eterocell/rhythhaus`.
- Do not add production dependencies, palette tokens, routes, string resources, persistence changes, or playback behavior. A JVM-test-only Compose UI-test/desktop-host dependency is permitted solely for the executable dismiss-semantics regression.
- Use a solid `HausColors.current.panel` dialog panel; do not use `rhythHausLiquidGlass`, transparent panel colors, or `LayerBackdrop` for migrated dialogs.
- In dark theme, derive a visibly light dialog scrim from the active palette; light theme retains restrained ink dimming.
- Preserve every existing dialog callback, dismissal behavior, semantics, validation/error state, selection state, and localized copy.
- Preserve playlist Now Playing bottom clearance and its back affordance.
- Do not commit; the user has not requested one.

---

### Task 1: Define Presentation Contracts

**Files:**
- Create: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/HausDialogTest.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreensTest.kt`

**Interfaces:**
- Produces: pure `HausDialogPresentation`/`hausDialogPresentation` and `PlaylistScreenLayoutPolicy`/`playlistTabPresentation` contracts that later UI code consumes.
- Consumes: `LightHausPalette`, `DarkHausPalette`, and Library home inset constants/policy.

- [ ] **Step 1: Write failing dialog presentation tests**

```kotlin
@Test
fun darkDialogPresentationUsesSolidPanelAndLightScrim() {
    val presentation = hausDialogPresentation(DarkHausPalette)

    assertEquals(DarkHausPalette.panel, presentation.panelColor)
    assertTrue(presentation.scrimColor.luminance() > DarkHausPalette.paper.luminance())
    assertEquals(24.dp, presentation.outerPadding)
    assertEquals(24.dp, presentation.cornerRadius)
}
```

Add paired light-palette and bounded-content/action-metric assertions. Keep tests Compose-free by asserting pure values only.

- [ ] **Step 2: Write failing playlist policy tests**

```kotlin
@Test
fun playlistLayoutMatchesLibraryHomeHorizontalInsetAndHasNoExtraTopInset() {
    assertEquals(20.dp, PlaylistScreenLayoutPolicy.horizontalPadding)
    assertEquals(0.dp, PlaylistScreenLayoutPolicy.additionalTopPadding)
}

@Test
fun lightPlaylistTabsUseContrastingExplicitColors() {
    val presentation = playlistTabPresentation(PlaylistTab.Saved, LightHausPalette)

    assertTrue(presentation.selectedContentColor != presentation.selectedContainerColor)
    assertTrue(presentation.unselectedContentColor != presentation.unselectedContainerColor)
}
```

Add an assertion for compact action/tab height, inside vertical margin, and line-height policy sufficient for full descender rendering.

- [ ] **Step 3: Run the focused RED tests**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ui.HausDialogTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --configuration-cache`

Expected: test compilation fails because the named presentation APIs do not yet exist.

### Task 2: Implement the Shared HausDialog Foundation

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/HausDialog.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/HausDialogTest.kt`

**Interfaces:**
- Produces: `internal data class HausDialogPresentation`, `internal fun hausDialogPresentation(palette: HausColorPalette)`, and an internal slot-based `@Composable HausDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit)`.
- Consumes: active `HausColors`, Compose semantics, Miuix `Card`, and caller content slots.

- [ ] **Step 1: Add the minimum policy implementation**

```kotlin
internal data class HausDialogPresentation(
    val panelColor: Color,
    val scrimColor: Color,
    val outerPadding: Dp = 24.dp,
    val cornerRadius: Dp = 24.dp,
    val maxPanelHeight: Dp = 480.dp,
)

internal fun hausDialogPresentation(palette: HausColorPalette): HausDialogPresentation =
    HausDialogPresentation(
        panelColor = palette.panel,
        scrimColor = if (palette.paper.luminance() < palette.ink.luminance()) {
            palette.paper.copy(alpha = 0.20f)
        } else {
            palette.ink.copy(alpha = 0.36f)
        },
    )
```

Use the actual project theme distinction if it has a safer existing discriminator; do not add a palette field.

- [ ] **Step 2: Add the shared composable shell**

Implement a full-size scrim with `dialog()` semantics, `paneTitle = title`, and an explicit `dismiss` action. Center an opaque panel with 24dp outer padding/corners, consume panel taps, and make the body scrollable inside a `heightIn(max = maxPanelHeight)` region. Do not put caller actions inside the scrolling body when that would make them unreachable.

- [ ] **Step 3: Run the focused GREEN tests**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ui.HausDialogTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --configuration-cache`

Expected: PASS.

### Task 3: Migrate Settings Dialogs

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDialogs.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt` if it only forwards `clearLibraryDialogBackdrop`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibrarySourceManagementTest.kt`

**Interfaces:**
- Consumes: `HausDialog` from Task 2.
- Preserves: `sourceDialogName`, `onClearLibrary`, `onRemoveSource`, `mutationsEnabled`, all strings, and existing action order.

- [ ] **Step 1: Lock existing source-dialog semantics**

Keep or add tests proving a long source name has an ellipsized visual form and complete accessibility form, and keep mutation gate coverage unchanged. Do not add Compose-host tests for behavior already captured by pure source helpers.

- [ ] **Step 2: Replace Clear Library’s shell**

Change `AnimatedClearLibraryDialogRoute` to place its existing title, body text, and cancel/clear row in `HausDialog`. Remove `backdrop: LayerBackdrop?` and all `rhythHausLiquidGlass`/transparent-card code. Preserve the current order: invoke `onClearLibrary`, then clear the visible dialog state in `SettingsScreen`.

- [ ] **Step 3: Replace Remove Folder’s shell and remove only dialog backdrop plumbing**

Put the existing source-name and message body plus Cancel/Remove actions in `HausDialog`. Retain `clearAndSetSemantics` for the full source accessibility name and the `mutationsEnabled` action gate. Remove `clearLibraryDialogBackdrop` from `SettingsScreen` and delete only callers/route parameters whose sole purpose was dialog glass; keep non-dialog backdrop recording untouched.

- [ ] **Step 4: Run source-management coverage**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache`

Expected: PASS.

### Task 4: Migrate Playlist Dialogs and Apply Playlist Policies

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreensTest.kt`

**Interfaces:**
- Consumes: `HausDialog`, `HausDialogPresentation`, `PlaylistScreenLayoutPolicy`, and `playlistTabPresentation`.
- Preserves: `PlaylistNameDraft`, `PlaylistPickerState`, `PlaylistModalNotice`, all route callbacks, mutation outcomes, and `NowPlayingBarContentPadding`.

- [ ] **Step 1: Add playlist policy implementations**

```kotlin
internal data class PlaylistScreenLayoutPolicy(
    val horizontalPadding: Dp = 20.dp,
    val additionalTopPadding: Dp = 0.dp,
    val itemSpacing: Dp = 18.dp,
)

internal fun playlistTabPresentation(tab: PlaylistTab, palette: HausColorPalette): PlaylistTabPresentation =
    // Return explicit selected and unselected container/content colors plus text-safe metrics.
```

Use palette foreground/background pairs that remain distinct in both provided palettes. Define the compact action metrics once and use them for every playlist dialog/notice button.

- [ ] **Step 2: Replace every playlist modal shell**

Replace `ModalCard` usage for create/rename, picker/inline creation, saved-playlist destructive confirmations, and Clear Upcoming confirmation with `HausDialog`. Delete `ModalCard` after the final call site moves. Preserve every existing `onDismiss`, `onConfirm`, mutation error notice, text draft, picker selection, and resource string.

- [ ] **Step 3: Apply frame and button policy**

Remove `safeContentPadding()` from the playlist frame, use the shared 20dp horizontal/system-top policy and 18dp rhythm, preserve its back control, and keep the existing bottom spacer. Pass explicit Miuix container/content colors to both Saved/Queue tabs and compact actions. Set stable line height and vertical inside margins so `g`, `y`, and localized glyphs fit inside the fixed button bounds.

- [ ] **Step 4: Run focused playlist tests**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistStateTest' --configuration-cache`

Expected: PASS.

### Task 5: Verify, Review, and Record Evidence

**Files:**
- Modify: `openspec/changes/playlist-dialog-polish/tasks.md`
- Modify: `progress.md`
- Modify: `roadmap.md`

**Interfaces:**
- Consumes: all completed implementation tasks and their test evidence.
- Produces: completed OpenSpec checkboxes and durable verification/manual-QA record.

- [ ] **Step 1: Run strict and platform verification**

```bash
openspec validate playlist-dialog-polish --strict
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
GIT_MASTER=1 git diff --check
```

Record the command outputs exactly. If iOS cannot pass, record its exact blocker and do not claim an iOS pass.

- [ ] **Step 2: Perform visual QA where runnable**

Run the desktop app when available and inspect compact/wide layouts and light/dark themes. Exercise every migrated dialog, long localized labels, keyboard focus/submit, scrim color, panel opacity, text fit, and Now Playing clearance. Record unavailable runtime/device cases as manual gaps rather than passes.

- [ ] **Step 3: Update durable artifacts and conduct final review**

Mark only evidenced OpenSpec tasks complete. Update `progress.md` and `roadmap.md` with route, files, exact verification, manual-QA limits, and next safe action. Use subagent-driven task review after each task and a final whole-change review before completion. Do not commit.

### Task 6: Verify Accessible Dismiss Semantics

**Files:**
- Modify: `shared/build.gradle.kts`
- Create: `shared/src/jvmTest/kotlin/com/eterocell/rhythaus/ui/HausDialogSemanticsJvmTest.kt`
- Modify: `openspec/changes/playlist-dialog-polish/tasks.md`

**Interfaces:**
- Consumes: `HausDialog(title, onDismiss, body, actions)` and Compose UI-test `SemanticsActions.Dismiss`.
- Produces: executable JVM coverage proving the existing semantic dismiss action invokes the callback and removes the dialog.

- [ ] **Step 1: Add a failing JVM UI test**

```kotlin
@OptIn(ExperimentalTestApi::class)
@Test
fun dismissSemanticsInvokesTheExistingDismissCallback() = runComposeUiTest {
    var visible by mutableStateOf(true)
    setContent {
        if (visible) HausDialog("Dialog", { visible = false }, body = {}, actions = {})
    }
    onNode(hasKey(SemanticsActions.Dismiss)).performSemanticsAction(SemanticsActions.Dismiss)
    waitForIdle()
    assertFalse(visible)
}
```

- [ ] **Step 2: Add the scoped JVM test harness**

In `jvmTest.dependencies`, add `implementation("org.jetbrains.compose.ui:ui-test:1.11.1")` and `implementation(compose.desktop.currentOs)`. Do not add Android/iOS test dependencies or production dependencies.

- [ ] **Step 3: Run the focused RED/GREEN test**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ui.HausDialogSemanticsJvmTest' --configuration-cache`

Expected: RED before the test harness/declaration is complete, then PASS after the minimal test-only configuration and test are present.
