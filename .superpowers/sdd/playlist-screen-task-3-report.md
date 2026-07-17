# Playlist Screen Task 3 Report

Status: DONE_WITH_CONCERNS

Route: openspec+superpowers / strict RED-GREEN / systematic-debugging / five-lane review
Owner: implementation
Scope: OpenSpec playlist-screen tasks 3.1-3.3 only

## Outcome

- Added typed `QueueMutationResult` and `QueueMutationRejection` outcomes, including current, stale, invalid-index, and commands-disabled rejections.
- Added controller-owned suspend commands `reorderUpcoming`, `removeUpcoming`, and `clearUpcoming` under the existing `sessionOperationMutex` boundary.
- Commands target `QueueOccurrence.id`, preserve the immutable prefix through the current occurrence, and mutate only the physical upcoming suffix.
- Accepted commands atomically commit against the latest `PlaybackState` with `MutableStateFlow.compareAndSet`; a failed compare retries validation from the newest state so a completed queue/current transition cannot be overwritten.
- Accepted commands regenerate runtime shuffle membership and emit one immediate checkpoint built from the exact committed state through the existing controller channel and `PlaybackSessionCoordinator` persistence path.
- Queue edits do not invoke the engine, allocate a generation, replace/restart the current occurrence, or change loading, position, status, repeat, shuffle, or stale-callback generation checks.
- No UI/routes, saved-playlist repository, session format, dependency, or Task 4-7 implementation was changed.

## Strict TDD evidence

Initial RED:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' --configuration-cache
```

Failed at `:shared:compileTestKotlinJvm` only because `QueueMutationResult`, `QueueMutationRejection`, `reorderUpcoming`, `removeUpcoming`, and `clearUpcoming` did not exist.

Upcoming-segment RED:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest.upcomingMutationsPreserveHistoryBeforeCurrentAndRejectHistoryTargets' --configuration-cache
```

Failed at the expected history-target assertion because the first implementation treated every non-current occurrence as upcoming. The production fix now preserves all queue entries through the current occurrence and mutates only the suffix.

Latest-state race RED:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest.inFlightMutationCannotOverwriteACompletedQueueReplacement' --configuration-cache
```

Failed at the expected final-current assertion: an in-flight mutation derived from an old state and overwrote a completed `setOccurrenceQueue` replacement. The production fix uses compare-and-set retry and checkpoints the exact committed snapshot.

Commands-disabled RED:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest.disabledCommandsPropagateToTransportAndEveryPublicUserCommandIsNoOp' --configuration-cache
```

Failed at test compilation only because `QueueMutationRejection.CommandsDisabled` did not exist. The production gate is now a KMP-safe `MutableStateFlow<Boolean>` and returns the typed reason.

Focused GREEN after all fixes:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' --configuration-cache --rerun-tasks
BUILD SUCCESSFUL in 6s
```

Final ordinary focused run:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' --configuration-cache
BUILD SUCCESSFUL in 416ms
```

## Coverage

- Duplicate track occurrences: reorder moves only the targeted occurrence ID and retains both occurrences.
- Rejections: current occurrence, stale/missing/history occurrence, invalid upcoming index, and disabled commands leave state untouched; current/stale/invalid also emit no checkpoint.
- Concurrency: two concurrent removals produce one `Applied` and one stale rejection; a deterministic large-queue interleaving proves an in-flight mutation cannot overwrite a completed queue replacement.
- Segment semantics: history before current is preserved; clear removes only the upcoming suffix.
- Transport: paused position/status, current occurrence, repeat mode, shuffle mode, active generation, and stale-callback filtering are preserved.
- Loading: clear during an in-flight load causes no second load or engine event and allows the same load/generation to complete.
- Checkpoint cardinality: accepted reorder produces exactly one complete immediate checkpoint matching the committed session snapshot.
- Persistence: coordinator `flush()` persists the accepted occurrence queue as ordered `SessionQueueEntry(occurrenceId, trackId)` pairs through the existing store path.

## Verification

Clean supported matrix after `./gradlew --stop`:

```text
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --configuration-cache --rerun-tasks
BUILD SUCCESSFUL in 1m 53s
120 actionable tasks: 120 executed
```

This proves the complete shared JVM suite, desktop compilation, Android debug assembly, and iOS simulator main compilation. It is not an iOS simulator test claim; Task 7 owns the full iOS test/FK proof gate.

```text
openspec validate playlist-screen --strict
Change 'playlist-screen' is valid

GIT_MASTER=1 git diff --check
pass (no output)
```

## Final restore/reconcile watermark correction

Status: DONE_WITH_CONCERNS

### Root cause

The coordinator's highest persisted revision was local to checkpoint-command handling. Normalized restore and reconcile performed direct `store.save(controller.sessionSnapshot())` calls inside their actor barriers, but those successful saves did not carry a revision and did not advance the checkpoint watermark. A mutation A could publish revision A and pause before checkpoint delivery; reconcile could publish and directly persist newer B and return `Applied`; then delayed checkpoint A could arrive and overwrite durable B. The same gap existed after normalized restore. Eventual B checkpoint delivery was not a valid correctness mechanism because B delivery could be cancelled, failed, or indefinitely withheld.

### Coherent persistence contract

- `PlaybackSessionController.restoreSession` and `reconcileSession` now return `RevisionedPlaybackSessionSnapshot`, containing the exact final snapshot and its in-memory publication revision from one controller state read.
- `PlaybackController` returns a non-null revision for every production restore/reconcile path, including empty/fail-safe normalization, surviving-current reconciliation, replacement loading, and normal restore.
- `PlaybackSessionCoordinator` owns one `highestPersistedCheckpointRevision` across checkpoint commands, normalized restore, and reconcile.
- `persistRevisioned` saves first and advances the watermark only after `store.save` succeeds. Reconcile completes `Applied`, and restore completes its reply, only after direct persistence and watermark advancement occur in the actor barrier.
- Failed saves preserve existing failed-safe behavior and do not advance the watermark. Cancellation/reply behavior remains unchanged.
- Null revisions remain only as explicit compatibility for synthetic legacy test controllers that also emit null checkpoints; the production controller always participates in revised ordering.
- `PlaybackSessionSnapshot`, DataStore codec/keys, and serialized session format are unchanged; revision metadata remains in memory only.

### Strict deterministic RED

```text
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest.reconcileAppliedEstablishesRevisionWatermarkBeforeDelayedMutationCheckpoint' \
  --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest.normalizedRestoreEstablishesRevisionWatermarkBeforeOlderCheckpoint' \
  --configuration-cache
```

Result: `BUILD FAILED in 9s`; `2 tests completed, 2 failed` on the final durable-state assertions.

The reconcile regression uses the real `PlaybackController` and coordinator. Mutation A pauses after CAS inside injected shuffle generation before checkpoint enqueue. Reconcile publishes and directly persists newer B. A gated controller withholds checkpoint delivery, then captures stale A while withholding all newer checkpoints indefinitely. The test asserts B is durable immediately when reconcile returns `Applied`, releases only stale A, flushes, and asserts B remains durable. Therefore neither the immediate assertion nor the post-flush assertion can be repaired by B's checkpoint.

The restore regression directly persists a normalized revised snapshot, injects an older revised checkpoint after `restoreOnce` returns, flushes, and observed the old checkpoint regress durability before the fix.

### GREEN evidence

Two regressions:

```text
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest.reconcileAppliedEstablishesRevisionWatermarkBeforeDelayedMutationCheckpoint' \
  --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest.normalizedRestoreEstablishesRevisionWatermarkBeforeOlderCheckpoint' \
  --configuration-cache
BUILD SUCCESSFUL in 7s
26 actionable tasks: 8 executed, 18 up-to-date
```

Complete controller/coordinator/DI suites, uncached:

```text
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' \
  --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' \
  --tests 'com.eterocell.rhythhaus.di.RhythHausDiTest' \
  --configuration-cache --rerun-tasks
BUILD SUCCESSFUL in 9s
90 tests completed
26 actionable tasks: 26 executed
```

Clean supported matrix:

```text
./gradlew --stop
./gradlew :shared:jvmTest \
  :desktopApp:compileKotlin \
  :androidApp:assembleDebug \
  :shared:compileKotlinIosSimulatorArm64 \
  --configuration-cache --rerun-tasks
BUILD SUCCESSFUL in 29s
111 actionable tasks: 111 executed
```

```text
openspec validate playlist-screen --strict
Change 'playlist-screen' is valid

GIT_MASTER=1 git diff --check
pass (no output)
```

Kotlin LSP diagnostics were requested for all five changed Kotlin files but `kotlin-ls` remains unavailable because installation was previously declined. Gradle compilation/tests provide executable Kotlin diagnostics.

### Self-review

- Verified the actor cannot process checkpoint A during restore/reconcile, and the direct B save plus watermark update occurs before the barrier reply, making `Applied`/successful restore a durable ordering guarantee.
- Verified watermark advancement follows successful save rather than preceding it; failed saves enter failed-safe and cannot suppress later checkpoints based on an unpersisted revision.
- Verified production restore/reconcile return exact snapshot/revision pairs from a single final state read, eliminating the old post-operation mutable-state reread.
- Verified B checkpoint delivery is withheld indefinitely in the reconcile regression; correctness does not rely on eventual repair.
- Verified normalized restore participates in the same watermark and rejects a later older checkpoint.
- Verified existing save-failure, throwing restore/reconcile, failed-safe lifetime, barrier ordering, null-only compatibility, duplicate checkpoint collection, and DI startup tests remain green.
- Verified no serialized persistence format, dependencies, UI, routes, saved-playlist work, or Tasks 4-7 changed.

### Re-completed task state

- [x] 3.1
- [x] 3.2
- [x] 3.3
- Tasks 4-7 remain unchecked.

This report remains intentionally uncommitted. Implementation/tests and OpenSpec task wording/status are committed with explicit staging; no push is performed.

Final restore/reconcile watermark commits:

- `2a861a4 refactor: expose revisioned session publications`
- `bf29e6b fix: order direct session persistence revisions`
- `5ff5f63 docs: record actor-ordered session persistence`
- All commits include the required Sisyphus footer and coauthor; no push was performed.

Post-commit focused verification:

```text
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' \
  --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' \
  --tests 'com.eterocell.rhythhaus.di.RhythHausDiTest' \
  --configuration-cache
BUILD SUCCESSFUL in 611ms

openspec validate playlist-screen --strict
Change 'playlist-screen' is valid

GIT_MASTER=1 git diff --check
pass (no output)
```

Kotlin LSP diagnostics were requested for all three modified Kotlin files but are unavailable because `kotlin-ls` is not installed and installation was previously declined. Gradle compilation/tests are the executable Kotlin diagnostics.

## Review

Initial five lanes:

- Hands-on QA: PASS.
- Security/robustness: PASS, no Critical/High findings.
- Goal/context/code-quality lanes: initially FAIL because a mutation could overwrite a completed synchronous queue/current transition.

Resolution:

- Added the deterministic failing interleaving regression.
- Replaced plain state assignment with compare-and-set retry against the latest state.
- Bound the checkpoint to the exact committed state.
- Added the explicit commands-disabled reason and state-flow command gate.
- Two fresh targeted re-reviews then returned PASS with no blocking correctness or OpenSpec 3.1-3.3 issue.

Self-review:

- Scoped diff reviewed against the Task 3 brief and OpenSpec 3.1-3.3.
- `PlaybackState.queue` remains occurrence-native; no track-ID targeting was introduced.
- Current occurrence and history prefix are preserved.
- Existing Task 2 session encoding, store, coordinator, engine generation, and callback provenance were not altered.
- No UI, navigation, resources, repository, dependencies, or Tasks 4-7 were touched.

## Concerns and non-blockers

- A verification attempt while review agents were concurrently running Gradle failed with a missing `in-progress-results-generic.bin`; after all reviewers completed, daemons were stopped and the clean 120-task matrix passed.
- One subsequent uncached focused-class run transiently failed the pre-existing asynchronous test `stopAfterCurrentStopsAtCurrentTrackEndWithoutAdvancing`; the clean full matrix had already passed the entire suite, the isolated test then passed five consecutive uncached runs, and the final focused class run passed. No Task 3 production path was implicated.
- Existing `Channel.UNLIMITED` checkpoint buffering and controller shutdown behavior were noted as broader hardening opportunities; Task 3 preserves the established persistence transport and does not expand lifecycle scope.
- Unrelated pre-existing edits in `.superpowers/sdd/progress.md`, `task-1-report.md`, and `task-2-report.md` were preserved and must not be staged with Task 3.

## OpenSpec state

- [x] 3.1
- [x] 3.2
- [x] 3.3
- Tasks 4-7 remain unchecked.

## Commits

- `20329f7 feat: add serialized upcoming queue commands`
- `79444dd docs: complete serialized queue command tasks`
- No push performed.
- This report remains uncommitted as required.

Next owner: Task 4 implementation only after this Task 3 slice is committed/reviewed.
Blockers: none for Task 3. Task 7 iOS simulator/FK proof remains a later whole-change gate.

## Checkpoint-order correction

Status: DONE_WITH_CONCERNS

### Root cause

The original compare-and-set fix protected runtime state from lost updates but did not order state publication with checkpoint enqueue. An accepted mutation could CAS state A, pause inside runtime shuffle regeneration before enqueueing checkpoint A, allow `setOccurrenceQueue` to publish replacement state B and enqueue checkpoint B, then resume and enqueue older checkpoint A. The checkpoint channel and `PlaybackSessionCoordinator` correctly preserved enqueue order, so durable state regressed from B to obsolete A even though runtime state remained B.

A post-enqueue `_state` equality check would retain a race window and was rejected. Serializing only the mutation method would also be insufficient because synchronous queue/current publishers such as queue replacement and selection compete outside `sessionOperationMutex`.

### Design

- Added an in-memory monotonic `checkpointRevision` to `PlaybackState` and `PlaybackCheckpoint`; `PlaybackSessionSnapshot`, DataStore keys, codec, and serialized session format are unchanged.
- Queue/current state publishers assign a revision only as part of their successful state CAS publication. Failed CAS attempts retry against latest state and receive a later revision, so revision order follows successful publication order rather than operation-start order.
- Queue replacement, selection/skip/completion through `loadSelected`, restore/reconcile queue publication, empty fail-safe queue publication, and accepted upcoming mutations use the shared CAS publication mechanism.
- Immediate checkpoints carry the revision of the exact state snapshot they represent. Progress checkpoints capture snapshot and revision from one `PlaybackState` read.
- The coordinator chooses the highest revision among adjacent production checkpoints and drops any later-arriving checkpoint older than the highest revision already persisted. Every accepted mutation still emits its required complete checkpoint; only a checkpoint proven obsolete by a newer committed revision is prevented from regressing durability.
- Checkpoints with `revision = null` retain the existing FIFO/last-adjacent behavior used by existing direct coordinator tests and non-production test controllers.

### Strict RED

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest.delayedMutationCheckpointCannotRegressNewerReplacementSnapshot' --configuration-cache
```

Result: `BUILD FAILED in 3s`; one test failed at the final durable replacement assertion. The deterministic test paused mutation A after its CAS but before checkpoint enqueue, completed replacement B and checkpoint B, resumed A, flushed the real coordinator, and observed obsolete A persisted after B.

### GREEN

Focused regression:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest.delayedMutationCheckpointCannotRegressNewerReplacementSnapshot' --configuration-cache
BUILD SUCCESSFUL in 5s
```

Complete controller/coordinator classes, uncached:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' --configuration-cache --rerun-tasks
BUILD SUCCESSFUL in 7s
26 actionable tasks: 26 executed
```

Clean supported matrix:

```text
./gradlew --stop
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --configuration-cache --rerun-tasks
BUILD SUCCESSFUL in 1m 40s
111 actionable tasks: 111 executed
```

```text
openspec validate playlist-screen --strict
Change 'playlist-screen' is valid

GIT_MASTER=1 git diff --check
pass (no output)
```

Kotlin LSP diagnostics were requested for all modified Kotlin files but remain unavailable because `kotlin-ls` is not installed and installation was previously declined. Gradle compilation/tests are the executable Kotlin diagnostics.

### Self-review

- Verified revisions are attached to successful state publication, not merely reserved operation order; a lower revision cannot publish after a higher revision without failing CAS and retrying.
- Verified replacement B receives a higher queue/current revision than paused mutation A, and delayed checkpoint A is ignored after B is durable.
- Verified every accepted mutation still sends one complete immediate checkpoint with its own committed snapshot/revision.
- Verified adjacent checkpoint coalescing selects the highest revision rather than the latest arrival when both revisions are present.
- Verified progress snapshot and revision come from one captured state.
- Verified the persisted `PlaybackSessionSnapshot` model, codec, DataStore format, engine generation, loading/position/status, repeat/shuffle modes, and stale callback protections are unchanged.
- Verified no UI, routes, dependencies, saved repository, or Tasks 4-7 were changed.

### Re-completed task state

- [x] 3.1
- [x] 3.2
- [x] 3.3
- Tasks 4-7 remain unchecked.

External authoritative re-review is delegated to the controller per user direction. This report remains uncommitted; the implementation and OpenSpec task status are committed explicitly.

Checkpoint-order correction commits:

- `75d8578 fix: preserve playback checkpoint publication order`
- `095c8a4 docs: record publication-ordered queue checkpoints`
- No push performed.

## Task 3 re-review coherent fix wave

Status: DONE_WITH_CONCERNS

### Reopened scope

- OpenSpec 3.1-3.3 were changed from checked to unchecked before production edits and re-completed only after implementation and verification.
- Tasks 4-7 remained unchecked. No UI, routes, saved-playlist repository, dependencies, or serialized DataStore format changed.

### Root cause and architecture

- `repeatMode`, `shuffleMode`, seek position, and playing progress changed persisted `PlaybackSessionSnapshot` content through direct state copies while retaining the prior `checkpointRevision`. A delayed mutation checkpoint with the same revision could therefore overwrite newer durable content.
- Every state publication that changes queue, current occurrence, position, repeat mode, or shuffle mode now uses the revisioned CAS publisher. Immediate and playing-progress checkpoints use the snapshot and revision from the exact returned/captured `PlaybackState`; equal revision therefore implies identical persisted content.
- Status/error/duration-only copies may retain revision because those fields are not serialized in `PlaybackSessionSnapshot`.
- Runtime shuffle order is now a revision-monotonic `RevisionedShuffleOrder` bound to exact source queue IDs and shuffle mode. Queue/mode changes regenerate order; current/position/status-only publications carry a still-valid order forward. A delayed lower-revision A cannot overwrite B, and an order for A's queue cannot be consumed for B.
- Coordinator compatibility is explicit: null/null checkpoints retain last-adjacent FIFO behavior before revised checkpoints appear; revised checkpoints outrank null during coalescing; after a revised checkpoint is durable, null checkpoints are ignored; revised checkpoints older than the highest persisted revision are ignored.
- `PlaybackSessionSnapshot`, codec, DataStore keys, and serialized session format remain unchanged.

### Strict deterministic RED

Command:

```text
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest.newerRepeatModeSurvivesDelayedMutationCheckpoint' \
  --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest.newerShuffleModeSurvivesDelayedMutationCheckpoint' \
  --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest.newerSeekPositionSurvivesDelayedMutationCheckpoint' \
  --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest.newerPlayingProgressSurvivesDelayedMutationCheckpoint' \
  --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest.delayedMutationShuffleCannotOverwriteNewerQueueShuffleOrder' \
  --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest.nullRevisionCannotRegressDurableStateAfterRevisedCheckpoint' \
  --configuration-cache
```

First invocation exposed a test-helper compile error (`async` without a `CoroutineScope` receiver); only the helper receiver was corrected. The strict behavioral rerun produced `BUILD FAILED in 3s`, `6 tests completed, 6 failed`:

- repeat expected `RepeatOne`, durable value was `StopAfterQueue`;
- shuffle expected `Off`, durable value was `On`;
- seek expected `700`, durable value was `0`;
- playing progress failed its newer-position assertion;
- runtime shuffle expected `upcoming-b-2`, skip selected `upcoming-b-1`;
- mixed compatibility expected revised `revised`, durable value was `legacy-null`.

Each controller persistence race pauses mutation A after queue CAS and before its checkpoint by blocking the injected shuffle factory, applies the newer state publication, resumes A, flushes the real `PlaybackSessionCoordinator`, and checks the durable store. The runtime shuffle test uses distinct A/B generated orders and asserts navigation follows B after A resumes.

### GREEN evidence

Six unchanged regressions:

```text
./gradlew :shared:jvmTest [the six --tests selectors above] --configuration-cache
BUILD SUCCESSFUL in 4s
26 actionable tasks: 8 executed, 18 up-to-date
```

Complete controller/coordinator classes, uncached:

```text
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' \
  --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' \
  --configuration-cache --rerun-tasks
BUILD SUCCESSFUL in 7s
26 actionable tasks: 26 executed
```

The first clean matrix attempt executed 111/111 tasks but had one unrelated timing-sensitive controller assertion: `stopAfterCurrentStopsAtCurrentTrackEndWithoutAdvancing` observed `Loading` instead of `Stopped`. With no code change, that exact test passed twice with `--rerun-tasks`; a fresh stopped-daemon full matrix then passed. This is recorded as a scheduling flake rather than concealed.

Final clean supported matrix:

```text
./gradlew --stop
./gradlew :shared:jvmTest \
  :desktopApp:compileKotlin \
  :androidApp:assembleDebug \
  :shared:compileKotlinIosSimulatorArm64 \
  --configuration-cache --rerun-tasks
BUILD SUCCESSFUL in 28s
111 actionable tasks: 111 executed
```

The matrix ran 328 JVM tests, desktop Kotlin compilation, Android debug assembly, and iOS simulator main compilation. Full iOS simulator tests remain Task 7 scope and are not claimed here.

```text
openspec validate playlist-screen --strict
Change 'playlist-screen' is valid

GIT_MASTER=1 git diff --check
pass (no output)
```

Kotlin LSP diagnostics were requested for all three modified Kotlin files but `kotlin-ls` remains unavailable because installation was previously declined. The clean Gradle compile/test matrix is the executable Kotlin diagnostic evidence.

### Self-review

- Verified all `PlaybackSessionSnapshot` fields (`queue`, `currentOccurrenceId`, `positionMillis`, `repeatMode`, `shuffleMode`) advance revision when changed.
- Verified immediate checkpoints use the exact published state rather than rereading mutable state after publication; progress checkpoints use one captured revisioned state.
- Verified coordinator drops delayed lower revisions and null checkpoints after revised durability begins, while preserving null-only test-controller compatibility.
- Verified shuffle publication is monotonic by revision and bound to queue IDs/mode, and existing stable shuffled navigation remains covered by the complete controller suite.
- Verified accepted Task 3 mutations still publish one complete immediate checkpoint and preserve transport state, generation, and stale callback protections.
- Verified no serialized persistence model/format, dependencies, UI, routes, saved-playlist work, or Tasks 4-7 changed.

### Re-completed task state

- [x] 3.1
- [x] 3.2
- [x] 3.3
- Tasks 4-7 remain unchecked.

The report remains intentionally uncommitted. Implementation/tests and OpenSpec task status are committed separately with explicit staging; no push is performed.

Final commits:

- `873e582 fix: order all playback snapshot publications`
- `2b8fb74 docs: record revision-bound queue checkpoints`
- Both commits include the required Sisyphus footer and coauthor; no push was performed.

Post-commit focused verification:

```text
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' \
  --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' \
  --configuration-cache
BUILD SUCCESSFUL in 633ms

openspec validate playlist-screen --strict
Change 'playlist-screen' is valid

GIT_MASTER=1 git diff --check
pass (no output)
```
