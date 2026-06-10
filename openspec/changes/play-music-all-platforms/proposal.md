## Why

RhythHaus needs a shared playback capability so users can play local music from the same Compose Multiplatform experience on Android, iOS, and macOS. Planning this first is important because playback touches platform-specific media engines, permissions, file access, lifecycle, and background/audio-session behavior that cannot be safely improvised in shared UI code.

## What Changes

- Introduce an audio playback capability for local tracks across Android, iOS, and macOS.
- Add a shared playback domain model and controller contract for play, pause, seek, queue selection, progress, errors, and lifecycle state.
- Add platform-specific playback engines behind expect/actual or injected interfaces.
- Add shared Compose UI state for now-playing controls without duplicating UI per platform.
- Define a staged implementation path that starts with foreground playback of already-discovered/imported local tracks.
- Defer background playback, lock-screen/Now Playing integration, playlists, streaming, and advanced audio effects to later changes.

## Capabilities

### New Capabilities
- `audio-playback`: Play, pause, seek, and observe local audio playback consistently across Android, iOS, and macOS.

### Modified Capabilities

## Impact

- Shared code under `shared/src/commonMain/kotlin/com/eterocell/rhythhaus` for playback models, state, controller interfaces, and UI integration.
- Platform code under `shared/src/androidMain`, `shared/src/iosMain`, and `shared/src/jvmMain` for playback engines.
- Android may use Media3/ExoPlayer or platform MediaPlayer after dependency decision.
- iOS may use AVFoundation/AVPlayer through Kotlin/Native interop or Swift bridge after implementation spike.
- macOS/desktop JVM may use a JVM audio library, JavaFX MediaPlayer, VLCJ, or another dependency after implementation spike.
- `androidApp`, `iosApp`, and `desktopApp` may need thin lifecycle/audio-session wiring but should not duplicate product UI.
- Future local-library discovery specs must provide playable track URIs/handles compatible with this playback contract.
