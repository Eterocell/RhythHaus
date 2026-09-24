# Task 4 correctness review

**Status:** Repair evidence recorded; post-repair review finds all three
Important findings addressed. Later Task 4 gates remain pending.

## Classification

**Important findings** — three acceptance/regression-coverage gaps remain before this task is Ready.

This classification records the pre-repair review state; the post-repair
resolution appears below.

## Findings

### Important: Exercise a multi-track row click through the Shared playback boundary

`shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShellJvmTest.kt:362-382` enables Favorite-only before the explicit row click, leaving a one-item projection, and then asserts a one-item queue. This cannot distinguish the required exact projected queue from an adapter that always queues only the clicked row; the feature-level test observes the callback but not `selectTrackFromTracks`/`PlaybackController`. Keep a query with at least two visible tracks for the explicit row click and assert the controller queue IDs in the complete projected order.

### Important: Run the selection regression through the compact Home route

`shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShellJvmTest.kt:304-319` is the only new Shared end-to-end query/selection test, but it mounts only at 1200dp, which selects the ListDetail branch and its direct Home composition. The compact branch at 420dp reaches Home through `LibraryRouteContent`’s `homeContent` callback, so a compact-only wiring regression could drop visible-ID reconciliation or queue behavior while every added test stays green. Execute the same boundary scenario for both adaptive widths with isolated state.

### Important: Assert active playback invariants when changing browse mode

`feature/library/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContentJvmTest.kt:222-225` only records that the feature `onPlayTrack` callback was not invoked; it has no `PlaybackController` or pre/post playback snapshot. Consequently, a Shared mode-change wiring regression that replaces the active queue, current track, generation, repeat/shuffle, or progress would still pass this test, despite mode changes being covered by the no-mutation contract. Add an active-controller boundary assertion for a flat-mode change, ideally alongside the compact/wide scenario.

## Production boundary review

The existing implementation establishes a single projection boundary: `LibraryHomeContent` computes `visibleTracks`, reports the complete `visibleTracks.map(Track::id)` sequence through `onVisibleTrackIdsChanged`, and passes that same sequence to row playback. `LibraryAppShell` routes the callback to `TrackSelectionAction.ReconcileVisible(HomeSongs, ids)`, and the reducer removes selected IDs absent from the projection while preserving retained IDs; `orderedSelectedTrackIds` follows the current visible sequence. The review found no production-source defect in the existing boundary.

## Scope and evidence

Reviewed commits `e5add1b0` and `1270d528` against `openspec/changes/sorting-filtering/specs/library-sorting-filtering/spec.md`, `openspec/changes/sorting-filtering/design.md`, and the Task 4 contract. The commits add tests and task ledger updates only; no formatter, linter, build, or full-suite command was run in this review lane.

## Repair status

The three Important evidence findings above are addressed by the follow-up
test-only repair. The Shared regression now runs at both compact `420.dp` and
wide `1200.dp` adaptive widths, keeps two tracks visible after the favorite
projection, and asserts the exact ordered queue received by the real
`PlaybackController` after an explicit row activation. It also snapshots active
playback before and after sort, favorite-filter, and Favorites-mode changes,
covering current track, queue, engine generation, repeat mode, shuffle mode,
status, position, duration, and normalized progress.

Focused verification for the repair passed:

```text
./gradlew :feature:library:impl:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryHomeContentJvmTest' --configuration-cache
BUILD SUCCESSFUL

./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryAppShellJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.TrackSelectionStateTest' --tests 'com.eterocell.rhythhaus.library.ui.LibraryBrowseQueryStateTest' --configuration-cache
BUILD SUCCESSFUL

git diff --check
(no output)
```

Task 4's later cross-platform, quality, independent-review, and archive tasks
remain pending; this status does not mark any later task complete.

## Post-repair resolution

The post-repair staged-diff review confirmed that all three findings are
addressed and found no residual production or test-contract defect. The
remaining pending work is limited to Task 4's later cross-platform, quality,
specification, independent-review, and archive gates.
