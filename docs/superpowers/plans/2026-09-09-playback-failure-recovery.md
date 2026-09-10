# Playback Failure Recovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development` (recommended) or `executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make terminal local-playback failures safely actionable through shared retry, skip, and queue-only removal controls.

**Architecture:** `:core:playback` owns structured failures and every queue transition. Platform engines map only their reliable native evidence into that contract; opaque errors remain `Unknown`. `:feature:nowplaying` consumes `PlaybackState` and invokes controller commands without seeing platform errors or library state.

**Tech Stack:** Kotlin Multiplatform, coroutines/StateFlow, Media3, AVFoundation bridge, macOS JNI bridge, Compose Multiplatform, Compose resources, Kotlin test, Compose JVM UI test.

**Spec:** `docs/superpowers/specs/2026-09-09-playback-failure-recovery-design.md`; `openspec/changes/playback-failure-recovery/specs/playback-failure-recovery/spec.md`

## Global Constraints

- Keep all platform-native exception/error-code APIs behind `:core:playback` source sets; Shared and `:feature:nowplaying` receive only `PlaybackError` and `PlaybackFailureKind`.
- `PlaybackError.kind` defaults to `Unknown`; no existing caller may need a migration-only placeholder.
- Classify only factual platform evidence. Boolean/opaque failures must remain `Unknown`.
- Recovery never deletes a media file, SQLDelight track row, library source, or playlist entry.
- Recovery actions apply only while commands are enabled and the controller has a current error occurrence.
- Skip never wraps. Remove chooses its successor from the pre-removal effective queue order, preserving shuffle behavior and duplicate occurrence IDs.
- Existing generation guards, `sessionOperationMutex`, `engineMutex`, immediate checkpoints, and cancellation behavior remain authoritative.
- The Now Playing recovery surface and all of its pointer/semantics actions exist only for an active error state.
- New Compose resources are added in English and Simplified Chinese with identical key sets.
- Do not change Media3 service lifecycle, iOS interruption/route-loss behavior, macOS route-loss behavior, source scanning, persistence schema, or platform dependencies.

---

### Task 1: Shared structured failure contract

**Files:**
- Modify: `core/playback/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt:66-75, 1012-1028`
- Modify: `core/playback/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt`

**Interfaces:**
- Produces `PlaybackFailureKind { MissingFile, AccessLost, UnsupportedFormat, DecoderFailure, Unknown }`.
- Produces `PlaybackError(message: String, cause: String? = null, kind: PlaybackFailureKind = Unknown)`.
- Produces `PlaybackFailureException(error: PlaybackError) : IllegalStateException(error.message)`.
- Consumes `PlatformPlaybackEngine.loadPaused` failures and existing `PlaybackEngineListener.onPlaybackError` callbacks.

- [ ] **Step 1: Write failing common behavior tests**

Add tests that call `loadPaused` through the existing `RecordingPlaybackEngine` configured to throw `PlaybackFailureException(PlaybackError("Missing", kind = MissingFile))`. Assert the controller state keeps `MissingFile` and its message rather than generic `Playback failed`. Add the parallel ordinary `IllegalStateException("opaque")` assertion for `Unknown`.

```kotlin
@Test
fun structuredLoadFailureSurvivesControllerAsyncCatch() = runBlocking {
    val controller = PlaybackController(
        RecordingPlaybackEngine(
            loadFailure = PlaybackFailureException(
                PlaybackError("File is no longer available", kind = PlaybackFailureKind.MissingFile),
            ),
        ),
    )
    val track = testTracks(1).single()
    controller.setQueue(listOf(track), track.id)
    awaitState { controller.state.value.status == PlaybackStatus.Error }

    assertEquals(PlaybackFailureKind.MissingFile, controller.state.value.error?.kind)
    assertEquals("File is no longer available", controller.state.value.error?.message)
}
```

- [ ] **Step 2: Run the focused test to verify RED**

Run:

```bash
./gradlew :core:playback:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest.structuredLoadFailureSurvivesControllerAsyncCatch' --configuration-cache
```

Expected: compilation failure because `PlaybackFailureKind` / `PlaybackFailureException` do not exist, or assertion failure because the controller overwrites the error.

- [ ] **Step 3: Add the minimal common failure types and preservation branch**

Place the enum and exception next to `PlaybackError`. Keep compatibility with default `Unknown`. In `runEngineAction`, use the embedded error only for `PlaybackFailureException`; retain the current generic fallback otherwise.

```kotlin
val failure = (throwable as? PlaybackFailureException)?.error
    ?: PlaybackError(
        message = "Playback failed",
        cause = throwable.message ?: throwable::class.simpleName,
    )
onPlaybackError(activeGeneration.value, failure)
```

Do not make a non-failure exception classify itself from string matching.

- [ ] **Step 4: Run focused core failure tests to verify GREEN**

Run the new structured and opaque-fallback tests plus existing stale-error coverage:

```bash
./gradlew :core:playback:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest.structuredLoadFailureSurvivesControllerAsyncCatch' --tests 'com.eterocell.rhythhaus.PlaybackControllerTest.opaqueLoadFailureUsesUnknownKind' --tests 'com.eterocell.rhythhaus.PlaybackControllerTest.staleEngineCallbacksAreIgnored' --configuration-cache
```

Expected: all pass.

- [ ] **Step 5: Commit Task 1**

```bash
git add core/playback/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt \
  core/playback/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt
git commit -m "feat: classify playback failures"
```

### Task 2: Queue-only error recovery commands

**Files:**
- Modify: `core/playback/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt:430-620, 1012-1125`
- Modify: `core/playback/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinatorTest.kt` only if an existing checkpoint harness is required to observe the production checkpoint stream.

**Interfaces:**
- Consumes Task 1 `PlaybackStatus.Error`, `PlaybackState.currentOccurrence`, `effectiveOrder()`, `sessionOperationMutex`, and existing engine test double.
- Produces `fun retryFailedTrack()`, `fun skipFailedTrack()`, and `suspend fun removeFailedTrack(): QueueMutationResult`.
- Produces only `QueueMutationResult.Applied`, `QueueMutationResult.Rejected(CommandsDisabled)`, or `QueueMutationResult.Rejected(StaleOccurrence)`; never a new rejection enum without a demonstrated consumer need.

- [ ] **Step 1: Write failing controller tests for each recovery transition**

Add exact behavior tests:

```kotlin
@Test
fun retryFailedTrackReloadsTheSameOccurrenceWithAutoplay() = runBlocking { /* error current; assert Load(current), Play */ }

@Test
fun skipFailedTrackLoadsEffectiveSuccessorWithoutWrapping() = runBlocking { /* shuffled queue; assert successor */ }

@Test
fun skipFailedTrackAtEffectiveEndLeavesErrorUnchanged() = runBlocking { /* no successor; assert no engine events */ }

@Test
fun removeFailedTrackRemovesOnlyCurrentOccurrenceAndAutoplaysSuccessor() = runBlocking { /* duplicate track IDs; assert exact occurrence IDs */ }

@Test
fun removeFinalFailedTrackClearsEngineAndPublishesIdleWithRemainingQueue() = runBlocking { /* assert current null/status Idle */ }

@Test
fun recoveryCommandsAreNoOpsOutsideEnabledCurrentErrorState() = runBlocking { /* idle, paused, disabled */ }
```

For removal, collect the existing checkpoint stream or use the existing session coordinator harness and assert its snapshot excludes only the failed occurrence.

- [ ] **Step 2: Run the focused recovery tests to verify RED**

Run:

```bash
./gradlew :core:playback:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest.retryFailedTrackReloadsTheSameOccurrenceWithAutoplay' --tests 'com.eterocell.rhythhaus.PlaybackControllerTest.removeFinalFailedTrackClearsEngineAndPublishesIdleWithRemainingQueue' --configuration-cache
```

Expected: unresolved controller commands.

- [ ] **Step 3: Implement error-gated retry and skip**

Both commands begin with one private helper that returns the current occurrence only when `commandsEnabled.value` and state has both `status == Error` and `error != null`. Retry invokes `loadSelected(current, autoPlay = true)`. Skip calculates `nextTrack(wrap = false)` before loading; it does nothing when absent.

```kotlin
private fun failedCurrentOccurrence(): QueueOccurrence? =
    _state.value.takeIf { commandsEnabled.value && it.status == PlaybackStatus.Error && it.error != null }
        ?.currentOccurrence
```

- [ ] **Step 4: Implement serialized current-occurrence removal**

Inside `sessionOperationMutex`, re-check the failed current occurrence for every iteration. Capture `nextTrack(wrap = false)` before removing current. CAS-publish a queue without only that `id`; publish its immediate checkpoint. If the captured successor remains, call `loadSelected(successor, autoPlay = true)` after publish. If absent, cancel pending load, reset progress checkpoint state, generate a new generation, clear the engine under `engineMutex`, then publish `PlaybackState(queue = remaining, status = Idle, currentOccurrenceId = null, repeatMode = previous.repeatMode, shuffleMode = previous.shuffleMode)` and checkpoint it.

Do not use `applyUpcomingQueueMutation`, because it preserves the failed current occurrence by design. Do not select a predecessor or wrap after removal.

- [ ] **Step 5: Run focused recovery and checkpoint tests to verify GREEN**

Run every new recovery test plus the existing upcoming-mutation and session checkpoint tests that touch queue mutations:

```bash
./gradlew :core:playback:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest.*FailedTrack*' :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest.*remove*' --configuration-cache
```

Expected: all pass, and no library-facing dependency is introduced.

- [ ] **Step 6: Commit Task 2**

```bash
git add core/playback/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt \
  core/playback/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt \
  shared/src/commonTest/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinatorTest.kt
git commit -m "feat: recover failed playback queue entries"
```

### Task 3: Platform-local failure classification

**Files:**
- Modify: `core/playback/src/androidMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.android.kt:160-190, 365-390`
- Modify: `core/playback/src/androidHostTest/kotlin/com/eterocell/rhythhaus/AndroidPlaybackMediaSessionTest.kt`
- Modify: `core/playback/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt:165-245, 325-355`
- Modify: `core/playback/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSAudioPlayerBridgeTest.kt`
- Modify: `core/playback/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt:25-75`
- Modify: `core/playback/src/jvmTest/kotlin/com/eterocell/rhythhaus/JvmPlaybackEngineTest.kt`

**Interfaces:**
- Consumes Task 1 `PlaybackError`, `PlaybackFailureKind`, and `PlaybackFailureException`.
- Produces internal platform mapping helpers that return `PlaybackError`; tests invoke helpers rather than constructing Android/Apple/JNI runtime error paths.
- Android mapping consumes `PlaybackException.errorCode` and optional concrete local `File` existence; iOS/macOS mapping consumes only their managed local-path availability plus opaque failure state.

- [ ] **Step 1: Write failing platform classification tests**

Add small, pure mapping tests:

```kotlin
@Test fun androidFileNotFoundMapsToMissingFile() { /* Media3 error code -> MissingFile */ }
@Test fun androidPermissionFailureMapsToAccessLost() { /* permission error code -> AccessLost */ }
@Test fun androidUnsupportedContainerMapsToUnsupportedFormat() { /* parsing code -> UnsupportedFormat */ }
@Test fun androidDecoderFailureMapsToDecoderFailure() { /* decoder code -> DecoderFailure */ }
@Test fun iosMissingManagedPathMapsToMissingFile() { /* absent path */ }
@Test fun opaqueIosProviderFailureRemainsUnknown() { /* false callback */ }
@Test fun missingMacPathMapsToMissingFile() { /* temp absent File */ }
@Test fun opaqueMacNativeLoadFailureRemainsUnknown() { /* bridge false */ }
```

- [ ] **Step 2: Run focused platform tests to verify RED**

Run:

```bash
./gradlew :core:playback:testAndroidHostTest --tests 'com.eterocell.rhythhaus.AndroidPlaybackMediaSessionTest.*MapsTo*' :core:playback:iosSimulatorArm64Test --tests 'com.eterocell.rhythhaus.IOSAudioPlayerBridgeTest.*MapsTo*' :core:playback:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest.*MapsTo*' --configuration-cache
```

Expected: compilation failure because mapping helpers do not exist.

- [ ] **Step 3: Implement Android mapping and structured terminal load failure**

Keep Media3 imports in Android only. Map only documented codes. Use a single helper similar to:

```kotlin
internal fun androidPlaybackError(error: PlaybackException): PlaybackError =
    PlaybackError(
        message = "Android could not play this audio file.",
        cause = error.message ?: error.errorCodeName,
        kind = androidFailureKind(error.errorCode),
    )
```

When `loadPaused` completes exceptionally, complete it with `PlaybackFailureException(mappedError)` and notify the listener with that same value. Do not map service-connection failure beyond `Unknown`.

- [ ] **Step 4: Implement iOS/macOS managed-path preflight and opaque fallback**

Before invoking a native loader, verify a resolved managed file path exists. Emit/throw `MissingFile` only if it does not. Convert current no-detail iOS provider failure and `MacAudioPlayerBridge.load == false` to `PlaybackFailureException(PlaybackError(..., kind = Unknown))`; do not derive a type from title/path text. Preserve all existing resource cleanup on iOS failure.

- [ ] **Step 5: Run focused platform suites to verify GREEN**

Run the exact Task 3 selectors and current engine regression classes:

```bash
./gradlew :core:playback:testAndroidHostTest :core:playback:iosSimulatorArm64Test --tests 'com.eterocell.rhythhaus.IOSAudioPlayerBridgeTest' :core:playback:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest' --configuration-cache
```

Expected: all pass. Record only actual simulator/device availability.

- [ ] **Step 6: Commit Task 3**

```bash
git add core/playback/src/androidMain core/playback/src/androidHostTest \
  core/playback/src/iosMain core/playback/src/iosTest \
  core/playback/src/jvmMain core/playback/src/jvmTest
git commit -m "feat: classify platform playback failures"
```

### Task 4: Error-only Now Playing recovery UI

**Files:**
- Modify: `feature/nowplaying/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingContent.kt:45-75, 220-345`
- Modify: `feature/nowplaying/src/commonMain/composeResources/values/strings.xml`
- Modify: `feature/nowplaying/src/commonMain/composeResources/values-zh/strings.xml`
- Modify: `feature/nowplaying/src/jvmTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingContentSemanticsJvmTest.kt`
- Modify: `feature/nowplaying/src/commonTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingContractsTest.kt` only if the module's actual common contract location is discovered during implementation; do not invent a second resource ledger.

**Interfaces:**
- Consumes Task 1 `PlaybackError.kind`, Task 2 commands, current `NowPlayingContent` status block, and existing `TransportButton` styling.
- Produces internal error-recovery test tags: `NowPlayingRetryFailureTestTag`, `NowPlayingSkipFailureTestTag`, and `NowPlayingRemoveFailureTestTag`.
- Produces no public Android-specific API and no action outside current `Error` state.

- [ ] **Step 1: Write failing JVM semantics tests**

Mount production `NowPlayingContent` with a real `PlaybackController` and error state. Assert exactly one of each recovery tag/action exists, invoke each one in isolated test cases, and assert only the expected controller/engine transition occurs. Mount paused/loading state and assert all three tags do not exist.

```kotlin
onNodeWithTag(NowPlayingRetryFailureTestTag).performClick()
waitForIdle()
assertEquals(listOf(EngineEvent.Load("second"), EngineEvent.Play), engine.events())

onNodeWithTag(NowPlayingRetryFailureTestTag).assertDoesNotExist()
onNodeWithTag(NowPlayingSkipFailureTestTag).assertDoesNotExist()
onNodeWithTag(NowPlayingRemoveFailureTestTag).assertDoesNotExist()
```

- [ ] **Step 2: Run semantics tests to verify RED**

Run:

```bash
./gradlew :feature:nowplaying:jvmTest --tests 'com.eterocell.rhythhaus.nowplaying.NowPlayingContentSemanticsJvmTest.*FailureRecovery*' --configuration-cache
```

Expected: unresolved recovery tags/actions or assertion failures because no recovery surface exists.

- [ ] **Step 3: Add resources and render the conditional surface**

Add listener-language resources for category summaries plus `Retry`, `Skip`, and `Remove from queue`, with matching Simplified Chinese keys. Derive a small internal UI model only if it prevents repeating category translation. In `NowPlayingControlsPane`, render the recovery section after the status text only when `playbackState.status == Error`, `playbackState.error != null`, and `playbackState.currentOccurrence != null`.

Use the project’s existing visible button language/semantics. The remove action uses `rememberCoroutineScope().launch { playbackController.removeFailedTrack() }`; retry/skip invoke synchronous controller commands. Do not disable these actions based on unrelated source-mutation state.

- [ ] **Step 4: Run focused semantics and resource tests to verify GREEN**

Run:

```bash
./gradlew :feature:nowplaying:jvmTest --tests 'com.eterocell.rhythhaus.nowplaying.NowPlayingContentSemanticsJvmTest' :feature:nowplaying:jvmTest --tests 'com.eterocell.rhythhaus.nowplaying.NowPlayingContractsTest' --configuration-cache
```

Expected: all pass. If a resource ownership ledger exists elsewhere, run its exact class instead of adding an incidental text-key test.

- [ ] **Step 5: Commit Task 4**

```bash
git add feature/nowplaying/src/commonMain feature/nowplaying/src/commonTest \
  feature/nowplaying/src/jvmTest
git commit -m "feat: add now playing failure recovery"
```

### Task 5: Final verification, acceptance, and closeout

**Files:**
- Modify: `openspec/changes/playback-failure-recovery/tasks.md`
- Modify: `progress.md`
- Modify: `roadmap.md`
- Archive after complete: `openspec/changes/archive/YYYY-MM-DD-playback-failure-recovery/`

**Interfaces:**
- Consumes completed Tasks 1–4 and the exact requirements in the linked OpenSpec delta.
- Produces verified OpenSpec task state and durable handoff evidence. No source-code API is produced.

- [ ] **Step 1: Review the assembled change before broad verification**

Use LSP references for each exported/changed core controller symbol, inspect `git diff --check`, and confirm the diff contains no source/library deletion operation or SQLDelight schema change. Remove dead legacy error UI/tests only when the new observable behavior supersedes them.

- [ ] **Step 2: Run focused and required automated validation**

Run each command separately:

```bash
./gradlew :core:playback:jvmTest :core:playback:testAndroidHostTest :core:playback:iosSimulatorArm64Test :feature:nowplaying:jvmTest --configuration-cache
./gradlew spotlessApply --configuration-cache
./gradlew spotlessCheck --configuration-cache
./gradlew detekt --configuration-cache
./gradlew architectureCheck --configuration-cache
./init.sh
openspec validate playback-failure-recovery --strict
git diff --check
```

Expected: every command returns success. Record any unavailable iOS simulator/device condition exactly; do not substitute a compile result for runtime acceptance.

- [ ] **Step 3: Execute available platform smoke acceptance**

For each available platform with a known playable local source, first load/play it, then make that selected path unavailable without deleting library metadata. Confirm its failure card shows; confirm retry returns error while still unavailable; confirm skip selects the next queued occurrence when present; confirm remove changes only the queue; restore/reselect a playable source and confirm the recovery surface disappears. Record unavailable hardware/system paths as blockers, not passes.

- [ ] **Step 4: Update durable records and commit/archive**

Check completed OpenSpec tasks, append exact evidence/blockers to `progress.md`, update the P0 row in `roadmap.md`, sync specs if the repository process requires it, and archive only after all acceptance tasks are complete. Commit source, specifications, plan, and handoff with one conventional message:

```bash
git add core/playback feature/nowplaying openspec docs/superpowers/specs \
  docs/superpowers/plans progress.md roadmap.md
git commit -m "feat: recover local playback failures"
```

## Plan self-review

- **Spec coverage:** Task 1 covers structured fail-closed errors; Task 2 covers retry/skip/removal, effective-order and checkpoint invariants; Task 3 covers platform-local classification; Task 4 covers error-only UI/accessibility/localization; Task 5 covers validation, platform smoke, records, and archival.
- **No-placeholder check:** The plan defines symbols, file boundaries, test names, commands, transition rules, and explicit non-goals. No open-ended implementation placeholders remain.
- **Type consistency:** `PlaybackFailureKind`, `PlaybackError`, `PlaybackFailureException`, `retryFailedTrack`, `skipFailedTrack`, and `removeFailedTrack` are defined once in Task 1/2 and used consistently in later tasks.
