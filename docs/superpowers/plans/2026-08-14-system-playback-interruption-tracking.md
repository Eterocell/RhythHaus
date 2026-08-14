# System Playback Interruption Tracking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development`.
> Execute one independently reviewable task at a time. Use the RED-to-GREEN loop in each
> platform task, and obtain an independent review before starting the next task.

**Authority:** Approved design `docs/superpowers/specs/2026-08-14-system-playback-interruption-tracking-design.md`, OpenSpec change `system-playback-interruption-tracking` (`proposal.md`, `design.md`, `tasks.md`, and `specs/playback-interruption-tracking/spec.md`), and baseline `caf6c9f`.

**Goal:** Make iOS and macOS playback status reflect system interruption and active output-route loss while preserving all common playback contracts. iOS shall pause active playback on interruption begin or route disconnect and resume only after interruption end with `shouldResume == true` when that interruption began during playback. macOS shall pause only when the previously tracked default output disappears, never for a benign output switch, and shall never auto-resume.

**Architecture:** Keep system notification knowledge in the owning platform bridges. Swift forwards semantic iOS interruption and route events through a new public iOS bridge interface; the iOS engine owns source identity, resume eligibility, audio-session reactivation, progress cancellation, Now Playing updates, and listener status publication. The macOS Objective-C++ bridge owns Core Audio HAL listener lifetime, device-presence classification, and an atomic one-shot pending flag; the existing 100 ms JVM progress scheduler consumes that flag before normal progress publication. `PlatformPlaybackEngine`, `PlaybackEngineListener`, `PlaybackStatus`, and `PlaybackController` in common code remain unchanged. No reverse JNI callback is introduced.

**Tech Stack:** Kotlin Multiplatform, Kotlin/Native iOS, Swift `AVAudioPlayer`/`AVAudioSession`/`NotificationCenter`, macOS Objective-C++ Core Audio HAL, JNI, AVFoundation, MediaPlayer, Gradle clang++ helper, Kotlin Test, generated WAV fixtures, coroutines, Spotless, Detekt, architectureCheck, OpenSpec, Xcode.

## Spec

- An interruption-began event is forwarded only when the Swift provider's authoritative `isPlaying` flag is true. The iOS engine records `wasPlayingBeforeInterruption`, pauses, cancels progress, updates Now Playing to rate `0.0`, publishes current progress, then publishes exactly one generation-valid `Paused` status.
- An interruption-ended event always clears the iOS resume flag. The iOS engine resumes only when `shouldResume` is true and the flag was set by the current interruption. It calls `provider.play()` and resumes state publication only when that returns true. The Swift provider's existing `play_()` operation reactivates `AVAudioSession` before starting the player, so every initial or resumed play follows the same executable session-activation path. The engine then updates Now Playing to rate `1.0`, publishes `Playing`, and restarts progress.
- An iOS `oldDeviceUnavailable` route event pauses active playback and clears resume eligibility. It does not auto-resume when a route returns. Ordinary route changes are ignored.
- Every iOS callback captures the generation and source version when installed and rejects stale callbacks before state mutation. `loadPaused`, track-switch teardown, `clear`, and `release` clear the flag; release also clears the provider's completion and interruption handlers.
- The Swift provider updates `isPlaying` in `play_()`, `pause()`, `stop()`, and `audioPlayerDidFinishPlaying`; notification callbacks are suppressed while paused. Observer tokens are removed during `deinit`.
- macOS listens on `kAudioObjectSystemObject` for both `kAudioHardwarePropertyDevices` and `kAudioHardwarePropertyDefaultOutputDevice`, with global scope and main element, using the same callback/proc/client-data tuple for add and remove. A callback reads available device IDs and the current default. It classifies the old tracked default as disconnected only when absent before adopting a fresh default; a default switch alone is benign. `DeviceIsAlive` may provide an early signal but is not authoritative.
- macOS route pending state is gated by expected-active playback. Pause, stop, reset, and release clear pending state. `consumeRouteDisconnected()` uses an atomic one-shot exchange. The JVM engine consumes before progress publication, pauses, stops progress, updates Now Playing, emits one valid `Paused` status and current progress, and does not auto-resume.
- Deterministic test hooks exercise classification state without faking global hardware: `consumeRouteDisconnected(): Boolean`, `simulateRouteSnapshotForTest(availableDeviceIds: LongArray, defaultOutputDeviceId: Long)`, and `liveRouteListenerCountForTest(): Long`. The native external signatures must use implementable JNI names/types for `jlong`, `jlongArray`, and `jboolean`.

## Global Constraints

- Write production and tests only in the exact implementation ledger below. Do not add a common contract, Android change, database migration, dependency, toolchain change, reverse JNI callback, Windows/Linux support, or extra production file.
- Public iOS declarations require declaration-specific KDoc. Preserve the existing completion-handler ABI and provider operation names.
- Swift generated Kotlin names are resolved by the Xcode build. Use `interruptionHandler?.onInterruptionEnded(shouldResume: shouldResume)` in the plan's intended Swift call shape; the Xcode compile is authoritative if generated import spelling differs.
- Native tests must use explicit snapshots, never physical AirPods, calls, Siri, or global-device mutation. No physical-device or real-system-panel runtime claim is permitted from these tests.
- SQLDelight schema and migrations are untouched. Common playback contracts are untouched.
- Formatting is applied before standalone Spotless and Detekt checks. Validation owner is the parent orchestrator.
- Closeout changes are permitted only in the final task: `openspec/changes/system-playback-interruption-tracking/tasks.md`, `progress.md`, and `roadmap.md`. Archive only after acceptance through `openspec-archive-change`; use the repository's default conventional-commit workflow after the implementation and closeout reviews.

## File Responsibility Map

| File | Responsibility |
|---|---|
| `core/playback/build.gradle.kts` | Add only `-framework CoreAudio` to `clang++` arguments. |
| `core/playback/src/iosMain/kotlin/com/eterocell/rhythhaus/IOSAudioPlayerBridge.kt` | Public KDoc'd interruption callback interface and provider property. |
| `core/playback/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt` | Generation/source-guarded interruption and route handling, resume state, progress, Now Playing, lifecycle cleanup. |
| `core/playback/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSAudioPlayerBridgeTest.kt` | Injectable fake provider, callback forwarding tests, public-engine interruption regressions and exact status sequences. |
| `iosApp/iosApp/Audio/RhythHausAudioPlayerProvider.swift` | Authoritative play-state tracking and NotificationCenter observer/filter/teardown implementation. |
| `core/playback/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt` | Injectable bridge factory, Kotlin/native route hook declarations, route consumption and engine behavior. |
| `core/playback/src/jvmTest/kotlin/com/eterocell/rhythhaus/JvmPlaybackEngineTest.kt` | Native snapshot classification/lifecycle hooks and injected-engine route-loss regressions. |
| `core/playback/src/nativeInterop/macos/rhythhaus_audio.mm` | CoreAudio listener, pure snapshot classification state, atomic gate, native hooks, balanced lifetime. |

## Task 1: iOS RED Regression And Bridge Contract

**Scope:** One atomic iOS TDD slice: tests first, then public bridge declarations and iOS production behavior. This task owns the iOS test and the three iOS production/Swift files only.

**Files:** `core/playback/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSAudioPlayerBridgeTest.kt`, `core/playback/src/iosMain/kotlin/com/eterocell/rhythhaus/IOSAudioPlayerBridge.kt`, `core/playback/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt`, `iosApp/iosApp/Audio/RhythHausAudioPlayerProvider.swift`.

- [ ] Extend `FakeIOSAudioPlayerProvider` with:
  ```kotlin
  override var interruptionHandler: IOSAudioInterruptionHandler? = null

  fun simulateInterruptionBegan() = interruptionHandler?.onInterruptionBegan()
  fun simulateInterruptionEnded(shouldResume: Boolean) =
      interruptionHandler?.onInterruptionEnded(shouldResume)
  fun simulateRouteDisconnected() = interruptionHandler?.onRouteDisconnected()
  ```
  Add bridge forwarding coverage that installs a handler, simulates all three events, asserts the exact event sequence, and asserts handler identity is retained.
- [ ] Add public-engine tests using `createIOSPlaybackEngine` with a simple `IOSRelativeFilePathResolver` and a listener recording status/progress. Cover `interruptionBeganWhilePlayingEmitsPaused`, `interruptionEndedWithShouldResumeAutoResumesWhenPlaying`, `interruptionEndedWithoutShouldResumeStaysPaused`, and `routeDisconnectPausesWithoutAutoResume`. Assert exact status subsequences and no resume after route disconnect. Test a paused interruption has no spurious pause.
- [ ] Run the focused test before adding the production interface. Expected RED: `IOSAudioPlayerProvider` has no `interruptionHandler`, so the fake/test compilation fails with the missing property/interface. Record the unsuppressed failure; do not soften assertions or skip the selector.
  ```bash
  ./gradlew :core:playback:iosSimulatorArm64Test --tests 'com.eterocell.rhythhaus.IOSAudioPlayerBridgeTest' --configuration-cache
  ```
- [ ] Add the public declaration-specific-KDoc interface exactly as the bridge contract:
  ```kotlin
  /** Receives system-initiated interruption and route events from the audio bridge. */
  public interface IOSAudioInterruptionHandler {
      /** System interrupted an actively-playing session. */
      public fun onInterruptionBegan()

      /** Ends an interruption; [shouldResume] is the system resume recommendation. */
      public fun onInterruptionEnded(shouldResume: Boolean)

      /** The active output route disconnected. */
      public fun onRouteDisconnected()
  }
  ```
  Add the KDoc'd property inside `IOSAudioPlayerProvider`:
  ```kotlin
  /** Receives system interruption and route notifications. */
  public var interruptionHandler: IOSAudioInterruptionHandler?
  ```
  Preserve `completionHandler` and all provider operation signatures.
- [ ] Implement Swift `isPlaying` updates in `play_()`, `pause()`, `stop()`, fade-stop, and completion. `play_()` must first call `AVAudioSession.sharedInstance().setActive(true)` and return `false` without publishing an active flag if session activation throws or `player.play()` fails; this is the approved resume-time audio-session reactivation without adding another public provider method. Register `NotificationCenter` observers for `AVAudioSession.interruptionNotification` and `AVAudioSession.routeChangeNotification`, store both tokens, and remove them in `deinit`. Parse interruption `.began`/`.ended`, `AVAudioSessionInterruptionOptionKey`, and route reason `.oldDeviceUnavailable`; only forward began/route events while the provider's locally tracked `isPlaying` flag is true, but always forward interruption end so Kotlin can clear resume eligibility. The intended generated-interface call is:
  ```swift
  interruptionHandler?.onInterruptionBegan()
  interruptionHandler?.onInterruptionEnded(shouldResume: options.contains(.shouldResume))
  interruptionHandler?.onRouteDisconnected()
  ```
  The Xcode build is authority for the generated Swift spelling. `play_()` must set `isPlaying` from the successful `player?.play()` result; `pause()`, `stop()`, and fade-stop set false even when no player exists; completion sets false before forwarding. Do not derive the notification gate from `AVAudioPlayer.isPlaying`, because the system may already have suppressed the player before the interruption notification is delivered.
- [ ] In `PlaybackEngine.ios.kt`, install an interruption handler capturing `generation` and `version` in `loadPaused` alongside completion. Add `wasPlayingBeforeInterruption`, `isCurrentSource` guards, and handlers that cancel/restart the existing progress job, pause the provider, publish current progress and Now Playing rate `0.0`, and emit `Paused`. A stale interruption-end callback returns before mutating current-source state. For a current-source allowed end, call `provider.play()`; only when it returns true publish rate `1.0` and `Playing` and restart `startProgressLoop(generation, version)`. The provider's `play_()` performs session reactivation. Clear the current source's flag after every current-source end, including disallowed, unavailable-provider, and failed-play paths. Route disconnect clears the flag and never resumes. Reset the flag and clear the handler in track switch, clear, and release; keep common interfaces unchanged.
- [ ] Run the focused iOS test and Xcode consumer build. Expected GREEN: focused Kotlin tests pass with exact status sequences, and Xcode compiles the Swift provider against the exported Kotlin interface. Xcode does not replace the simulator test.
  ```bash
  ./gradlew :core:playback:iosSimulatorArm64Test --tests 'com.eterocell.rhythhaus.IOSAudioPlayerBridgeTest' --configuration-cache
  /usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
  ```
- [ ] Review the iOS slice independently, then stage only the four task files and commit the green atomic slice:
  ```bash
  git add -- core/playback/src/iosMain/kotlin/com/eterocell/rhythhaus/IOSAudioPlayerBridge.kt core/playback/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt core/playback/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSAudioPlayerBridgeTest.kt iosApp/iosApp/Audio/RhythHausAudioPlayerProvider.swift
  git commit -m "feat: track iOS playback interruptions"
  ```

## Task 2: macOS RED Native Snapshot Tests And Linkage

**Scope:** One atomic macOS TDD slice: deterministic test hooks and JVM engine regressions first, then Gradle linkage, native classification/listener lifetime, Kotlin bridge seam, and engine consumption. No global hardware is faked.

**Files:** `core/playback/src/jvmTest/kotlin/com/eterocell/rhythhaus/JvmPlaybackEngineTest.kt`, `core/playback/build.gradle.kts`, `core/playback/src/nativeInterop/macos/rhythhaus_audio.mm`, `core/playback/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt`.

- [ ] Add JVM tests for listener count after bridge creation/release, one-shot consumption, active tracked-device removal, benign default switch, both callback orders, paused suppression, reset/release clearing, and injected-engine route loss. Use explicit snapshots such as:
  ```kotlin
  bridge.simulateRouteSnapshotForTest(longArrayOf(10L, 20L), 10L)
  bridge.simulateRouteSnapshotForTest(longArrayOf(10L, 20L), 20L)
  assertFalse(bridge.consumeRouteDisconnected())

  bridge.simulateRouteSnapshotForTest(longArrayOf(10L), 10L)
  bridge.simulateRouteSnapshotForTest(longArrayOf(), 20L)
  assertTrue(bridge.consumeRouteDisconnected())
  assertFalse(bridge.consumeRouteDisconnected())
  ```
  The benign switch fixture must retain device `10` in the available array; replacing it with an array containing only `20` is a removal, not a benign switch. Both HAL addresses use the same callback, and that callback ignores notification ordering by re-reading the complete current snapshot. Model either callback order by applying the same final snapshot (`availableDeviceIds = [20]`, `defaultOutputDeviceId = 20`) twice after tracking device `10`; assert the first application records one loss before adopting device `20`, the second does not erase or duplicate it, and one-shot consumption returns `true` then `false`. There must be no property-specific classification branch to test. Use the existing generated WAV helper, latches, and `createJvmPlaybackEngine(bridge)` for the engine regression; assert route loss emits one `Paused`, progress stops, and a paused bridge produces no spurious status.
- [ ] Run the exact JVM selector before adding hooks. Expected RED: unresolved `simulateRouteSnapshotForTest`, `consumeRouteDisconnected`, `liveRouteListenerCountForTest`, and internal bridge factory APIs. Record the unsuppressed failure.
  ```bash
  ./gradlew :core:playback:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest' --configuration-cache
  ```
- [ ] Add only CoreAudio linkage in `buildMacosAudioHelper`:
  ```kotlin
  args(
      "-dynamiclib",
      "-std=c++17",
      "-fobjc-arc",
      "-framework", "Foundation",
      "-framework", "AVFoundation",
      "-framework", "MediaPlayer",
      "-framework", "AppKit",
      "-framework", "CoreAudio",
      // existing include/source/output arguments
  )
  ```
  Do not alter dependencies, SDK versions, toolchains, or other framework flags.
- [ ] Add internal Kotlin bridge hooks with implementable native signatures:
  ```kotlin
  internal fun consumeRouteDisconnected(): Boolean =
      withHandle(::nativeConsumeRouteDisconnected)

  internal fun simulateRouteSnapshotForTest(
      availableDeviceIds: LongArray,
      defaultOutputDeviceId: Long,
  ): Boolean = withHandle {
      nativeSimulateRouteSnapshotForTest(it, availableDeviceIds, defaultOutputDeviceId)
  }

  internal fun liveRouteListenerCountForTest(): Long =
      synchronized(lifetimeLock) { nativeLiveRouteListenerCountForTest() }

  internal fun createJvmPlaybackEngine(bridge: MacAudioPlayerBridge): PlatformPlaybackEngine =
      MacOSNativePlaybackEngine(bridge)
  ```
  Keep `public fun createJvmPlaybackEngine()` unchanged and change the private engine constructor to receive `bridge: MacAudioPlayerBridge`.
- [ ] In `rhythhaus_audio.mm`, import `<CoreAudio/CoreAudio.h>`, and include `<atomic>` and `<vector>` as needed. Add the HAL listener registration in native object init and remove both properties before the retained object is released. Use one callback and identical client-data tuple for add/remove on `kAudioObjectSystemObject`, `kAudioHardwarePropertyDevices`, and `kAudioHardwarePropertyDefaultOutputDevice`, with `kAudioObjectPropertyScopeGlobal` and `kAudioObjectPropertyElementMain`. Track the previous default and available IDs. Each callback reads both current values; classify the old tracked ID as disconnected only if it is absent before adopting the fresh default. A default switch with the old ID still available is benign. `kAudioDevicePropertyDeviceIsAlive` is optional early information only.
- [ ] Gate native pending state on expected-active playback and clear it from native pause, stop, reset, and release paths. Use `std::atomic_bool` one-shot exchange. Add the JNI functions with exact Kotlin-compatible types:
  ```cpp
  extern "C" JNIEXPORT jboolean JNICALL
  Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeConsumeRouteDisconnected(
      JNIEnv *, jobject, jlong handle);

  extern "C" JNIEXPORT jboolean JNICALL
  Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeSimulateRouteSnapshotForTest(
      JNIEnv *, jobject, jlong handle, jlongArray availableDeviceIds,
      jlong defaultOutputDeviceId);

  extern "C" JNIEXPORT jlong JNICALL
  Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeLiveRouteListenerCountForTest(
      JNIEnv *, jobject);
  ```
  The simulation hook must exercise the same pure snapshot classifier, not mutate OS-global hardware. `liveRouteListenerCountForTest()` counts actual live HAL property-listener registrations: expect `2` after bridge creation, `2` after every reset because the old pair is removed before the replacement pair is registered, and `0` after final release. Add/remove calls must use identical object/address/proc/client-data tuples and remain balanced.
- [ ] Update `publishProgress(generation, version)` to consume the route flag before reading/publishing progress. On `true`, re-check the publication identity and expected-active state, stop progress, pause the bridge, update Now Playing to `Paused`, emit current progress and exactly one valid `Paused`, then return. Do not auto-resume. Do not consume from a stopped paused progress task; direct bridge consumption remains available for tests and native gating prevents stale events.
- [ ] Run the exact JVM selector and the generic Xcode build (the Xcode build remains required for the iOS Swift path, while this task verifies the native macOS helper through JVM tests). Expected GREEN: native snapshot tests prove removal, benign switch, callback ordering, paused suppression, one-shot behavior, and balanced listener lifetime; injected engine tests prove status handling.
  ```bash
  ./gradlew :core:playback:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest' --configuration-cache
  /usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
  ```
- [ ] Review the macOS slice independently, then stage only its four task files and commit the green atomic slice:
  ```bash
  git add -- core/playback/build.gradle.kts core/playback/src/nativeInterop/macos/rhythhaus_audio.mm core/playback/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt core/playback/src/jvmTest/kotlin/com/eterocell/rhythhaus/JvmPlaybackEngineTest.kt
  git commit -m "feat: track macOS output route loss"
  ```

## Task 3: Cross-Platform Verification And OpenSpec Closeout

**Scope:** Final acceptance only. Do not change the eight-file implementation ledger in this task. Closeout files are allowed only after all implementation tests and independent reviews pass.

**Files:** `openspec/changes/system-playback-interruption-tracking/tasks.md`, `progress.md`, `roadmap.md`.

- [ ] Inspect the complete diff against the approved proposal, design, tasks, and capability spec. Confirm only the eight ledger files changed in production/test implementation, no common playback contract changed, and no extra production file was invented. Run `git diff --check` and a scope audit over the exact ledger.
- [ ] Run focused regressions and whole core/shared suites:
  ```bash
  ./gradlew :core:playback:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest' --configuration-cache
  ./gradlew :core:playback:iosSimulatorArm64Test --tests 'com.eterocell.rhythhaus.IOSAudioPlayerBridgeTest' --configuration-cache
  ./gradlew :core:playback:jvmTest --configuration-cache
  ./gradlew :core:playback:iosSimulatorArm64Test --configuration-cache
  ./gradlew :shared:jvmTest --configuration-cache
  ./gradlew :shared:iosSimulatorArm64Test --configuration-cache
  ```
  Expected GREEN: all selected suites pass; record exact pass/fail/skip evidence.
- [ ] Run the Swift consumer build because Gradle iOS tests do not compile Swift:
  ```bash
  /usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
  ```
  Expected GREEN: the generated Kotlin interface and Swift provider compile together. This does not prove physical interruption behavior.
- [ ] Run quality and repository checks in the required order:
  ```bash
  ./gradlew spotlessApply --configuration-cache
  ./gradlew spotlessCheck --configuration-cache
  ./gradlew detekt --configuration-cache
  ./gradlew architectureCheck --configuration-cache
  openspec validate system-playback-interruption-tracking --strict
  ./init.sh
  git diff --check
  ```
  Expected GREEN: each command exits zero. Run `spotlessCheck` and `detekt` as standalone commands, not only through another aggregate. Record exact blockers and never claim an unavailable Xcode/device check passed.
- [ ] Update `openspec/changes/system-playback-interruption-tracking/tasks.md` checkboxes with evidence, add a `progress.md` handoff using Route/Owner/Input/Output/Verification/Next owner/Blockers/Commit, and add one concise completed entry to `roadmap.md`. State explicitly that no physical AirPods, call, Siri, or live route runtime claim exists.
- [ ] Review and stage only the three closeout files:
  ```bash
  git add -- openspec/changes/system-playback-interruption-tracking/tasks.md progress.md roadmap.md
  git commit -m "docs: close playback interruption tracking"
  ```
  This documentation/closeout commit must remain separate from both production implementation commits.
- [ ] After all accepted tasks and closeout review, archive only through `openspec-archive-change` and commit the archive using the repository's default conventional commit workflow. Do not archive before strict validation and acceptance.

## Completion Boundary

The implementation is complete when the two atomic production commits, the separate closeout commit, all focused and aggregate verification commands, strict OpenSpec validation, `./init.sh`, diff check, and exact scope audit are recorded as passing. Automated evidence covers injected callbacks, deterministic CoreAudio classification, source/lifecycle guards, Swift compilation, and native listener balance. It does not cover physical AirPods takeover, headphone removal, calls, Siri, or other live system-panel behavior.
