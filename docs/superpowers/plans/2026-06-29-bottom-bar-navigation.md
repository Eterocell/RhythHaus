# Bottom Bar Navigation, Settings, Search — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Settings and Search screens, move ImportAudioCard to Settings, add navigation buttons to the bottom bar with safe area padding, and simplify the start page.

**Architecture:** Two new full-screen composables (SettingsScreen, SearchScreen) controlled by boolean state flags in App(). The NowPlayingBar gains two icon buttons that toggle these screens. ImportAudioCard moves out of the start page LazyColumn into SettingsScreen.

**Tech Stack:** Compose Multiplatform (commonMain), Kotlin

## Global Constraints

- All UI in `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/`
- Follow existing patterns: `HausPanel`, `HausInk`, `HausMuted`, `HausPaper`, `HausPulse` color palette
- Use `modifier` parameter pattern for composables
- Safe area: use `Modifier.safeContentPadding()` or platform-specific insets from existing infra
- Search: case-insensitive contains match, no external search index

---

## File Structure

| File | Action | Purpose |
|------|--------|---------|
| `App.kt` | Modify | Remove ImportAudioCard from LazyColumn; add `showSettings`/`showSearch` state; wire new screens |
| `NowPlayingBar.kt` | Modify | Add Search + Settings icon buttons; add bottom safe area padding |
| `SettingsScreen.kt` | **Create** | Settings composable with ImportAudioCard, Clear Library |
| `SearchScreen.kt` | **Create** | Search field + filtered track list |

---

### Task 1: Update App.kt — remove ImportAudioCard, add navigation state

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`

**Interfaces:**
- Consumes: existing `App()`, `LibraryHomeScreen()`, `ImportAudioCard()`, `folderPickerLauncher`, `onClearLibrary`
- Produces: `showSettings: Boolean`, `showSearch: Boolean`, `onShowSettings`, `onShowSearch` — passed into `LibraryHomeScreen` and `NowPlayingBar`

- [ ] **Step 1: Add `showSettings` and `showSearch` state to `App()`**

In `App()` (after line `var showClearDialog`), add:

```kotlin
var showSettings by remember { mutableStateOf(false) }
var showSearch by remember { mutableStateOf(false) }
```

- [ ] **Step 2: Pass new state and callbacks to `LibraryHomeScreen`**

Update the `LibraryHomeScreen(...)` call in `App()`:

```kotlin
LibraryHomeScreen(
    snapshot = snapshot,
    libraryTracks = libraryTracks,
    tagLibReader = tagLibReader,
    playbackController = controller,
    folderPickerLauncher = folderPickerLauncher,
    importMessage = importMessage,
    scanProgress = scanProgress,
    scanJob = scanJob,
    showClearDialog = showClearDialog,
    onShowClearDialog = { showClearDialog = it },
    onClearLibrary = {
        repository.clearAll()
        libraryTracks = emptyList()
        showClearDialog = false
    },
    showSettings = showSettings,       // NEW
    onShowSettings = { showSettings = it }, // NEW
    showSearch = showSearch,           // NEW
    onShowSearch = { showSearch = it }, // NEW
)
```

- [ ] **Step 3: Add `showSettings`, `showSearch` params to `LibraryHomeScreen` signature**

Update the function signature (around line 190):

```kotlin
@Composable
fun LibraryHomeScreen(
    snapshot: LibrarySnapshot,
    libraryTracks: List<LibraryTrack>,
    tagLibReader: TagLibReader,
    playbackController: PlaybackController,
    folderPickerLauncher: PlatformFolderPickerLauncher,
    importMessage: String?,
    scanProgress: ScanProgress?,
    scanJob: Job?,
    showClearDialog: Boolean,
    onShowClearDialog: (Boolean) -> Unit,
    onClearLibrary: () -> Unit,
    showSettings: Boolean,              // NEW
    onShowSettings: (Boolean) -> Unit,  // NEW
    showSearch: Boolean,                // NEW
    onShowSearch: (Boolean) -> Unit,    // NEW
    modifier: Modifier = Modifier,
) {
```

- [ ] **Step 4: Remove `ImportAudioCard` from the LazyColumn**

Delete the `item { ImportAudioCard(...) }` block (currently lines 300-307):

```kotlin
// DELETE this block:
if (scanProgress?.isActive != true) {
    item {
        ImportAudioCard(
            folderPickerLauncher = folderPickerLauncher,
            importMessage = importMessage,
            hasImportedTracks = snapshot.tracks.isNotEmpty(),
            onClearLibrary = { onShowClearDialog(true) },
        )
    }
}
```

- [ ] **Step 5: Add Settings/Search to `NowPlayingBar` call in `LibraryHomeScreen`**

Update the `NowPlayingBar(...)` call:

```kotlin
if (selectedTrack != null) {
    NowPlayingBar(
        track = selectedTrack,
        playbackState = playbackState,
        onPlayPause = { ... },
        onExpand = { showNowPlayingScreen = true },
        onSettings = { onShowSettings(true) },   // NEW
        onSearch = { onShowSearch(true) },        // NEW
        modifier = Modifier.align(Alignment.BottomCenter),
    )
}
```

- [ ] **Step 6: Add Settings and Search screen rendering after the `if (showClearDialog)` block**

After the clear dialog block (around line 425), add:

```kotlin
if (showSettings) {
    SettingsScreen(
        folderPickerLauncher = folderPickerLauncher,
        importMessage = importMessage,
        scanProgress = scanProgress,
        scanJob = scanJob,
        hasImportedTracks = snapshot.tracks.isNotEmpty(),
        onClearLibrary = { onShowClearDialog(true) },
        onDismiss = { onShowSettings(false) },
    )
}

if (showSearch) {
    SearchScreen(
        libraryTracks = libraryTracks,
        tagLibReader = tagLibReader,
        playbackController = playbackController,
        playbackState = playbackState,
        onDismiss = { onShowSearch(false) },
    )
}
```

Add imports at the top (SettingsScreen and SearchScreen are in the same package):

```kotlin
// No import needed — same package
```

- [ ] **Step 7: Run build to verify no compilation errors**

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache 2>&1 | grep -E 'BUILD|error:'
```

Expected: `BUILD SUCCESSFUL` (the new composable references will fail until Tasks 2-3 create them — that's expected at this stage)

- [ ] **Step 8: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
git commit -m "refactor: remove ImportAudioCard from start page, add Settings/Search state wiring"
```

---

### Task 2: Create SettingsScreen.kt

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SettingsScreen.kt`

**Interfaces:**
- Consumes: `PlatformFolderPickerLauncher`, `ScanProgress`, `Job`, `Boolean`, callback lambdas
- Produces: `SettingsScreen()` composable

- [ ] **Step 1: Create `SettingsScreen.kt`**

```kotlin
package com.eterocell.rhythhaus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job

@Composable
fun SettingsScreen(
    folderPickerLauncher: PlatformFolderPickerLauncher,
    importMessage: String?,
    scanProgress: ScanProgress?,
    scanJob: Job?,
    hasImportedTracks: Boolean,
    onClearLibrary: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxSize(), color = HausPaper) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeContentPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // Title bar with back button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("< Back", color = HausPulse, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Settings",
                        color = HausInk,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                    )
                }

                // Manage Music section
                Text(
                    text = "Manage Music",
                    color = HausInk,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )

                // Scanning card
                if (scanProgress?.isActive == true) {
                    val sp = scanProgress
                    val ss = sp.session!!
                    ScanningCard(
                        foldersVisited = ss.foldersVisited,
                        filesVisited = ss.filesVisited,
                        tracksAdded = ss.tracksAdded,
                        onCancel = { scanJob?.cancel() },
                    )
                }

                // Add music folder button
                Button(
                    onClick = folderPickerLauncher::launch,
                    enabled = folderPickerLauncher.isAvailable && scanProgress?.isActive != true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = HausInk,
                        contentColor = HausPaper,
                        disabledBackgroundColor = HausMuted.copy(alpha = 0.28f),
                        disabledContentColor = HausMuted,
                    ),
                ) {
                    Text(
                        text = if (folderPickerLauncher.isAvailable) "Add music folder" else "Folder picker not available yet",
                        fontWeight = FontWeight.Black,
                    )
                }

                // Status message
                importMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = HausMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                // Clear Library
                if (hasImportedTracks) {
                    Button(
                        onClick = onClearLibrary,
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = HausPulse.copy(alpha = 0.15f),
                            contentColor = HausPulse,
                        ),
                    ) {
                        Text("Clear Library", fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Run build**

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache 2>&1 | grep -E 'BUILD|error:'
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SettingsScreen.kt
git commit -m "feat: add Settings screen with Manage Music panel"
```

---

### Task 3: Create SearchScreen.kt

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt`

**Interfaces:**
- Consumes: `List<LibraryTrack>`, `TagLibReader`, `PlaybackController`, `PlaybackState`
- Produces: `SearchScreen()` composable

- [ ] **Step 1: Create `SearchScreen.kt`**

```kotlin
package com.eterocell.rhythhaus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SearchScreen(
    libraryTracks: List<LibraryTrack>,
    tagLibReader: TagLibReader,
    playbackController: PlaybackController,
    playbackState: PlaybackState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val filtered = remember(query, libraryTracks) {
        if (query.isBlank()) emptyList()
        else libraryTracks.filter { track ->
            track.title.contains(query, ignoreCase = true) ||
            track.artist.contains(query, ignoreCase = true) ||
            (track.album?.contains(query, ignoreCase = true) == true)
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(modifier = modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxSize(), color = HausPaper) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeContentPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Title bar with back
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("< Back", color = HausPulse, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Search", color = HausInk, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }

                // Search field
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    placeholder = { Text("Track, artist, or album...", color = HausMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = HausInk,
                        focusedBorderColor = HausPulse,
                        unfocusedBorderColor = HausMuted.copy(alpha = 0.3f),
                        cursorColor = HausPulse,
                    ),
                )

                // Results
                if (query.isNotBlank()) {
                    Text(
                        text = "${filtered.size} result${if (filtered.size != 1) "s" else ""}",
                        color = HausMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    if (filtered.isEmpty()) {
                        Text(
                            text = "No tracks match \"$query\"",
                            color = HausMuted,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(filtered, key = { it.id }) { track ->
                                SearchResultRow(
                                    track = track,
                                    isNowPlaying = playbackState.currentTrack?.id == track.id,
                                    isPlaying = playbackState.isPlaying,
                                    onClick = {
                                        val playable = libraryTracks.map { it.toPlayableTrack() }
                                        playbackController.setQueue(playable, track.id)
                                        playbackController.play()
                                    },
                                )
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    track: LibraryTrack,
    isNowPlaying: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isNowPlaying) HausPanel else HausPaper,
    ) {
        Row(
            modifier = Modifier.padding(12.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = if (isNowPlaying && isPlaying) HausPulse else HausInk,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = listOfNotNull(track.artist, track.album).joinToString(" · "),
                    color = HausMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
            if (isNowPlaying && isPlaying) {
                EqualizerStrip(active = true)
            }
        }
    }
}
```

Add the import for `EqualizerStrip` if not already accessible (it's defined in `App.kt` in the same package, so no import needed).

If `EqualizerStrip` is private in App.kt, make it internal instead:

In `App.kt`, change:
```kotlin
private fun EqualizerStrip(active: Boolean) {
```
to:
```kotlin
@Composable
internal fun EqualizerStrip(active: Boolean) {
```

- [ ] **Step 2: Run build**

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache 2>&1 | grep -E 'BUILD|error:'
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt
git commit -m "feat: add Search screen with track/artist/album filtering"
```

---

### Task 4: Update NowPlayingBar — add Search/Settings buttons and safe area

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`

**Interfaces:**
- Consumes: existing `NowPlayingBar` signature, `onSettings: () -> Unit`, `onSearch: () -> Unit`
- Produces: updated `NowPlayingBar()` with icon buttons and safe area

- [ ] **Step 1: Read current NowPlayingBar.kt to understand layout**

Use `read_file` on `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt` and note the current structure. The existing bar shows track info left, play/pause center, expand on tap.

- [ ] **Step 2: Add `onSettings` and `onSearch` parameters**

Update the signature:

```kotlin
@Composable
fun NowPlayingBar(
    track: PlayableTrack,
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    onExpand: () -> Unit,
    onSettings: () -> Unit,   // NEW
    onSearch: () -> Unit,      // NEW
    modifier: Modifier = Modifier,
) {
```

- [ ] **Step 3: Add Search and Settings icon buttons**

In the Row that contains the track info and play/pause button, add two small icon buttons on the right side, before the overall clickable area. Place them between the play/pause button and the right edge.

Add after the play/pause button:

```kotlin
// Search button
IconButton(onClick = onSearch, modifier = Modifier.size(36.dp)) {
    Text("🔍", fontSize = 16.sp)
}
// Settings button  
IconButton(onClick = onSettings, modifier = Modifier.size(36.dp)) {
    Text("⚙️", fontSize = 16.sp)
}
```

If the `NowPlayingBar` uses a Row layout, insert these before the closing of the Row. Use `Spacer(Modifier.weight(1f))` before these buttons to push them to the right.

- [ ] **Step 4: Add bottom safe area padding**

Wrap the entire bar content with `WindowInsets` padding for the bottom navigation bar:

```kotlin
Surface(
    modifier = modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.navigationBars)
        .windowInsetsPadding(WindowInsets.safeDrawing),
    ...
) {
```

Or use `Modifier.safeContentPadding()` if that helper exists in the project. Check the existing `safeContentPadding()` usage in `App.kt` line 279.

The pattern used in `App.kt` is:
```kotlin
Modifier.safeContentPadding()
```

Add this to the `NowPlayingBar`'s root `Surface` modifier:

```kotlin
Surface(
    modifier = modifier
        .fillMaxWidth()
        .safeContentPadding(),
    ...
) {
```

- [ ] **Step 5: Run build**

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache 2>&1 | grep -E 'BUILD|error:'
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt
git commit -m "feat: add search/settings buttons and safe area to NowPlayingBar"
```

---

### Task 5: Final verification

- [ ] **Step 1: Run full build on all platforms**

```bash
./gradlew :shared:jvmTest :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug --configuration-cache 2>&1 | grep -E 'BUILD|FAILED|error:'
```

Expected: `BUILD SUCCESSFUL` for all targets

- [ ] **Step 2: Run `./init.sh` for full harness verification**

```bash
./init.sh 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL` for all tasks

- [ ] **Step 3: Commit any remaining changes**

```bash
git status --short
git add -A
git commit -m "chore: finalize bottom bar navigation, settings, and search"
```
