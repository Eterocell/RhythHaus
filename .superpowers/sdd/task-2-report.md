# Playlist Screen Task 2 Report

Status: DONE

## Scope

- Implemented OpenSpec `playlist-screen` tasks 2.1-2.5 only.
- Preserved the committed Task 1 invariant: saved-playlist playback accepts `PlaylistEntry.id` as `QueueOccurrence.id` through the occurrence-native selection/controller APIs.
- Did not implement queue editing commands, routes, UI, or Task 7 evidence lifecycle.
- Did not modify Task 1 persistence APIs or dependencies.

## RED evidence

Initial RED command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --tests 'com.eterocell.rhythhaus.library.LibraryPlaybackSelectionTest' --tests 'com.eterocell.rhythhaus.session.PlaybackSessionSnapshotTest' --tests 'com.eterocell.rhythhaus.session.PlaybackSessionStoreJvmTest' --configuration-cache
```

Result: expected failure at `:shared:compileTestKotlinJvm` because `QueueOccurrence`, `SessionQueueEntry`, occurrence selection/controller methods, and ordered-pair codec APIs did not exist.

Additional strict-TDD RED cycles:

- `sessionQueueCodecRetainsTheExistingMaximumQueueCount` failed with `IllegalArgumentException`, proving pair framing accidentally halved the 10,000-entry boundary before the frame-count fix.
- `snapshotCodecRejectsDuplicateOccurrencesAndCurrentOutsideQueue` failed because `encodeSnapshot` accepted invalid snapshots before the review fix.

## GREEN implementation

- `PlaybackState.queue` is `List<QueueOccurrence>` with `currentOccurrenceId`; `currentTrack` remains a derived compatibility view.
- Generic visible lists receive fresh queue occurrence IDs; occurrence-native selection accepts stable saved-playlist entry IDs.
- Current lookup, restart selection, skip, shuffle, progress checkpoint keys, restore, and reconciliation use occurrence identity.
- Engine loads continue to receive `QueueOccurrence.track`; media identity remains `track.id`.
- `PlaybackSessionSnapshot` stores ordered `SessionQueueEntry(occurrenceId, trackId)` values and an explicit current occurrence.
- DataStore writes one framed `queue_entries` pair value, writes explicit `current_occurrence_id`, and removes legacy queue/current keys on successful save.
- Legacy `queue_ids`/`current_id` reads normalize deterministically to `legacy-<index>-<trackId>` occurrences; coordinator restore persists the normalized new snapshot.
- Duplicate occurrence IDs are rejected; duplicate track IDs are preserved.
- Restore remains paused without autoplay, and surviving-current reconciliation updates metadata without reload or transport/position change.

## Verification

- Focused Task 2 command: PASS (`BUILD SUCCESSFUL`; all selected playback, selection, snapshot, and store tests).
- `./gradlew :shared:jvmTest --configuration-cache`: PASS before final review; final supported matrix below reran the complete JVM suite.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: PASS after review fix (`BUILD SUCCESSFUL in 4s`).
- `./gradlew :shared:compileKotlinIosSimulatorArm64 --configuration-cache`: PASS after review fix (`BUILD SUCCESSFUL in 8s`).
- `openspec validate playlist-screen --strict`: PASS (`Change 'playlist-screen' is valid`).
- `GIT_MASTER=1 git diff --check`: PASS.
- Kotlin LSP diagnostics unavailable because `kotlin-ls` is not installed and installation was previously declined; Gradle compilation/tests were used as executable language checks.
- Existing Android `MediaMetadata.Builder.setArtworkData` deprecation warning only.

## Self-review

- Found and fixed the pair-frame count boundary so 10,000 queue entries remain supported under the existing 1 MiB encoded-size limit.
- Independent review found one Important issue: public `encodeSnapshot` did not validate duplicate occurrence IDs or current membership. Added a failing regression, fixed validation, and reran all gates.
- Independent review found no Critical issues and approved queue identity, engine identity, DataStore/legacy behavior, paused restore, duplicate reconciliation, and scope after excluding the fixed finding.
- Final scoped diff review found no Task 3-7 implementation and no Task 1 API changes.

## Files

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelection.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionSnapshot.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionStore.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelectionTest.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionSnapshotTest.kt`
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionStoreJvmTest.kt`
- `openspec/changes/playlist-screen/tasks.md` (2.1-2.5 only)

`PlaybackSessionController.kt` required no source change because its existing snapshot/track method signatures already carry the occurrence-aware snapshot through the coordinator boundary.

## Commits

- `db98aa1` `feat: preserve queue occurrence identity`
- `25423e5` `feat: persist playback queue occurrences`
- `b83e78d` `docs: complete occurrence-aware playback tasks`

## Blockers and concerns

- No Task 2 blocker.
- Full iOS simulator tests were not required for this focused Task 2 slice; iOS simulator main compilation passed. The repository's known common-test `Thread` blocker remains owned by Task 7.
- Pre-existing dirty `.superpowers/sdd/progress.md` and `.superpowers/sdd/task-1-report.md` were preserved and excluded from staging.
- This scratch report is intentionally not committed per the task instruction.

## Follow-up: bounded occurrence IDs

Status: DONE

### Finding and root cause

The original generated forms, `queue-<counter>-<trackId>` and `legacy-<index>-<trackId>`, embedded the complete library-track ID. A valid track ID at `PlaybackSessionCodec.maxIdCharacters` (4,096 characters) therefore produced an invalid occurrence ID longer than the same codec limit. `DataStorePlaybackSessionStore.save()` correctly rejected the normalized/checkpoint snapshot, and `PlaybackSessionCoordinator` entered process-lifetime `FailedSafe`.

OpenSpec tasks 2.2-2.5 were reopened before tests and re-completed only after the focused and supported verification below. Task 2.1 remained complete because `PlaylistEntry.id == QueueOccurrence.id` for saved-playlist playback was unchanged.

### Strict RED

Tests were added first in `PlaybackSessionStoreJvmTest` using the real `Preferences` DataStore, real `PlaybackController`, and real `PlaybackSessionCoordinator`:

- a legacy track ID exactly 4,096 characters, restored and normalized through the coordinator;
- a generic visible-list track ID exactly 4,096 characters, checkpointed and flushed through the coordinator.

Command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionStoreJvmTest.legacyMaximumLengthTrackRestoresAndNormalizesWithoutFailedSafe' --tests 'com.eterocell.rhythhaus.session.PlaybackSessionStoreJvmTest.genericMaximumLengthTrackCheckpointPersistsWithoutFailedSafe' --configuration-cache
```

Result: expected failure, `BUILD FAILED in 2s`; 2 tests completed, 2 failed. Both failed at the assertion expecting `PlaybackSessionPhase.Ready`, proving persistence rejected the oversized generated occurrence IDs and moved the coordinator to `FailedSafe`.

### GREEN implementation

- Generic visible-list occurrence IDs are now `queue-<controller UUID>-<monotonic counter>`. The existing cross-platform `uuid4()` seam provides a bounded controller namespace, and the counter provides process-local uniqueness without including track content.
- Legacy occurrence IDs are now deterministic `legacy-<queue index>` values. Queue index guarantees uniqueness within every valid legacy queue and preserves deterministic current-occurrence normalization, including duplicate track IDs.
- Track IDs remain unchanged in `SessionQueueEntry.trackId`; saved-playlist occurrence IDs remain the durable `PlaylistEntry.id`; codec count, character, UTF-8, and total encoded-size limits are unchanged.
- Existing shuffle tests now resolve occurrence IDs through the queue rather than parsing track IDs from their representation, and explicitly verify generated ID uniqueness and bounds.

Boundary GREEN command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionStoreJvmTest.legacyMaximumLengthTrackRestoresAndNormalizesWithoutFailedSafe' --tests 'com.eterocell.rhythhaus.session.PlaybackSessionStoreJvmTest.genericMaximumLengthTrackCheckpointPersistsWithoutFailedSafe' --configuration-cache
```

Result: pass, `BUILD SUCCESSFUL in 5s`.

Focused Task 2 command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --tests 'com.eterocell.rhythhaus.library.LibraryPlaybackSelectionTest' --tests 'com.eterocell.rhythhaus.session.PlaybackSessionSnapshotTest' --tests 'com.eterocell.rhythhaus.session.PlaybackSessionStoreJvmTest' --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' --configuration-cache
```

Result after final UUID-seam refactor: pass, `BUILD SUCCESSFUL in 5s`; 101 selected tests, zero failures.

Supported matrix:

```text
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Result: pass, `BUILD SUCCESSFUL in 4s`; 101 actionable tasks: 12 executed, 89 up-to-date. Only the existing Android `MediaMetadata.Builder.setArtworkData` deprecation warning was emitted.

```text
./gradlew :shared:compileKotlinIosSimulatorArm64 --configuration-cache
```

Result: pass, `BUILD SUCCESSFUL in 4s`; 20 actionable tasks: 4 executed, 16 up-to-date.

### Self-review

- Generic IDs are independent of raw track length and unique across controller namespaces and counters within the process.
- Legacy IDs are independent of raw track length, deterministic for the same ordered legacy queue, and unique by queue index even when track IDs repeat.
- Both real-stack boundary tests assert coordinator `Ready`, persisted track/current identity, legacy-key removal, and bounded occurrence IDs.
- Duplicate selection, current occurrence, shuffle, restore, reconciliation, codec bounds, paused restore, and failed-safe behavior remain covered by the 101-test focused group.
- No Task 3 command, public persistence requirement, dependency, route, or UI change was introduced.
- Kotlin LSP remains unavailable because `kotlin-ls` is not installed and installation was previously declined; Gradle compilation/tests are the executable language checks.

### Follow-up commits

- `e83dac8` `fix: bound generic playback occurrence IDs`
- `d5eaa26` `fix: bound legacy playback occurrence IDs`
- Evidence commit: the commit containing this report append.
