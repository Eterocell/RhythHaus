# Task 3 Report: Apply Selection Policy to Library Lists

## Result

DONE

Implemented Task 3 only in the dedicated `restart-current-track-selection` worktree. Library home song rows and album/artist drill-down rows now use the shared `selectLibraryTrackForPlayback(...)` policy, while Now Playing transport controls remain dedicated `togglePlayPause()` actions.

## Scope

Changed:

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt`
- `.superpowers/sdd/task-3-report.md`

Not changed: SearchScreen, playback controller/helper behavior, OpenSpec, roadmap, progress, plan/spec artifacts, platform engines, dependencies, styling, navigation behavior, backdrop behavior, or scroll behavior.

## Implementation

- Library home song rows retain `onTrackSelected(track.id)` and existing selection visuals, then call `selectLibraryTrackForPlayback(...)` with `snapshot.tracks.map { it.toPlayableTrack() }` as the visible queue.
- `LibraryAppShell.playPauseFromTracks` was replaced by the selection-focused `selectTrackFromTracks`, which delegates to the shared selection helper.
- `LibraryRouteContent` now receives that callback as `onTrackClickFromTracks`.
- Album and artist routes pass their rendered `albumTracks` or `artistTracks` list as the visible queue and preserve the app-level selected-track update.
- `DrillDownView` now separates `onTrackClick: (Track) -> Unit` from `onPlayPause: (Track) -> Unit`:
  - track rows update local selection visuals and invoke only `onTrackClick(track)`;
  - the drill-down `NowPlayingBar` invokes only `onPlayPause(currentTrack)`;
  - route-level transport callbacks call `playbackController.togglePlayPause()` directly.
- The fixed root Now Playing bar also keeps dedicated `playbackController.togglePlayPause()` behavior.
- Now Playing expansion, settings/search navigation, back navigation, backdrop recording, nested scrolling, and row styling remain unchanged.

## RED Evidence

The callback-separation test was added before the production seam:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
```

Expected result observed: `BUILD FAILED in 13s` during `:shared:compileTestKotlinJvm` because the new policy did not exist.

Key errors:

```text
Unresolved reference 'DrillDownTrackAction'.
Unresolved reference 'drillDownTrackAction'.
```

This proves the regression test targeted the missing row-versus-transport callback distinction.

## GREEN Evidence

After adding the pure policy and production wiring, the focused navigation test passed:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
BUILD SUCCESSFUL in 4s
```

Required focused suites passed:

```text
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' \
  --tests 'com.eterocell.rhythhaus.library.LibraryPlaybackSelectionTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' \
  --configuration-cache
BUILD SUCCESSFUL in 1s
```

Required shared JVM compilation passed:

```text
./gradlew :shared:compileKotlinJvm --configuration-cache
BUILD SUCCESSFUL in 770ms
```

Additional check:

```text
GIT_MASTER=1 git diff --check
```

Passed with no output before commit.

Kotlin LSP diagnostics were not run because Kotlin LSP is unavailable by prior user choice. The brief-designated Gradle JVM compilation is the compiler gate and passed.

## Self-Review

- Home visible queue is exactly `snapshot.tracks` converted to playable tracks.
- Album/artist visible queues are exactly their rendered track lists.
- Row selection no longer invokes the transport callback.
- Now Playing transport does not invoke the shared selection helper.
- `onTrackSelected`, local drill-down selection visuals, Now Playing expansion, navigation, backdrop, scroll handling, and dedicated `togglePlayPause()` behavior are preserved.
- No SearchScreen or unrelated file was modified.
- The five implementation/test files were committed atomically because the cross-file callback rename and direct regression seam must compile together.

## Commits

- `f9bd957` — `feat: apply restart selection to library lists`
- Report commit: recorded separately after this report was written.

## Concerns

- None. Automated verification is intentionally limited to the focused suites and shared JVM compilation required by the Task 3 brief.
