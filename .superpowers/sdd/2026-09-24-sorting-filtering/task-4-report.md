# Task 4 report: selection and playback reconciliation

**Status:** Review evidence repaired; focused Shared and Library JVM tests pass.
The post-repair review found no residual contract defect. Task 4's later
cross-platform, quality, independent-review, and archive tasks remain pending.

## Existing production boundary

No production source change was needed. The accepted Task 1–3 implementation already has the required single projection boundary:

- `LibraryHomeContent` derives `visibleTracks`, reports the full `visibleTracks.map(Track::id)` sequence through `onVisibleTrackIdsChanged`, and passes that same list to row playback.
- `LibraryAppShell` converts the callback to `TrackSelectionAction.ReconcileVisible(HomeSongs, ids)`.
- The existing selection reducer removes only selected IDs absent from that sequence, so retained IDs remain selected while `orderedSelectedTrackIds` follows the newly projected order.

## Regression coverage

- A constrained-height Home test verifies that query and flat-mode changes publish the complete projected ID sequence even when the list rows are outside the viewport, and that the changes do not request playback.
- A Shared shell test now runs the same query/selection flow through both the compact `420.dp` `LibraryRouteContent`/Home callback and the wide `1200.dp` adaptive Home composition. It starts active playback, selects two tracks, reverses the projection, and verifies the selected playlist order follows the projection. It then applies a favorite filter and switches to the Favorites browse mode, confirming the excluded selected ID is removed while selection mode remains active for the retained ID.
- The Shared shell test captures an active playback invariant before each query/mode change and asserts that current track, complete queue order, engine generation, repeat mode, shuffle mode, status, position, duration, and normalized progress remain unchanged.
- The explicit row activation uses two visible favorite tracks and verifies the actual `PlaybackController` queue contains the complete final visible sequence in order, rather than only the clicked row.

## Review repair

The three Important evidence gaps identified in `task-4-review.md` are
addressed without production changes:

1. The explicit row-click scenario keeps two favorite tracks visible and
   asserts both the selected current track and the exact ordered controller
   queue.
2. The Shared scenario is isolated and repeated at `420.dp` and `1200.dp`,
   covering compact callback composition as well as the existing wide branch.
3. The scenario snapshots active playback before and after sort, favorite
   filter, and Favorites-mode changes, including the engine generation and
   normalized progress.

## Verification evidence

```text
./gradlew :feature:library:impl:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryHomeContentJvmTest' --configuration-cache
BUILD SUCCESSFUL in 392ms
57 actionable tasks: 10 executed, 47 up-to-date

./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryAppShellJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.TrackSelectionStateTest' --tests 'com.eterocell.rhythhaus.library.ui.LibraryBrowseQueryStateTest' --configuration-cache
BUILD SUCCESSFUL in 6s
146 actionable tasks: 25 executed, 121 up-to-date

The repaired regression was also run directly:

./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryAppShellJvmTest.queryAndModeChangesReconcileSelectionAcrossAdaptiveHomeRoutesWithoutMutatingPlayback' --configuration-cache
BUILD SUCCESSFUL in 9s
155 actionable tasks: 26 executed, 129 up-to-date

git diff --check
(no output)
```

Final post-repair rerun (the two focused suites were chained with
`git diff --check`) returned:

```text
:feature:library:impl:jvmTest — BUILD SUCCESSFUL in 325ms
57 actionable tasks: 10 executed, 47 up-to-date
:shared:jvmTest — BUILD SUCCESSFUL in 343ms
146 actionable tasks: 24 executed, 122 up-to-date
git diff --check — no output
```

No formatter, linter, or project-wide suite was run at this task boundary.

## Prior implementation commit

`e5add1b0 test: cover library browse selection reconciliation`

The test/report repair commit is the current conventional commit containing
this report and the repaired Shared regression.
