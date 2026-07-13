# Task 1 Report: Playback controller restart command

## Status

DONE

## Changed files

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt`
  - Added `PlaybackController.restartCurrentTrack()` beside the existing transport methods.
  - Loading requests autoplay after the in-flight load; idle/error reload the selected track with autoplay; loaded states serialize `seekTo(0L)` before `play()` in one engine action.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt`
  - Extended the recording engine with ordered load/seek/play/pause events and a suspendable load gate.
  - Added all eight restart and toggle regression tests required by the task brief.
- `.superpowers/sdd/task-1-report.md`
  - Records RED/GREEN evidence, scope, self-review, commit, and concerns.

## Strict RED evidence

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache
```

Result: `BUILD FAILED in 1s` during `:shared:compileTestKotlinJvm` before any production edit. The compiler reported seven `Unresolved reference 'restartCurrentTrack'` errors at the new restart test call sites. The same run also exposed one independent test generic-inference typo at the pause-event assertion; that assertion was corrected while production code remained unchanged. The required missing-command RED was therefore observed directly.

## GREEN evidence

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache
```

Result: `BUILD SUCCESSFUL in 4s`; `25 actionable tasks: 8 executed, 17 up-to-date`; configuration cache reused. All `PlaybackControllerTest` tests passed.

## Requirement coverage

- Playing, paused, and stopped loaded tracks emit exactly `Seek(0L), Play` after restart.
- Loading restart emits no stale seek, waits for the existing load, then plays.
- Error restart reloads the current track and autoplays.
- Queue IDs, repeat mode, and shuffle mode remain unchanged.
- Restart without a current track is a no-op.
- `togglePlayPause()` remains unchanged and does not seek.
- Seek and play execute inside one `launchEngineAction`, preserving engine mutex serialization and ordering.
- `PlatformPlaybackEngine`, platform engines, UI, dependencies, OpenSpec, roadmap, progress, plan, and spec files were not changed.

## Self-review

- Diff is limited to the controller, its direct tests, and this required report.
- Implementation matches the exact state branches and values in the task brief.
- No queue/repeat/shuffle mutation was introduced.
- No change was made to `togglePlayPause()` or platform interfaces/engines.
- Test engine records events before listener notifications and supports a gated load.
- No type suppression, skipped tests, deleted tests, unrelated refactor, or dependency change was introduced.

## Commit

- `53801dfd069ce3ade34bf75dc2beba86ec4dac85` — `feat: add current track restart command`

## Concerns

None.

## Review Fix

### Findings resolved

- Replaced the cross-thread `MutableList` event recorder with an unlimited coroutine `Channel`, and routed event recording, clearing, snapshots, waits, and assertions through channel operations with defined memory visibility.
- Added synchronous assertions that restart resets `positionMillis` to `0L` and clears `error` while loading remains gated and before the Error-state reload completes.
- Removed the arbitrary `delay(50)` from the no-current-track test; it now immediately compares the complete state and the concurrency-safe event snapshot.
- Kept `PlaybackController` production behavior unchanged.

### Changed files

- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt`
- `.superpowers/sdd/task-1-report.md`

### Verification

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache
```

Output: `BUILD SUCCESSFUL in 1s`; `25 actionable tasks: 5 executed, 20 up-to-date`; configuration cache reused. All `PlaybackControllerTest` tests passed.
