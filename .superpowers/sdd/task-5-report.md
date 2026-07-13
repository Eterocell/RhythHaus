# Task 5 Report: Playback Session FIFO Coordinator

## Scope

- Implemented only the Task 5 coordinator and focused common tests.
- Preserved the review-clean Task 1-4 controller, store, platform engines, dependencies, lifecycle wiring, UI, SQL, OpenSpec, progress, and roadmap files.
- Existing unrelated modifications to `.superpowers/sdd/task-1-report.md` and `.superpowers/sdd/task-2-report.md` were left untouched.

## Root design

- `PlaybackSessionCoordinator` owns one unlimited FIFO `Channel<Command>` and one process-scope actor coroutine.
- Commands are `Restore`, `Checkpoint`, `Reconcile`, and `Flush`.
- Only an immediately adjacent run of `Checkpoint` commands is drained and collapsed to its newest complete snapshot. The first non-checkpoint is retained and processed next, so restore, reconcile, and flush remain strict barriers.
- Restore disables controller/platform commands before reading, applies the persisted snapshot through `PlaybackSessionController`, and saves the normalized `sessionSnapshot()` before collection on normal success. Every non-cancelled restore terminal path then starts exactly one checkpoint collector, marks it ready, reenables commands, and only then exposes `Ready` or `FailedSafe`.
- Reconciliation submitted during restore stays in the actor queue. Normal reconciliation applies controller state, saves its normalized snapshot, and returns `Applied`. Failed-safe reconciliation still applies controller state in memory, skips persistence, and returns `FailedSafeApplied`.
- Unexpected read failure applies the empty paused controller boundary, reenables commands, completes restore, and enters process-lifetime `FailedSafe`.
- Any save failure transitions once to process-lifetime `FailedSafe`; later checkpoints and restores do not persist, while flush/reconcile/restore replies still complete.
- `flush()` awaits collector readiness, then awaits the ordered checkpoint fence before placing its actor barrier.

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
- Normal restore preserves normalized-save-before-collection order; failed and pre-existing `FailedSafe` restore paths use the same guarded terminal collector startup.
- Persistence is permanently disabled after the first read/save failure; no future waiter depends on actor failure propagation.
- No dependencies, platform engines, lifecycle/DI wiring, store semantics, SQL, UI, OpenSpec, progress, roadmap, or earlier task reports were changed.

## Concerns

- Abrupt process termination may still lose an in-flight write, matching the approved durability limit.
- Task 6 must provide the actual process-owned scope and singleton lifecycle wiring; this task intentionally does not implement it.

## Review fixes

### Root causes

- `flush()` used `yield()` to guess that the checkpoint collector had forwarded prior controller checkpoints. This provided no happens-before relationship between controller checkpoint transport, the collector, and the coordinator actor.
- The actor job was not retained. Actor cancellation or unexpected collector failure could close execution without closing/draining the command channel, so already accepted and future reply-bearing commands could wait forever.
- The checkpoint collector was an unsupervised process-scope child whose exceptional completion did not explicitly terminate the actor or reject future commands.
- Restore treated every throwable as an operational failure but had no direct regression for `controller.restoreSession()` throwing. The empty fallback reused the same method without verifying that a one-time throwing controller could recover safely.

### RED evidence

Command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' --tests 'com.eterocell.rhythhaus.PlaybackControllerTest.checkpointFenceCompletesAfterCollectorConsumesEveryPriorCheckpoint' --configuration-cache
```

Result: `BUILD FAILED` in `:shared:compileTestKotlinJvm` because `PlaybackSessionController.awaitCheckpointFence()` and `PlaybackController.awaitCheckpointFence()` did not exist. The controlled coordinator tests also required the missing deterministic fence and terminal actor contract.

### Fence contract

- The Task 4 checkpoint channel now transports internal ordered envelopes: complete checkpoint snapshots and fence acknowledgements.
- `PlaybackSessionController.awaitCheckpointFence()` inserts a fence into that same unlimited ordered channel and waits for the sole collector to consume every prior envelope.
- `PlaybackSessionCoordinator.flush()` awaits the controller fence before enqueueing its actor `Flush` barrier. No yield, delay, scheduler ordering, or snapshot reread is used.
- Collector startup occurs exactly once in restore's terminal `finally` path for both `Ready` and `FailedSafe`, before restore completion and command reenabling, so failed restore can still fence and flush without hanging.

### Shutdown contract

- The coordinator retains its actor and collector jobs.
- Actor termination closes the command channel, cancels the collector, and drains active, pending, and queued reply-bearing commands with the terminal exception.
- Future enqueue detects the closed channel/dead actor and throws immediately rather than orphaning a reply.
- Process cancellation is propagated as cancellation, not converted to persistence `FailedSafe`.
- Collector exceptional completion records the cause, closes the command channel, and cancels the actor; the actor's terminal drain then rejects current, queued, and future callers.

### GREEN and verification evidence

- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 6s`; controlled fence, 25-repeat race, cancellation drain, collector failure, and throwing restore included).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 6s`).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryPlaybackSelectionTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 668ms`).
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL in 5s`).
- `GIT_MASTER=1 git diff --check`: pass (no output).

### Review self-check

- Prior checkpoint emission is deterministically ordered before flush return.
- Flush during `FailedSafe` remains available because restore always starts the sole collector before completing.
- Cancellation cannot be mistaken for read/save failure.
- No accepted reply-bearing command is intentionally left without terminal completion.
- The throwing-controller test verifies first restore throws, empty fallback is attempted once and succeeds, commands reenable, `FailedSafe` persists for process lifetime, future reconcile/flush complete, and later checkpoints never save.
- No platform engine, store, dependency, Task 6 lifecycle/DI/App, SQL, UI, product documentation, OpenSpec, progress, roadmap, or Task 1-4 report changed.

### Review fix commits

- `3767734` — `fix: fence playback checkpoints before flush`
- `be17cc1` — `fix: complete playback coordinator shutdown`
- Durable review evidence commit follows.

## Collector readiness follow-up

### Root cause

The production checkpoint fence correctly rejects calls while its sole collector is inactive. Coordinator `flush()` called that fence immediately, so a flush concurrent with the first restore failed instead of waiting. The pre-existing `FailedSafe` restore branch also returned before terminal collector startup.

### RED evidence

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' --configuration-cache
```

Result: `BUILD FAILED`; four controlled tests failed. Flush during blocked successful restore, flush during a blocked throwing read, and cancellation while waiting completed too early because the inactive fence threw. Pre-restore reconcile save failure followed by restore also threw `collector inactive` because the early `FailedSafe` branch skipped collector startup.

### Readiness contract

- The coordinator owns one process-lifetime `CompletableDeferred<Unit>` for collector readiness.
- Flush awaits readiness before requesting the ordered controller fence and enqueueing `Flush`.
- Normal successful restore still saves normalized state before collection.
- Every non-cancelled restore terminal path, including throwing read/save/restore and pre-existing `FailedSafe`, starts the same collector exactly once, completes readiness, reenables commands, and completes restore.
- Actor/collector shutdown completes readiness exceptionally, so waiting flush callers cannot hang.
- Cancellation unwind does not start a replacement collector.

### GREEN evidence

- `PlaybackSessionCoordinatorTest`: pass (`BUILD SUCCESSFUL in 387ms`; 19 tests).
- Full `PlaybackControllerTest`: pass (`BUILD SUCCESSFUL in 4s`).
- `LibraryPlaybackSelectionTest`: pass (`BUILD SUCCESSFUL in 717ms`).
- `:shared:compileKotlinJvm`: pass (`BUILD SUCCESSFUL in 5s`).
- `GIT_MASTER=1 git diff --check`: pass.

### Readiness self-review

- Flush cannot invoke the production fence before the coordinator collector is active.
- Flush submitted during restore remains pending and then uses normal fence/barrier ordering.
- Early `FailedSafe` restore no longer bypasses collector startup, command reenabling, or reply completion.
- Collector startup remains single-instance across repeated restore calls.
- Cancellation and collector failure terminate readiness exceptionally and preserve actor draining.

### Readiness fix commits

- `584e73d` — `fix: await playback checkpoint collector readiness`
- Durable readiness evidence commit follows.
