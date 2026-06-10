## Context

RhythHaus currently has a shared Compose Multiplatform UI slice with deterministic demo music data, but no real playback layer. The app targets Android, iOS, and macOS first. Audio playback is inherently platform-specific: each target has different media APIs, supported URI/file handles, audio-session behavior, permissions, lifecycle semantics, and background playback rules.

This change plans foreground playback of supported local tracks through a shared controller and platform-specific engines. It assumes local-library discovery or import will provide a playable local track handle; if discovery is not implemented yet, the first implementation may use bundled/sample/test audio handles or manually supplied handles for engine verification.

## Goals / Non-Goals

**Goals:**

- Define a shared playback domain model that works across Android, iOS, and macOS.
- Define a shared playback controller API for play, pause, stop, seek, selecting queue items, and observing state.
- Hide platform engines behind expect/actual or injected platform implementations.
- Integrate playback state into shared Compose now-playing controls.
- Implement foreground playback for supported local audio files on Android, iOS, and macOS.
- Keep platform entry modules thin and avoid duplicating product UI.

**Non-Goals:**

- Background playback.
- Lock-screen controls / Android notification controls / iOS Now Playing center.
- Playlist management beyond a simple in-memory queue.
- Streaming or network audio.
- Equalizer, audio effects, gapless playback, crossfade, lyrics, or visualizers.
- Full local-library scanning; that should be a separate OpenSpec change unless explicitly folded in later.
- Windows/Linux support.

## Decisions

### Decision 1: Shared API with platform engines

Use a shared `PlaybackController` contract and platform-specific `PlaybackEngine` implementations. Common code owns queue state, user intents, formatting, and UI state. Platform code owns actual media loading, playback, seeking, duration discovery when available, progress polling callbacks, and resource release.

Rationale: Compose UI and app behavior should stay consistent, while the media stack must use native or platform-appropriate APIs.

Alternatives considered:

- Fully shared JVM/native library: unlikely to work uniformly on iOS and may introduce heavy dependencies.
- Separate platform UI/player implementations: faster to prototype, but conflicts with the shared UI product goal.

### Decision 2: Foreground playback first

The first implementation should support foreground in-app playback only. Background playback and OS media controls require separate platform policies and UX decisions.

Rationale: This keeps the first playback slice small enough to verify on all three current platforms.

Alternatives considered:

- Implement background playback immediately: higher risk because Android foreground service/notification policy, iOS audio session/background modes, and macOS menu/remote controls differ.

### Decision 3: Stable shared playback state

Use a shared immutable state model with fields such as:

- current track id
- current track metadata
- queue ids
- status: idle/loading/buffering/playing/paused/stopped/error
- position milliseconds
- duration milliseconds when known
- error message when recoverable failure occurs

Rationale: Shared Compose UI can render deterministically and tests can verify pure reducer/state logic without platform players.

Alternatives considered:

- Let platform player state leak into UI: less boilerplate, but harms testability and cross-platform consistency.

### Decision 4: Track source abstraction before scanner details

Represent playback input as a platform-safe local audio handle rather than assuming all platforms can use the same file path or URI string.

Possible common model:

```kotlin
data class PlayableTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val durationMillis: Long?,
    val source: AudioSource,
)

sealed interface AudioSource {
    val stableKey: String
}
```

The actual source representation may require expect/actual wrappers or platform mapping.

Rationale: Android content URIs, iOS media-library assets/imported files, and macOS file URLs are not equivalent.

Alternatives considered:

- Use plain file paths everywhere: simple on macOS, often wrong for Android MediaStore and iOS.
- Use plain URI strings everywhere: may work for some cases, but hides permission/lifetime semantics.

### Decision 5: First-slice engine choices and supported local handles

Use lightweight platform engines for the first foreground playback slice:

- Android: platform `MediaPlayer`, chosen to avoid introducing Media3/ExoPlayer before local scanner/import and background playback decisions are made.
- iOS: Kotlin/Native interop with AVFAudio `AVAudioPlayer`, chosen for simple foreground playback of local file URLs without a Swift bridge.
- macOS/desktop JVM: Java Sound `Clip`, chosen because it is dependency-free and does not change macOS DMG packaging.

First-slice format support is intentionally conservative:

- Android/iOS: platform-decoded local audio files supplied as file paths or URL strings.
- macOS/JVM: Java Sound-supported local files, primarily WAV/AIFF/AU unless a later dependency adds MP3/AAC/FLAC.
- Demo library tracks currently have metadata only. Pressing play before scanner/import provides a recoverable user-facing error instead of pretending bundled audio exists.

Rationale: This gives the shared controller/UI and platform seams real code on Android, iOS, and macOS without broadening into scanner/import, background audio, or heavyweight codec dependencies.

Future revisit triggers:

- Add Media3/ExoPlayer when Android background audio, notification controls, or robust MediaStore content URI playback are specified.
- Reconsider Swift bridge or MusicKit/media-library integration when iOS local-library access is specified.
- Reconsider JavaFX MediaPlayer, VLCJ, or another library when macOS MP3/AAC/FLAC support and packaging trade-offs are specified.

## Risks / Trade-offs

- iOS local file/media access may not match Android/macOS assumptions → Mitigation: keep source abstraction explicit and decide iOS discovery/import separately.
- Desktop JVM audio dependency may complicate native DMG packaging → Mitigation: spike before committing to a library and verify `desktopApp` packaging impact.
- Android Media3 adds dependency weight → Mitigation: compare against MediaPlayer for first local-file-only slice.
- Progress polling can leak coroutines/resources → Mitigation: lifecycle/release requirement and tests for controller disposal behavior.
- Background playback expectations may surprise users → Mitigation: explicitly mark background playback as non-goal in UI/spec until implemented.
- Codec support varies by platform → Mitigation: document supported formats for the first slice and expose user-visible errors.

## Migration Plan

1. Add shared playback model/controller interfaces and pure tests.
2. Add a fake playback engine for common tests and previews.
3. Add shared now-playing UI controls wired to fake/controller state.
4. Spike Android engine with one supported local/sample audio source.
5. Spike iOS engine with one supported local/sample audio source.
6. Spike macOS/JVM engine with one supported local/sample audio source.
7. Choose platform dependencies based on spike results and update tasks/spec notes if necessary.
8. Integrate real engines behind the shared controller.
9. Run full harness verification via `./init.sh` plus any manual app playback checks available.

Rollback strategy: keep playback behind new controller/UI state. If a platform engine fails, keep fake/demo playback disabled for production on that platform and record the platform blocker without breaking existing shared UI.

## Open Questions

- Which first audio formats are officially supported: mp3 only, or mp3/aac/flac/wav?
- Should Android use Media3/ExoPlayer immediately, or start with platform MediaPlayer?
- Should iOS playback be implemented directly in Kotlin/Native via AVFoundation, or via Swift bridge?
- Which JVM/macOS playback library gives the best packaging/codecs trade-off?
- What will provide initial playable local handles: bundled sample, manual file picker/import, or separate library scanner change?
