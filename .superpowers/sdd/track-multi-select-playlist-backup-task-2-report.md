# Task 2 Report: Long-Press Rows, Checkboxes, and Eligible Surfaces

Status: DONE_WITH_CONCERNS

## Summary

Implemented the Task 2 row/callback surface for Library Home Songs, album detail, artist detail, and Search. The eligible rows now use a shared combined-click contract: ordinary click plays only outside selection, long press outside selection starts selection without playback, and click/long press during selection toggles without playback. Library and Search rows render real 44dp MIUIX checkboxes in selection mode, expose checked/toggle semantics plus a labeled accessible long-click action, preserve independent now-playing state, and no longer render per-row Add to Playlist buttons.

## RED evidence

Command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.TrackSelectionSemanticsJvmTest' --configuration-cache
```

Result: expected failure at `:shared:compileTestKotlinJvm` before production wiring. The compiler reported unresolved `TrackRowGesture`, `TrackRowActivation`, and `trackRowActivation`, plus missing selection-aware `TrackRow` parameters (`isNowPlaying`, `selectionModeActive`, `isSelected`, `onPlay`, `onToggleSelection`, `onStartSelection`). Test-fixture-only import/source errors found in the first attempt were corrected and RED was rerun so the remaining failures were exclusively the missing Task 2 contracts.

## GREEN evidence

Focused semantics/dispatch GREEN:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.TrackSelectionSemanticsJvmTest' --configuration-cache
```

Result: `BUILD SUCCESSFUL`; five tests cover activation dispatch, accessible labeled long-click selection entry, checked/now-playing coexistence, checkbox exactly-once/no-play behavior, and selection-mode row-click exactly-once/no-play behavior.

Focused Task 1 + Task 2 + navigation matrix:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.TrackSelectionStateTest' --tests 'com.eterocell.rhythhaus.library.ui.TrackSelectionSemanticsJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
```

Result: `BUILD SUCCESSFUL` after the final test change.

Broad verification:

```text
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Result: `BUILD SUCCESSFUL` (101 actionable tasks; shared JVM tests, desktop compile, and Android debug assembly all completed). The first invocation reached the 120-second tool timeout while Android TagLib native helpers were compiling and reported no failure; the exact command was rerun with a 600-second timeout and passed.

Additional checks:

- `git diff --check`: pass before staging.
- `git diff --staged --check`: pass before commit.
- Kotlin LSP diagnostics: unavailable because `kotlin-ls` is not installed and installation was previously declined; Gradle Kotlin compilation passed for JVM, desktop, and Android.
- Independent read-only review: row-level contract, exact eligible surfaces, page keys, checkbox semantics, Search reconciliation, Home Songs browse-mode cleanup, and Task 3 scope were reviewed. Its one actionable test-gap finding was closed by adding the selection-mode row-click exactly-once regression.
- Visual QA: source/semantics inspection confirms real reusable Compose/MIUIX controls, existing Haus tokens/ripple, and 44dp checkbox targets. End-to-end selected-state screenshots are not claimable until Task 3 provides shell-owned selection state.

## Exact committed files

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/HausClickable.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/TrackSelectionSemanticsJvmTest.kt`

## Commit

`d8396d5687a9672a73b4c78e4ede8931889a7e06 feat: add long-press selection to track lists`

The commit includes the required Sisyphus footer and `Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>` trailer. Explicit staging excluded `.superpowers/sdd/progress.md`, `.superpowers/sdd/task-1-report.md`, `openspec/changes/track-multi-select-playlist-backup/tasks.md`, and this controller-facing report.

## Concerns / Task 3 handoff

Task 2 intentionally exposes and threads `TrackSelectionState` / `TrackSelectionAction` callbacks with inert defaults but does not add authoritative state to `LibraryAppShell.kt`, because the brief assigns app-shell selection state, route/back lifetime, contextual bottom content, and generalized picker ownership to Task 3 and forbids inventing shell state here. Consequently the row surfaces compile and are independently testable, but production selection remains inactive until Task 3 passes shell-owned state/action callbacks into `LibraryHomeContent`, `LibraryRouteContent`, and `LibraryRouteOverlays`. The now-obsolete Home `onAddToPlaylist` argument also remains at the unmodified Task 3-owned `LibraryAppShell` call boundary; eligible row buttons themselves are removed.

## Reviewer coverage follow-up

Status: COMPLETE

### RED

Added `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/search/SearchSelectionPoliciesJvmTest.kt` before changing production. The test directly composes the separately implemented Search row and calls wished-for production policies for Search reconciliation and Home browse-mode lifecycle.

Command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.search.SearchSelectionPoliciesJvmTest' --configuration-cache
```

After correcting test-fixture-only constructor/lambda errors, the clean RED failed at `:shared:compileTestKotlinJvm` because:

- `SearchResultRow` was private and therefore not directly executable from the Search JVM test;
- `dispatchSearchVisibleTrackIds` did not exist;
- `dispatchHomeBrowseModeChange` did not exist.

No production code had been changed when this RED was captured.

### Minimal GREEN extraction

- `SearchScreen.kt`: made the actual `SearchResultRow` internal; extracted `dispatchSearchVisibleTrackIds`, and the existing `LaunchedEffect(visibleTrackIds)` now invokes that exact production policy.
- `LibraryHomeContent.kt`: extracted `dispatchHomeBrowseModeChange`, and the existing `BrowseModePicker` callback now invokes that exact production policy.
- No shell state, picker, bottom bar, route ownership, or unrelated behavior changed.

### Direct regression coverage

`SearchSelectionPoliciesJvmTest` contains five executable tests proving:

1. Search normal row click plays once outside selection and does not toggle.
2. Search long click starts selection once and does not play.
3. Search selection-mode row click and checkbox click each toggle exactly once and never play.
4. Successive filtered visible-ID sets emit the corresponding ordered `ReconcileVisible(TrackSelectionPageKey.Search, ids)` actions.
5. Home Songs -> Albums and Home Songs -> Artists each emit exactly one `RouteChanged(null)` before the requested browse-mode callback.

### GREEN and requested matrix

Focused new class:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.search.SearchSelectionPoliciesJvmTest' --configuration-cache
```

Result: `BUILD SUCCESSFUL`.

Task 1/Task 2/navigation matrix:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.TrackSelectionStateTest' --tests 'com.eterocell.rhythhaus.library.ui.TrackSelectionSemanticsJvmTest' --tests 'com.eterocell.rhythhaus.search.SearchSelectionPoliciesJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
```

Result: `BUILD SUCCESSFUL` (35 actionable tasks, 6 executed, 29 up-to-date).

Kotlin LSP remained unavailable because `kotlin-ls` is not installed and installation was previously declined; JVM production/test compilation completed successfully. `git diff --check` and `git diff --staged --check` passed.

### Follow-up commit

`64112b624ae2495bfb4a127ed80fb2d80e414339 test: cover search and home selection policies`

The new commit was not amended and includes the required Sisyphus footer and co-author trailer. Only `SearchScreen.kt`, `LibraryHomeContent.kt`, and `SearchSelectionPoliciesJvmTest.kt` were staged; controller evidence remained unstaged.
