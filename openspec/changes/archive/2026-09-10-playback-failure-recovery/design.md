# Design: Playback failure recovery

The shared core owns a `PlaybackFailureKind` and extends `PlaybackError` with a default `Unknown` kind. `PlaybackFailureException` carries the structured error out of `PlatformPlaybackEngine.loadPaused`; `PlaybackController.runEngineAction` preserves it instead of overwriting the category with generic text. Existing status/error generation guards remain authoritative.

Platform engines classify only reliable native evidence. Android maps Media3's documented local-source, permission, parser, and decoder error codes; iOS/macOS preflight their managed local path for absence, but opaque native/provider Boolean failures stay `Unknown`. No platform leaks an Android/Apple/JNI error type into the public Shared state.

The controller gets error-gated retry, non-wrapping skip, and serialized current-occurrence removal. Removal calculates the successor from the existing effective order before changing the queue. It removes only that occurrence; it autoplays the successor or, with no successor, clears the engine and publishes idle with the remaining queue. Every accepted queue transition uses the existing checkpoint lifecycle.

Now Playing renders an optional recovery surface only for an active error. Its buttons invoke controller commands; remove launches the suspend command under Compose scope. No recovery accessibility or pointer action remains when state exits error. Strings/semantics are feature-owned.
