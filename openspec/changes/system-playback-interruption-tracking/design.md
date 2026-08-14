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

The Swift provider maintains a single `isPlaying` flag at the play/pause/stop/completion choke points and suppresses interruption/route callbacks when playback was already paused. The iOS engine owns `wasPlayingBeforeInterruption`, the generation/source guard, session reactivation, progress-job cancellation, and status publication.

### 4. iOS route loss is separate from interruption resumption

An iOS route disconnect is handled from `routeChangeNotification` rather than by opting into `setPrefersInterruptionOnRouteDisconnect`. Route loss pauses without auto-resume because it has no `shouldResume` recommendation. An ordinary route switch is ignored.

### 5. macOS uses an atomic pending flag and existing progress scheduling

The macOS HAL callback records a thread-safe pending route-loss flag when the active output device is removed or unavailable. `MacOSNativePlaybackEngine` consumes that flag before normal progress publication in its existing 100 ms scheduler and performs the same pause/status/Now Playing update as a user pause.

Polling avoids adding reverse-JNI global-reference and method-ID lifetime machinery. The bounded latency is acceptable for an output-route event, and completion detection already uses the same progress loop. The native bridge receives an injectable/testable construction seam so JVM tests can trigger the pending flag without depending on physical audio hardware.

### 6. Stale platform events are rejected by the existing source identity

The iOS interruption handler captures the active generation/source version when it is installed. Every callback checks that identity before mutating playback. The macOS scheduler likewise checks the active publication identity before handling a consumed route event. Track switches, clear, and release reset any pending interruption-resume state.

## Risks / Trade-offs

- **[macOS device-change false positives]** A default-output-device change can be a benign user-selected switch. → Track the previously active device and mark pending route loss only when that device is no longer present/unavailable; cover the predicate with a native test hook rather than pausing on every change.
- **[Native lifetime race]** Core Audio callbacks can outlive a player reset/release. → Register and remove the HAL listener under the existing native handle lifetime boundary, use an atomic flag, and assert listener counts return to zero in JVM tests.
- **[iOS observer lifetime]** Notification observers could retain the provider or fire after a track is replaced. → Store/remove observer tokens with the Swift provider lifetime and use generation/source guards in Kotlin.
- **[Interruption/manual pause race]** A user pause between interruption begin and end could be followed by a system-recommended resume. → The initial implementation follows the selected `shouldResume` contract and records this edge as a known limitation; the event handler must still clear its resume flag after every interruption end.
- **[API deprecation horizon]** Apple has announced future deprecation of iOS interruption enum types. → Keep parsing isolated inside the Swift bridge so migration to the replacement inactive/resumption notifications does not alter the Kotlin engine contract.
- **[Unverified hardware behavior]** Real AirPods takeover, calls, and Siri cannot be reliably automated in repository tests. → Claim only compilation and injected-event regression evidence until separate device/runtime evidence exists.

## Migration Plan

1. Add the RED regressions to the existing iOS and JVM playback test suites.
2. Add the iOS bridge callback contract and Swift notification observers; implement generation-safe engine handling and verify iOS simulator tests.
3. Add the macOS HAL listener, atomic pending flag, bridge injection seam, and progress-loop consumption; verify JVM/native bridge tests.
4. Run focused tests, formatting, Detekt, architecture checks, and the repository verification command required by `AGENTS.md`.
5. Rollback is a source-level revert of the platform observer/handler changes; common playback contracts and persisted data are unchanged, so no migration or data rollback is required.

## Open Questions

None. The remaining macOS device-presence predicate is an implementation detail bounded by the selected HAL listener approach and the stated route-loss behavior.
