## Why

Hardware and peripheral media controls do not work on Android. Pressing play/pause on a wired cable inline remote, a Bluetooth headset, or the lock-screen/notification transport does nothing. This is the Android analogue of the previously fixed iOS AirPods/Now-Playing remote-command bug.

Root cause (investigated via systematic-debugging, Phase 1): the Android engine in `shared/src/androidMain/.../PlaybackEngine.android.kt` builds a standalone Media3 `MediaSession` inside Activity-scoped code (`MainActivity` -> `setRhythHausAndroidContext`). The OS delivers hardware media buttons as `android.intent.action.MEDIA_BUTTON` broadcasts, which require a registered `MediaButtonReceiver` backed by a `MediaSessionService` to receive and route them. RhythHaus declares neither (verified: no `MediaSessionService`, no `MediaButtonReceiver`, and no `<service>`/`<receiver>` in `androidApp/src/main/AndroidManifest.xml`). A bare Activity-scoped `MediaSession` has no delivery target for those broadcasts, so cable/BT/lock-screen controls are silently dropped.

The current `play-music-all-platforms` capability intentionally deferred this: its `audio-playback` spec states Android session/metadata wiring "SHALL NOT by itself claim long-running background playback support." This change adds the deferred transport-control capability.

## What Changes

- Move the Android ExoPlayer + Media3 `MediaSession` into a `MediaSessionService` (foreground-capable) so the OS can route media-button and transport events to it.
- Declare the `MediaSessionService` and the Media3 `MediaButtonReceiver` (with the `MEDIA_BUTTON` intent filter) plus the required foreground-service/media permissions in `androidApp/src/main/AndroidManifest.xml`.
- Load the full playback queue into the Media3 player as a playlist so next/previous transport commands operate on real adjacent items rather than a single `setMediaItem`.
- Add audio attributes + handle-audio-becoming-noisy so unplugging the cable / disconnecting BT auto-pauses (standard expectation).
- Keep the shared `PlaybackController` as the source of truth; bridge Media3 transport callbacks to the existing `PlaybackEngineListener` (`onSkipToNext`/`onSkipToPrevious`/play/pause/seek) so shared state and UI stay authoritative and platform-neutral.

## Capabilities

### New Capabilities
- `android-media-controls`: Respond to Android hardware/peripheral media buttons (wired cable remote, Bluetooth headset) and system transport controls (lock screen, notification) and keep them in sync with shared playback state.

### Modified Capabilities
- `audio-playback`: lift the Android background/transport-control deferral now that a `MediaSessionService` exists.

## Impact

- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.android.kt`: engine restructured to own/connect to a `MediaSessionService`-hosted player; queue loaded as a Media3 playlist; audio attributes + become-noisy handling added.
- New Android `MediaSessionService` subclass (shared androidMain or androidApp, kept thin).
- `androidApp/src/main/AndroidManifest.xml`: declare service + `MediaButtonReceiver` + `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (and `POST_NOTIFICATIONS` for API 33+) permissions.
- `shared/build.gradle.kts` already depends on `androidx.media3.session` + `media3.exoplayer`; no new dependency expected.
- iOS and macOS engines are unaffected.

## Non-goals

- No change to iOS or macOS playback (iOS remote commands already fixed).
- No Android Auto / cast / streaming support.
- No new product UI; transport surfaces are the OS-provided controls plus existing shared Compose UI.

## Verification

- Automated: shared common/JVM tests stay green; `./gradlew :androidApp:assembleDebug` succeeds; new androidMain unit coverage where logic is testable without a device (queue->playlist mapping, listener bridging).
- Manual (device required, recorded as blocker until done): play a track, press play/pause on a wired cable inline remote and a Bluetooth headset, use lock-screen next/previous, and unplug mid-playback to confirm auto-pause.
