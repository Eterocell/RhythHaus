# Task 5 Report: Playback Session FIFO Coordinator

## Scope

- Implemented only the Task 5 coordinator and focused common tests.
- Preserved the review-clean Task 1-4 controller, store, platform engines, dependencies, lifecycle wiring, UI, SQL, OpenSpec, progress, and roadmap files.
- Existing unrelated modifications to `.superpowers/sdd/task-1-report.md` and `.superpowers/sdd/task-2-report.md` were left untouched.

## Root design

- `PlaybackSessionCoordinator` owns one unlimited FIFO `Channel<Command>` and one process-scope actor coroutine.
- Commands are `Restore`, `Checkpoint`, `Reconcile`, and `Flush`.
- Only an immediately adjacent run of `Checkpoint` commands is drained and collapsed to its newest complete snapshot. The first non-checkpoint is retained and processed next, so restore, reconcile, and flush remain strict barriers.
- Restore disables controller/platform commands before reading, applies the persisted snapshot through `PlaybackSessionController`, saves the controller's normalized `sessionSnapshot()`, starts exactly one single-consumer checkpoint collector, reenables commands, and only then exposes `Ready` or `FailedSafe`.
- Reconciliation submitted during restore stays in the actor queue. Normal reconciliation applies controller state, saves its normalized snapshot, and returns `Applied`. Failed-safe reconciliation still applies controller state in memory, skips persistence, and returns `FailedSafeApplied`.
- Unexpected read failure applies the empty paused controller boundary, reenables commands, completes restore, and enters process-lifetime `FailedSafe`.
- Any save failure transitions once to process-lifetime `FailedSafe`; later checkpoints and restores do not persist, while flush/reconcile/restore replies still complete.
- `flush()` yields once before placing its actor barrier so a checkpoint already emitted into the controller's process-owned single-consumer flow can enqueue before the barrier.

## Strict TDD evidence

### RED

Command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' --configuration-cache
```

Result: `BUILD FAILED` during `:shared:compileTestKotlinJvm` because `PlaybackSessionCoordinator`, `PlaybackSessionPhase`, and `PlaybackSessionReconcileResult` were absent. This was the expected meaningful RED before production implementation.

The tests cover:

- command gating and restore order;
- normalized save before checkpoint collection;
- reconciliation queued during restore;
- adjacent-checkpoint-only collapse;
- restore/reconcile/flush barriers;
- flush completion;
- save failure;
- throwing read;
- restore load normalization failure;
- process-lifetime failed-safe future callers;
- one checkpoint collector after repeated restore.

### GREEN

Command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' --configuration-cache
```

Result: `BUILD SUCCESSFUL in 1s`; 10 focused coordinator tests passed.

## Verification

- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' --configuration-cache`: pass.
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 690ms`).
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL in 2s`).
- `GIT_MASTER=1 git diff --check`: pass (no output).
- Kotlin LSP is unavailable in this repository; Gradle compilation is the required compiler gate.

## Files

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinatorTest.kt`
- `.superpowers/sdd/task-5-report.md`

## Commits

- `d2abf52` — `feat: coordinate playback session persistence`
- Durable report commit follows this implementation commit.

## Self-review

- The actor is the sole persistence serialization boundary.
- Checkpoint collapse does not consume across a barrier.
- Restore replies and command reenabling are in a finally-equivalent path.
- The collector starts only after normalized restore persistence succeeds and is guarded against duplicates.
- Persistence is permanently disabled after the first read/save failure; no future waiter depends on actor failure propagation.
- No dependencies, platform engines, lifecycle/DI wiring, store semantics, SQL, UI, OpenSpec, progress, roadmap, or earlier task reports were changed.

## Concerns

- Abrupt process termination may still lose an in-flight write, matching the approved durability limit.
- Task 6 must provide the actual process-owned scope and singleton lifecycle wiring; this task intentionally does not implement it.
