# Move Dev Panel to NowPlayingScreen + Replace Stop with Previous Track

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the TagLib developer metadata panel from the main library screen into the NowPlaying detail screen (showing only the current track), and replace the stop ■ button in the transport controls with a previous-track ⏮ button.

**Architecture:** The DeveloperPanel currently lives in `LibraryHomeScreen` and iterates all `libraryTracks`. It will be removed from `LibraryHomeScreen` and placed inside `NowPlayingScreen`, rendering only the current track via a new `DeveloperTrackPanel` composable. The stop button in `NowPlayingScreen`'s transport row will be replaced by a previous-track button that navigates backwards in the queue.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform

## Global Constraints

- Shared-first KMP: all changes in `shared/src/commonMain/`
- No unrelated refactors, no dependency changes
- Semantic conventional commits: `fix:` or `feat:`
- Verification: `./gradlew :shared:compileKotlinMetadata --configuration-cache`
- Do not remove the existing `DeveloperPanel` composable — it will still be used elsewhere (or kept for reference); the call site in `LibraryHomeScreen` is what changes
- The `NowPlayingScreen` currently receives `track: Track, playbackState: PlaybackState, playbackController: PlaybackController, onBack: () -> Unit` — add `tagLibReader: TagLibReader` and `currentLibraryTrack: LibraryTrack?` parameters

---

### Task 1: Move DeveloperPanel to NowPlayingScreen + Replace Stop with Previous Track

Both changes touch the same files (`NowPlayingScreen.kt` and `App.kt`), so they are combined into one task.

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`

**Interfaces:**
- Consumes: `TagLibReader`, `LibraryTrack`, `PlaybackController`, `PlaybackState`, `Track`, `DeveloperMetadataRow` (existing composable in App.kt)
- Produces: N/A (modified NowPlayingScreen signature with expanded parameters)

#### Part A: Remove DeveloperPanel from LibraryHomeScreen

- [ ] **Step 1: Remove DeveloperPanel call from LibraryHomeScreen**

In `App.kt`, `LibraryHomeScreen` (around line 303-310), remove the `DeveloperPanel(...)` item block:

```kotlin
                        item {
                            DeveloperPanel(
                                libraryTracks = libraryTracks,
                                tagLibReader = tagLibReader,
                                expanded = devPanelExpanded,
                                onToggle = { devPanelExpanded = !devPanelExpanded },
                            )
                        }
```

- [ ] **Step 2: Remove unused devPanelExpanded state**

In `LibraryHomeScreen` (around line 206):
```kotlin
    var devPanelExpanded by remember { mutableStateOf(false) }
```
Remove this line. It is no longer needed.

#### Part B: Add current-track-only DeveloperPanel to NowPlayingScreen

- [ ] **Step 3: Update NowPlayingScreen signature**

In `NowPlayingScreen.kt`, add two new parameters. Change the function signature from:

```kotlin
@Composable
fun NowPlayingScreen(
    track: Track,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
)
```

to:

```kotlin
@Composable
fun NowPlayingScreen(
    track: Track,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    tagLibReader: TagLibReader,
    currentLibraryTrack: LibraryTrack?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
)
```

Add imports at top of NowPlayingScreen.kt:
```kotlin
import com.eterocell.rhythhaus.library.LibraryTrack
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import com.eterocell.rhythhaus.library.SupportedAudio
```

- [ ] **Step 4: Add DeveloperTrackPanel to NowPlayingScreen body**

In `NowPlayingScreen.kt`, after the transport controls section (after line 236), add a collapsible developer panel for the current track. Insert between the transport controls `Row` closing `}` and the `Spacer(Modifier.height(16.dp))`:

```kotlin
            // Developer: TagLib metadata for current track
            if (currentLibraryTrack != null) {
                Spacer(Modifier.height(18.dp))
                DeveloperTrackPanel(
                    track = currentLibraryTrack,
                    tagLibReader = tagLibReader,
                )
            }
```

- [ ] **Step 5: Add DeveloperTrackPanel composable**

Add a new composable at the bottom of `NowPlayingScreen.kt` (before the `statusLabel` function):

```kotlin
@Composable
private fun DeveloperTrackPanel(
    track: LibraryTrack,
    tagLibReader: TagLibReader,
) {
    var expanded by remember { mutableStateOf(false) }
    val devBgColor = HausInk.copy(alpha = 0.06f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(devBgColor)
            .clickable { expanded = !expanded }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "DEV · TagLib",
                    color = HausInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.6.sp,
                )
                Text(
                    text = track.displayName,
                    color = HausMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = if (expanded) "▲" else "▼",
                color = HausPulse,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val rawResult = remember(track.audioSource.stableKey) {
                    when (track.audioSource) {
                        is AudioSource.FilePath -> tagLibReader.readPath(resolvePathForMetadata(track.audioSource.path))
                        is AudioSource.Uri -> null
                    }
                }
                val properties = remember(track.audioSource.stableKey) {
                    when (track.audioSource) {
                        is AudioSource.FilePath -> tagLibReader.readProperties(resolvePathForMetadata(track.audioSource.path))
                        is AudioSource.Uri -> emptyMap()
                    }
                }

                when {
                    rawResult == null -> Text(
                        text = "URI source — TagLib requires a filesystem path",
                        color = HausPulse,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    rawResult is TagReadResult.Found -> {
                        rawTagLines((rawResult as TagReadResult.Found).metadata).forEach { (label, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = label,
                                    color = HausMuted,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = value,
                                    color = HausInk,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 12.dp).weight(1f, fill = false),
                                )
                            }
                        }
                        if (properties.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "ALL PROPERTIES (${properties.size})",
                                color = HausInk,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                            )
                            properties.entries.sortedBy { it.key }.forEach { (key, value) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = key,
                                        color = HausMuted,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = value,
                                        color = HausInk,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(start = 8.dp).weight(1f, fill = false),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```

Required additional imports for NowPlayingScreen.kt:
```kotlin
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.SupportedAudio
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import com.eterocell.rhythhaus.library.appLocalMusicFolderPath
```

#### Part C: Replace stop button with previous track

- [ ] **Step 6: Replace stop button with previous track in NowPlayingScreen**

In `NowPlayingScreen.kt`, in the transport controls `Row` (around lines 190-200), replace the stop button:

```kotlin
                // Stop
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(HausPanel)
                        .clickable(onClick = playbackController::stop),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "■", color = HausInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
```

Replace with a previous track button:

```kotlin
                // Previous track
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(HausPanel)
                        .clickable {
                            val queue = playbackState.queue
                            val currentId = playbackState.currentTrack?.id
                            val currentIndex = queue.indexOfFirst { it.id == currentId }
                            val prevTrack = queue.getOrNull(currentIndex - 1) ?: queue.lastOrNull()
                            prevTrack?.let { playbackController.selectTrack(it.id, autoPlay = true) }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "⏮", color = HausInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
```

#### Part D: Update callers

- [ ] **Step 7: Update LibraryHomeScreen call to NowPlayingScreen**

In `App.kt`, `LibraryHomeScreen` (around lines 257-263), update the `NowPlayingScreen` call to pass `tagLibReader` and `currentLibraryTrack`:

Before:
```kotlin
        if (showNowPlayingScreen && selectedTrack != null) {
            NowPlayingScreen(
                track = selectedTrack,
                playbackState = playbackState,
                playbackController = playbackController,
                onBack = { showNowPlayingScreen = false },
            )
        }
```

After:
```kotlin
        if (showNowPlayingScreen && selectedTrack != null) {
            val currentLibTrack = libraryTracks.firstOrNull { it.id == selectedTrack.id }
            NowPlayingScreen(
                track = selectedTrack,
                playbackState = playbackState,
                playbackController = playbackController,
                tagLibReader = tagLibReader,
                currentLibraryTrack = currentLibTrack,
                onBack = { showNowPlayingScreen = false },
            )
        }
```

- [ ] **Step 8: Update DrillDownView call to NowPlayingScreen**

In `App.kt`, `DrillDownView` (around lines 1080-1086), update similarly. Since `DrillDownView` doesn't receive `libraryTracks` or `tagLibReader`, we need to add them as parameters, or pass `currentLibraryTrack = null`.

Simplest fix: pass `null` for `currentLibraryTrack` and `tagLibReader` in DrillDownView. Update:

```kotlin
    if (showNowPlayingScreen && currentTrack != null) {
        NowPlayingScreen(
            track = currentTrack,
            playbackState = playbackState,
            playbackController = playbackController,
            tagLibReader = createTagLibReader(),  // or accept it as a parameter
            currentLibraryTrack = null,
            onBack = { showNowPlayingScreen = false },
        )
    }
```

Wait — `DrillDownView` doesn't have `tagLibReader`. Let me add `tagLibReader: TagLibReader` as a parameter to `DrillDownView`, and update its caller in `LibraryHomeScreen`.

In `DrillDownView` signature, add:
```kotlin
    tagLibReader: TagLibReader,
```

And in the two callers inside `LibraryHomeScreen` (album drill-down around line 217-233, artist drill-down around line 239-255), pass `tagLibReader`:

Album drill-down:
```kotlin
        DrillDownView(
            title = album.album,
            subtitle = "${albumTracks.size} tracks · ${album.artist ?: "Unknown artist"}",
            tracks = albumTracks,
            selectedTrack = selectedAlbumTrack,
            playbackState = playbackState,
            playbackController = playbackController,
            tagLibReader = tagLibReader,
            ...
```

Artist drill-down:
```kotlin
        DrillDownView(
            title = artist.artist,
            subtitle = "${artist.albumCount} albums · ${artistTracks.size} tracks",
            tracks = artistTracks,
            selectedTrack = selectedArtistTrack,
            playbackState = playbackState,
            playbackController = playbackController,
            tagLibReader = tagLibReader,
            ...
```

Then in `DrillDownView`'s `NowPlayingScreen` call, pass:
```kotlin
        NowPlayingScreen(
            track = currentTrack,
            playbackState = playbackState,
            playbackController = playbackController,
            tagLibReader = tagLibReader,
            currentLibraryTrack = null,
            onBack = { showNowPlayingScreen = false },
        )
```

- [ ] **Step 9: Build and verify**

Run: `./gradlew :shared:compileKotlinMetadata --configuration-cache`
Expected: BUILD SUCCESSFUL

- [ ] **Step 10: Full verification**

Run: `./init.sh`
Expected: BUILD SUCCESSFUL

- [ ] **Step 11: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
git commit -m "fix: move dev panel to now playing screen, replace stop with prev track"
```
