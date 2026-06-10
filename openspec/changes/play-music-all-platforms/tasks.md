## 1. Shared playback foundation

- [x] 1.1 Add shared playback domain models for playable tracks, audio source identity, playback status, playback error, and playback state.
- [x] 1.2 Add a shared playback controller contract for load/play/pause/stop/seek/select queue/release.
- [x] 1.3 Add a platform playback engine abstraction that hides Android, iOS, and JVM/macOS player APIs.
- [x] 1.4 Add a fake playback engine for common tests and Compose previews.
- [x] 1.5 Add common tests for state transitions: idle → loading → playing, playing → paused, seek, error recovery, and release.

## 2. Shared Compose playback UI

- [x] 2.1 Replace the demo-only now-playing button with playback controls driven by shared playback state.
- [x] 2.2 Add play/pause, seek/progress display, current track status, and recoverable error rendering to shared Compose UI.
- [x] 2.3 Add accessibility labels/content descriptions for playback controls so future mobile UI tests can target them across platforms.
- [x] 2.4 Verify shared UI compiles on desktop and Android after playback UI wiring.

## 3. Android playback spike and implementation

- [x] 3.1 Decide Android engine candidate for first slice: platform `MediaPlayer` for minimal dependency while scanner/import decisions are pending; Media3 remains a likely upgrade when background playback and library integration are planned.
- [x] 3.2 Add Android-specific engine implementation for one supported local/sample audio source.
- [x] 3.3 Wire Android lifecycle/resource release enough to avoid player leaks during Activity/app disposal.
- [ ] 3.4 Verify Android debug build and, when device/emulator is available, manually confirm foreground play/pause/seek.

## 4. iOS playback spike and implementation

- [x] 4.1 Decide iOS implementation path: Kotlin/Native AVFAudio `AVAudioPlayer` interop for first local-file foreground playback slice; Swift bridge can be reconsidered if media-library/MusicKit integration requires it.
- [x] 4.2 Add iOS-specific engine implementation for one supported local/sample audio source.
- [x] 4.3 Configure foreground audio-session behavior needed for in-app playback without claiming background playback support.
- [ ] 4.4 Verify iOS simulator shared tests and, when simulator/device playback is available, manually confirm foreground play/pause/seek.

## 5. macOS/JVM playback spike and implementation

- [x] 5.1 Decide macOS/JVM engine candidate: Java Sound `Clip` for dependency-free WAV/AIFF first-slice playback; richer codecs may require a later JavaFX/VLCJ decision.
- [x] 5.2 Add JVM/macOS-specific engine implementation for one supported local/sample audio source.
- [x] 5.3 Verify desktop compile/run behavior and check whether chosen dependency affects macOS DMG packaging.
- [ ] 5.4 Manually confirm foreground play/pause/seek on macOS.

## 6. Cross-platform integration and acceptance

- [x] 6.1 Ensure shared controller selects the correct platform engine without duplicating product UI in app modules.
- [x] 6.2 Document supported first-slice audio formats and known platform limitations.
- [x] 6.3 Run `./init.sh` and record results in `progress.md`.
- [x] 6.4 Perform reviewer-level acceptance against `audio-playback` requirements and record remaining risks.
- [ ] 6.5 Update OpenSpec task status and archive only after implementation and verification are complete.
