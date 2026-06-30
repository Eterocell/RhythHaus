# UI/UX Fixes Batch Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the first shared Compose UI/UX polish batch: empty-home import CTA, adaptive album grid, Songs browsing, Search clear/dismiss behavior, larger compact controls, and removal of normal-user TagLib developer panels.

**Architecture:** Keep the changes shared-first in `shared/src/commonMain/kotlin/com/eterocell/rhythhaus`. Add small pure helpers for layout and browse behavior where tests can prove behavior without UI screenshots. Avoid platform, scanner, playback-engine, database, dependency, or native UI changes.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Miuix components, Material Icons already present, Kotlin common tests, Gradle configuration cache.

## Global Constraints

- Work in `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/.worktrees/ui-ux-fixes-batch`; use absolute paths for file tools.
- OpenSpec change: `openspec/changes/ui-ux-fixes-batch`.
- Superpowers design: `docs/superpowers/specs/2026-07-01-ui-ux-fixes-batch-design.md`.
- No new dependencies.
- No native SwiftUI, Android, or desktop-specific UI rewrite.
- No scanner, metadata extraction, playback engine, MediaSession, audio-session, or database schema changes.
- No playlists, genres, folders/sources, recently added, queue redesign, or stable album identity redesign in this batch.
- No live visual QA claims without device/screenshot evidence.
- Preserve Albums and Artists behavior unless the task explicitly changes shared browse rendering.
- Use existing Miuix `Text`, `Button`, `Card`, `Surface` patterns and existing `Modifier.hausClickable`.
- After code changes, run at least `./gradlew :shared:compileKotlinJvm --configuration-cache`; final task runs broad JVM/desktop/Android verification.

---

### Task 1: Empty Home onboarding and adaptive album grid

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryBrowser.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Test: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryBrowserTest.kt`
- Modify: `openspec/changes/ui-ux-fixes-batch/tasks.md`

**Interfaces:**
- Produces: `fun albumGridColumnsForWidth(widthDp: Float): Int`
- Consumes: existing `ImportAudioCard(...)`, `AlbumCard(...)`, `BrowseMode.Albums`

- [ ] **Step 1: Write failing common tests for album breakpoints**

Create `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryBrowserTest.kt` with:

```kotlin
package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryBrowserTest {
    @Test
    fun albumGridUsesTwoColumnsBelowTabletWidth() {
        assertEquals(2, albumGridColumnsForWidth(0f))
        assertEquals(2, albumGridColumnsForWidth(559f))
    }

    @Test
    fun albumGridUsesThreeColumnsForTabletWidth() {
        assertEquals(3, albumGridColumnsForWidth(560f))
        assertEquals(3, albumGridColumnsForWidth(899f))
    }

    @Test
    fun albumGridUsesFourColumnsForDesktopWidth() {
        assertEquals(4, albumGridColumnsForWidth(900f))
        assertEquals(4, albumGridColumnsForWidth(1400f))
    }
}
```

- [ ] **Step 2: Run test to verify RED**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryBrowserTest' --configuration-cache`

Expected: FAIL because `albumGridColumnsForWidth` is not defined.

- [ ] **Step 3: Add helper implementation**

In `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryBrowser.kt`, after `enum class BrowseMode { Albums, Artists }`, add:

```kotlin
fun albumGridColumnsForWidth(widthDp: Float): Int = when {
    widthDp >= 900f -> 4
    widthDp >= 560f -> 3
    else -> 2
}
```

- [ ] **Step 4: Show import CTA on empty Home and adapt album rows**

In `LibraryHomeScreen` Home branch in `App.kt`:

1. Insert an `ImportAudioCard(...)` item after `HeaderSection(snapshot)` when `snapshot.tracks.isEmpty()`.
2. Replace the hardcoded `val albumRows = albums.chunked(2)` album rendering with a `BoxWithConstraints` item that calls `albumGridColumnsForWidth(maxWidth.value)` and chunks by that count.
3. Keep `AlbumCard` unchanged and keep spacer fill for short final rows.

Target shape:

```kotlin
item {
    HeaderSection(snapshot)
}
if (snapshot.tracks.isEmpty()) {
    item {
        ImportAudioCard(
            folderPickerLauncher = folderPickerLauncher,
            importMessage = importMessage,
            hasImportedTracks = false,
            onClearLibrary = onClearLibrary,
        )
    }
}
```

Album section target shape:

```kotlin
if (browseMode == BrowseMode.Albums) {
    item {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = albumGridColumnsForWidth(maxWidth.value)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                albums.chunked(columns).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { albumGroup ->
                            AlbumCard(
                                album = albumGroup,
                                modifier = Modifier.weight(1f),
                                onClick = { pushRoute(LibraryRoute.AlbumDetail(albumGroup.album)) },
                            )
                        }
                        repeat(columns - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 5: Verify GREEN**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryBrowserTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: both BUILD SUCCESSFUL.

- [ ] **Step 6: Update task evidence and commit**

Update `openspec/changes/ui-ux-fixes-batch/tasks.md` Task 1 checkboxes with evidence lines. Commit:

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryBrowser.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryBrowserTest.kt openspec/changes/ui-ux-fixes-batch/tasks.md
git commit -m "feat: improve empty library album browsing"
```

### Task 2: Songs browse mode

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryBrowser.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Test: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryBrowserTest.kt`
- Modify: `openspec/changes/ui-ux-fixes-batch/tasks.md`

**Interfaces:**
- Consumes: `BrowseMode`, `TrackRow`, `PlaybackController.setQueue`, `PlaybackController.togglePlayPause`
- Produces: `BrowseMode.Songs` and Home rendering for individual tracks

- [ ] **Step 1: Add failing test for browse modes**

Append to `LibraryBrowserTest`:

```kotlin
@Test
fun browseModesIncludeAlbumsArtistsAndSongsInOrder() {
    assertEquals(listOf(BrowseMode.Albums, BrowseMode.Artists, BrowseMode.Songs), BrowseMode.entries.toList())
}
```

- [ ] **Step 2: Run test to verify RED**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryBrowserTest' --configuration-cache`

Expected: FAIL because `BrowseMode.Songs` is missing.

- [ ] **Step 3: Extend BrowseMode**

Change `LibraryBrowser.kt` enum to:

```kotlin
enum class BrowseMode { Albums, Artists, Songs }
```

- [ ] **Step 4: Render songs in Home**

In `App.kt` Home branch, change the Albums/Artists conditional to handle `BrowseMode.Songs` with a `when (browseMode)`.

Songs target behavior:

```kotlin
BrowseMode.Songs -> {
    items(snapshot.tracks, key = { it.id }) { track ->
        TrackRow(
            track = track,
            selected = track.id == selectedTrackId,
            onClick = {
                selectedTrackId = track.id
                val playableTracks = snapshot.tracks.map { it.toPlayableTrack() }
                if (playbackState.currentTrack?.id != track.id || playbackState.status == PlaybackStatus.Idle) {
                    playbackController.setQueue(playableTracks, track.id)
                }
                playbackController.togglePlayPause()
            },
        )
    }
}
```

Keep Albums and Artists behavior from before Task 2.

- [ ] **Step 5: Verify GREEN**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryBrowserTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: both BUILD SUCCESSFUL.

- [ ] **Step 6: Update task evidence and commit**

Update `openspec/changes/ui-ux-fixes-batch/tasks.md` Task 2 checkboxes with evidence lines. Commit:

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryBrowser.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryBrowserTest.kt openspec/changes/ui-ux-fixes-batch/tasks.md
git commit -m "feat: add songs browse mode"
```

### Task 3: Search behavior and compact touch targets

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/BackChip.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- Modify: `openspec/changes/ui-ux-fixes-batch/tasks.md`

**Interfaces:**
- Consumes: `SearchScreen(onDismiss)`, `Modifier.hausClickable`, existing Material Search/Settings icons
- Produces: Search clear action, Search dismiss after result click, >=44dp BackChip/Search/Settings controls

- [ ] **Step 1: Add clear action to Search field**

In `SearchScreen.kt`, update the search field Box content so the `BasicTextField` leaves trailing space when query is non-empty and a clear action appears aligned to end. Use existing Miuix `Text` and `hausClickable`.

Target shape inside the field Box:

```kotlin
BasicTextField(
    value = query,
    onValueChange = { query = it },
    modifier = Modifier
        .fillMaxWidth()
        .padding(end = if (query.isNotEmpty()) 44.dp else 0.dp)
        .focusRequester(focusRequester),
    singleLine = true,
    textStyle = TextStyle(color = HausColors.current.ink, fontSize = 15.sp),
    cursorBrush = SolidColor(HausColors.current.pulse),
)
if (query.isNotEmpty()) {
    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .height(44.dp)
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(999.dp))
            .hausClickable { query = "" }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("Clear", color = HausColors.current.pulse, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}
```

Add imports for `height` only if needed; wildcard layout import already exists.

- [ ] **Step 2: Dismiss Search after result selection**

In `SearchResultRow` `onClick` lambda in `SearchScreen.kt`, after `playbackController.play()`, call `onDismiss()`:

```kotlin
onClick = {
    val playable = libraryTracks.map { it.toPlayableTrack() }
    playbackController.setQueue(playable, track.id)
    playbackController.play()
    onDismiss()
}
```

- [ ] **Step 3: Increase BackChip touch target**

In `BackChip.kt`, add `import androidx.compose.foundation.layout.heightIn` and set the outer Box modifier to include `.heightIn(min = 44.dp)` before inner padding.

- [ ] **Step 4: Increase bottom-bar Search/Settings targets**

In `NowPlayingBar.kt`, change Search and Settings Box `.size(32.dp)` modifiers to `.size(44.dp)`. Keep icon size at 18dp, content descriptions unchanged, and the row arrangement unchanged unless compilation requires minor spacing adjustment.

- [ ] **Step 5: Verify compile**

Run: `./gradlew :shared:compileKotlinJvm --configuration-cache`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Update task evidence and commit**

Update `openspec/changes/ui-ux-fixes-batch/tasks.md` Task 3 checkboxes with evidence lines. Commit:

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/BackChip.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt openspec/changes/ui-ux-fixes-batch/tasks.md
git commit -m "fix: polish search and compact controls"
```

### Task 4: Remove user-facing developer panels

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify: `openspec/changes/ui-ux-fixes-batch/tasks.md`

**Interfaces:**
- Consumes: normal Now Playing screen UI
- Produces: no user-facing `DEV · TagLib` or TagLib property-map panels in normal UI

- [ ] **Step 1: Remove expanded Now Playing developer panel**

In `NowPlayingScreen.kt`, remove the `DeveloperTrackPanel(...)` call and the surrounding `if (currentLibraryTrack != null)` block.

Then remove the now-unused `DeveloperTrackPanel`, `rawTagLines`, `trackDisplay`, and `artworkLabel` functions from `NowPlayingScreen.kt` if they have no call sites in that file.

Remove now-unused imports such as `AnimatedVisibility`, `FontFamily`, `LibraryTrack`, `resolvePathForMetadata`, `TagLibReader`, `TagReadResult`, and raw metadata alias only if compilation says they are unused after cleanup. Keep the function parameters if removing them requires broader call-site churn; prefer minimal safe cleanup.

- [ ] **Step 2: Remove dead Home developer panel code if unused**

In `App.kt`, verify `DeveloperPanel` and `DeveloperMetadataRow` have no call sites. If no call sites exist, remove `DeveloperPanel`, `DeveloperMetadataRow`, `AudioSource.devLabel`, `rawTagLines`, `trackDisplay`, and `artworkLabel` from `App.kt` only if they are now dead. Remove imports that become unused.

Do not remove metadata/TagLib code that is still used by scanner, playback, or normal artwork rendering.

- [ ] **Step 3: Verify source text is gone**

Run:

```bash
rg 'DEV · TagLib|ALL PROPERTIES|URI source — TagLib requires a filesystem path' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt
```

Expected: no matches, exit 1.

- [ ] **Step 4: Verify compile and broad JVM/desktop/Android**

Run:

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Expected: both BUILD SUCCESSFUL. If known flaky `JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion` fails once, rerun the targeted test and the broad command, recording exact evidence.

- [ ] **Step 5: Update task evidence and commit**

Update `openspec/changes/ui-ux-fixes-batch/tasks.md` Task 4 checkboxes with evidence lines. Commit:

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt openspec/changes/ui-ux-fixes-batch/tasks.md
git commit -m "fix: remove user facing developer panels"
```

### Task 5: Final OpenSpec evidence and handoff

**Files:**
- Modify: `openspec/changes/ui-ux-fixes-batch/tasks.md`
- Modify: `progress.md`

**Interfaces:**
- Consumes: completed Tasks 1-4 commits
- Produces: validated OpenSpec task evidence and session handoff

- [ ] **Step 1: Validate OpenSpec**

Run: `openspec validate ui-ux-fixes-batch --strict`

Expected: `Change 'ui-ux-fixes-batch' is valid`.

- [ ] **Step 2: Run final verification**

Run:

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

Expected: Gradle commands BUILD SUCCESSFUL and xcodebuild prints installed Xcode version. If iOS validation fails because `xcodebuild` or simulator tooling is unavailable, record exact output and do not claim iOS passed.

- [ ] **Step 3: Update OpenSpec tasks**

Mark remaining Task 5 checkboxes in `openspec/changes/ui-ux-fixes-batch/tasks.md` with exact evidence from Steps 1-2.

- [ ] **Step 4: Update progress.md handoff**

Prepend a new handoff entry to `progress.md` using the actual evidence from this branch. Include these headings exactly: `Route`, `Owner`, `Scope`, `Implementation`, `Verification`, `Acceptance`, `Changed files`, `Next owner`, `Blockers`, and `Commit`. Fill every heading with concrete branch facts from `git log --oneline main..HEAD`, `git status --short`, `git diff --name-only main..HEAD`, and the verification output from this task. Do not leave placeholders in the final `progress.md` entry.

- [ ] **Step 5: Commit final evidence**

Run:

```bash
git add openspec/changes/ui-ux-fixes-batch/tasks.md progress.md
git commit -m "docs: record ui ux fixes batch evidence"
```

If `tasks.md` and `progress.md` were already committed by prior tasks and no changes remain, do not create an empty commit; report that final evidence is already committed.
