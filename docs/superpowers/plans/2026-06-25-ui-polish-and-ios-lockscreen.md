# UI Polish + iOS Lockscreen Fixes

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix three bugs: Clear Library button font mismatch, NowPlayingScreen next-track UI staleness, and iOS lockscreen player panel missing.

**Architecture:** Three independent one-file fixes in the shared Compose UI and iOS playback engine.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, iOS AVFAudio/MediaPlayer cinterop

## Global Constraints

- Shared-first KMP: put all fixes in `shared/src/` unless platform-specific
- No unrelated refactors, no dependency changes
- Semantic conventional commits: `fix: ...`
- Verification: `./init.sh` (shared JVM test + desktop compile + Android debug + iOS simulator test)
- No background playback or notification-service scope

---

### Task 1: Clear Library Button Font Consistency

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt:539`

**Interfaces:**
- Consumes: `ImportAudioCard` composable button styles
- Produces: N/A

The "Add music folder" button inside `ImportAudioCard` uses `fontWeight = FontWeight.Black` (line 527). The "Clear Library" button currently uses `fontWeight = FontWeight.Medium` (line 539). Match them.

- [ ] **Step 1: Change fontWeight in Clear Library button**

In `App.kt`, function `ImportAudioCard`, the Clear Library `Text` composable (approx line 539):

```kotlin
Text("Clear Library", fontSize = 13.sp, fontWeight = FontWeight.Medium)
```

Change to:

```kotlin
Text("Clear Library", fontSize = 13.sp, fontWeight = FontWeight.Black)
```

- [ ] **Step 2: Build and verify**

Run: `./gradlew :shared:compileKotlinMetadata --configuration-cache`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
git commit -m "fix: match Clear Library button font weight to Add music folder button"
```

---

### Task 2: Fix NowPlayingScreen Next-Track UI Staleness

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt:203-210` (LibraryHomeScreen caller)
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt:1076-1078` (DrillDownView caller)

**Interfaces:**
- Consumes: `PlaybackController.state` (StateFlow<PlaybackState>), `PlayableTrack.id`
- Produces: N/A

**Root cause:** `NowPlayingScreen` displays `track: Track` from its caller's local `selectedTrackId` state. When the next-track button calls `playbackController.selectTrack(nextTrack.id)`, the controller updates `playbackState.currentTrack` but neither `LibraryHomeScreen` nor `DrillDownView` updates their local `selectedTrackId` to match. The result: audio advances to the next track but the screen still shows the previous track's title, artist, artwork, etc.

**Fix:** In both callers, add a `LaunchedEffect` keyed on `playbackState.currentTrack?.id` that updates `selectedTrackId`.

- [ ] **Step 1: Fix LibraryHomeScreen selectedTrackId reactivity**

In `LibraryHomeScreen` (around line 203), after the existing `var selectedTrackId` line, add:

```kotlin
LaunchedEffect(playbackState.currentTrack?.id) {
    playbackState.currentTrack?.id?.let { selectedTrackId = it }
}
```

Make sure `LaunchedEffect` is imported (`import androidx.compose.runtime.LaunchedEffect`).

- [ ] **Step 2: Fix DrillDownView selectedTrackId reactivity**

In `DrillDownView` (around line 1076), after the existing `var selectedTrackId` line, add:

```kotlin
LaunchedEffect(playbackState.currentTrack?.id) {
    playbackState.currentTrack?.id?.let { selectedTrackId = it }
}
```

- [ ] **Step 3: Build and verify**

Run: `./gradlew :shared:compileKotlinMetadata --configuration-cache`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
git commit -m "fix: update NowPlayingScreen track info when advancing to next track"
```

---

### Task 3: iOS Lockscreen Player Panel

**Files:**
- Modify: `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt`

**Interfaces:**
- Consumes: `PlayableTrack`, `AVAudioplayer`, platform MediaPlayer cinterop
- Produces: N/A

**Root cause:** The iOS engine sets `MPNowPlayingInfoCenter.nowPlayingInfo` with track metadata and elapsed time, but does NOT register `MPRemoteCommandCenter` handlers. Without remote command registration, iOS won't show the Now Playing widget on the lockscreen or Control Center. Identical to the macOS bug fixed in a prior session.

**Fix:** Add `MPRemoteCommandCenter` registration in `configureAudioSession()`. Register play, pause, togglePlayPause, stop, and changePlaybackPosition commands. Set `playbackState` on `MPNowPlayingInfoCenter` when play/pause/stop.

- [ ] **Step 1: Add MPRemoteCommandCenter imports**

Add to existing imports at top of `PlaybackEngine.ios.kt`:

```kotlin
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatus
import platform.MediaPlayer.MPChangePlaybackPositionCommandEvent
import kotlinx.cinterop.ObjCAction
```

Note: Kotlin/Native cinterop may use block-based APIs differently. The critical imports are `MPRemoteCommandCenter` and `MPRemoteCommandHandlerStatus`.

- [ ] **Step 2: Register remote commands in configureAudioSession**

Replace the existing `configureAudioSession()` with:

```kotlin
private fun configureAudioSession() {
    val session = AVAudioSession.sharedInstance()
    session.setCategory(AVAudioSessionCategoryPlayback, error = null)
    session.setActive(true, error = null)
    registerRemoteCommands()
}

private fun registerRemoteCommands() {
    val commandCenter = MPRemoteCommandCenter.sharedCommandCenter()
    commandCenter.playCommand.setEnabled(true)
    commandCenter.pauseCommand.setEnabled(true)
    commandCenter.togglePlayPauseCommand.setEnabled(true)
    commandCenter.stopCommand.setEnabled(true)
    commandCenter.changePlaybackPositionCommand.setEnabled(true)

    commandCenter.playCommand.addTargetWithHandler { _ ->
        player?.play()
        MPRemoteCommandHandlerStatus.MPRemoteCommandHandlerStatusSuccess
    }
    commandCenter.pauseCommand.addTargetWithHandler { _ ->
        player?.pause()
        MPRemoteCommandHandlerStatus.MPRemoteCommandHandlerStatusSuccess
    }
    commandCenter.togglePlayPauseCommand.addTargetWithHandler { _ ->
        val p = player
        if (p != null) {
            if (p.isPlaying()) { p.pause() } else { p.play() }
        }
        MPRemoteCommandHandlerStatus.MPRemoteCommandHandlerStatusSuccess
    }
    commandCenter.stopCommand.addTargetWithHandler { _ ->
        player?.stop()
        MPRemoteCommandHandlerStatus.MPRemoteCommandHandlerStatusSuccess
    }
    commandCenter.changePlaybackPositionCommand.addTargetWithHandler { event ->
        if (event is MPChangePlaybackPositionCommandEvent) {
            player?.currentTime = event.positionTime
        }
        MPRemoteCommandHandlerStatus.MPRemoteCommandHandlerStatusSuccess
    }
}
```

- [ ] **Step 3: Set MPNowPlayingInfoCenter.playbackState on play/pause/stop**

In `play()`, after `listener?.onPlaybackStatus(PlaybackStatus.Playing)`, add:
```kotlin
MPNowPlayingInfoCenter.defaultCenter().playbackState = 1L  // MPNowPlayingPlaybackStatePlaying
```

In `pause()`, after `listener?.onPlaybackStatus(PlaybackStatus.Paused)`, add:
```kotlin
MPNowPlayingInfoCenter.defaultCenter().playbackState = 2L  // MPNowPlayingPlaybackStatePaused
```

In `stop()`, after `listener?.onPlaybackStatus(PlaybackStatus.Stopped)`, add:
```kotlin
MPNowPlayingInfoCenter.defaultCenter().playbackState = 0L  // MPNowPlayingPlaybackStateStopped
```

In `release()`, before setting `nowPlayingInfo = null`, add:
```kotlin
MPNowPlayingInfoCenter.defaultCenter().playbackState = 0L
```

- [ ] **Step 4: Build and verify iOS**

Run: `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Full verification**

Run: `./init.sh`
Expected: BUILD SUCCESSFUL for all platforms

- [ ] **Step 6: Commit**

```bash
git add shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt
git commit -m "fix: register iOS MPRemoteCommandCenter handlers for lockscreen player panel"
```
