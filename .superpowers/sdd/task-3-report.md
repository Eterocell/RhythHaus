# Task 3 Report: Generation-Safe Paused Loads and Platform Transport Gating

## Scope

Implemented the Task 3 engine contract and production transport gates only. No Task 4 restore, checkpoint, reconciliation, coordinator, store, DI, application lifecycle, SQL, dependency, or UI behavior was added.

## Files

### Production

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt`
  - Added `LoadedPlayback`.
  - Replaced `load` with suspend `loadPaused(track, generation)`, plus `clear(generation)` and `setUserTransportEnabled`.
  - Tagged every stale-capable callback with a generation.
  - Made `PlaybackController` the sole monotonically increasing generation allocator and ignored stale callback generations.
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.android.kt`
  - Added generation+nonce `Media3RequestToken` values encoded in each `MediaItem` media ID.
  - Added observable-current-token filtering for player callbacks.
  - Cleared play intent before prepare and completed paused load only on matching READY.
  - Wired engine transport enablement to the process-shared bridge gate.
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/RhythHausTransportBridge.kt`
  - Added process-shared transport state and production-used command/action gate helpers.
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/RhythHausPlaybackService.kt`
  - Made `SkipRoutingPlayer` remove gated commands and reject play, pause, stop, seek, next, and previous before forwarding.
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/PlaybackDispatchers.android.kt`
  - Uses `Dispatchers.Default` for controller engine serialization so Android host tests and blocking preparation do not depend on an Android main Looper.
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt`
  - Added immutable generation/source-version capture for completion and progress sources.
  - Invalidated old sources on load, clear, and release.
  - Added pure internal `IOSRemoteTransportGate`; disabled handlers return command-failed without provider work.
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt`
  - Added immutable generation/source-version capture.
  - Retained transport state independently of the native handle and reapplied it immediately after handle creation/reset.
- `shared/src/nativeInterop/macos/rhythhaus_audio.mm`
  - Added native transport-enabled state checked by play, pause, toggle, stop, and seek remote handlers.
  - Added JNI test seams that execute the same native gate behavior.

### Tests and compile adaptations

- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelectionTest.kt`
- `shared/src/androidHostTest/kotlin/com/eterocell/rhythhaus/AndroidPlaybackMediaSessionTest.kt`
- `shared/src/androidHostTest/kotlin/com/eterocell/rhythhaus/RhythHausTransportBridgeTest.kt`
- `shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSNowPlayingInfoTest.kt`
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/JvmPlaybackEngineTest.kt`

## RED Evidence

Command:

```bash
./gradlew :shared:testAndroidHostTest --configuration-cache
```

Result: failed meaningfully during `:shared:compileAndroidHostTest` because the new contract and gates did not exist. Representative failures:

- unresolved `Media3RequestTokenTracker` and `Media3RequestToken`;
- `buildAndroidPlaybackMediaItem` did not accept a request token;
- unresolved `RhythHausTransportBridge.setTransportEnabled` and `forHostTest`;
- `PlaybackEngineListener` did not accept generation arguments;
- unresolved `LoadedPlayback`, `loadPaused`, `clear`, and `setUserTransportEnabled`.

This established RED before production edits.

## GREEN Evidence

Exact Task 3 verification chain:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest' --configuration-cache && \
./gradlew :shared:testAndroidHostTest --configuration-cache && \
./gradlew :shared:compileKotlinIosSimulatorArm64 --configuration-cache && \
GIT_MASTER=1 git diff --check
```

Result: pass.

- Focused common/JVM tests: `BUILD SUCCESSFUL`.
- Android host tests: `BUILD SUCCESSFUL` (existing `MediaMetadata.Builder.setArtworkData` deprecation warning only when compilation was required).
- iOS simulator Arm64 main compilation: `BUILD SUCCESSFUL`.
- `GIT_MASTER=1 git diff --check`: exit 0, no output.

## Existing Enabled Behavior Coverage

- Existing controller play, pause, stop, seek, restart, completion, repeat, shuffle, lazy artwork, and library-selection tests remain in the passing focused/host suites.
- Android host coverage proves enabled service/bridge play, seek, and next paths still forward.
- iOS pure gate coverage proves disabled play/seek performs no action and explicit reenable restores success.
- macOS bridge/native coverage proves disabled state survives reset/load, rejects remote play/seek without native effect, and explicit reenable restores seek/play behavior.
- Existing native macOS load, play, pause, seek, stop, progress, Now Playing, and remote-registration tests pass.

## Commits

- Implementation/test commit: recorded after this report was initially written.
- Durable report commit: recorded after the implementation commit.

## Self-Review

- Controller is the only generation allocator.
- Every stale-capable engine callback carries and is filtered by generation.
- Android uses a unique generation+nonce token and compares it with the observable current media item before callback acceptance.
- Android clears `playWhenReady`, pauses before prepare, and only acknowledges matching READY as paused.
- Android transport gating is process-shared and enforced in the production service wrapper, not only in the engine.
- iOS completion/progress sources capture immutable generation/source identity and old sources are invalidated.
- macOS bridge transport state is retained outside the native handle and applied before remote registration/acceptance after reset.
- No Task 4 persistence orchestration or restore behavior was introduced.
- User-owned changes in `.superpowers/sdd/task-1-report.md` and `task-2-report.md` were not staged or modified by this task.

## Concerns

- Kotlin LSP is unavailable by contract; Gradle and native compilation were used as diagnostic gates.
- iOS test sources were updated for the pure gate, but the authoritative Task 3 GREEN chain requires iOS main compilation rather than the full iOS test task. The iOS gate test is therefore compile-covered when the project later runs its full iOS simulator test convention.
- Android host tests cannot instantiate several Android framework-backed Media3 value objects without Robolectric-style implementations. Token encoding and production gate functions are tested as pure production-used seams; the production `SkipRoutingPlayer` wiring is compiler-checked in Android main.

## Review-Fix Addendum

### Root Causes

1. Android connection failure and matching-token `onPlayerError` reported errors but never settled `pendingLoad`, so `loadPaused()` could suspend forever and block later serialized engine work.
2. Controller engine-action handling caught `CancellationException` as a generic failure and published `PlaybackStatus.Error`. Android request cleanup was global mutable state without request ownership, so a superseded load's cancellation could clear a replacement.
3. Android's temporary `Dispatchers.Default` workaround removed Media3 Main-looper confinement. Connected calls and request-state mutation could run from arbitrary threads.
4. macOS scheduled callbacks captured generation/version only for the initial predicate, then `publishProgress()` read mutable active values during emission, allowing a replacement between check and publish.
5. macOS JNI test seams duplicated native remote-handler gate conditions and actions rather than exercising the same production functions.
6. Android host transport tests exercised a synthetic recorder instead of the adapter used by `SkipRoutingPlayer`.
7. Android release did not invalidate the token or cancel an outstanding load acknowledgement.

### Focused RED Evidence

Android request-state/router tests were added first and failed during Android-host test compilation with unresolved production seams:

```text
./gradlew :shared:testAndroidHostTest \
  --tests 'com.eterocell.rhythhaus.AndroidPlaybackMediaSessionTest' \
  --tests 'com.eterocell.rhythhaus.RhythHausTransportBridgeTest' \
  --configuration-cache

Unresolved reference 'AndroidPlaybackRequestState'.
Unresolved reference 'AndroidControllerOperations'.
Unresolved reference 'AndroidControllerExecutor'.
Unresolved reference 'ServiceTransportRouter'.
```

The controller cancellation regression then failed after cancellation was logged and converted through the generic error path; the test proved generation N was superseded while generation N+1 loaded, but the old cancellation was not treated as normal cancellation.

The macOS focused tests were added before production changes and failed compilation with:

```text
Unresolved reference 'MacProgressPublicationGate'.
Unresolved reference 'invokeRemotePauseForTest'.
Unresolved reference 'invokeRemoteToggleForTest'.
Unresolved reference 'invokeRemoteStopForTest'.
```

### Fixes

- Added synchronized `AndroidPlaybackRequestState` with request ownership, exceptional settlement, owner-only cancellation cleanup, clear/release invalidation, and replacement-safe token handling.
- Connection failure now atomically fails the active request; matching-token player error fails the matching request before publishing the listener error.
- `loadPaused` uses `finally` owner cleanup. `clear` and `release` cancel/invalidate outstanding requests.
- `PlaybackController.runEngineAction` rethrows `CancellationException` before generic error publication.
- Restored Android production Main-looper confinement through `AndroidControllerExecutor` and `AndroidControllerOperations`; production uses the application Main handler, while Android host tests use a deterministic single-thread fallback or an injected recording executor.
- Added production `ServiceTransportRouter`; `SkipRoutingPlayer` and host tests use its exact command-availability and routing methods for play, pause, stop, both seek overloads, next, and previous.
- Added `MacProgressPublicationGate`; progress callbacks carry immutable generation/version through position collection and revalidate immediately before progress and completion emission.
- Extracted native production remote functions for play, pause, toggle, stop, and seek. Both `MPRemoteCommand` handlers and JNI tests call those same functions.

### Review-Fix GREEN Evidence

Focused controller/macOS mechanisms:

```text
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.PlaybackControllerTest.supersededBlockedLoadCancelsNormallyAndReplacementCanBecomeReady' \
  --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest.staleMacProgressPublicationIsRejectedAfterSourceReplacement' \
  --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest.nativeRemoteOperationsShareProductionGateForAllCommands' \
  --configuration-cache
BUILD SUCCESSFUL
```

Focused Android failure/routing mechanisms:

```text
./gradlew :shared:testAndroidHostTest \
  --tests 'com.eterocell.rhythhaus.AndroidPlaybackMediaSessionTest' \
  --tests 'com.eterocell.rhythhaus.RhythHausTransportBridgeTest' \
  --configuration-cache
BUILD SUCCESSFUL
```

Complete required Task 3 chain:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest' --configuration-cache
BUILD SUCCESSFUL

./gradlew :shared:testAndroidHostTest --configuration-cache
BUILD SUCCESSFUL

./gradlew :shared:compileKotlinIosSimulatorArm64 --configuration-cache
BUILD SUCCESSFUL

GIT_MASTER=1 git diff --check
exit 0, no output
```

### Review-Fix Self-Review

- Every Android pending load reaches ready, exceptional failure, owner cancellation, clear, or release settlement.
- Superseded load cleanup checks token ownership and cannot clear a replacement request.
- Android controller operations are serialized through the production executor seam; production resolves to Main looper semantics.
- `SkipRoutingPlayer` and host tests share `ServiceTransportRouter`; no duplicate test-only routing logic remains.
- macOS publication uses captured generation/version for both progress and completion and revalidates after the deterministic race gate.
- Native remote handler/JNI test behavior shares the same five production operation functions.
- Enabled transport behavior remains covered.
- No Task 4, persistence coordinator, DI, App, OpenSpec, progress, roadmap, SQL, dependency, or UI files were touched.

### Review-Fix Commits

- Recorded after implementation verification.

### Review-Fix Concerns

- Kotlin LSP remains unavailable; Gradle and native compilation are the diagnostic gates.
- Android host runtime does not provide a real `Looper`; the production executor resolves to the Main handler on Android and uses a deterministic single-thread fallback only when framework Main-looper construction is unavailable in host tests.
