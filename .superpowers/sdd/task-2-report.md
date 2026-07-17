# Task 2 Report: Single-owner artwork lazy sequence and chrome

## Status

Task 2 is GREEN. The Task 1 pure list-position geometry now has compiling production callers, so Tasks 1 and 2 form one GREEN integration unit.

## Files changed

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapse.kt`
  - Preserved the Task 1 `ArtworkCollapseGeometry.snapshot(firstVisibleItemIndex, firstVisibleItemScrollOffset)` API unchanged.
  - Added the exact pure `ArtworkHeaderItemPolicy`, `artworkHeaderItemPolicy`, `DrillDownListSpacing`, and `ArtworkDrillDownListSpacing` seams from the brief.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapseTest.kt`
  - Added valid-range and zero-range item-policy tests.
  - Added exact 20 dp row inset, 18 dp item gap, and zero artwork-slice gap coverage.
  - Retained explicit Loading/Unavailable Miuix and Available Artwork owner coverage.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt`
  - Made `DrillDownMiuixScrollChrome` no-artwork-only.
  - Added clipped upper and sticky-lower artwork slices that place aligned portions of the same fixed square `ArtworkImage` with `ContentScale.Crop`, `ArtworkImageRole.Hero`, the existing artwork accessibility string, and the existing scrim/alpha curves.
  - Added a safe-inset, button-sized 44 dp artwork back control with no full-screen input overlay.
  - Simplified `DrillDownScrollbar` to direct `scrollToItem(index = targetIndex, scrollOffset = 0)` with no reset callback or updated-state capture.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt`
  - Replaced the failed nested-scroll/sibling-scrollable/layout-compensation architecture with one literal `LazyColumn` and one `LazyListState`.
  - Artwork order is keyed upper slice, sticky lower slice, section, keyed track rows, and Now Playing spacer.
  - Artwork rows use local tested 20 dp horizontal inset and 18 dp bottom gaps; artwork slices remain full-bleed with no global spacing.
  - Loading and Unavailable retain the unchanged Miuix list padding, arrangement, chrome, and sole Miuix nested-scroll connection.
  - Preserved raw `onScrollPositionChanged(listState.toLibraryScrollPosition())` reporting and the existing scrollbar/Now Playing sibling positions.

## RED

After adding only the policy/spacing tests, ran:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' \
  --configuration-cache
```

Result: expected combined Task 1+2 failure at `:shared:compileKotlinJvm` (`BUILD FAILED in 2s`). The compiler stopped before test compilation because the intentionally dirty Task 1 callers still referenced six removed APIs:

- `artworkChromeHeightPx`
- `scrollbarTargetsTop`
- `rememberArtworkCollapseState`
- `artworkListTopPaddingPx`
- `artworkListVisualOffsetPx`
- `artworkListViewportExtensionPx`

This is the exact integration boundary recorded in `task-1-report.md`; no unrelated source or test failure occurred. Because production compilation failed first, Gradle could not yet surface the new missing policy symbols independently. The policy seams were then added exactly as specified, and obsolete callers were replaced rather than restoring compatibility declarations from the rejected architecture.

## GREEN

Initial caller-integration compile:

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Result: pass, `BUILD SUCCESSFUL in 5s`. This is the first Task 1 GREEN production integration.

Initial policy/navigation GREEN:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' \
  --configuration-cache
```

Result: pass, `BUILD SUCCESSFUL in 4s`.

Expanded focused GREEN:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' \
  --tests 'com.eterocell.rhythhaus.ui.ArtworkImageTest' \
  --tests 'com.eterocell.rhythhaus.library.ArtworkLazyLoadingTest' \
  --configuration-cache
```

Result: pass after final wiring, `BUILD SUCCESSFUL in 5s`.

Final shared JVM compiler gate:

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Result: pass, `BUILD SUCCESSFUL in 337ms`.

Diff hygiene:

```bash
GIT_MASTER=1 git diff --check
```

Result: pass with no output.

## Invariant review

- Production search for `NestedScrollConnection`, `rememberArtworkCollapseState`, `Modifier.scrollable`, custom `.layout {`, `artworkListVisualOffsetPx`, `artworkListViewportExtensionPx`, `artworkListTopPaddingPx`, `expandFully`, `scrollbarTargetsTop`, `onScrollToTop`, and `rememberUpdatedState`: no matches.
- `LibraryDetailContent.kt` contains exactly one `LazyColumn(` call and one `nestedScroll(` call. The latter is `miuixScrollBehavior.nestedScrollConnection` inside the no-artwork modifier branch only.
- Artwork production sequence contains `artwork-upper`, sticky `artwork-lower`, `section`, track ID keys, and `now-playing-spacer`.
- Upper and lower slices both render the same fixed `expandedHeight x expandedHeight` artwork plane; only the lower plane is translated by the Task 1 image offset. Each slice clips its own bounds, and no global padding or arrangement splits them.
- Artwork back control has only safe-inset offsets and `.size(44.dp)`; it does not use `fillMaxWidth`, `fillMaxHeight`, or `matchParentSize`.
- Scrollbar top naturally restores item zero with explicit `scrollOffset = 0`; no imperative collapse reset exists.
- Raw list indices/offsets remain unchanged. Existing `LibraryNavigationTest` cases cover increasing/decreasing offsets within one item and increasing/decreasing indices across item boundaries.
- Loading and Unavailable still classify as Miuix; only resolved Available bytes select artwork mode.

## Diagnostics and concerns

- Kotlin LSP diagnostics could not run because `kotlin-ls` is not installed and installation was previously declined. Focused JVM tests and `:shared:compileKotlinJvm` are the executable Kotlin validation evidence.
- No desktop prototype/build configuration, OpenSpec/docs/progress/roadmap, navigation/playback logic, or unrelated dirty file was modified by this task.
- No commit was created, as required.
- This task did not run live desktop/device visual or physical trackpad QA. The approved disposable single-owner prototype already passed the physical macOS interaction gate; production pixel/gesture confirmation remains a manual follow-up rather than an automated GREEN claim.
