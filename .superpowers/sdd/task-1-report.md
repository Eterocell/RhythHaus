# Task 1 Report

## Scope

Implemented only Task 1 from `.superpowers/sdd/task-1-brief.md`. The unrelated `.cortexkit/` working-tree item was preserved and not staged. Existing `PlaylistEntry.id` identity, `items(..., key = { it.entry.id })`, and `savedPlaylistPlaybackRequest(..., row.entry.id)` semantics remain unchanged.

## RED evidence

Added the first JVM Compose interaction test before production policy changes:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --configuration-cache
```

Result: expected RED. `:shared:compileTestKotlinJvm FAILED` because the newly added policy assertions referenced the not-yet-existing `PlaylistDetailRowMode`, `PlaylistDetailRowAction`, `playlistDetailRowActions`, and `playlistDragTargetIndex(..., rowCount = ...)`; the new Compose test also failed against the pre-change always-editable row semantics. Gradle ended with `BUILD FAILED`.

The required common policy test command was also run in the RED phase:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --configuration-cache
```

Result: expected RED at test compilation for the same not-yet-existing production policy symbols. Gradle ended with `BUILD FAILED`.

## Implementation

- Added `PlaylistDetailRowMode` and `PlaylistDetailRowAction` projection policy.
- Default rows now expose playback content (artwork, title, artist/album metadata, and formatted duration) and hide drag/move/remove mutation controls.
- Added a stable row content description containing title, artist, album, and the exact formatted duration.
- Kept row playback wired through the existing entry-id-based `savedPlaylistPlaybackRequest` call.
- Added bounded drag target projection support through the optional row-count clamp.
- Raised edit-mode mutation controls to 44dp minimum targets without changing entry identity helpers.

## GREEN evidence

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --configuration-cache --rerun-tasks
```

Result: `BUILD SUCCESSFUL in 9s`; 1 test completed successfully.

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --configuration-cache --rerun-tasks
```

Result: `BUILD SUCCESSFUL in 8s`; focused suite completed successfully.

## Changed files

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreensTest.kt`
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistEditModeSemanticsJvmTest.kt`
- `.superpowers/sdd/task-1-report.md`

## Commit

Committed as the authorized task-scoped commit:

```text
feat: add playlist playback-first row policy
```

## Concerns

- Full repository verification was not requested for this task and was not run; focused JVM tests were run instead.
- `.cortexkit/` remains an unrelated untracked working-tree item and is intentionally not included.
