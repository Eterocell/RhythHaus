# Crossfade architecture

## Context

All three adapters currently expose one current player. The controller already fences selection generation, playback-run natural completion and gain ownership. ReplayGain preference authority and startup gating are process-owned. Crossfade must preserve those boundaries rather than create a parallel playback controller.

Alternatives: serial fade-out/fade-in is simpler but does not satisfy overlap; two independent PlatformPlaybackEngine instances duplicate Android sessions/audio focus and Apple remote handlers; two internal voices in each existing engine preserve one system authority and are selected.

## Common contract

Add `CrossfadePlaybackEngine : PlatformPlaybackEngine` in `core/playback/src/commonMain/kotlin/com/eterocell/rhythhaus/Crossfade.kt` with:

- `suspend prepareIncoming(track: PlayableTrack, generation: Long): LoadedPlayback`: stage a silent successor without changing the active voice or emitting normal listener events.
- `startIncoming(playbackRunId: Long)`: start staged successor at its configured zero gain and atomically make it the engine's logical current; normal status/progress/errors now carry incoming generation/run.
- `setCrossfadeGains(outgoingGain: Float, incomingGain: Float)`: update both voices; validate finite 0..1 independently.
- `finishCrossfade()`: release only outgoing resources, preserving incoming generation/run/position.
- `cancelPreparedIncoming()`: release a staged, not-promoted voice without altering outgoing.

After startIncoming, ordinary play/pause act on both while outgoing remains; seek retires outgoing before seeking current; stop/clear/release dispose both and restore unity. `setPlaybackGain` targets logical current. Old outgoing completion may be delivered with captured identity and is consumed only by the retiring transaction; it must not be reinterpreted using the incoming token. One native session and remote-transport dispatcher remain. Existing fake/test engines need not acquire this capability; all production first-platform factories must return capable engines.

`CrossfadeDuration { Off, Seconds3, Seconds5, Seconds8 }`, `CrossfadeRuntime` (monotonic playback scheduling), and `effectiveCrossfadeWindowMillis(requestedMillis: Long, outgoingMillis: Long, incomingMillis: Long, remainingMillis: Long): Long?` own pure policy. Linear interpolation is clamped, never additive gain above unity. No periodic work occurs when Off or ineligible.

## Controller transaction

A transition object owns token, outgoing occurrence/generation/run, incoming occurrence/generation/run, loaded durations, remaining-time window and incoming elapsed position. It lives under selectionGate; native changes use engineMutex. Prepare once when within configured window plus 2 seconds; keep outgoing current until start confirmation. Allocate a unique incoming generation without publishing Loading or invalidating outgoing callbacks. Once readiness and eligibility are revalidated under the gate, start incoming at zero. Confirmed Playing promotes current and publishes started/checkpoint once. Gain ticks use incoming advancing position; pause freezes progress. Incoming selection cannot recursively stage a third voice until outgoing is retired.

At natural outgoing completion, retire its voice with no queue advance. Failure before promotion preserves normal outgoing completion; after promotion it reports incoming failure. Every queue/transport/reconcile/timer/gain-mode mutation rechecks transition membership and ownership. Queue changes leaving both members and successor unchanged may retain overlap; changes invalidating either cancel/retire safely. Retire never clears or releases the new winning generation. Disposal and pre-play gain remain fenced by existing gain version and generation.

## Native adapters

Android: two ExoPlayers inside existing MediaSessionService. One session-facing routing facade exposes logical current and routes external transport through controller; internal prepare/start/gain commands retain same-UID engine admission. Secondary does not independently acquire audio focus; route/focus/noisy callbacks pause both. Promote system-facing player at incoming start without letting Media3 auto-advance. Release descriptors/players on cancelled preparation.

iOS: extend the narrow provider seam with separate stage/start/two-gain/retire operations; Swift retains primary and staged/retiring AVAudioPlayers under main-thread serialized ownership. One AVAudioSession and remote command registration. Capture player/generation/run at callback origin, never consult mutable current token for old events. Preserve provider fixture compatibility by migrating repository consumers together; do not export extra feature modules.

macOS: extend the existing native audio handle to own two AVAudioPlayers and JNI operations. Keep one Now Playing/remote and Core Audio route observer; callback userdata lifetime must outlive in-flight callbacks. Retire only the outgoing audio player, not the entire native handle or route observers.

## Persistence/UI

Reuse the ReplayGain preference store/owner patterns in new `CrossfadePreferenceStore` and `CrossfadePreferenceOwner`; process singleton DI, cancellation-safe accepted publication, and restoration in PlaybackProcessLifecycle before session readiness. Settings owns neutral `SettingsCrossfadeDuration` enum; Shared maps it. No database or session schema change; transient overlap is never restored. Localized duration radios remove click/focus actions while a confirmation or write is active; errors retain accepted choice.

## Verification

Pure policy boundaries; controller generation/run races; native simultaneous position advancement; per-voice gains; mode restore and cancelled accepted writes; Settings modal/EN-ZH semantics. Use disposable WAVs for native overlap, unchanged SHA-256 and session/checkpoint behavior. Android/iOS physical listening remains user-owned. Existing Core baseline failures are separate, not suppressed/re-pinned. Build/format gates run after independent writers settle.
