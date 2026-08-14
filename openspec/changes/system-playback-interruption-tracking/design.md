## Context

See `proposal.md` for the user-facing motivation and `specs/playback-interruption-tracking/spec.md` for the behavioral contract.

The common playback contracts already carry `Playing` and `Paused` status events, and `PlaybackController` already updates the UI from those events. The missing behavior is confined to platform observation and the existing engine-to-listener status path:

- iOS uses a Swift-owned `AVAudioPlayer` provider exposed through `IOSAudioPlayerProvider`. The provider already owns completion forwarding and remote transport integration.
- iOS system interruptions are delivered through `AVAudioSession.interruptionNotification`; output removal is delivered through `AVAudioSession.routeChangeNotification` with `oldDeviceUnavailable`.
- macOS has no `AVAudioSession` interruption subsystem. Its native AVAudioPlayer bridge must observe Core Audio HAL output-device changes.
- Android already enables both Media3 audio-focus handling and becoming-noisy handling and is not part of this change.

## Goals / Non-Goals

**Goals:**

- Keep `PlatformPlaybackEngine`, `PlaybackEngineListener`, and common playback state APIs unchanged.
- Deliver generation/source-guarded iOS interruption and route events through a platform-only bridge callback.
- Resume iOS playback only for an interruption that ended with the system's `shouldResume` recommendation and was active before the interruption.
- Detect macOS loss of the active output device without introducing a reverse-JNI callback; consume the event from the existing progress scheduler.
- Make platform event handling and native lifetime behavior regression-testable with the existing iOS fake-provider and macOS native test-hook patterns.

**Non-Goals:**

- Android changes or new Android behavior.
- Auto-resume after route disconnect on either iOS or macOS.
- A new common playback event or controller state for distinguishing system pause from user pause.
- Treating every output-device switch as a disconnect; only removal/unavailability of the active route is actionable.

## Decisions

### 1. Observe platform notifications in the owning bridge

The iOS Swift provider registers `AVAudioSession.interruptionNotification` and `routeChangeNotification`, parses their platform-specific payloads, and forwards a small Kotlin-visible event interface. macOS registers a Core Audio HAL property listener in `rhythhaus_audio.mm`.

This keeps AVAudioPlayer and OS notification knowledge in the platform owners. Direct Kotlin/Native notification interop was rejected because it would move notification dictionary parsing into cinterop code and split the existing Swift-owned delegate responsibility.

### 2. Reuse `onPlaybackStatus` for controller synchronization

The platform engines translate external pause/resume events into the existing `PlaybackEngineListener.onPlaybackStatus` callback. No common API or controller branch is added. This preserves the current status ownership and avoids a public `onPlaybackInterrupted` contract whose only consumer would be the existing status state.

### 3. iOS bridge callback contract carries semantic event boundaries

`IOSAudioInterruptionHandler` exposes three platform-only callbacks:

- `onInterruptionBegan()` for an actively-playing session;
- `onInterruptionEnded(shouldResume: Boolean)` for both allowed and disallowed endings;
- `onRouteDisconnected()` for `oldDeviceUnavailable` while playing.

The Swift provider maintains a single `isPlaying` flag at the play/pause/stop/completion choke points and suppresses interruption/route callbacks when playback was already paused. Its existing `play()` operation reactivates `AVAudioSession` before starting the player and reports failure to Kotlin. The iOS engine owns `wasPlayingBeforeInterruption`, the generation/source guard, resume eligibility, progress-job cancellation, and status publication; it publishes `Playing` only after `provider.play()` succeeds, without adding another public provider operation.

### 4. iOS route loss is separate from interruption resumption

An iOS route disconnect is handled from `routeChangeNotification` rather than by opting into `setPrefersInterruptionOnRouteDisconnect`. Route loss pauses without auto-resume because it has no `shouldResume` recommendation. An ordinary route switch is ignored.

### 5. macOS uses an atomic pending flag and existing progress scheduling

The macOS bridge registers the same callback/client-data tuple on `kAudioObjectSystemObject` for `kAudioHardwarePropertyDevices` and `kAudioHardwarePropertyDefaultOutputDevice`, both at global scope/main element. The callback re-reads both the available `AudioDeviceID` list and current default output. It first classifies the previously tracked output as disconnected when that ID is absent, then updates the tracked ID for a benign default-output switch; callback delivery order therefore cannot erase the removed-device evidence. A default-device change alone is never a disconnect. `kAudioDevicePropertyDeviceIsAlive == 0` may be used only as an early unavailability signal, not as the authoritative removal predicate.

The callback records a thread-safe pending route-loss flag only when playback is expected active. `MacOSNativePlaybackEngine` consumes that one-shot flag before normal progress publication in its existing 100 ms scheduler and performs the same pause/status/Now Playing update as a user pause. Play/pause/stop/reset/release keep the native playback-active gate and pending flag coherent so an event received while paused cannot become a stale pause on the next play.

Polling avoids adding reverse-JNI global-reference and method-ID lifetime machinery. The bounded latency is acceptable for an output-route event, and completion detection already uses the same progress loop. The native bridge receives an injectable/testable construction seam so JVM tests can trigger the pending flag without depending on physical audio hardware.

The native helper imports Core Audio HAL declarations and the Gradle `clang++` invocation links `-framework CoreAudio`; this is the only build-configuration change.

### 6. Stale platform events are rejected by the existing source identity

The iOS interruption handler captures the active generation/source version when it is installed. Every callback checks that identity before mutating playback. The macOS scheduler likewise checks the active publication identity before handling a consumed route event. Track switches, clear, and release reset any pending interruption-resume state.

### 7. iOS playback state has one main-thread confinement boundary

The iOS engine and Swift provider use the main thread as their single mutable-state owner. The engine's public entry points, progress work, remote-command bodies, provider installation/clearing, listener assignment/clearing, and all bridge callbacks are dispatched or asserted on that same main-thread boundary before source checks or state mutation. The Swift provider registers `NotificationCenter` observers for `AVAudioSession.sharedInstance()` on the main queue and keeps player, authoritative-playing, handler, and observer-token state main-thread confined. This is an ownership contract, not merely a Kotlin lock: no mutable engine/provider state is accessed from an arbitrary dispatcher concurrently with notification delivery.

The full handler-installed load sequence is covered by one failure cleanup path. If loading or any subsequent setup before successful engine ownership fails, both completion and interruption handlers are cleared before the existing failure is propagated. Current-source interruption ends consume resume eligibility on every path, including failed provider resume; duplicate end and completion callbacks are terminal and cannot publish or restart playback again. Listener replacement/clearing, track replacement, and release are lifecycle boundaries covered by tests.

## Risks / Trade-offs

- **[macOS device-change false positives]** A default-output-device change can be a benign user-selected switch. → On both system-property callbacks, compare the previously tracked ID against the fresh `kAudioHardwarePropertyDevices` array before adopting the fresh default; mark pending route loss only when the old ID disappeared while playback was expected active. Cover removal, benign switch, callback ordering, paused-state suppression, and one-shot consumption with native test hooks.
- **[Native lifetime race]** Core Audio callbacks can outlive a player reset/release. → Register and remove the HAL listener under the existing native handle lifetime boundary, use an atomic flag, and assert listener counts return to zero in JVM tests.
- **[iOS observer lifetime]** Notification observers could retain the provider or fire after a track is replaced. → Store/remove observer tokens with the Swift provider lifetime and use generation/source guards in Kotlin.
- **[Interruption/manual pause race]** A user pause between interruption begin and end could be followed by a system-recommended resume. → The initial implementation follows the selected `shouldResume` contract and records this edge as a known limitation; the event handler must still clear its resume flag after every interruption end.
- **[API deprecation horizon]** Apple has announced future deprecation of iOS interruption enum types. → Keep parsing isolated inside the Swift bridge so migration to the replacement inactive/resumption notifications does not alter the Kotlin engine contract.
- **[Unverified hardware behavior]** Real AirPods takeover, calls, and Siri cannot be reliably automated in repository tests. → Claim only compilation and injected-event regression evidence until separate device/runtime evidence exists.

## Migration Plan

1. Add the RED regressions to the existing iOS and JVM playback test suites.
2. Add the iOS bridge callback contract and Swift notification observers; implement generation-safe engine handling and verify iOS simulator tests.
3. Link CoreAudio and add the macOS HAL listener, atomic pending flag, bridge injection seam, and progress-loop consumption; verify JVM/native bridge tests.
4. Run focused tests, formatting, Detekt, architecture checks, and the repository verification command required by `AGENTS.md`.
5. Rollback is a source-level revert of the platform observer/handler changes; common playback contracts and persisted data are unchanged, so no migration or data rollback is required.

## Open Questions

None. The remaining macOS device-presence predicate is an implementation detail bounded by the selected HAL listener approach and the stated route-loss behavior.
