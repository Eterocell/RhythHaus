# Playlist edit mode Task 4 report

## RED evidence

Added the Compose interaction/layout test first, then ran:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest.detailReportsActualLazyListScrollPositionAndUsesMeasuredFinalClearanceWithoutLocalBar' --configuration-cache --rerun-tasks
```

Exact result: `BUILD FAILED in 28s` during `:shared:compileTestKotlinJvm` because `PlaylistDetailScreen` had no `listState` or `onScrollPositionChanged` parameters. Compiler diagnostics were:

```text
No parameter with name 'listState' found.
No parameter with name 'onScrollPositionChanged' found.
Unresolved reference 'it'.
```

This was the intended assertion-path RED: the production detail had no way to expose its actual list state or report its scroll position.

## Implementation

- `PlaylistDetailScreen` now owns a real remembered `LazyListState` by default, accepts a caller-provided state for interaction coverage, and reports `listState.toLibraryScrollPosition()` from a keyed `LaunchedEffect`.
- The detail passes that same state through `PlaylistScreenFrame` to its actual `LazyColumn`. The hub uses the frame's independent `rememberLazyListState()` default, so hub behavior is unchanged.
- `LibraryRouteContent` passes its existing `onScrollPositionChanged` seam into playlist detail. `LibraryAppShell` already supplies `appState::updateNowPlayingBarVisibilityForScroll`, exactly as it does for album and artist detail.
- Existing measured `bottomContentPadding` forwarding remains intact. The final keyed spacer remains the last detail list item and uses that supplied `Dp`; its test tag permits exact layout verification.
- No playlist-local footer or Bottom Bar was introduced. The action header remains a sibling before the list, preserving Task 3 boundaries and responsive edit rows.
- `BottomBarModeTest` was not changed because matching, stale, hidden, and unmeasured clearance/visibility cases are already covered. `LibraryNavigationTest` already covers down/up/jitter reducer behavior.

## GREEN evidence

Focused suite:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --tests 'com.eterocell.rhythhaus.BottomBarModeTest' --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache --rerun-tasks
```

Exact result: `BUILD SUCCESSFUL in 15s`; all `35 actionable tasks` executed.

Desktop compilation:

```text
./gradlew :desktopApp:compileKotlin --configuration-cache
```

Exact result: `BUILD SUCCESSFUL in 1s`; `31 actionable tasks: 4 executed, 27 up-to-date`.

Whitespace validation:

```text
git diff --check
```

Exact result: exit status 0 with no output.
