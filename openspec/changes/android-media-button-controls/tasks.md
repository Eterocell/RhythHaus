## 1. Spike: engine <-> MediaSessionService connection model

- [x] 1.1 Spike how the shared `PlatformPlaybackEngine` connects to a Media3 `MediaSessionService`. Decision: connect via `MediaController` (`SessionToken` + `MediaController.Builder(...).buildAsync()`), store the app `Context` via `setRhythHausAndroidContext`.
- [x] 1.2 Decide skip semantics. Decision: shared `PlaybackController` keeps queue ownership; the service player is a `ForwardingPlayer` (`SkipRoutingPlayer`) that advertises next/previous and routes them via `RhythHausTransportBridge` to `onSkipToNext`/`onSkipToPrevious` rather than advancing an internal media3 playlist.
- [x] 1.3 Confirmed `androidx.media3.session` 1.10.1 provides `MediaSessionService` + `MediaButtonReceiver`; guava `ListenableFuture` present transitively. No new dependency.

## 2. MediaSessionService + manifest wiring

- [x] 2.1 Added `RhythHausPlaybackService : MediaSessionService` owning the ExoPlayer + `MediaSession`, returns session from `onGetSession()`.
- [x] 2.2 Declared the service in `androidApp/src/main/AndroidManifest.xml` with the `androidx.media3.session.MediaSessionService` intent filter (`foregroundServiceType="mediaPlayback"`).
- [x] 2.3 Declared `androidx.media3.session.MediaButtonReceiver` with the `android.intent.action.MEDIA_BUTTON` intent filter and `android:exported="true"`.
- [x] 2.4 Added `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, and `POST_NOTIFICATIONS` permissions.

## 3. Engine restructure

- [x] 3.1 Restructured `PlaybackEngine.android.kt` to connect to the service-owned player via `MediaController` instead of building a standalone Activity-scoped `MediaSession`.
- [x] 3.2 Queue ownership stays in the shared controller; next/previous routed through the bridge (see 1.2). (Single `setMediaItem` per load retained intentionally.)
- [x] 3.3 Bridged media3 transport/media-button callbacks to `PlaybackEngineListener` (play/pause/seek via controller listener; skip via bridge).
- [x] 3.4 Set `AudioAttributes` (usage=media, content=music, `handleAudioFocus=true`) and `setHandleAudioBecomingNoisy(true)` for auto-pause on output disconnect.
- [x] 3.5 Handled async connection readiness with a single latest `pendingAction` run on connect (mirrors existing `playWhenLoaded`).
- [x] 3.6 `release()` tears down controller + future + bridge handlers.

## 4. Tests

- [x] 4.1 `RhythHausTransportBridgeTest` covers bridge handler invocation and engine listener->bridge wiring.
- [x] 4.2 Existing `AndroidPlaybackMediaSessionTest` covers media-item/metadata mapping.

## 5. Verification and acceptance

- [x] 5.1 `./gradlew :shared:jvmTest` -> green.
- [x] 5.2 `./gradlew :androidApp:assembleDebug` -> green; merged manifest contains service+receiver+permission.
- [ ] 5.3 Manual device matrix: wired cable inline-remote play/pause, Bluetooth play/pause, lock-screen/notification next/previous, unplug-mid-playback auto-pause.
- [x] 5.4 Record verification + remaining manual blockers in `progress.md`; update OpenSpec task status.
- [x] 5.5 Reviewer-level acceptance against `android-media-controls` requirements: reviewer subagent found 2 Critical (C1 pendingAction overwrite, C2 missing POST_NOTIFICATIONS runtime request) + 3 Important (I1 uncancelled scope, I2 release-during-connect leak, I3 double-release) — all addressed. Archive only after manual device validation (5.3) passes.
