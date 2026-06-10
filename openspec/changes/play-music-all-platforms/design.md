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

### Decision 5: Playback backend direction and supported local handles

Use platform-native or platform-preferred media backends for the production direction:

- Android: prefer Media3/ExoPlayer instead of platform `MediaPlayer` for robust content URI handling, lifecycle integration, future background playback, notification controls, and codec behavior.
- iOS: use a native Apple audio backend. The current Kotlin/Native AVFAudio `AVAudioPlayer` implementation is native and acceptable for simple imported local-file foreground playback; AVFoundation/`AVPlayer` or a Swift bridge can be considered when iOS document/media-library access or richer playback behavior requires it.
- macOS desktop: prefer a native macOS audio backend instead of Java Sound for production playback. Because the desktop app currently runs on JVM, this likely requires an explicit bridge/dependency decision, such as AVFoundation through a Kotlin/JVM bridge, a small Swift/Objective-C helper, or another macOS-native media layer that preserves DMG packaging.

First-slice format support is intentionally conservative:

- Android/iOS: platform-decoded local audio files supplied as file paths or URL strings.
- macOS: native-backend-supported local files supplied as file paths or file URLs.
- No sample/demo playback fallback should be used; playback should load real imported or scanned local audio sources.

Rationale: The initial Java Sound and MediaPlayer choices were dependency-light first-slice spikes to prove the shared controller/UI seams. They are not the preferred long-term product backends. Media3 is the better Android product direction, iOS should stay on native Apple audio APIs, and macOS should use a native backend rather than Java Sound's limited codec surface.

Follow-up backend migration triggers:

- Migrate Android foreground playback from `MediaPlayer` to Media3/ExoPlayer before treating Android playback as product-grade.
- Keep iOS playback on native Apple audio APIs; choose between the existing `AVAudioPlayer`, AVFoundation `AVPlayer`, or a Swift bridge based on import/media-library needs.
- Replace macOS Java Sound playback with a native macOS backend after selecting the bridge/dependency approach and verifying DMG packaging.

## Risks / Trade-offs

- iOS local file/media access may not match Android/macOS assumptions → Mitigation: keep source abstraction explicit and decide iOS discovery/import separately.
- Desktop native audio integration may complicate JVM/macOS packaging → Mitigation: spike the bridge/dependency and verify `desktopApp` compile and DMG packaging impact before broadening feature scope.
- Android Media3 adds dependency weight → Mitigation: accept the dependency for product-grade Android playback once the migration task starts, and keep the shared controller boundary unchanged.
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
- Which Media3 subset and Android lifecycle integration should be used for the first product-grade Android playback slice?
- Should iOS stay with Kotlin/Native `AVAudioPlayer`, move to AVFoundation `AVPlayer`, or use a Swift bridge when document/media-library integration is added?
- Which macOS-native audio bridge/backend gives the best codec support and DMG packaging trade-off for a JVM desktop app?
- What will provide persistent local handles after manual import: folder scanner, MediaStore/media-library integration, app-local copies, or bookmark/security-scoped URLs?
