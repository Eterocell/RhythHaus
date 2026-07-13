# Final Release Playback Persistence Fixes

Base: `65f7c9e`

## Android callback provenance after READY

- Root cause: `AndroidPlaybackRequestState.ready()` removed the only token-to-generation state, while callbacks and progress used either the now-empty pending request or mutable `activeGeneration`.
- RED: focused Android-host compilation failed because durable observable provenance/capture/revalidation APIs did not exist.
- GREEN: pending settlement is separate from immutable observable source provenance. READY retains provenance; replacement, clear, cancellation ownership, failure, and release invalidate it as appropriate. Progress captures token/generation and revalidates the current MediaItem immediately before emission.
- Tests: READY follow-up callback provenance, replacement rejection, and blocked old progress capture rejection.
- Files: `PlaybackEngine.android.kt`, `AndroidPlaybackMediaSessionTest.kt`.
- Commit: `4d7092b fix: retain android loaded source provenance`.

## Reconciliation replacement-load failure durability

- Root cause: `PlaybackController.reconcileSession()` converted replacement load failure into an empty paused success, so the coordinator persisted the transient empty state and returned `Applied`.
- RED: controller regression expected the load failure to propagate after coherent fail-safe state, but no exception escaped.
- GREEN: reconciliation still applies a coherent empty paused in-memory state, then rethrows the non-cancellation failure. The coordinator enters process-lifetime `FailedSafe`, returns `FailedSafeApplied`, and never saves the transient state or later checkpoints/reconciles.
- Tests: direct controller propagation/state regression and coordinator durable save-count/content/future-operation regression.
- Files: `Playback.kt`, `PlaybackControllerTest.kt`, `PlaybackSessionCoordinatorTest.kt`.
- Commit: `148b218 fix: preserve durable session on reconcile failure`.

## Natural terminal completion checkpoint

- Root cause: `stopAtCurrentTrackEnd()` changed status and exact terminal position but emitted no immediate checkpoint; persistence remained at the last progress bucket.
- RED: StopAfterCurrent/terminal StopAfterQueue test timed out waiting for a terminal checkpoint.
- GREEN: the shared terminal-stop helper resets progress deduplication and emits one complete immediate snapshot. Replacement branches retain their existing checkpoint path and receive no duplicate.
- Tests: exact one checkpoint for both terminal modes using `1234ms`, plus coordinator flush persistence without another progress callback.
- Files: `Playback.kt`, `PlaybackControllerTest.kt`, `PlaybackSessionCoordinatorTest.kt`.
- Commit: `6b474ce fix: checkpoint terminal playback completion`.

## macOS remote handler lifetime

- Root cause: each native handle registered five process-global targets but retained no returned tokens, so release/reset could not remove exact registrations and dead weak handlers accumulated.
- RED: JVM native test failed to compile because the production native live-count seam did not exist.
- GREEN: each `RhythHausAudioPlayer` retains command/target pairs from the real registration path, registration remains idempotent, and native release removes each exact target before relinquishing the handle. The synchronized production instrumentation reports aggregate live registrations.
- Tests: duplicate registration stays at five; repeated reset/register cycles return to zero then five; final release reports zero.
- Files: `PlaybackEngine.jvm.kt`, `rhythhaus_audio.mm`, `JvmPlaybackEngineTest.kt`.
- Commit: `de0783f fix: release macos remote command handlers`.

## Verification evidence

- Focused Android-host regression: pass (`AndroidPlaybackMediaSessionTest`, `BUILD SUCCESSFUL in 9s`).
- Focused controller/coordinator reconciliation regressions: pass (`BUILD SUCCESSFUL in 3s`).
- Focused terminal controller/coordinator regressions: pass (`BUILD SUCCESSFUL in 2s`).
- Focused native handler lifetime regression: pass (`BUILD SUCCESSFUL in 4s`).
- Focused suites: `PlaybackControllerTest`, `PlaybackSessionCoordinatorTest`, `JvmPlaybackEngineTest`, and `LibrarySourceManagementTest`: pass (`BUILD SUCCESSFUL in 1m 17s`).
- Full Android-host suite: pass (`BUILD SUCCESSFUL in 4s`).
- iOS simulator main compilation: `:shared:compileKotlinIosSimulatorArm64`: pass (`BUILD SUCCESSFUL in 5s`). No iOS test pass is claimed; the known common-test `Thread` blocker remains.
- `GIT_MASTER=1 git diff --check`: pass.
- Full release command: `./gradlew :shared:jvmTest :shared:testAndroidHostTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 1m 18s`, 113 tasks).
- LSP: Kotlin diagnostics unavailable because `kotlin-ls` was previously declined. Standalone clang diagnostics could not resolve `jni.h`; the Gradle native helper build compiled the Objective-C++ source successfully in focused and full verification.

No dependencies, schemas, UI, roadmap, progress, OpenSpec task status, or Task 1-6 reports were changed.

## Final Oracle blocker: macOS native-handle lifetime serialization

- Root cause: `MacAudioPlayerBridge` read `handle` independently for every JNI call, while the scheduled progress callback could remain in flight after `ScheduledFuture.cancel(false)`. Concurrent `resetPlayer()` or `releasePlayer()` could therefore release that native object between progress position/duration/Now Playing calls, causing use-after-free or mixing old/new source handles. Generation publication checks protected Kotlin events, not native object lifetime.
- RED: focused `JvmPlaybackEngineTest` compilation failed with unresolved `withLifetimeBoundaryForTest` and `currentHandleIdentityForTest`, proving the production bridge had no lifetime-owner boundary or deterministic identity seam.
- Lifetime contract: one per-bridge monitor now owns `handle`, retained transport configuration, every handle-using JNI call, reset, final release, and finalizer cleanup. Reset/release cannot call `nativeRelease` until an in-flight operation exits; after final release no handle operation can enter. Public one-call bridge methods use the same boundary without nested helper locking. Progress position read, duration read, and Now Playing position update are one compound `readAndUpdateProgress` operation against one owned handle/source lifetime.
- GREEN regressions: an operation blocked inside the production lifetime boundary prevents reset, then observes its original lifetime identity before reset safely creates a new identity; a second blocked operation prevents final release, after which the handle identity is zero and later normal/test operations retain the existing released-bridge failure contract.
- Preservation: native remote-handler token cleanup remains in `nativeRelease`; reset still creates/configures a new handle with retained transport state; remote command behavior is unchanged.
- Files: `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt`, `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/JvmPlaybackEngineTest.kt`.
- Commit: `eb115e8 fix: serialize macos native handle lifetime`.
- Verification:
  - Focused lifetime regressions: pass (`BUILD SUCCESSFUL in 5s`).
  - Full `JvmPlaybackEngineTest`: pass (`BUILD SUCCESSFUL in 1m 17s`).
  - Focused `PlaybackControllerTest` and `PlaybackSessionCoordinatorTest`: pass (`BUILD SUCCESSFUL in 1s`).
  - Full release command: pass (`BUILD SUCCESSFUL in 1m 18s`, 113 tasks).
  - iOS simulator main compile: pass (`BUILD SUCCESSFUL in 370ms`); no iOS test pass is claimed.
  - `openspec validate persist-playback-session --strict`: pass (`Change 'persist-playback-session' is valid`).
  - `GIT_MASTER=1 git diff --check`: pass.

## Final Oracle blocker: permanent macOS bridge release

- Root cause: lifetime serialization prevented native use-after-free, but `releasePlayer()` only zeroed `handle`; a later or queued `resetPlayer()` could acquire the monitor and unconditionally create a new native handle, resurrecting a bridge after final release.
- RED: both focused regressions failed before production changes: sequential release/reset did not throw, and the controlled single-thread queue allowed release followed by reset to recreate the handle.
- GREEN: `released` is a one-way Boolean owned exclusively by `lifetimeLock`. Final release and finalizer set it before relinquishing the handle; repeated release remains idempotent. Every handle operation, transport mutation, lifetime test operation, and reset requires the bridge to be unreleased. Reset after final release fails before `nativeCreate`, leaving identity and native handler count at zero.
- Concurrent ordering: a real production lifetime operation holds the monitor while final release and reset are queued in a single-thread executor. Release acquires the monitor first after the operation exits; queued reset then fails and cannot recreate native state.
- Preservation: ordinary pre-release reset, retained transport state, exact remote-handler cleanup in `nativeRelease`, and compound progress locking remain unchanged.
- Files: `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt`, `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/JvmPlaybackEngineTest.kt`.
- Commit: `95334ce fix: make macos bridge release permanent`.
- Verification:
  - Focused sequential/concurrent regressions: pass (`BUILD SUCCESSFUL in 4s`).
  - Full `JvmPlaybackEngineTest`: pass (`BUILD SUCCESSFUL in 1m 16s`).
  - Focused `PlaybackControllerTest` and `PlaybackSessionCoordinatorTest`: pass (`BUILD SUCCESSFUL in 699ms`).
  - Full release command: pass (`BUILD SUCCESSFUL in 1m 18s`, 113 tasks).
  - iOS simulator main compile: pass (`BUILD SUCCESSFUL in 397ms`); no full iOS test pass is claimed.
  - Strict OpenSpec validation: pass (`Change 'persist-playback-session' is valid`).
  - `GIT_MASTER=1 git diff --check`: pass.
