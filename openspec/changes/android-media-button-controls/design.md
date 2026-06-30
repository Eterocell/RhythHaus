## Context

Android cable/Bluetooth/lock-screen media controls do nothing. Root cause (Phase 1 systematic-debugging): `PlaybackEngine.android.kt:45` builds a standalone Media3 `MediaSession` in Activity-scoped code with no `MediaSessionService` and no `MediaButtonReceiver`. The OS delivers hardware media buttons as `MEDIA_BUTTON` broadcasts that need a registered receiver + service to land on. This is the Android twin of the iOS AirPods bug (missing `MPRemoteCommandCenter` handlers).

Constraints:
- Shared `PlaybackController` (`shared/src/commonMain/.../Playback.kt`) owns the queue and is the source of truth; the engine is a thin `PlatformPlaybackEngine` behind `expect/actual`. The fix must not move queue logic into the engine.
- Project rule (AGENTS.md): Miuix UI only, shared-first, no new product UI per platform.
- Cable/BT/lock-screen behavior cannot be validated in CI — manual on-device validation is required and must be recorded as a blocker until performed.

## Goals / Non-Goals

Goals:
- Hardware media buttons (wired cable remote, Bluetooth) toggle playback.
- System transport (lock screen / notification) play/pause/next/previous work and stay in sync with shared state.
- Next/previous operate on the real queue.
- Auto-pause on audio-becoming-noisy (cable unplug / BT disconnect).

Non-goals:
- iOS/macOS changes, Android Auto, cast, streaming, new product UI.

## Decisions

### Decision: Host the player in a Media3 `MediaSessionService`
Media3 only routes `MEDIA_BUTTON` broadcasts through a `MediaButtonReceiver`, which forwards to a `MediaSessionService`/`MediaLibraryService`. A bare Activity-scoped session has no delivery target. So the ExoPlayer + `MediaSession` move into a `MediaSessionService`; the engine connects to it (e.g. via `MediaController`/session token) and bridges callbacks to `PlaybackEngineListener`.

Alternatives rejected:
- Receiver-only without a service: media3's `MediaButtonReceiver` still requires a service target; does not fully work.
- Custom `BroadcastReceiver` for `MEDIA_BUTTON`: re-implements what media3 already provides and fights the framework. Rejected.

### Decision: Load the queue as a Media3 playlist
Today `load()` calls `setMediaItem` (single item), so even if next/prev arrived there'd be nothing to skip to. Map the shared queue to `List<MediaItem>` so media3's built-in next/previous map onto adjacent tracks; bridge media3's transition back to `onSkipToNext/onSkipToPrevious` (or drive shared controller selection) so shared state stays authoritative. Exact ownership of skip semantics (let media3 advance vs. delegate to controller) is the main implementation question to resolve in the spike task.

### Decision: Audio attributes + handle-audio-becoming-noisy
Set `AudioAttributes` (usage=media, contentType=music) with `setHandleAudioBecomingNoisy(true)` on the ExoPlayer so unplug/disconnect auto-pauses — standard media-app behavior and part of the reported symptom class.

### Decision: Keep the service thin, shared controller authoritative
The service hosts the player; the shared `PlaybackController` remains the source of truth for queue/state. Transport callbacks bridge into the existing `PlaybackEngineListener` contract — no duplicate state machine.

## Risks / Trade-offs

- Foreground-service + notification + `POST_NOTIFICATIONS` (API 33+) and `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (API 34+) add manifest/permission surface and lifecycle complexity. Mitigation: lean on media3's auto-managed `MediaNotification`; keep service minimal.
- Engine<->service connection is async (`MediaController` future); load/play timing must handle the connection not being ready. Mitigation: queue intents until connected, mirroring existing `playWhenLoaded` pattern.
- Manual-only validation for the core symptom. Mitigation: record as explicit blocker; unit-test the testable seams (queue mapping, listener bridging).

## Migration / Verification

- `./gradlew :shared:jvmTest` and `:androidApp:assembleDebug` stay green.
- New androidMain coverage for queue->MediaItem mapping and listener bridging where device-independent.
- Manual device matrix: wired cable remote play/pause, BT play/pause, lock-screen next/prev, unplug auto-pause. Record in `progress.md`; do not claim fixed until done.
