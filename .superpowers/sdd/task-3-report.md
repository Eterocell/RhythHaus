# Task 3 Report: Contextual Bottom Bar and Multi-Track Picker

## Scope and route

- Route: `openspec+superpowers` / approved Task 3 execution.
- Worktree: `/var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-track-multi-select-playlist-backup`.
- Input: `.superpowers/sdd/task-3-brief.md`, accepted Task 1 commit `e0ab6e7`, and accepted Task 2 commit `64112b6`.
- No backup codec, repository import, platform document, Settings backup, OpenSpec, plan, progress, roadmap, dependency, or toolchain work was performed.

## Implementation

- `LibraryAppShell` now owns one remembered `TrackSelectionState` and dispatches all eligible Home Songs, album, artist, and Search callbacks through the Task 1 reducer.
- Back consumes active selection before Now Playing dismissal or route pop. Shell route push/pop/detail/recovery paths clear selection before transition, and the existing Home browse-mode dispatcher clears selection before leaving Songs.
- The shell derives picker input only with `orderedSelectedTrackIds` against the active page's visible order. Search visible IDs are captured from its existing reconciliation callback.
- `PlaylistPickerState`, `AddToPlaylistPickerState`, and `PlaylistInlineCreateRequest` now require ordered non-empty lists of non-blank IDs. Existing single-row entry points adapt to a singleton list; playlist-detail browser state and visible-order confirmation remain unchanged.
- Picker dismissal closes only the picker. Mutation failure retains picker and selection. Successful append or inline create closes the picker and dispatches `TrackSelectionAction.Completed`.
- Added reusable `TrackSelectionBar` with selected count, Cancel selection, and Add selected tracks to playlist. It reuses Haus palette and Miuix controls, system navigation-bar padding, and 44dp minimum button targets.
- The app shell renders exactly one active bottom-slot child. Selection precedes Now Playing; Hidden composes no input surface.
- The active slot is keyed and measured by `(LibraryBottomBarContent, heightPx)`. Lists accept only a matching active measurement as bottom clearance; offset and alpha reject null/stale measurements too, preventing stale fixed-height assumptions and pre-measure overlap.
- Home, Search, album/artist drill-down, playlist hub, and playlist detail receive the measured active clearance. Search's fixed `80.dp` spacer and drill-down's independent Now Playing renderer were removed.
- Added exact English/Chinese resources for singular/plural selected count, cancellation, and Add to Playlist semantics.

## Strict RED evidence

Initial focused RED command:

```text
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.ui.PlaylistStateTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' \
  --tests 'com.eterocell.rhythhaus.BottomBarModeTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' \
  --configuration-cache --rerun-tasks
```

Expected failure: `:shared:compileTestKotlinJvm` failed only on missing list-valued picker parameters and missing Task 3 bottom-content, measured-clearance, Back-decision, page-key, and semantics contracts.

Additional RED micro-cycles:

- Null/stale active measurement and picker success/failure retention tests failed only on missing `LibraryBottomBarMeasurement` and `trackSelectionActionAfterPickerOutcome`.
- Wide Home Songs selection precedence failed because the initial route-equality policy did not account for the visible list-detail master pane.
- Stale album selection on Playlist Hub failed until unsupported-route matching was explicit.
- Reviewer regression failed only on missing `activeBottomBarAlpha` before the stale/unmeasured rendering repair.

## GREEN evidence

Focused Task 1-3 command after implementation and reviewer fix:

```text
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.ui.TrackSelectionStateTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.PlaylistStateTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' \
  --tests 'com.eterocell.rhythhaus.BottomBarModeTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.TrackSelectionSemanticsJvmTest' \
  --tests 'com.eterocell.rhythhaus.search.SearchSelectionPoliciesJvmTest' \
  --configuration-cache --rerun-tasks
```

Result: `BUILD SUCCESSFUL in 12s`; 26/26 tasks executed.

Broad supported gate initially passed in 9s. A later attempt was accidentally run concurrently with the forced focused `jvmTest` process and produced widespread unrelated `NoClassDefFoundError` plus one playback assertion. Systematic debugging identified concurrent writes to the same shared test outputs as the differentiator: the simultaneous focused process passed. No source change was made. After stopping Gradle daemons and cleaning through Gradle, the same broad gate ran sequentially:

```text
./gradlew --stop
./gradlew :shared:clean --no-configuration-cache
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Result: clean passed, then broad `BUILD SUCCESSFUL in 6s`; 101 actionable tasks, 31 executed, 11 from cache, 59 up-to-date. This includes 325 JVM tests, desktop compilation, and Android debug assembly. The unchanged Android `MediaMetadata.Builder.setArtworkData` deprecation warning remains.

## Diagnostics, formatting, and diff hygiene

- Kotlin LSP diagnostics were unavailable because `kotlin-ls` is not installed and installation was previously declined. Gradle compilation/tests are the executable Kotlin diagnostics.
- `GIT_MASTER=1 git diff --check`: pass with no output.
- Root `spotlessCheck` is blocked by a pre-existing untouched import-order violation in `androidApp/build.gradle.kts`.
- `:shared:spotlessAndroidXmlCheck`: pass.
- `:shared:spotlessKotlinCheck` is blocked by 55 pre-existing untouched shared violations, including `AndroidPlaybackMediaSessionTest.kt`, `RhythHausTransportBridgeTest.kt`, and `PlaybackEngine.android.kt`. No out-of-scope formatting rewrite was performed.

## Review and visual evidence

- Behavior/lifetime reviewer: PASS with no Critical or Important findings. It confirmed shell ownership, Back/navigation/browse cleanup, Search reconciliation, `orderedSelectedTrackIds`, picker dismissal/failure/success retention, and unchanged playlist-detail browser behavior.
- Bottom-slot/UI reviewer initially found one Important stale/unknown measurement path in physical offset/initial visibility. A strict RED/GREEN repair made matching active measurement control clearance, offset, and alpha. Focused re-review: PASS with no remaining Critical or Important finding.
- Source-level design-system checks confirm Haus tokens, Miuix controls, 44dp targets, localized count/cancel/add semantics, one bottom child, and no hidden interception layer.
- Runtime visual/touch evidence remains unverified: no emulator/device/desktop screenshot run was performed. Compact/wide transition smoothness, pixel-level clearance, CJK rendering, focus traversal, and touch behavior are not claimed as visual passes.

## Changed Task 3 files

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/TrackSelectionBar.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistState.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt`
- `shared/src/commonMain/composeResources/values/strings.xml`
- `shared/src/commonMain/composeResources/values-zh/strings.xml`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/BottomBarModeTest.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistStateTest.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreensTest.kt`

Controller-owned dirty evidence explicitly excluded from staging and commit:

- `.superpowers/sdd/progress.md`
- `.superpowers/sdd/task-1-report.md`
- `.superpowers/sdd/task-2-report.md`
- `openspec/changes/track-multi-select-playlist-backup/tasks.md`

## Acceptance

- Requirement matched: yes for Task 3 automated/source scope.
- Scope controlled: yes.
- Focused and broad automated verification: pass.
- Review: pass after one reviewer-fix RED/GREEN cycle.
- Runtime visual/device acceptance: unverified and not claimed.
- Blockers: none for Task 3 implementation or commit.
