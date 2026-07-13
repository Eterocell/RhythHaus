# Task 4 Report: Playback session controller restore and checkpoints

## Status

DONE_WITH_CONCERNS

## Changed files

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionController.kt`
  - Adds the exact internal session-controller interface from the authoritative brief.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt`
  - Implements controller/platform command gating, paused restore, live reconciliation, complete immediate snapshots, and keyed playing-progress checkpoints.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt`
  - Adds focused command-gate, restore, reconciliation, checkpoint/coalescing/reset, stale-callback, and restore-load-failure coverage.

## Strict RED evidence

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache
```

Result: expected `BUILD FAILED in 1s` during `:shared:compileTestKotlinJvm`. The compiler reported unresolved `checkpoints`, `setCommandsEnabled`, `restoreSession`, and `reconcileSession` references at the new focused test call sites. No production files had been edited.

## GREEN evidence

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache
```

Result: `BUILD SUCCESSFUL in 1s`; `25 actionable tasks: 6 executed, 19 up-to-date`; the complete focused controller suite passed after the final checkpoint-reset test.

## Additional verification

- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryPlaybackSelectionTest' --configuration-cache` — `BUILD SUCCESSFUL in 985ms`; existing selection behavior passed.
- `./gradlew :shared:compileKotlinJvm --configuration-cache` — `BUILD SUCCESSFUL in 609ms`; common/JVM production compilation passed.
- `GIT_MASTER=1 git diff --check` — pass with no output.
- Kotlin `lsp_diagnostics` was attempted for all three changed Kotlin files, but `kotlin-ls` is not installed and installation was previously declined. Gradle is the compiler gate as required by the task context.

## Requirement coverage

- `setCommandsEnabled(false)` propagates to `PlatformPlaybackEngine.setUserTransportEnabled(false)` and makes queue/select, repeat/shuffle setters and toggles, play/pause/stop/seek/toggle/restart/skip APIs no-ops with no state mutation, engine action, or checkpoint.
- Generation-filtered engine callbacks remain accepted independently of the user-command gate.
- Restore reconciles saved IDs against supplied tracks, retains modes, regenerates only runtime shuffle order, loads paused, validates generation, clamps/seeks/pauses, publishes `Paused`, never calls play, and emits one normalized immediate snapshot.
- Missing restore current selects the first survivor at zero; no survivors clear with a new generation and retain modes; load failure applies an empty paused fail-safe state.
- Reconciliation preserves a surviving current item without reload, position, or status changes; otherwise it loads the first survivor paused at zero or clears when none survive. Every branch emits a complete immediate checkpoint.
- Complete immediate snapshots cover queue/current, seek, pause, stop, repeat/shuffle, restore normalization, and reconciliation.
- Playing progress is coalesced by `(activeGeneration, currentTrackId, whole-second bucket)` and the key resets on load/select/seek/stop/restore/reconcile. Immediate transitions do not share or suppress the progress key.
- Task 3 cancellation, generation provenance, engine mutex serialization, lazy artwork loading, and existing enabled repeat/shuffle/completion/restart behavior remain covered by the full focused controller suite.

## Self-review

- Scope is limited to the exact Task 4 implementation/test files plus this required Task 4 report.
- No Task 5 coordinator/store observation or Task 6 DI/App/lifecycle integration was added.
- No platform engine internals, dependencies, SQL, UI, OpenSpec, progress, or roadmap files were changed.
- Existing unrelated modifications to Task 1 and Task 2 reports were left untouched and are excluded from the Task 4 commit.
- No type suppression, skipped/deleted tests, exact shuffle-order persistence, amend, or history rewrite was used.

## Commit

- `59a1895` — `feat: restore paused playback sessions`
- The implementation commit contains the three exact production/direct-test files. This report is committed separately so implementation and durable evidence remain atomic.

## Concerns

- Kotlin LSP diagnostics are unavailable; focused Gradle tests and compilation are the source diagnostic evidence.
- The authoritative OpenSpec task checklist remains globally unchecked from prior task execution; this task intentionally does not modify OpenSpec artifacts per the user scope prohibition.

## Review findings fix

### Root causes

- Restore/reconcile released `engineMutex` after `loadPaused` validation and reacquired it for seek/pause. A newer operation could allocate/load another generation in that gap, allowing stale seek/pause to target the newer native item.
- `restartCurrentTrack`, manual next/previous, and completion-driven replacements changed persisted current/position state through `loadSelected` or direct position reset without their own immediate checkpoint.
- `MutableSharedFlow(extraBufferCapacity = 64)` plus unchecked `tryEmit` had no replay before collector startup and silently discarded checkpoints once the extra buffer was exhausted.
- `associateBy` selected the last supplied duplicate track while queue mapping retained repeated runtime IDs, so malformed runtime input had neither stable first-wins metadata nor unique queue order.

### RED evidence

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache
```

Result: the focused run reproduced the findings before production edits. `restoreAndReconcileEngineTransactionsCannotInterleave` failed because reconciliation replaced state while restore A remained between load acknowledgement and seek/pause. `restoreAndReconcileDeduplicateRuntimeTrackIdsWithStableFirstWinsMetadata` failed because duplicate supplied IDs selected last-occurrence metadata and repeated queue IDs remained. The pre-collector and slow-collector checkpoint tests did not complete under the lossy non-replay flow, and the command exceeded the 120-second test timeout after reporting the deterministic assertion failures.

### Fixes

- Added a session-operation mutex and hold it across restore/reconcile state normalization and the complete engine transaction. Generation allocation, `loadPaused`, returned-generation checks, clamp, seek, pause, and final state publication now run inside one `engineMutex` section; a newer session operation cannot begin between validation and seek/pause.
- Added exactly one immediate checkpoint after restart, successful manual next/previous replacement, and each completion-driven replacement. Existing `setQueue` and `selectTrack` emissions remain unchanged and are not duplicated.
- Replaced the shared flow with an unlimited `Channel<PlaybackCheckpoint>` exposed through `receiveAsFlow()`. The documented contract is one process-owned persistence consumer; synchronous producers use non-blocking `trySend`, delivery is ordered/lossless while open, and release closes the channel.
- Restore and reconciliation deduplicate supplied tracks and queue IDs with stable first-occurrence order/metadata. The persisted codec is unchanged.

### GREEN and regression evidence

- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache` — `BUILD SUCCESSFUL in 1s`; 40 focused controller tests passed.
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryPlaybackSelectionTest' --configuration-cache` — `BUILD SUCCESSFUL in 640ms`.
- `./gradlew :shared:compileKotlinJvm --configuration-cache` — `BUILD SUCCESSFUL in 3s`.
- `GIT_MASTER=1 git diff --check` — pass with no output.
- Kotlin LSP remains unavailable because `kotlin-ls` is not installed and installation was previously declined.

### Review-fix self-review

- The blocked-load test proves safe ordering: restore A completes seek/pause before reconciliation B can load/seek/pause its replacement.
- Existing Task 3 superseded-load cancellation coverage remains green; the added operation mutex does not replace engine serialization or swallow cancellation.
- Immediate transition tests verify zero-position/current snapshots before any playing-progress checkpoint.
- Checkpoint tests prove ordered delivery before collector startup and lossless delivery of 80 discrete mutations to a slow collector.
- Duplicate-ID tests prove stable first-wins metadata and unique order for restore input and an existing duplicate queue at reconciliation.
- Scope remains `Playback.kt`, `PlaybackControllerTest.kt`, and this Task 4 report. Task 1-2 report modifications remain untouched and excluded.

### Review-fix commits

- `92e5480` — `fix: harden playback session controller ordering`
- Durable report evidence is recorded in the following documentation commit.
