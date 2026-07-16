# Task 1 Report: Pure list-position artwork geometry

## Files changed

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapse.kt`
  - Replaced the nested-scroll/state-based artwork collapse model with `ArtworkCollapseSnapshot` slice geometry and list-position-derived progress.
  - Preserved `DrillDownArtwork`, `DrillDownScrollOwner`, and `drillDownScrollOwner(artwork)` with `Available` as the artwork-owner branch.
  - Removed the obsolete state, nested-scroll, consumption, and list/chrome helper APIs.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapseTest.kt`
  - Replaced connection/consumption tests with the brief's exact list-position geometry cases.
  - Preserved the artwork/no-artwork branch-selection assertions.
- `.superpowers/sdd/task-1-report.md`
  - Added this task evidence report.

## RED

Command:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' \
  --configuration-cache
```

Result: expected failure during `:shared:compileTestKotlinJvm`.

Failure reason: the replacement tests referenced the new four-field `ArtworkCollapseSnapshot` and `snapshot(firstVisibleItemIndex, firstVisibleItemScrollOffset)` contract, while production still exposed the old three-field snapshot and `snapshot(offsetPx)` API. The output contained only these intended `ArtworkCollapseTest.kt` compilation mismatches; no unrelated test failure occurred.

## GREEN

Command:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' \
  --configuration-cache
```

Result: blocked during `:shared:compileKotlinJvm` before the focused test could execute.

Blocker: the task-forbidden, already-dirty callers in `LibraryChrome.kt` and `LibraryDetailContent.kt` still reference APIs explicitly removed by Task 1 (`artworkChromeHeightPx`, `scrollbarTargetsTop`, `rememberArtworkCollapseState`, `artworkListTopPaddingPx`, `artworkListVisualOffsetPx`, and `artworkListViewportExtensionPx`). I did not restore compatibility declarations or edit those files because that would retain the disproven architecture and violate the task scope.

## Self-review

- `ArtworkCollapse.kt` contains pure arithmetic plus artwork branch selection; it no longer imports Compose runtime state, nested-scroll types, or `Offset`.
- `collapseRangePx` uses the required non-negative range calculation.
- Zero/inverted ranges return one collapsed slice; non-zero ranges clamp list offset and derive progress from item index/offset.
- Obsolete declarations are absent from `ArtworkCollapse.kt`.
- No UI production file other than `ArtworkCollapse.kt` was changed by this task.
- Kotlin LSP diagnostics could not run because `kotlin-ls` is not installed and installation was previously declined.

## Concerns

The focused GREEN command cannot pass until the later single-lazy-list integration task updates the dirty `LibraryChrome.kt` and `LibraryDetailContent.kt` callers. Those files remain intentionally untouched here.
