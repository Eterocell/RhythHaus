# System Playback Interruption & Route Tracking — Design

Date: 2026-08-14
Status: draft — awaiting user review
Route: openspec+superpowers

## Summary

RhythHaus must reflect system-initiated playback state changes in the UI. Today,
when the operating system pauses or interrupts playback without a user command —
an AirPods takeover by another device, an incoming call, Siri, or an unplugged
headphone — the platform engine never emits a status change, so
`PlaybackController` stays `Playing` and the UI keeps showing the pause button
(i.e. it falsely claims to be playing).

This change adds **system interruption + output-route observation** to the iOS and
macOS platform playback engines. The engines translate those observations into
the existing `PlaybackEngineListener.onPlaybackStatus(Paused/Playing)` callback,
so the controller and UI sync with no public playback-contract change.

## Problem & Root Cause

- iOS engine (`core/playback/src/iosMain/.../PlaybackEngine.ios.kt`) and macOS
  native bridge (`core/playback/src/nativeInterop/macos/rhythhaus_audio.mm`) are
  both backed by `AVAudioPlayer`.
- The only delegate callback wired today is
  `audioPlayerDidFinishPlaying` (completion). There is **no observation** of:
  - iOS `AVAudioSession.interruptionNotification` (call / Siri / session claim), or
  - iOS `AVAudioSession.routeChangeNotification` (AirPods taken over / unplugged), or
  - macOS Core Audio default-output-device change (AirPods taken over / unplugged).
- The engine's `MPRemoteCommandCenter` handlers cover only **user-initiated**
  lock-screen / Control Center / hardware-button commands, not system
  interruptions.
- Consequence: the system pauses `AVAudioPlayer` (or, on macOS, silently reroutes
  audio), the engine's progress loop sees `isPlaying() == false` and silently stops
  publishing, but `onPlaybackStatus(Paused)` is never emitted → stale `Playing` UI.

Key research facts (verified against Apple Docs / androidx source, 2026-08):

- iOS: `AVAudioSession.interruptionNotification` is the correct interruption
  signal. `.began` means the session is no longer active (AVAudioPlayer
  auto-pauses); `.ended` carries an optional `shouldResume` option that is the
  only sanctioned trigger for auto-resume.
- iOS: **AirPods-takeover / headphone-unplug is a route change, not an
  interruption** — by default no interruption fires and audio keeps playing to
  another output. It must be observed via
  `AVAudioSession.routeChangeNotification` with reason `oldDeviceUnavailable`.
- macOS: **`AVAudioSession` does not exist on macOS**, and the deprecated
  `AVAudioPlayerDelegate` interruption methods are not listed for macOS. The
  correct mechanism is a Core Audio HAL property listener
  (`AudioObjectAddPropertyListener` on `kAudioHardwarePropertyDefaultOutputDevice`).
  `AVAudioPlayer` does not auto-pause on route change on macOS.
- Android: already handled — `RhythHausPlaybackService` sets both
  `handleAudioFocus = true` and `setHandleAudioBecomingNoisy(true)`. Out of scope.

## Goals / Non-goals

Goals:

- iOS: reflect `.began` interruptions and `oldDeviceUnavailable` route changes as
  `Paused`; auto-resume on `.ended` + `shouldResume` only when playback was active
  before the interruption.
- macOS: reflect default-output-device loss (AirPods taken over / unplugged) as
  `Paused`.
- Zero change to the public `PlatformPlaybackEngine` /
  `PlaybackEngineListener` contracts in `core/playback/src/commonMain`.

Non-goals:

- Android (already handled — verified, no change).
- Arbitrary output-device *switching* (e.g. user manually picks speakers) is not
  an interruption and must not pause.
- Distinguishing "system pause" from "user pause" in the controller state machine
  (not needed for this bug; auto-resume is decided inside the engine).
- Windows/Linux packaging or product support.

## Design Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Observation location | Platform bridge (Swift for iOS, ObjC++ `.mm` for macOS) | Existing delegate-callback architecture already funnels `AVAudioPlayer` events through these bridges; Kotlin/Native cinterop for `NSNotificationCenter` userInfo dictionaries is awkward. |
| Status signaling | Reuse existing `onPlaybackStatus(Paused/Playing)` | No public interface change; controller and UI sync for free. |
| Auto-resume trigger | iOS `.ended` + `shouldResume` **and** `wasPlayingBeforeInterruption` | Mirrors Apple's documented pattern; never auto-resumes a track the user had already paused. |
| macOS communication | Native HAL listener sets a flag; engine's existing 100 ms progress loop consumes it (poll) | macOS completion is already detected by polling in the same loop; avoids introducing the first native→Kotlin JNI callback (global-ref/method-ID/lifetime machinery) for an event whose 100 ms latency is imperceptible. |
| iOS route-change vs `setPrefersInterruptionOnRouteDisconnect` | Use `routeChangeNotification` + `oldDeviceUnavailable` | Established, well-documented API; the alternative is newer and semantically merges route loss into interruption. |

Rejected alternatives (see "Alternatives" below).

## Public API Surface

All changes are platform-layer. The common playback contracts
(`PlatformPlaybackEngine`, `PlaybackEngineListener`, `PlaybackStatus`,
`PlaybackController`) are **unchanged**.

### iOS — `core/playback/src/iosMain/.../IOSAudioPlayerBridge.kt`

New public interface (visible to Swift through the shared iOS framework export,
same as the existing `IOSAudioPlayerCompletionHandler`):

```kotlin
/** Receives system-initiated interruption and route events from the audio bridge. */
public interface IOSAudioInterruptionHandler {
    /** System interrupted an actively-playing session (call, Siri, route claim). */
    public fun onInterruptionBegan()

    /**
     * Interruption ended. [shouldResume] mirrors
     * `AVAudioSessionInterruptionOption.shouldResume`: true means the app is
     * allowed to resume playback.
     */
    public fun onInterruptionEnded(shouldResume: Boolean)

    /** The active output route disconnected (e.g. headphones/AirPods removed). */
    public fun onRouteDisconnected()
}
```

`IOSAudioPlayerProvider` gains one property:

```kotlin
    /** Receives system interruption/route notifications. */
    public var interruptionHandler: IOSAudioInterruptionHandler?
```

### macOS — `core/playback/src/jvmMain/.../PlaybackEngine.jvm.kt`

`MacAudioPlayerBridge` gains (internal test seams + one production read):

```kotlin
    /** Returns and clears the pending route-disconnect flag raised by the HAL listener. */
    internal fun consumeRouteDisconnected(): Boolean

    internal fun invokeRouteDisconnectForTest(): Boolean   // sets the flag
    internal fun liveRouteListenerCountForTest(): Long      // 0 or 1
```

Engine seam for test injection (mirrors iOS `IOSAudioPlayerBridge.provider`):

```kotlin
public fun createJvmPlaybackEngine(): PlatformPlaybackEngine =
    MacOSNativePlaybackEngine(MacAudioPlayerBridge())

internal fun createJvmPlaybackEngine(bridge: MacAudioPlayerBridge): PlatformPlaybackEngine =
    MacOSNativePlaybackEngine(bridge)
```

## iOS Design

### Bridge (Swift `RhythHausAudioPlayerProvider.swift`)

1. Track an authoritative `isPlaying` boolean, updated in `play_()` (true),
   `pause()` / `stop()` (false), and `audioPlayerDidFinishPlaying` (false). This
   is the single choke point for all play/pause paths (engine `play()`/`pause()`
   and remote-command handlers all route through it).
2. Register `NotificationCenter` observers on the main thread for:
   - `AVAudioSession.interruptionNotification`
   - `AVAudioSession.routeChangeNotification`
3. On interruption:
   - `.began`: if `isPlaying`, set `isPlaying = false`, invoke
     `interruptionHandler?.onInterruptionBegan()`.
   - `.ended`: read `AVAudioSessionInterruptionOptionKey`; invoke
     `interruptionHandler?.onInterruptionEnded(shouldResume = options.contains(.shouldResume))`.
4. On route change with reason `AVAudioSessionRouteChangeReason.oldDeviceUnavailable`:
   if `isPlaying`, set `isPlaying = false`, invoke
   `interruptionHandler?.onRouteDisconnected()`.

Observers are added when the bridge is created and removed on deinit.

### Engine (`IOSPlaybackEngine`)

Add a `wasPlayingBeforeInterruption: Boolean` field (default `false`), and a
generation-guarded interruption handler built per `loadPaused`, mirroring the
existing `completionHandler(generation, version)` pattern:

- `onInterruptionBegan()` — guard `isCurrentSource(generation, version)`:
  - `wasPlayingBeforeInterruption = true`
  - `progressJob?.cancel()`
  - `audioProvider?.pause()`
  - `updateNowPlayingInfo(positionMillis = current, playbackRate = 0.0)`
  - `listener?.onPlaybackProgress(generation, pos, durationMillis)`
  - `listener?.onPlaybackStatus(generation, PlaybackStatus.Paused)`
- `onInterruptionEnded(shouldResume)` — guard generation:
  - if `shouldResume && wasPlayingBeforeInterruption`:
    - `audioProvider?.play()`; the Swift provider's existing `play_()` operation
      first reactivates `AVAudioSession` and returns `false` if activation or
      playback fails
    - `updateNowPlayingInfo(positionMillis = current, playbackRate = 1.0)`
    - `listener?.onPlaybackStatus(generation, PlaybackStatus.Playing)`
    - `startProgressLoop(generation, version)`
  - `wasPlayingBeforeInterruption = false` (always)
- `onRouteDisconnected()` — guard generation:
  - `wasPlayingBeforeInterruption = false`
  - `progressJob?.cancel()`
  - `audioProvider?.pause()`
  - `updateNowPlayingInfo(positionMillis = current, playbackRate = 0.0)`
  - `listener?.onPlaybackProgress(generation, pos, durationMillis)`
  - `listener?.onPlaybackStatus(generation, PlaybackStatus.Paused)`

Reset `wasPlayingBeforeInterruption = false` in `loadPaused`, `clear`, `release`,
and `releaseForTrackSwitch`. Set `provider.interruptionHandler = <handler>` in
`loadPaused` and clear it in `release`.

Interruption callbacks arrive on the main thread; the engine reacts on
`Dispatchers.Main` (the existing `scope`), consistent with remote-command
handling. Progress-loop cancellation and status emission are idempotent against
the generation guard, so stale events from a superseded load are dropped.

## macOS Design

### Native bridge (`rhythhaus_audio.mm`)

1. In `nativeCreate` (process/device-level, not per-track), register a Core Audio
   HAL property listener:
   `AudioObjectAddPropertyListener(kAudioObjectSystemObject,
   &kAudioHardwarePropertyDefaultOutputDevice, callback, self)`.
2. The callback runs on the Core Audio dispatch queue; it records an atomic
   `routeDisconnectPending = true` when the previously-active default output
   device has become unavailable (removed from the device set / `kAudioObjectUnknown`),
   and not on benign device switches.
3. Remove the listener in `nativeRelease`.
4. Test seams: `nativeInvokeRouteDisconnectForTest` (sets the flag as if the HAL
   callback fired) and `nativeLiveRouteListenerCountForTest` (returns 1/0).

### Engine (`MacOSNativePlaybackEngine`)

- `startProgressUpdates`'s 100 ms scheduled task first checks
  `bridge.consumeRouteDisconnected()`; on `true`, call `handleRouteDisconnected`
  and skip the normal `publishProgress`.
- `handleRouteDisconnected(generation, version)`:
  - `stopProgressUpdates()`
  - `bridge.pause()`
  - `bridge.updateNowPlayingPlaybackState(PlaybackStatus.Paused)`
  - `listener?.onPlaybackStatus(activeGeneration, PlaybackStatus.Paused)`
  - `listener?.onPlaybackProgress(activeGeneration, bridge.currentPositionMillis(), durationMillis)`
- No auto-resume on macOS (no `shouldResume` concept; the user manually resumes).

## Data Flow

```
System event ──▶ Bridge (Swift/.mm) ──▶ Engine handler ──▶ listener.onPlaybackStatus
                                                                        │
                                                          PlaybackController.state.status
                                                                        │
                                                              UI play/pause button
```

iOS:
```
AVAudioSession.interruptionNotification (.began)
  → Swift provider (was playing) → interruptionHandler.onInterruptionBegan()
  → IOSPlaybackEngine → onPlaybackStatus(Paused) → controller → UI shows play button ✓

AVAudioSession.interruptionNotification (.ended, shouldResume)
  → Swift provider → onInterruptionEnded(true)
  → IOSPlaybackEngine (wasPlayingBeforeInterruption) → play() + onPlaybackStatus(Playing) ✓

AVAudioSession.routeChangeNotification (oldDeviceUnavailable)
  → Swift provider (was playing) → onRouteDisconnected()
  → IOSPlaybackEngine → onPlaybackStatus(Paused) ✓
```

macOS:
```
Core Audio default-output-device loss
  → .mm HAL callback sets routeDisconnectPending
  → engine 100ms loop consumeRouteDisconnected() == true
  → MacOSNativePlaybackEngine → onPlaybackStatus(Paused) ✓
```

## Error Handling & Edge Cases

- **Stale events across track switch:** every bridge→engine event is guarded by
  `isCurrentSource(generation, version)`; events from a superseded load are dropped.
- **Interruption while paused:** bridge only fires `onInterruptionBegan` /
  `onRouteDisconnected` when `isPlaying`; no redundant `Paused` emission.
- **Interruption ended without `shouldResume`:** engine stays paused and clears
  `wasPlayingBeforeInterruption`; never auto-resumes.
- **Route change on a benign device switch:** iOS only reacts to
  `oldDeviceUnavailable`; macOS only to actual default-output-device loss, not
  manual switching.
- **`shouldResume` with no prior playback:** impossible — `wasPlayingBeforeInterruption`
  is only set by `onInterruptionBegan`, which is only fired while playing.
- **Manual pause during an interruption (known limitation):** if the user manually
  pauses (in-app or remote) *after* `.began` but before `.ended`, the engine would
  still auto-resume on `.ended` + `shouldResume`. This rare race is accepted out of
  scope; recording it here rather than silently ignoring it.
- **Provider unavailable or resume failure:** `onInterruptionEnded`'s resume path
  re-checks `audioProvider` non-null and publishes `Playing` only when `play()`
  returns true. Swift `play_()` reactivates `AVAudioSession` before starting the
  player, so no additional public provider method is needed.

## Testing Strategy

Follow TDD: write RED regressions first (they fail against current code), then
implement GREEN.

### iOS — `:core:playback:iosSimulatorArm64Test`

Extend `FakeIOSAudioPlayerProvider` to store and simulate the new
`interruptionHandler` (mirrors the existing `simulateNativeCompletion()`):

1. `interruptionBeganWhilePlayingEmitsPaused` — RED: no handler exists today.
   GREEN: engine emits `Paused` after a prior `Playing`.
2. `interruptionEndedWithShouldResumeAutoResumesWhenPlaying` — engine emits
   `Playing` after `onInterruptionEnded(shouldResume = true)` when it was playing.
3. `interruptionEndedWithoutShouldResumeStaysPaused` — no `Playing` emission.
4. `routeDisconnectPausesWithoutAutoResume` — emits `Paused`; subsequent
   `onInterruptionEnded(true)` does not resume (flag cleared).
5. `interruptionHandlerForwarding` — bridge-level: `interruptionHandler` retains
   the handler and forwards `onInterruptionBegan`/`onInterruptionEnded`/
   `onRouteDisconnected` (parallels `IOSAudioPlayerBridgeTest`).

Assert the exact `PlaybackStatus` sequence, not just the final state.

### macOS — `:core:playback:jvmTest`

1. `routeListenerIsRegisteredAndReleased` — `liveRouteListenerCountForTest()` is
   1 after creation, 0 after `releasePlayer()` (RED: no listener API today).
2. `consumeRouteDisconnectedClearsOnRead` — `invokeRouteDisconnectForTest()`
   then `consumeRouteDisconnected()` returns `true` once, then `false`.
3. `routeDisconnectDuringPlaybackPausesEngine` — via
   `createJvmPlaybackEngine(bridge)` injectable seam: load + play, invoke
   `invokeRouteDisconnectForTest()`, assert `Paused` is emitted (latch within the
   100 ms loop) and progress stops.
4. `routeDisconnectWhilePausedIsNoop` — no spurious `Paused` when already paused.

### Existing suites must stay green

- `:core:playback:jvmTest`, `:core:playback:iosSimulatorArm64Test`,
  `:shared:jvmTest`, `:shared:iosSimulatorArm64Test`, `architectureCheck`.

## Verification Commands

```bash
./gradlew :core:playback:jvmTest --configuration-cache
./gradlew :core:playback:iosSimulatorArm64Test --configuration-cache
./gradlew :shared:jvmTest --configuration-cache
./gradlew spotlessApply --configuration-cache
./gradlew spotlessCheck --configuration-cache
./gradlew detekt --configuration-cache
./gradlew architectureCheck --configuration-cache
./init.sh
```

iOS framework consumer build (Swift bridge compiles against the new interface):
`xcodebuild` generic iOS Simulator build of `iosApp`.

## Scope Boundaries & Known Limitations

- No public common-contract change; `architectureCheck` allow-list unaffected.
- No Android change (verified already handled).
- Runtime/device/system-panel behavior (real AirPods takeover, real call) is not
  automated; the Kotlin-side reaction is fully unit-tested via injected fakes,
  and the Swift/ObjC observer registration is validated by compilation + the
  existing delegate-forwarding pattern (runtime claims require their own command
  evidence, per project rules).
- iOS 27 deprecates `AVAudioSession.InterruptionType/Options` in favor of
  `AVAudioSessionDidBecomeInactiveNotification` + a resumption-recommendation
  notification; the app targets current iOS (26), and this design uses the still
  fully-supported interruption/route-change APIs. The migration is a follow-up,
  not part of this change.
- macOS route-loss detection depends on precise Core Audio device-presence
  semantics (distinguishing "device removed" from "device switched"); the exact
  predicate is specified in the implementation plan and covered by the native
  test hook. This is the highest-uncertainty portion of the change.

## Alternatives Considered

- **Kotlin/Native `NSNotificationCenter` cinterop (rejected):** keeps logic in
  Kotlin but forces awkward userInfo-dictionary interop and fragments the
  delegate-callback responsibility the bridge already owns.
- **New public `onPlaybackInterrupted` listener callback (rejected):** expands the
  public contract (strict KDoc burden) and the controller state machine for no
  benefit — `onPlaybackStatus` already carries the needed signal.
- **iOS `setPrefersInterruptionOnRouteDisconnect` (rejected for now):** newer API,
  merges route loss into interruption semantics; `routeChangeNotification` +
  `oldDeviceUnavailable` is the established path.
- **macOS JNI native→Kotlin callback (rejected):** event-driven but introduces the
  first reverse-JNI callback (global refs, method-ID caching, cross-thread
  marshalling, reset/release lifetime) for an event whose 100 ms poll latency is
  imperceptible; polling matches the existing completion-detection pattern.
