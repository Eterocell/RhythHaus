## Context

The iOS `IOSPlaybackEngine` (`shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt`) manages `MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo` to populate the Lock Screen / Control Center Now Playing panel. The current code conditionally writes `MPMediaItemPropertyPlaybackDuration` only when `durationMillis` is non-null:

```kotlin
durationMillis?.let { put(MPMediaItemPropertyPlaybackDuration, it.toDouble() / 1_000.0) }
```

`durationMillis` is resolved at `load()` time as:
```kotlin
durationMillis = track.durationMillis ?: (audioPlayer.duration * 1_000.0).toLong().takeIf { it > 0L }
```

If both `track.durationMillis` (from TagLib) and `audioPlayer.duration` (from AVAudioPlayer after `prepareToPlay()`) are null/0, `durationMillis` stays null, the duration key is omitted, and iOS greys out the slider and transport row. AirPods gestures still work because `MPRemoteCommandCenter` handler routing is independent of `nowPlayingInfo` state.

The macOS counterpart (`rhythhaus_audio.mm`) sets `MPNowPlayingPlaybackState` explicitly, but this is entitlement-gated on iOS (commit `8d88904` confirmed the no-op). So iOS must rely entirely on the `nowPlayingInfo` dictionary.

## Goals / Non-Goals

**Goals:**
- Ensure `MPMediaItemPropertyPlaybackDuration` is always present in `nowPlayingInfo` when a track is loaded and playing
- Re-probe `AVAudioPlayer.duration` after `play()` starts, since `duration` may return 0 during `prepareToPlay()` but become valid once the audio frames are decoded
- Fall back to `MPNowPlayingInfoPropertyIsLiveStream = true` if duration is truly unknown after all probes, so iOS shows a "live" indicator instead of a greyed-out slider

**Non-Goals:**
- Rewriting the Now Playing threading model (background-thread writes were tested and ruled out on simulator)
- Re-introducing `MPNowPlayingInfoCenter.playbackState` writes (entitlement-gated, no-op on iOS)
- Changing the `PlatformPlaybackEngine` interface or `PlaybackController` queue logic
- Modifying the macOS Obj-C implementation (`rhythhaus_audio.mm`)
- Changing TagLib metadata extraction (the `AudioMetadata.ios.kt` null return is a separate concern)

## Decisions

### Decision 1: Re-probe `audioPlayer.duration` in `play()` — not in `load()`

**Rationale**: `AVAudioPlayer.duration` may return 0 during `prepareToPlay()` for certain formats (the audio decoder hasn't finished reading the file headers). Once `play()` is called and the decoder starts processing frames, `duration` typically becomes valid. Re-probing in `play()` catches this case without blocking the UI thread during `load()`.

**Alternative considered**: Re-probe in a delayed coroutine after `load()`. Rejected because `play()` is the natural point where the audio is actually being decoded, and it's already called on the engine's coroutine scope.

### Decision 2: Use `MPNowPlayingInfoPropertyIsLiveStream = true` as last-resort fallback

**Rationale**: If `audioPlayer.duration` is still 0 after `play()`, the file genuinely has no duration metadata. Setting `IsLiveStream = true` tells iOS to render a "live" indicator instead of a greyed-out slider — a better UX than a disabled control.

**Alternative considered**: Set a sentinel duration value (e.g., `1.0`). Rejected because it would show a slider that immediately reaches 100%, which is misleading.

### Decision 3: Keep `buildIOSNowPlayingDictionary` conditional, but add the fallback in `updateNowPlayingInfo`

**Rationale**: The dictionary builder should remain a pure data function. The fallback logic (IsLiveStream) belongs in `updateNowPlayingInfo()` where the runtime state (`durationMillis`, `audioPlayer.duration`) is available.

## Risks / Trade-offs

- **[Risk] `audioPlayer.duration` returns 0 even after `play()` for some formats** → Mitigation: the `IsLiveStream = true` fallback handles this case gracefully.
- **[Risk] `IsLiveStream = true` changes the lock screen UX for tracks with genuinely unknown duration** → Mitigation: this only applies when duration is truly unresolvable, which should be rare for normal music files (TagLib + AVAudioPlayer should cover most formats).
- **[Risk] Real-device behavior differs from simulator** → Mitigation: the `[NP-DBG]` instrumentation logs remain in place for real-device verification; the fix is defensive (always present duration key or IsLiveStream) so it can't make things worse.
