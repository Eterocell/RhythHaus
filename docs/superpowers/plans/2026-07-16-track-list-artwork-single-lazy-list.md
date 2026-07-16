# Track List Artwork Single-Lazy-List Replacement Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the failed artwork nested-scroll architecture with one `LazyColumn` that owns artwork and track-row input, restores naturally at item zero, and preserves no-artwork Miuix behavior.

**Architecture:** Artwork-backed pages represent the expanded square as a non-sticky upper image slice followed by a sticky lower toolbar slice. Both slices clip aligned portions of one fixed square image, while collapse progress is read-only state derived from `LazyListState`; there is no mutable collapse offset, nested-scroll adapter, sibling scrollable, or dynamic viewport compensation. Loading, unavailable, failed, and absent artwork remain on the existing Miuix branch.

**Tech Stack:** Kotlin 2.4.10, Compose Multiplatform 1.11.1, Miuix 0.9.3, Kotlin Multiplatform common tests, Gradle.

## Global Constraints

- Treat `docs/superpowers/plans/2026-07-15-track-list-artwork-collapse.md` as superseded historical evidence; do not execute it.
- Use strict RED/GREEN TDD: every production behavior begins with a focused failing test that fails for the expected missing API or behavior.
- One `LazyColumn` and one `LazyListState` are the sole vertical input owner on artwork-backed pages.
- Expanded artwork height equals the available drill-down width; collapsed height equals the system-bar top inset plus 56 dp.
- For a valid range, lazy item zero is the non-sticky upper slice of `expandedHeight - collapsedHeight`; the sticky lower slice has `collapsedHeight`.
- Both slices render aligned clips of one fixed square image placement. They must not independently crop the image.
- A safe-inset back overlay may cover only its button-sized target and must remain at least 44 dp.
- Artwork mode must not use an artwork `NestedScrollConnection`, sibling `Modifier.scrollable`, dynamic top content padding, translated/extended viewport, full-size input overlay, or platform pointer controller.
- No-artwork pages retain their current Miuix scroll behavior, glass chrome, large title, divider, content padding, and safe insets.
- Preserve album/artist routes, lazy artwork loading, row selection, visible-queue playback, scrollbar direct navigation, Now Playing spacing, and scroll-direction reporting.
- Remove the disposable desktop prototype after its evidence has been transferred to production tests and acceptance.
- Do not install dependencies, change toolchains, add Windows/Linux support, or modify unrelated files.
- Do not commit unless the user explicitly requests it.

---

### Task 1: Pure list-position artwork geometry

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapse.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapseTest.kt`

**Interfaces:**
- Preserves: `DrillDownArtwork`, `DrillDownScrollOwner`, and `drillDownScrollOwner(artwork)` so only resolved `TrackArtworkLoadState.Available` selects artwork mode.
- Produces:

```kotlin
internal data class ArtworkCollapseSnapshot(
    val upperSliceHeightPx: Float,
    val lowerSliceHeightPx: Float,
    val lowerSliceImageOffsetPx: Float,
    val progress: Float,
)

internal data class ArtworkCollapseGeometry(
    val expandedHeightPx: Float,
    val collapsedHeightPx: Float,
) {
    val collapseRangePx: Float
    fun snapshot(
        firstVisibleItemIndex: Int,
        firstVisibleItemScrollOffset: Int,
    ): ArtworkCollapseSnapshot
}
```

- Removes: `ArtworkCollapseConsumption`, `ArtworkCollapseState`, `rememberArtworkCollapseState`, `consumeUpward`, `consumeDownward`, `expandFully`, `artworkListTopPaddingPx`, `artworkListVisualOffsetPx`, `artworkListViewportExtensionPx`, `artworkChromeHeightPx`, and `scrollbarTargetsTop`.

- [ ] **Step 1: Replace connection-state tests with failing list-position tests**

Keep the existing artwork/no-artwork branch-selection assertions, then write these exact behavioral cases:

```kotlin
@Test
fun listPositionDerivesExpandedPartialAndCollapsedProgress() {
    assertEquals(
        ArtworkCollapseSnapshot(240f, 80f, -240f, 0f),
        geometry.snapshot(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0),
    )
    assertEquals(
        ArtworkCollapseSnapshot(240f, 80f, -240f, 0.5f),
        geometry.snapshot(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 120),
    )
    assertEquals(
        ArtworkCollapseSnapshot(240f, 80f, -240f, 1f),
        geometry.snapshot(firstVisibleItemIndex = 1, firstVisibleItemScrollOffset = 0),
    )
}

@Test
fun itemZeroOffsetZeroRestoresExpandedArtwork() {
    assertEquals(0f, geometry.snapshot(0, 0).progress)
}

@Test
fun offsetsClampToTheArtworkRange() {
    assertEquals(0f, geometry.snapshot(0, -20).progress)
    assertEquals(1f, geometry.snapshot(0, 500).progress)
}

@Test
fun zeroAndInvertedRangesUseOneCollapsedSlice() {
    listOf(
        ArtworkCollapseGeometry(80f, 80f),
        ArtworkCollapseGeometry(60f, 80f),
    ).forEach { invalid ->
        assertEquals(
            ArtworkCollapseSnapshot(0f, 80f, 0f, 1f),
            invalid.snapshot(0, 40),
        )
    }
}

@Test
fun slicesFormOneSquareAndShareOneImagePlacement() {
    val snapshot = geometry.snapshot(0, 0)
    assertEquals(320f, snapshot.upperSliceHeightPx + snapshot.lowerSliceHeightPx)
    assertEquals(-snapshot.upperSliceHeightPx, snapshot.lowerSliceImageOffsetPx)
}

@Test
fun resizedGeometryRecomputesFromCurrentListPosition() {
    val resized = ArtworkCollapseGeometry(200f, 80f)
    assertEquals(1f, resized.snapshot(0, 220).progress)
    assertEquals(120f, resized.snapshot(0, 220).upperSliceHeightPx)
}
```

Delete connection-event tests that call `onPreScroll` or `onPostScroll`; they lock the disproven architecture rather than product behavior.

- [ ] **Step 2: Run the focused test and verify RED**

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' \
  --configuration-cache
```

Expected: test compilation fails on the replacement snapshot fields and `snapshot(firstVisibleItemIndex, firstVisibleItemScrollOffset)` API. The failure must not come from unrelated tests.

- [ ] **Step 3: Implement the minimal pure geometry**

Use these semantics:

```kotlin
val collapseRangePx: Float
    get() = (expandedHeightPx - collapsedHeightPx).coerceAtLeast(0f)

fun snapshot(firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int): ArtworkCollapseSnapshot {
    val range = collapseRangePx
    if (range == 0f) return ArtworkCollapseSnapshot(0f, collapsedHeightPx, 0f, 1f)
    val consumed = if (firstVisibleItemIndex > 0) {
        range
    } else {
        firstVisibleItemScrollOffset.toFloat().coerceIn(0f, range)
    }
    return ArtworkCollapseSnapshot(
        upperSliceHeightPx = range,
        lowerSliceHeightPx = collapsedHeightPx,
        lowerSliceImageOffsetPx = -range,
        progress = consumed / range,
    )
}
```

The file must no longer import Compose runtime state, nested-scroll types, or geometry `Offset`.

- [ ] **Step 4: Run focused GREEN**

Run the command from Step 2. Expected: all `ArtworkCollapseTest` cases pass.

- [ ] **Step 5: Review Task 1 scope**

Confirm the file contains pure arithmetic and branch selection only, all removed APIs have no remaining declarations, and no production UI file has changed yet.

---

### Task 2: Single-owner artwork lazy sequence and chrome

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapseTest.kt`
- Modify only if a scroll-direction regression requires it: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt`

**Interfaces:**
- Consumes: `ArtworkCollapseGeometry.snapshot(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)` from Task 1.
- Produces: artwork-mode item policy and composables with exactly one vertical input owner.
- Keeps: `DrillDownMiuixScrollChrome` for no-artwork pages only.
- Simplifies: `DrillDownScrollbar(listState, modifier)`; top navigation uses its existing `listState.scrollToItem(0)` call and no external reset callback.

- [ ] **Step 1: Add failing pure policy coverage for item composition and spacing**

Add small pure seams to `ArtworkCollapse.kt` and tests rather than source-text assertions:

```kotlin
internal enum class ArtworkHeaderItemPolicy { UpperAndStickyLower, StickyLowerOnly }

internal fun artworkHeaderItemPolicy(geometry: ArtworkCollapseGeometry): ArtworkHeaderItemPolicy =
    if (geometry.collapseRangePx > 0f) {
        ArtworkHeaderItemPolicy.UpperAndStickyLower
    } else {
        ArtworkHeaderItemPolicy.StickyLowerOnly
    }

internal data class DrillDownListSpacing(
    val horizontalPaddingDp: Float,
    val itemGapDp: Float,
    val artworkSliceGapDp: Float,
)

internal val ArtworkDrillDownListSpacing = DrillDownListSpacing(20f, 18f, 0f)
```

Test valid and zero ranges, exact 20 dp row inset, exact 18 dp content gap, and exact zero gap between artwork slices. Also assert that `drillDownScrollOwner` still selects Miuix for Loading/Unavailable and Artwork only for Available.

- [ ] **Step 2: Run focused tests and verify RED**

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' \
  --configuration-cache
```

Expected: compilation fails only because the item-policy/spacing seams do not exist.

- [ ] **Step 3: Add the minimal policy seams and restore GREEN before UI wiring**

Implement the exact enum, function, and values above in `ArtworkCollapse.kt`, then rerun Step 2. Expected: tests pass.

- [ ] **Step 4: Split chrome responsibilities**

Refactor `LibraryChrome.kt` so `DrillDownMiuixScrollChrome` accepts no artwork snapshot or artwork bytes and renders only the unchanged no-artwork Miuix path.

Add focused artwork composables with these responsibilities:

```kotlin
@Composable
internal fun DrillDownArtworkUpperSlice(
    artworkBytes: ByteArray,
    expandedHeight: Dp,
    upperSliceHeight: Dp,
    modifier: Modifier = Modifier,
)

@Composable
internal fun DrillDownArtworkStickySlice(
    title: String,
    artworkBytes: ByteArray,
    expandedHeight: Dp,
    collapsedHeight: Dp,
    imageOffsetY: Dp,
    progress: Float,
    modifier: Modifier = Modifier,
)

@Composable
internal fun DrillDownArtworkBackButton(
    progress: Float,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
)
```

Each slice must:

- clip its own bounds;
- place an `expandedHeight x expandedHeight` `ArtworkImage` at a fixed origin;
- translate only the lower image by `imageOffsetY`;
- use `ContentScale.Crop`, `ArtworkImageRole.Hero`, the existing artwork accessibility text, and the same scrim gradient;
- preserve the existing alpha curves: collapsed title `(progress * 3f).coerceIn(0f, 1f)`, large title `(1f - progress * 2f).coerceIn(0f, 1f)`, and animated paper fade;
- avoid any `scrollable`, `nestedScroll`, or pointer handler.

The back composable may be overlaid outside the list but its modifier must size only the safe-inset 44 dp target. It must not use `fillMaxWidth`, `fillMaxHeight`, or `matchParentSize`.

- [ ] **Step 5: Replace artwork-mode layout in `DrillDownView`**

Inside `BoxWithConstraints`:

```kotlin
val artworkBytes = (topBarArtworkState as? TrackArtworkLoadState.Available)?.bytes
val hasTopBarArtwork = artworkBytes != null
val artworkGeometry = ArtworkCollapseGeometry(
    expandedHeightPx = with(density) { maxWidth.toPx() },
    collapsedHeightPx = with(density) { collapsedChromeHeight.toPx() },
)
val artworkSnapshot by remember(listState, artworkGeometry) {
    derivedStateOf {
        artworkGeometry.snapshot(
            firstVisibleItemIndex = listState.firstVisibleItemIndex,
            firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
        )
    }
}
```

Do not write to list or collapse state from `derivedStateOf`.

Use one `LazyColumn(state = listState, modifier = Modifier.fillMaxSize())`. Apply `.nestedScroll(miuixScrollBehavior.nestedScrollConnection)` only when `hasTopBarArtwork` is false. Do not attach an artwork connection.

For artwork mode, build this exact order:

```kotlin
if (artworkSnapshot.upperSliceHeightPx > 0f) {
    item(key = "artwork-upper") { DrillDownArtworkUpperSlice(...) }
}
stickyHeader(key = "artwork-lower") { DrillDownArtworkStickySlice(...) }
item(key = "section") { DrillDownListItem { SectionLabel(...) } }
items(tracks, key = { it.id }) { track -> DrillDownListItem { TrackRow(...) } }
item(key = "now-playing-spacer") { Spacer(...) }
```

For no-artwork mode, preserve the existing top `PaddingValues`, `Arrangement.spacedBy(18.dp)`, 20 dp horizontal padding, section/rows/spacer, and Miuix chrome.

Artwork mode cannot use global `Arrangement.spacedBy` or global horizontal padding because either would split/inset the artwork slices. Add:

```kotlin
@Composable
private fun DrillDownListItem(
    bottomGap: Dp = 18.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = bottomGap),
    ) { content() }
}
```

Render `DrillDownArtworkBackButton` after the list with only button-sized bounds. Keep `DrillDownScrollbar` and the Now Playing overlay in their current sibling positions.

Remove all artwork-mode `.layout`, `.offset`, viewport extension, dynamic/fixed artwork `contentPadding`, parent artwork `nestedScroll`, and sibling `Modifier.scrollable` code and imports.

- [ ] **Step 6: Simplify the scrollbar**

Remove `onScrollToTop`, `rememberUpdatedState`, and `scrollbarTargetsTop`. Retain direct index targeting and call:

```kotlin
listState.scrollToItem(index = targetIndex, scrollOffset = 0)
```

When `targetIndex == 0`, this naturally restores the upper artwork slice. Do not add an external callback.

- [ ] **Step 7: Preserve scroll-direction reporting**

Keep `onScrollPositionChanged(listState.toLibraryScrollPosition())` unchanged. The consumer compares index/offset direction only, so raw header indices are intentional. Add or retain `LibraryNavigationTest` cases proving increasing index/offset hides the Now Playing bar and decreasing index/offset shows it; do not normalize indices to track-row indices.

- [ ] **Step 8: Run focused GREEN and compilation**

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' \
  --tests 'com.eterocell.rhythhaus.ui.ArtworkImageTest' \
  --tests 'com.eterocell.rhythhaus.library.ArtworkLazyLoadingTest' \
  --configuration-cache

./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: all focused tests pass and shared JVM compilation succeeds.

- [ ] **Step 9: Review Task 2 invariants**

Search changed production sources and confirm artwork mode contains no `NestedScrollConnection`, `rememberArtworkCollapseState`, `Modifier.scrollable`, custom viewport `layout`, collapse-driven list `offset`, or `expandFully`. Confirm no-artwork code still attaches only Miuix.

---

### Task 2A: Progressive solid sticky chrome

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapse.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapseTest.kt`

**Interfaces:**
- Produces: `internal fun artworkChromeSolidAlpha(progress: Float): Float = progress.coerceIn(0f, 1f)`.
- Changes: `DrillDownArtworkStickySlice` contains the solid paper fade and both title chips directly; no artwork `LayerBackdrop`, `drawBackdrop`, Miuix blur, measured sticky bounds, or sibling artwork-chrome overlay remains.
- Preserves: one lazy input owner, aligned artwork slices, title/back contrast, unchanged no-artwork Miuix chrome, and unchanged bottom/Now Playing bar rendering.

- [ ] **Step 1: Add failing solid-alpha policy tests**

```kotlin
@Test
fun artworkChromeSolidAlphaClampsCollapseProgress() {
    assertEquals(0f, artworkChromeSolidAlpha(-0.2f))
    assertEquals(0f, artworkChromeSolidAlpha(0f))
    assertEquals(0.5f, artworkChromeSolidAlpha(0.5f))
    assertEquals(1f, artworkChromeSolidAlpha(1f))
    assertEquals(1f, artworkChromeSolidAlpha(1.4f))
}
```

- [ ] **Step 2: Run focused RED**

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' \
  --configuration-cache
```

Expected: test compilation fails only because `artworkChromeSolidAlpha` does not exist.

- [ ] **Step 3: Implement the pure alpha policy**

Add the one-line clamping function to `ArtworkCollapse.kt`, then rerun Step 2 and expect GREEN.

- [ ] **Step 4: Remove artwork blur/overlay machinery and wire the solid treatment**

Delete `StickyArtworkOverlayBoundsPx`, `StickyArtworkOverlayPlacement`, `stickyArtworkOverlayPlacement`, root/sticky `onGloballyPositioned` state, and `DrillDownArtworkStickyOverlay`. Restore title, progress, and chrome rendering directly to `DrillDownArtworkStickySlice`. After the aligned artwork plane and before title chips, render:

```kotlin
Modifier
    .matchParentSize()
    .background(HausColors.current.paper.copy(alpha = artworkChromeSolidAlpha(progress)))
```

Do not pass a backdrop to the artwork sticky slice and do not alter `NowPlayingBar` or its call site. Keep title width bounds, ellipsis, alpha curves, the button-sized back overlay, and no-artwork Miuix chrome. Do not add pointer input, `scrollable`, or nested scroll.

- [ ] **Step 5: Run focused GREEN and compiler gate**

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' \
  --configuration-cache

./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: tests and compilation pass. Review that solid alpha is zero/partial/one at expanded/partial/pinned progress, no artwork blur/placement symbols remain, and the bottom bar diff is empty.

---

### Task 3: Remove the disposable desktop prototype

**Files:**
- Modify: `desktopApp/build.gradle.kts`
- Delete: `desktopApp/src/main/kotlin/com/eterocell/rhythhaus/ArtworkScrollPrototype.kt`
- Delete: `desktopApp/src/test/kotlin/com/eterocell/rhythhaus/ArtworkScrollPrototypeTest.kt`

**Interfaces:**
- Removes: `runArtworkScrollPrototype` and `ArtworkScrollPrototypeKt`.
- Preserves: normal `MainKt`, `compose.desktop.application.mainClass`, Koin startup, and macOS packaging.

- [ ] **Step 1: Confirm prototype evidence is preserved**

Verify the passing prototype commands and physical macOS result are already recorded in `progress.md` and the amended OpenSpec design before deleting files.

- [ ] **Step 2: Remove the prototype task and exclusive dependencies**

Delete `runArtworkScrollPrototype`, `testImplementation(libs.kotlin.testJunit)`, `implementation(libs.compose.material3)`, and `implementation(libs.compose.material.icons.extended)` only if no remaining desktop source or test uses them. Keep all pre-existing dependencies byte-for-byte.

- [ ] **Step 3: Delete prototype source and test**

Delete both prototype Kotlin files. Do not alter `desktopApp/src/main/kotlin/com/eterocell/rhythhaus/main.kt`.

- [ ] **Step 4: Verify desktop cleanup**

```bash
./gradlew :desktopApp:compileKotlin :desktopApp:test --configuration-cache
```

Expected: build succeeds and no `ArtworkScrollPrototype` or `runArtworkScrollPrototype` references remain.

---

### Task 4: Automated replacement verification

**Files:**
- Modify after evidence exists: `openspec/changes/track-list-artwork-collapse/tasks.md`
- Do not modify unrelated failing tests or platform code.

- [ ] **Step 1: Run strict OpenSpec validation**

```bash
openspec validate track-list-artwork-collapse --strict
```

Expected: `Change 'track-list-artwork-collapse' is valid`.

- [ ] **Step 2: Run the supported JVM/desktop/Android matrix**

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Expected: success. Record any warnings exactly; do not call a failed command passing.

- [ ] **Step 3: Check Xcode and attempt iOS tests**

```bash
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

Expected: record actual results. If unchanged `AppScanCancellationTest.kt` JVM-only `Thread` references still block test compilation, record exact lines and do not fix that unrelated issue or claim iOS success.

- [ ] **Step 4: Run diagnostics and diff hygiene**

Run Kotlin LSP diagnostics on changed Kotlin files if the server is available. If installation remains declined, record that Gradle compilation/tests are the executable language checks.

```bash
GIT_MASTER=1 git diff --check
GIT_MASTER=1 git status --short
```

Expected: no whitespace errors and only intended prototype, source/test, design/OpenSpec, and evidence changes.

- [ ] **Step 5: Update OpenSpec task state conservatively**

Mark Tasks 4.1-4.5 and 5.1 complete only when their corresponding source/test/cleanup/command evidence exists. Leave manual and visual tasks pending until live evidence exists.

---

### Task 5: Production physical macOS and visual acceptance

**Files:**
- Modify only after results exist: `progress.md`
- Update/create: `.superpowers/sdd/track-list-artwork-collapse-task-4-report.md`
- Update/create: `.superpowers/sdd/track-list-artwork-collapse-task-5-report.md`

- [ ] **Step 1: Launch the clean production desktop app**

```bash
./gradlew :desktopApp:run --configuration-cache
```

Confirm the launched process uses normal `MainKt` and contains no prototype entry point.

- [ ] **Step 2: Perform artwork-backed album and artist input QA**

With a physical macOS trackpad, verify on both album and artist pages:

1. movement over every non-button artwork region scrolls the same list as rows;
2. partial collapse follows travel continuously;
3. full collapse pins the lower slice at system inset plus 56 dp;
4. rows scroll normally only after the collapse range is exhausted;
5. deep reverse movement restores the complete square without a dead boundary event;
6. scrollbar top returns to `firstVisibleItemIndex == 0` and `firstVisibleItemScrollOffset == 0`;
7. the back target works while expanded, partial, and collapsed.

Any failure blocks completion and returns to systematic debugging; do not add another speculative scroll handler.

- [ ] **Step 3: Perform visual QA**

At compact and wide desktop widths, inspect:

- no visible seam, duplicate crop, gap, overlap, or clipping between image slices;
- correct square at item zero;
- stable sticky toolbar and scrim;
- existing large/collapsed title alpha transitions and paper fade;
- safe-inset 44 dp back target;
- track spacing, scrollbar, and Now Playing overlay;
- English and available CJK title text for clipping or baseline drift.

Use the repository visual-QA workflow and record screenshots/diffs when available. Do not claim pixel or CJK acceptance if screenshots cannot be inspected.

- [ ] **Step 4: Verify no-artwork Miuix behavior**

Open album and artist pages in Loading, Unavailable/failed, and absent artwork states where available. Confirm the existing Miuix large title, glass chrome, divider, padding, back interaction, and row behavior remain unchanged.

- [ ] **Step 5: Run specification, code-quality, and Oracle gates**

Obtain specification-compliance review first, code-quality review second, then final Oracle review. Fix only findings introduced by this replacement and rerun affected checks.

---

### Task 6: Durable completion evidence

**Files:**
- Modify: `openspec/changes/track-list-artwork-collapse/tasks.md`
- Modify: `progress.md`
- Modify: `roadmap.md` item 21 only if its existing completion statement must be corrected with replacement evidence
- Update/create: `.superpowers/sdd/track-list-artwork-collapse-task-4-report.md`
- Update/create: `.superpowers/sdd/track-list-artwork-collapse-task-5-report.md`

- [ ] **Step 1: Record exact evidence**

Record route, owner, input, output, every command/result, physical macOS pass/fail, visual-QA limitations, changed files, review verdicts, blockers, and next owner. Preserve the history of three failed attempts and the passing prototype as rationale; do not rewrite them as production success.

- [ ] **Step 2: Reconcile OpenSpec and roadmap**

Mark Tasks 4-5 complete only when their evidence exists. Preserve roadmap item 22 and all unrelated user edits byte-for-byte. Do not archive the OpenSpec change unless explicitly requested.

- [ ] **Step 3: Run final scope checks**

```bash
openspec validate track-list-artwork-collapse --strict
GIT_MASTER=1 git diff --check
GIT_MASTER=1 git status --short
```

Expected: strict validation and diff hygiene pass; status contains no disposable prototype files and no unintended source changes.

- [ ] **Step 4: Commit only if explicitly authorized**

If the user requests commits, use the repository semantic style and pair implementation with its direct tests. Before committing, inspect status, diff, and recent history and stage only intended files. Otherwise leave all work uncommitted and report that clearly.

## Execution Notes

- Recommended execution mode: `subagent-driven-development`, one task at a time with specification review followed by code-quality review.
- Current dirty production files contain failed-attempt changes. Replace them deliberately; do not reset or overwrite unrelated user work.
- The disposable prototype has already passed physical macOS routing acceptance. Production acceptance must still be performed because image slicing, sticky visuals, and integration differ from the prototype.
