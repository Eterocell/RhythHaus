# iOS Native SwiftUI Migration — Phase A: Shared Cleanup + SwiftUI Core

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove Compose Multiplatform UI from the shared KMP module (keeping domain/data/SQLDelight/TagLib), build a native iOS SwiftUI app consuming `Shared.framework`, and verify Android Compose still works.

**Architecture:** Shared KMP module exports data models (`PlayableTrack`, `PlaybackState`, `Track`, `AlbumGroup`, `LibraryTrack`), database (`SqlDelightLibraryRepository`), scanner (`LibraryScanner`), metadata reader (`AudioMetadataReader`), TagLib reader, color palette (`HausColors`), and data-processing helpers (`LibraryBrowser` grouping functions). iOS SwiftUI consumes `Shared.framework` and builds its own native `AVAudioPlayer` engine + SwiftUI views. Android keeps its Compose UI unchanged.

**Tech Stack:** Kotlin Multiplatform (shared core), SwiftUI (iOS UI), Compose Multiplatform (Android UI), SQLDelight, TagLib

## Global Constraints

- Android must still build and pass existing tests (`./gradlew :androidApp:assembleDebug :shared:testAndroidHostTest`)
- Shared JVM tests must pass (`./gradlew :shared:jvmTest`)
- iOS SwiftUI app must compile in Xcode and launch
- Do NOT remove the `desktopApp` module — it stays for macOS (future Phase C)
- Semantic conventional commits: `refactor:`, `feat:`, `fix:`
- No unrelated refactors, no dependency upgrades unless required
- Platform boundary: iOS playback engine goes in Swift, Android in `androidMain`, macOS stays in `jvmMain` for now
- Keep `:taglib` module unchanged

---

### Task 1: Extract shared helpers from App.kt into proper locations

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryBrowser.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/MusicModels.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`

**Interfaces:**
- Consumes: `Track`, `PlayableTrack`, `LibraryTrack`, `AudioSource`, `LibrarySnapshot`, `TrackAccent`
- Produces: `Track.toPlayableTrack()` (public extension), `LibraryTrack.toUiTrack()` (public extension), `librarySnapshot()` (public function), `TrackAccent.Companion.forIndex()` (public factory)

The following helpers are currently `private` inside `App.kt` and must be moved to shared locations so Android Compose and future iOS SwiftUI can both consume them:

1. `Track.toPlayableTrack()` (App.kt:1368) → Move to `MusicModels.kt` as a public extension function
2. `librarySnapshot()` (App.kt:1378) → Move to `MusicModels.kt` as a public function. The `libraryTrackAccent()` function (App.kt:1396) must also move — rename to `accentForIndex()` and make public.
3. `LibraryTrack.toUiTrack()` — already exists in `LibraryBrowser.kt:83` as a private function. Make it `internal` instead of `private`.

- [ ] **Step 1: Move `Track.toPlayableTrack()` to MusicModels.kt**

Add at the end of `MusicModels.kt` (after the `formatDuration` function):

```kotlin
fun Track.toPlayableTrack(): PlayableTrack = PlayableTrack(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMillis = durationSeconds.takeIf { it > 0 }?.times(1_000L),
    source = source,
    artworkBytes = artworkBytes,
)
```

Remove the same function from `App.kt`.

- [ ] **Step 2: Move `librarySnapshot()` and `libraryTrackAccent()` to MusicModels.kt**

`libraryTrackAccent()` (App.kt around line 1396):
```kotlin
private fun libraryTrackAccent(index: Int): TrackAccent {
    val hues = listOf(
        0xFF111018L to 0xFF776F66L,
        0xFF1A1422L to 0xFF794A4AL,
        0xFF14202AL to 0xFF4B6B7AL,
        0xFF1A1E1AL to 0xFF5C784CL,
        0xFF201A16L to 0xFF7A6448L,
        0xFF161A24L to 0xFF4B5C7AL,
        0xFF1A1420L to 0xFF6E4B7AL,
    )
    val (start, end) = hues[index % hues.size]
    return TrackAccent(start = start, end = end)
}
```

Move both to `MusicModels.kt`, making `libraryTrackAccent` public and renaming to `accentForIndex`:

```kotlin
fun accentForIndex(index: Int): TrackAccent {
    val hues = listOf(
        0xFF111018L to 0xFF776F66L,
        0xFF1A1422L to 0xFF794A4AL,
        0xFF14202AL to 0xFF4B6B7AL,
        0xFF1A1E1AL to 0xFF5C784CL,
        0xFF201A16L to 0xFF7A6448L,
        0xFF161A24L to 0xFF4B5C7AL,
        0xFF1A1420L to 0xFF6E4B7AL,
    )
    val (start, end) = hues[index % hues.size]
    return TrackAccent(start = start, end = end)
}

fun librarySnapshot(tracks: List<LibraryTrack>): LibrarySnapshot {
    val uiTracks = tracks.mapIndexed { index, track ->
        Track(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            durationSeconds = ((track.durationMillis ?: 0L) / 1_000L).toInt(),
            accent = accentForIndex(index),
            source = track.audioSource,
            trackNumber = track.trackNumber,
            discNumber = track.discNumber,
            artworkBytes = track.artworkBytes,
        )
    }
    val nowPlayingId: String? = null
    return LibrarySnapshot(
        title = "RhythHaus",
        subtitle = "",
        tracks = uiTracks,
        nowPlayingTrackId = nowPlayingId,
    )
}
```

Remove both functions from `App.kt`. Update the call in `App.kt` from `libraryTrackAccent(index)` to `accentForIndex(index)` — but wait, since we're removing the call site entirely (the function moves to MusicModels.kt), the call in App.kt stays as-is since `App.kt` uses these via the `librarySnapshot` function which is being moved.

Actually, `App.kt`'s `librarySnapshot` function uses `libraryTrackAccent` — since we're moving the whole `librarySnapshot` function, we just need to make sure `App.kt` calls the moved version. The `App.kt` caller of `librarySnapshot` will import it from `MusicModels.kt`.

- [ ] **Step 3: Make `LibraryTrack.toUiTrack()` internal in LibraryBrowser.kt**

In `LibraryBrowser.kt` line 83, change `private fun` to `internal fun`:

```kotlin
internal fun LibraryTrack.toUiTrack(): Track = Track(
```

- [ ] **Step 4: Build and verify**

```bash
./gradlew :shared:compileKotlinMetadata --configuration-cache
./gradlew :shared:jvmTest --configuration-cache
```

Must return BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/MusicModels.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryBrowser.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
git commit -m "refactor: extract shared helpers from App.kt to MusicModels.kt and LibraryBrowser.kt"
```

---

### Task 2: Remove Compose UI from shared module

**Files:**
- Delete: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Delete: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`
- Delete: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- Delete: `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/MainViewController.kt`
- Delete: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Greeting.kt`
- Delete: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/GreetingUtil.kt`

**Interfaces:**
- Consumes: N/A (these are the UI files being removed)
- Produces: N/A (cleaner shared module without Compose UI)

- [ ] **Step 1: Delete Compose UI files**

```bash
rm shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
rm shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt
rm shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt
rm shared/src/iosMain/kotlin/com/eterocell/rhythhaus/MainViewController.kt
```

- [ ] **Step 2: Remove unused files (demo/template code)**

```bash
rm shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Greeting.kt
rm shared/src/commonMain/kotlin/com/eterocell/rhythhaus/GreetingUtil.kt
```

- [ ] **Step 3: Remove Compose-specifically-used imports from commonMain, move to androidMain**

The `shared/build.gradle.kts` currently has these in `commonMain.dependencies`:
```kotlin
implementation(libs.miuix.ui)           // Compose UI framework — move to androidMain
implementation(libs.compose.runtime)     // Compose runtime — move to androidMain
implementation(libs.compose.foundation)  // Compose foundation — move to androidMain
implementation(libs.compose.ui)          // Compose UI — move to androidMain
implementation(libs.compose.components.resources)
implementation(libs.compose.uiToolingPreview)
implementation(libs.androidx.lifecycle.viewmodelCompose)
implementation(libs.androidx.lifecycle.runtimeCompose)
```

Move the Compose-UI-specific dependencies from `commonMain.dependencies` to `androidMain.dependencies`. Keep `kotlinx.coroutinesCore`, `sqldelight.runtime`, `sqldelight.coroutines`, `kermit` in commonMain.

Also remove the Compose plugins from shared/build.gradle.kts:
```kotlin
alias(libs.plugins.compose.multiplatform)  // Remove
alias(libs.plugins.compose.compiler)       // Remove
```

And remove the `compose.resources {}` block if present.

- [ ] **Step 4: Remove the commonMain `App.kt` imports from shared sources**

Search for any remaining references to deleted files in shared/commonMain and shared/androidMain. The Android `MainActivity.kt` references `App()` from the shared package — since we deleted `App.kt`, we need to either:
- Create a new `App.kt` in `androidMain` (Android-specific Compose UI)
- Or have androidApp reference a locally-defined composable

Wait — the plan should be: the Android app keeps its Compose UI but it's now defined in `androidApp/src/main/kotlin/com/eterocell/rhythhaus/AndroidApp.kt` instead of `shared/src/commonMain/.../App.kt`. The `MainActivity.kt` calls `App()`. We need to:

a) Create `androidApp/src/main/kotlin/com/eterocell/rhythhaus/AndroidApp.kt` with the full Android Compose UI (the current App.kt content)
b) Update `MainActivity.kt` to call `AndroidApp()` instead of `App()`
c) OR: keep App.kt in `shared/src/androidMain` (not commonMain) and have Android use it

Actually, the cleanest approach: move the current `App.kt` to `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/App.kt`. This keeps the Compose UI in the shared module but only on the Android source set. The `MainActivity.kt` already calls `App()` from the shared package.

But wait — `App.kt` also contains non-Compose helper functions (`DeveloperPanel`, `ImportAudioCard`, `Track.toPlayableTrack()` which we already moved, etc.). We need to carefully extract only the Compose composables.

Simpler approach: move the entire `App.kt` to `androidMain`. The shared module no longer has `App()` composable. Android `MainActivity.kt` calls `App()` which is now in `androidMain`. iOS no longer has `MainViewController.kt` which previously called `App()`.

The `App.kt` file also imports from `compose.ui`, `compose.foundation`, `compose.runtime`, `miuix.kmp.basic` — these are all Compose dependencies that we're moving to androidMain.

- [ ] **Step 5: Move `App.kt` to androidMain**

```bash
mv shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/androidMain/kotlin/com/eterocell/rhythhaus/App.kt
```

Wait — we deleted App.kt in Step 1. We need to NOT delete it, but move it.

Correction to Step 1: Instead of deleting App.kt, move it:
```bash
mv shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/androidMain/kotlin/com/eterocell/rhythhaus/App.kt
```

But `NowPlayingScreen.kt` and `NowPlayingBar.kt` are called from `App.kt` — they should also move to androidMain:
```bash
mv shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt shared/src/androidMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt
mv shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt shared/src/androidMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt
```

And `HausColors.kt` is used by both Compose UI (color constants) and can stay in commonMain — it has no Compose imports, just a Kotlin object with `Long` color values.

Also, `ArtworkDecoder.kt` in commonMain has `decodeArtwork()` which is a Compose-specific function (returns `ImageBitmap`). This needs to move to androidMain too, or we keep it common and just have the Android side use it.

Let me think about `ArtworkDecoder.kt`:
```kotlin
expect fun ByteArray.decodeArtwork(): androidx.compose.ui.graphics.ImageBitmap?
```

This has `expect` in commonMain with `androidx.compose.ui.graphics.ImageBitmap` — that's a Compose dependency. The iOS actual returns `null`. We should:
- Move `ArtworkDecoder.kt` to androidMain (it's Compose-specific)
- Delete `ArtworkDecoder.ios.kt` (iOS won't use it)
- Delete `ArtworkDecoder.jvm.kt` (macOS still in Compose, but desktop app uses App.kt which also moved... wait, desktop app also calls App(). So macOS needs ArtworkDecoder too if we keep Compose on desktop.)

Actually, since we're keeping the desktop app with Compose for now (Phase C later), and desktop app calls `App()` from shared, moving App.kt to androidMain breaks the desktop app.

Options:
A) Keep desktop app with a thin wrapper that also has App.kt in jvmMain
B) Move desktop to SwiftUI now (too much for this session)
C) Duplicate App.kt to both androidMain and jvmMain

Option A is cleanest: move App.kt + companions to androidMain, and create a mirror in jvmMain. But that's code duplication.

Actually, the pragmatic approach for this migration: create a new `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/` directory that stays in commonMain but only contains Compose UI code. Then keep Compose in commonMain for Android + desktop, and iOS SwiftUI bypasses it.

Wait, that defeats the purpose. The whole point is to remove Compose from shared so the iOS framework export doesn't include Compose.

Let me take a different, cleaner approach:

**Keep App.kt, NowPlayingScreen.kt, NowPlayingBar.kt in androidMain.** The desktop app should also have its own entry point. But desktop currently calls `App()` from shared.

The desktop app (`desktopApp/src/main/kotlin/com/eterocell/rhythhaus/main.kt`) is:
```kotlin
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "RhythHaus") {
        App()
    }
}
```

If `App()` moves to androidMain, desktop can't access it. We need either:
- A jvmMain copy of App.kt
- Or convert desktop to a jvmMain Compose entry point

For now, let's keep Compose on desktop but it uses its own App.kt in jvmMain. We copy the file (not great, but works for migration). Or better: move App.kt to a new location that both androidMain and jvmMain can access... no, KMP source sets don't work that way.

Simplest solution: put App.kt in jvmMain too (copy). In Phase C when macOS moves to SwiftUI, we delete the jvmMain copy. For now:

- `shared/src/androidMain/.../App.kt` — Android Compose UI
- `shared/src/jvmMain/.../App.kt` — macOS/desktop Compose UI (copied, to be removed in Phase C)
- `shared/src/androidMain/.../NowPlayingScreen.kt`
- `shared/src/androidMain/.../NowPlayingBar.kt`
- `shared/src/jvmMain/.../NowPlayingScreen.kt` (copied)
- `shared/src/jvmMain/.../NowPlayingBar.kt` (copied)

Wait, that's a lot of duplication. Let me step back.

Actually, the REAL goal is: "iOS framework should not include Compose." We can achieve this by keeping Compose in shared but only for the android + jvm targets, NOT for iOS. How?

Kotlin Multiplatform has a feature called "source set hierarchy" where we can create a shared source set between android + jvm but not iOS. Something like:

```kotlin
kotlin {
    sourceSets {
        val androidAndJvmMain by creating {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(androidAndJvmMain)
        jvmMain.get().dependsOn(androidAndJvmMain)
    }
}
```

But this is complex and not how "commonMain" works in KMP's default hierarchy.

Actually wait — KMP already supports this pattern. The Compose Multiplatform plugin adds `compose` to commonMain. If we remove the plugin from `shared/build.gradle.kts`, Compose is removed from ALL targets. But we still need it for Android and JVM.

The approach: remove Compose plugin from `shared`, add it to `androidApp/build.gradle.kts` and `desktopApp/build.gradle.kts`, and move ALL UI code into those modules.

This is actually the cleanest approach:

1. Remove Compose from `shared/build.gradle.kts`
2. Move `App.kt`, `NowPlayingScreen.kt`, `NowPlayingBar.kt`, `ArtworkDecoder.kt` to `androidApp/src/main/kotlin/`
3. Keep `App.kt` etc. available via Android app module
4. Desktop app also gets its own copy (or we create a separate shared-compose module)

But that's a big refactor. For this session, let me do this:

**Simplified approach — only move iOS-specific changes:**

1. Keep Compose in shared commonMain for android + jvm
2. Remove `MainViewController.kt` (iOS entry)
3. Build iOS SwiftUI app that consumes Shared.framework (which includes Compose symbols but iOS doesn't call them)
4. The iOS SwiftUI app just doesn't invoke any Compose code — it uses domain/data classes from Shared.framework

This is much simpler and avoids breaking Android + desktop. The iOS framework binary will include Compose symbols, but they won't be called — the SwiftUI layer only consumes `PlayableTrack`, `PlaybackState`, `LibrarySnapshot`, etc.

This is the minimal-change approach. Let me restructure the plan.

Actually, the user's motivation is:
1. Better iOS debugging
2. Better scroll performance

Both are achieved by building a SwiftUI app that renders natively. We don't need to REMOVE Compose from shared — we just need to not USE it on iOS. The shared framework can still export everything; iOS SwiftUI picks what it needs.

Let me simplify the plan significantly.<｜end▁of▁thinking｜>Let me reassess — the minimal practical approach: keep Compose in shared for Android/desktop, just build iOS SwiftUI that consumes `Shared.framework` without calling Compose code. This avoids breaking Android + desktop.

<｜｜DSML｜｜tool_calls>
<｜｜DSML｜｜invoke name="write_file">
<｜｜DSML｜｜parameter name="content" string="true"># iOS Native SwiftUI App — Migration Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a native iOS SwiftUI app consuming `Shared.framework` for domain/data, replacing the Compose-based `MainViewController` host. Android and macOS desktop keep their Compose UI unchanged.

**Architecture:** Shared KMP module unchanged — keeps Compose, domain, data, SQLDelight, TagLib. The `Shared.framework` exports everything. iOS SwiftUI app uses only the domain/data classes (`PlayableTrack`, `PlaybackState`, `Track`, `AlbumGroup`, `LibraryTrack`, `LibrarySnapshot`, etc.) and builds its own native `AVAudioPlayer` engine + SwiftUI views. The existing `MainViewController.kt` and `ContentView.swift` wrapper are replaced.

**Tech Stack:** Shared KMP (unchanged), iOS SwiftUI (new), Android Compose (unchanged), macOS Compose/JVM (unchanged)

## Global Constraints

- Shared module must remain unchanged (no Compose removal, no dep changes)
- Android must still build and pass tests
- Shared JVM tests must pass
- iOS simulator tests must pass
- Semantic conventional commits
- No unrelated refactors
- `:taglib` module unchanged

---

### Task 1: Move shared helpers out of App.kt

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/MusicModels.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryBrowser.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`

**Why:** The iOS SwiftUI app needs to create `PlayableTrack` from `Track` and compute `LibrarySnapshot` from `LibraryTrack` — these helpers are currently private in `App.kt`. Move them to shared locations.

- [ ] **Step 1: Add `Track.toPlayableTrack()` to MusicModels.kt**

At the end of `MusicModels.kt`, add:

```kotlin
fun Track.toPlayableTrack(): PlayableTrack = PlayableTrack(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMillis = durationSeconds.takeIf { it > 0 }?.times(1_000L),
    source = source,
    artworkBytes = artworkBytes,
)
```

Remove the identical `private fun Track.toPlayableTrack()` from `App.kt` (~line 1368).

- [ ] **Step 2: Add `librarySnapshot()` and `accentForIndex()` to MusicModels.kt**

Add after `formatDuration` in `MusicModels.kt`:

```kotlin
fun accentForIndex(index: Int): TrackAccent {
    val hues = listOf(
        0xFF111018L to 0xFF776F66L,
        0xFF1A1422L to 0xFF794A4AL,
        0xFF14202AL to 0xFF4B6B7AL,
        0xFF1A1E1AL to 0xFF5C784CL,
        0xFF201A16L to 0xFF7A6448L,
        0xFF161A24L to 0xFF4B5C7AL,
        0xFF1A1420L to 0xFF6E4B7AL,
    )
    val (start, end) = hues[index % hues.size]
    return TrackAccent(start = start, end = end)
}

fun librarySnapshot(tracks: List<LibraryTrack>): LibrarySnapshot {
    val uiTracks = tracks.mapIndexed { index, track ->
        Track(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            durationSeconds = ((track.durationMillis ?: 0L) / 1_000L).toInt(),
            accent = accentForIndex(index),
            source = track.audioSource,
            trackNumber = track.trackNumber,
            discNumber = track.discNumber,
            artworkBytes = track.artworkBytes,
        )
    }
    return LibrarySnapshot(
        title = "RhythHaus",
        subtitle = "",
        tracks = uiTracks,
        nowPlayingTrackId = null,
    )
}
```

Remove `private fun librarySnapshot()` and `private fun libraryTrackAccent()` from `App.kt`. Update `App.kt`'s call site to use the now-public `librarySnapshot()` (it's in the same package, no import change needed).

- [ ] **Step 3: Make `LibraryTrack.toUiTrack()` internal in LibraryBrowser.kt**

In `LibraryBrowser.kt` line 83, change `private fun LibraryTrack.toUiTrack()` to `internal fun LibraryTrack.toUiTrack()`:

```kotlin
internal fun LibraryTrack.toUiTrack(): Track = Track(
```

- [ ] **Step 4: Add `formatMillis` to Playback.kt (already exists, just note it's public)**

The `formatMillis` function is already public in `Playback.kt`. The `formatDuration` function is already public in `MusicModels.kt`. Both are usable from SwiftUI via the framework export.

- [ ] **Step 5: Build and verify**

```bash
./gradlew :shared:compileKotlinMetadata --configuration-cache
./gradlew :shared:jvmTest --configuration-cache
```

Must return BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/MusicModels.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryBrowser.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
git commit -m "refactor: extract shared helpers from App.kt for iOS SwiftUI consumption"
```

---

### Task 2: Build iOS SwiftUI AudioEngine

**Files:**
- Create: `iosApp/iosApp/AudioEngine.swift`

**Interfaces:**
- Consumes: `Shared.PlayableTrack` (from Shared.framework), `AVAudioPlayer`, `MPNowPlayingInfoCenter`, `MPRemoteCommandCenter`
- Produces: `AudioEngine` class with `load(track:)`, `play()`, `pause()`, `stop()`, `seek(to:)`, `release()`, `@Published var state: AudioEngineState`

- [ ] **Step 1: Create `AudioEngineState` model and `AudioEngine` class**

Create `iosApp/iosApp/AudioEngine.swift`:

```swift
import AVFoundation
import MediaPlayer
import Shared

struct AudioEngineState {
    var status: String = "idle"  // "idle", "loading", "playing", "paused", "stopped"
    var positionSeconds: Double = 0
    var durationSeconds: Double = 0
    var currentTrack: PlayableTrack? = nil
}

class AudioEngine: ObservableObject {
    @Published var state = AudioEngineState()
    private var player: AVAudioPlayer?
    private var timer: Timer?
    private var remoteCommandsRegistered = false
    var onComplete: (() -> Void)?
    var onSkipNext: (() -> Void)?
    var onSkipPrevious: (() -> Void)?

    func load(track: PlayableTrack) {
        release()
        state.status = "loading"

        guard let url = urlForSource(source: track.source) else {
            state.status = "error"
            return
        }

        do {
            try configureAudioSession()
            let audioPlayer = try AVAudioPlayer(contentsOf: url)
            guard audioPlayer.prepareToPlay() else {
                state.status = "error"
                return
            }
            player = audioPlayer
            state.currentTrack = track
            state.durationSeconds = track.durationMillis?.doubleValue ?? audioPlayer.duration
            if state.durationSeconds <= 0, audioPlayer.duration > 0 {
                state.durationSeconds = audioPlayer.duration
            }
            state.positionSeconds = 0
            updateNowPlaying(position: 0, rate: 0)
            state.status = "paused"
        } catch {
            state.status = "error"
            print("[AudioEngine] load error: \(error)")
        }
    }

    func play() {
        guard let player else { return }
        player.play()
        state.status = "playing"
        MPNowPlayingInfoCenter.default().playbackState = .playing
        updateNowPlaying(position: player.currentTime, rate: 1)
        startTimer()
    }

    func pause() {
        timer?.invalidate()
        player?.pause()
        state.status = "paused"
        MPNowPlayingInfoCenter.default().playbackState = .paused
        if let p = player {
            updateNowPlaying(position: p.currentTime, rate: 0)
        }
    }

    func stop() {
        timer?.invalidate()
        player?.stop()
        player?.currentTime = 0
        state.positionSeconds = 0
        state.status = "stopped"
        MPNowPlayingInfoCenter.default().playbackState = .stopped
        updateNowPlaying(position: 0, rate: 0)
    }

    func seek(to seconds: Double) {
        player?.currentTime = seconds
        if let p = player {
            state.positionSeconds = p.currentTime
            updateNowPlaying(position: p.currentTime, rate: p.isPlaying ? 1 : 0)
        }
    }

    func release() {
        timer?.invalidate()
        player?.stop()
        player = nil
        state.currentTrack = nil
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
        MPNowPlayingInfoCenter.default().playbackState = .stopped
    }

    // MARK: - Private

    private func configureAudioSession() throws {
        let session = AVAudioSession.sharedInstance()
        try session.setCategory(.playback)
        try session.setActive(true)
        if !remoteCommandsRegistered {
            remoteCommandsRegistered = true
            registerRemoteCommands()
        }
    }

    private func registerRemoteCommands() {
        let center = MPRemoteCommandCenter.shared()
        center.playCommand.isEnabled = true
        center.pauseCommand.isEnabled = true
        center.togglePlayPauseCommand.isEnabled = true
        center.stopCommand.isEnabled = true
        center.changePlaybackPositionCommand.isEnabled = true
        center.previousTrackCommand.isEnabled = true
        center.nextTrackCommand.isEnabled = true

        center.playCommand.addTarget { [weak self] _ in
            self?.play()
            return .success
        }
        center.pauseCommand.addTarget { [weak self] _ in
            self?.pause()
            return .success
        }
        center.togglePlayPauseCommand.addTarget { [weak self] _ in
            guard let self, let p = self.player else { return .noSuchContent }
            if p.isPlaying { self.pause() } else { self.play() }
            return .success
        }
        center.stopCommand.addTarget { [weak self] _ in
            self?.stop()
            return .success
        }
        center.changePlaybackPositionCommand.addTarget { [weak self] event in
            guard let positionEvent = event as? MPChangePlaybackPositionCommandEvent else { return .noSuchContent }
            self?.seek(to: positionEvent.positionTime)
            return .success
        }
        center.previousTrackCommand.addTarget { [weak self] _ in
            self?.onSkipPrevious?()
            return .success
        }
        center.nextTrackCommand.addTarget { [weak self] _ in
            self?.onSkipNext?()
            return .success
        }
    }

    private func updateNowPlaying(position: Double, rate: Double) {
        guard let track = state.currentTrack else { return }
        var info: [String: Any] = [
            MPMediaItemPropertyTitle: track.title,
            MPMediaItemPropertyArtist: track.artist,
            MPNowPlayingInfoPropertyElapsedPlaybackTime: max(0, position),
            MPNowPlayingInfoPropertyPlaybackRate: rate,
        ]
        if let album = track.album, !album.isEmpty {
            info[MPMediaItemPropertyAlbumTitle] = album
        }
        if state.durationSeconds > 0 {
            info[MPMediaItemPropertyPlaybackDuration] = state.durationSeconds
        }
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }

    private func startTimer() {
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 0.25, repeats: true) { [weak self] _ in
            guard let self, let p = self.player else { return }
            let pos = p.currentTime
            self.state.positionSeconds = pos
            if p.isPlaying, self.state.durationSeconds > 0, pos >= self.state.durationSeconds {
                self.onComplete?()
            }
        }
    }

    private func urlForSource(source: AudioSource) -> URL? {
        if let fileSource = source as? AudioSource.FilePath {
            let path = fileSource.path
            if path.hasPrefix("/") {
                return URL(fileURLWithPath: path)
            } else {
                // Resolve relative path via app documents directory
                let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
                return docs.appendingPathComponent(path)
            }
        }
        if let uriSource = source as? AudioSource.Uri {
            return URL(string: uriSource.value)
        }
        return nil
    }
}
```

- [ ] **Step 2: Add the AudioEngine.swift file to Xcode project**

The file must be added to the `iosApp.xcodeproj`. This can be done by:
1. Opening Xcode and dragging the file into the `iosApp` group, OR
2. Using a script to add it to `project.pbxproj`

For now, create the file and note in the report that it needs to be added to the Xcode project. The Xcode project modification is a separate step.

Actually, let's use `ruby` or a CLI tool to add the file. Or we can use `xcodebuild` with a scheme. Let's defer Xcode project modification to Task 4 (iOS app wiring) and just create the Swift file for now.

- [ ] **Step 3: Commit**

```bash
git add iosApp/iosApp/AudioEngine.swift
git commit -m "feat: add iOS native AudioEngine with system media controls"
```

---

### Task 3: Build iOS SwiftUI App Entry Point

**Files:**
- Modify: `iosApp/iosApp/iOSApp.swift`
- Modify: `iosApp/iosApp/ContentView.swift`

**Interfaces:**
- Consumes: `Shared.*` framework types, `AudioEngine`
- Produces: SwiftUI `App` with `@StateObject` engine + navigation

- [ ] **Step 1: Rewrite `iOSApp.swift` as the app entry with Shared framework init**

Replace `iosApp/iosApp/iOSApp.swift`:

```swift
import SwiftUI
import Shared

@main
struct iOSApp: App {
    @StateObject private var engine = AudioEngine()
    @StateObject private var libraryStore = LibraryStore()

    init() {
        // Initialize the Shared framework database
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        try? FileManager.default.createDirectory(at: docs, withIntermediateDirectories: true)
        let marker = docs.appendingPathComponent("Put Music Files Here.txt")
        if !FileManager.default.fileExists(atPath: marker.path) {
            try? "Drop your music files (.mp3, .flac, .wav, .m4a) here.\n"
                .write(to: marker, atomically: true, encoding: .utf8)
        }
    }

    var body: some Scene {
        WindowGroup {
            LibraryView(engine: engine, libraryStore: libraryStore)
        }
    }
}
```

- [ ] **Step 2: Rewrite `ContentView.swift` as `LibraryView.swift`**

Rename `ContentView.swift` to `LibraryView.swift` and replace content with the main library screen (Task 4 will build this). For now, a minimal placeholder:

Create `iosApp/iosApp/LibraryView.swift`:

```swift
import SwiftUI
import Shared

class LibraryStore: ObservableObject {
    @Published var tracks: [LibraryTrack] = []
    @Published var snapshot = LibrarySnapshot(
        title: "RhythHaus",
        subtitle: "",
        tracks: [],
        nowPlayingTrackId: nil
    )

    private var database: RhythHausDatabase?
    private var repository: SqlDelightLibraryRepository?

    init() {
        do {
            database = try createLibraryDatabase()
            if let db = database {
                repository = SqlDelightLibraryRepository(database: db)
                refresh()
            }
        } catch {
            print("[LibraryStore] init error: \(error)")
        }
    }

    func refresh() {
        guard let repo = repository else { return }
        tracks = repo.tracks()
        snapshot = LibrarySnapshotKt.librarySnapshot(tracks: tracks)
    }
}

struct LibraryView: View {
    @ObservedObject var engine: AudioEngine
    @ObservedObject var libraryStore: LibraryStore

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    // Header
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("RhythHaus")
                                .font(.largeTitle.weight(.black))
                            Text("\(libraryStore.tracks.count) tracks")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                    }
                    .padding(.horizontal)

                    // TODO: Album/Artist grid — Task 5

                    // Track list
                    ForEach(libraryStore.snapshot.tracks, id: \.id) { track in
                        TrackRow(track: track, engine: engine)
                    }
                }
            }
            .navigationBarHidden(true)
        }
    }
}

struct TrackRow: View {
    let track: Track
    @ObservedObject var engine: AudioEngine

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(track.title)
                    .font(.body.weight(.semibold))
                    .lineLimit(1)
                Text("\(track.artist) · \(track.album)")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }
            Spacer()
            Button(action: { playTrack(track) }) {
                Image(systemName: "play.fill")
                    .font(.title3)
            }
        }
        .padding(.horizontal)
        .padding(.vertical, 6)
    }

    private func playTrack(_ track: Track) {
        engine.load(track: TrackKt.toPlayableTrack(track))
        engine.play()
    }
}
```

Note: `TrackKt.toPlayableTrack` is how Kotlin extension functions are exposed to Swift via the framework. If this doesn't compile, the alternative is `PlayableTrack(id:track.id, title:track.title, artist:track.artist, ...)` — construct it directly.

- [ ] **Step 3: Delete the old ContentView.swift**

```bash
rm iosApp/iosApp/ContentView.swift
```

- [ ] **Step 4: Remove the old MainViewController.kt (no longer needed)**

```bash
rm shared/src/iosMain/kotlin/com/eterocell/rhythhaus/MainViewController.kt
```

- [ ] **Step 5: Commit**

```bash
git add iosApp/iosApp/iOSApp.swift iosApp/iosApp/LibraryView.swift iosApp/iosApp/AudioEngine.swift shared/src/iosMain/kotlin/com/eterocell/rhythhaus/MainViewController.kt
git rm iosApp/iosApp/ContentView.swift
git commit -m "feat: replace iOS Compose host with native SwiftUI app entry"
```

---

### Task 4: Add Swift files to Xcode project + verify iOS build

**Files:**
- Modify: `iosApp/iosApp.xcodeproj/project.pbxproj`

**Interfaces:**
- Consumes: Xcode project structure
- Produces: Modified pbxproj with new Swift files referenced

- [ ] **Step 1: Add new Swift files to the Xcode project**

The project.pbxproj needs new entries for:
- `iosApp/iosApp/AudioEngine.swift`
- `iosApp/iosApp/LibraryView.swift`
- Remove `iosApp/iosApp/ContentView.swift`

This requires generating new UUIDs and adding file references, build file entries, and group entries. Since pbxproj editing is fragile, use a Python script or manual approach.

Simpler approach: use `xcodebuild` with a `.xcscheme` that auto-discovers source files, OR just add the files manually in Xcode.

For the subagent-driven workflow: the subagent should add these files to the Xcode project by editing the pbxproj directly or using `plutil`/`PlistBuddy`. The pbxproj format is well-understood.

- [ ] **Step 2: Build the iOS framework**

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 --configuration-cache
```

Must succeed — the `Shared.framework` must be built before Xcode can compile.

- [ ] **Step 3: Try Xcode build**

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 16' build 2>&1 | tail -20
```

If this fails, fix compile errors and retry.

- [ ] **Step 4: Commit**

```bash
git add iosApp/iosApp.xcodeproj/project.pbxproj
git commit -m "chore: add SwiftUI source files to Xcode project"
```

---

### Task 5: Build iOS SwiftUI NowPlayingView + Album grid

**Files:**
- Create: `iosApp/iosApp/NowPlayingView.swift`
- Modify: `iosApp/iosApp/LibraryView.swift`

- [ ] **Step 1: Create NowPlayingView.swift**

A full-screen now playing view with album artwork area, track info, seek bar, and transport controls (prev, play/pause, next).

- [ ] **Step 2: Add album grid to LibraryView**

Display `AlbumGroup` items in a 2-column grid with artwork tiles.

- [ ] **Step 3: Add artist list to LibraryView**

Toggle between Album view and Artist view.

- [ ] **Step 4: Add album/artist drill-down navigation**

When tapping an album or artist, navigate to a detail view showing tracks.

- [ ] **Step 5: Wire now playing bar at bottom**

Show a mini now-playing bar at the bottom when a track is playing.

- [ ] **Step 6: Build and verify in Xcode**

- [ ] **Step 7: Commit**

---

### Task 6: Full verification

- [ ] **Step 1: Android still builds**

```bash
./gradlew :androidApp:assembleDebug --configuration-cache
```

- [ ] **Step 2: Shared tests pass**

```bash
./gradlew :shared:jvmTest :shared:iosSimulatorArm64Test --configuration-cache
```

- [ ] **Step 3: Full harness**

```bash
./init.sh
```

- [ ] **Step 4: Commit any remaining changes**
