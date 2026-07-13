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
