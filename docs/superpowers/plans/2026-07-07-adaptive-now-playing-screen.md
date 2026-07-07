# Adaptive Now Playing Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an adaptive expanded Now Playing layout that preserves the current compact phone screen and uses a wide artwork-left/controls-right split layout on tablet/desktop-sized windows.

**Architecture:** Keep `NowPlayingScreen(...)` as the single expanded overlay content entry point called by `NowPlayingExpandOverlay`. Add a tested Now Playing-specific adaptive helper that mirrors the current Library threshold policy. Refactor `NowPlayingScreen.kt` into private artwork and controls panes, then choose compact or split layout with local Compose `BoxWithConstraints`/`Row` primitives.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform shared `commonMain`, Miuix basic components, existing `PlaybackController`, existing `LibraryNavigationTest`, OpenSpec.

## Global Constraints

- Compact phone Now Playing layout and behavior must remain unchanged.
- Wide Now Playing layout uses option A: artwork/accent visual on the left; track metadata, status, progress, shuffle/repeat controls, and transport controls on the right.
- Adaptive wide threshold matches the Library helper: split at width >= 840dp, or width >= 600dp with height / width < 1.2.
- Do not add queue, up-next, lyrics, or extra detail pane scope.
- Do not add `miuix-navigation3-adaptive` or Navigation3 adaptive dependencies.
- Preserve expanded overlay animation, drag-to-collapse, left-edge swipe back, back dismissal, seek, shuffle, repeat, previous, play/pause, and next behavior.
- Keep implementation in shared Compose common code.
- Verification target: `openspec validate adaptive-now-playing-screen --strict`, focused JVM tests, `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`, `/usr/bin/xcrun xcodebuild -version`, and `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`.

---

### Task 1: Now Playing Adaptive Layout Helper

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt`

**Interfaces:**
- Produces: `enum class NowPlayingAdaptiveLayoutMode { Compact, Split }`
- Produces: `fun nowPlayingAdaptiveLayoutModeFor(widthDp: Float, heightDp: Float): NowPlayingAdaptiveLayoutMode`
- Later tasks consume the helper from `NowPlayingScreen.kt`.

- [ ] **Step 1: Write failing tests**

Add these tests to `LibraryNavigationTest` near the existing adaptive layout tests:

```kotlin
@Test
fun nowPlayingAdaptiveLayoutUsesCompactForPhonePortrait() {
    assertEquals(
        NowPlayingAdaptiveLayoutMode.Compact,
        nowPlayingAdaptiveLayoutModeFor(widthDp = 390f, heightDp = 844f),
    )
}

@Test
fun nowPlayingAdaptiveLayoutUsesCompactForNarrowPortraitTablet() {
    assertEquals(
        NowPlayingAdaptiveLayoutMode.Compact,
        nowPlayingAdaptiveLayoutModeFor(widthDp = 700f, heightDp = 1000f),
    )
}

@Test
fun nowPlayingAdaptiveLayoutUsesSplitForWideTablet() {
    assertEquals(
        NowPlayingAdaptiveLayoutMode.Split,
        nowPlayingAdaptiveLayoutModeFor(widthDp = 840f, heightDp = 1180f),
    )
}

@Test
fun nowPlayingAdaptiveLayoutUsesSplitForLandscapeMediumWidth() {
    assertEquals(
        NowPlayingAdaptiveLayoutMode.Split,
        nowPlayingAdaptiveLayoutModeFor(widthDp = 700f, heightDp = 500f),
    )
}

@Test
fun nowPlayingAdaptiveLayoutUsesSplitForDesktopWidth() {
    assertEquals(
        NowPlayingAdaptiveLayoutMode.Split,
        nowPlayingAdaptiveLayoutModeFor(widthDp = 1200f, heightDp = 800f),
    )
}
```

- [ ] **Step 2: Run tests and verify RED**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
```

Expected: FAIL because `NowPlayingAdaptiveLayoutMode` and `nowPlayingAdaptiveLayoutModeFor` do not exist.

- [ ] **Step 3: Add helper**

In `LibraryNavigation.kt`, add near `LibraryAdaptiveLayoutMode`:

```kotlin
enum class NowPlayingAdaptiveLayoutMode {
    Compact,
    Split,
}

fun nowPlayingAdaptiveLayoutModeFor(
    widthDp: Float,
    heightDp: Float,
): NowPlayingAdaptiveLayoutMode = when (libraryAdaptiveLayoutModeFor(widthDp, heightDp)) {
    LibraryAdaptiveLayoutMode.Compact -> NowPlayingAdaptiveLayoutMode.Compact
    LibraryAdaptiveLayoutMode.ListDetail -> NowPlayingAdaptiveLayoutMode.Split
}
```

- [ ] **Step 4: Run tests and verify GREEN**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt
git commit -m "test: add now playing adaptive layout rules"
```

### Task 2: Extract Now Playing Artwork and Controls Panes

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`

**Interfaces:**
- Consumes: existing `NowPlayingScreen(...)` parameters.
- Produces private composables inside `NowPlayingScreen.kt`:
  - `NowPlayingArtworkPane(...)`
  - `NowPlayingControlsPane(...)`
  - `CompactNowPlayingLayout(...)`
- Must not change the public `NowPlayingScreen(...)` signature.

- [ ] **Step 1: Capture baseline compile**

Run:

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: `BUILD SUCCESSFUL` before refactor.

- [ ] **Step 2: Extract shared state value carrier**

Inside `NowPlayingScreen.kt`, add a private data class near the top-level private helpers:

```kotlin
private data class NowPlayingUiState(
    val durationMillis: Long,
    val positionMillis: Long,
    val statusText: String,
    val isPlaying: Boolean,
    val shuffleEnabled: Boolean,
    val repeatContentDescription: String,
    val shuffleContentDescription: String,
)
```

- [ ] **Step 3: Extract artwork pane**

Move the current artwork `Card` block into this private composable:

```kotlin
@Composable
private fun NowPlayingArtworkPane(
    track: Track,
    artworkBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    brush: Brush,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        cornerRadius = 32.dp,
        colors = CardDefaults.defaultColors(color = HausColors.current.ink),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(brush),
            contentAlignment = Alignment.Center,
        ) {
            if (artworkBitmap != null) {
                Image(
                    bitmap = artworkBitmap,
                    contentDescription = stringResource(Res.string.album_artwork),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = track.title.take(3).uppercase(),
                    color = Color.White.copy(alpha = 0.48f),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}
```

If `ImageBitmap` import is needed, add `import androidx.compose.ui.graphics.ImageBitmap` and use `ImageBitmap?` in the signature.

- [ ] **Step 4: Extract controls pane**

Move the current track info, status, progress scrubber, shuffle/repeat row, transport controls, and bottom spacer into this private composable:

```kotlin
@Composable
private fun NowPlayingControlsPane(
    track: Track,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    uiState: NowPlayingUiState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = track.title,
                color = HausColors.current.ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(Res.string.track_artist_album_format, track.artist, track.album),
                color = HausColors.current.muted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (track.trackNumber != null) {
                Text(
                    text = stringResource(Res.string.track_number_format, track.trackNumber),
                    color = HausColors.current.muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = uiState.statusText,
            color = HausColors.current.muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )

        Spacer(Modifier.height(12.dp))

        MusicProgressScrubber(
            positionMillis = uiState.positionMillis,
            durationMillis = uiState.durationMillis,
            onSeek = playbackController::seekTo,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaybackModeButton(
                selected = uiState.shuffleEnabled,
                contentDescription = uiState.shuffleContentDescription,
                onClick = playbackController::toggleShuffleMode,
            ) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = null,
                    tint = if (uiState.shuffleEnabled) Color.White else HausColors.current.ink,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            PlaybackModeButton(
                selected = playbackState.repeatMode == RepeatMode.RepeatPlaylist || playbackState.repeatMode == RepeatMode.RepeatOne,
                contentDescription = uiState.repeatContentDescription,
                onClick = playbackController::cycleRepeatMode,
            ) {
                val repeatIcon = when (playbackState.repeatMode) {
                    RepeatMode.RepeatOne -> Icons.Filled.RepeatOne
                    RepeatMode.StopAfterCurrent -> Icons.Filled.Filter1
                    else -> Icons.Filled.Repeat
                }
                Icon(
                    imageVector = repeatIcon,
                    contentDescription = null,
                    tint = if (playbackState.repeatMode == RepeatMode.RepeatPlaylist || playbackState.repeatMode == RepeatMode.RepeatOne) Color.White else HausColors.current.ink,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(HausColors.current.panel)
                    .hausClickable { playbackController.skipToPrevious() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = stringResource(Res.string.previous_track),
                    tint = HausColors.current.ink,
                    modifier = Modifier.size(26.dp),
                )
            }

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(HausColors.current.pulse)
                    .hausClickable { playbackController.togglePlayPause() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (uiState.isPlaying) stringResource(Res.string.pause) else stringResource(Res.string.play),
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(HausColors.current.panel)
                    .hausClickable { playbackController.skipToNext() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = stringResource(Res.string.next_track),
                    tint = HausColors.current.ink,
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
```

- [ ] **Step 5: Add compact layout wrapper**

Add:

```kotlin
@Composable
private fun CompactNowPlayingLayout(
    track: Track,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    uiState: NowPlayingUiState,
    artworkBitmap: ImageBitmap?,
    brush: Brush,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .safeContentPadding()
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        NowPlayingArtworkPane(
            track = track,
            artworkBitmap = artworkBitmap,
            brush = brush,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(18.dp))
        NowPlayingControlsPane(
            track = track,
            playbackState = playbackState,
            playbackController = playbackController,
            uiState = uiState,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
```

- [ ] **Step 6: Route existing screen through compact wrapper**

In `NowPlayingScreen`, replace the old inner `Column` with a call to `CompactNowPlayingLayout(...)`. Keep the existing root `Surface` and `leftEdgeSwipeBack(onBack)` unchanged. Build `uiState` from the currently derived values:

```kotlin
val uiState = NowPlayingUiState(
    durationMillis = durationMillis,
    positionMillis = positionMillis,
    statusText = statusText,
    isPlaying = isPlaying,
    shuffleEnabled = shuffleEnabled,
    repeatContentDescription = repeatContentDescription,
    shuffleContentDescription = shuffleContentDescription,
)
```

- [ ] **Step 7: Run focused compile**

Run:

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt
git commit -m "refactor: split now playing screen panes"
```

### Task 3: Wide Split Now Playing Layout

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`

**Interfaces:**
- Consumes: `nowPlayingAdaptiveLayoutModeFor(widthDp, heightDp): NowPlayingAdaptiveLayoutMode`
- Consumes: `CompactNowPlayingLayout(...)`, `NowPlayingArtworkPane(...)`, `NowPlayingControlsPane(...)` from Task 2.
- Produces: wide split rendering in `NowPlayingScreen`.

- [ ] **Step 1: Add wide layout composable**

Add in `NowPlayingScreen.kt`:

```kotlin
@Composable
private fun WideNowPlayingLayout(
    track: Track,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    uiState: NowPlayingUiState,
    artworkBitmap: ImageBitmap?,
    brush: Brush,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .safeContentPadding()
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.48f),
            contentAlignment = Alignment.Center,
        ) {
            NowPlayingArtworkPane(
                track = track,
                artworkBitmap = artworkBitmap,
                brush = brush,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.52f),
            contentAlignment = Alignment.Center,
        ) {
            NowPlayingControlsPane(
                track = track,
                playbackState = playbackState,
                playbackController = playbackController,
                uiState = uiState,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
```

- [ ] **Step 2: Choose layout in NowPlayingScreen**

Replace the direct compact call inside `Surface` with:

```kotlin
BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    when (nowPlayingAdaptiveLayoutModeFor(widthDp = maxWidth.value, heightDp = maxHeight.value)) {
        NowPlayingAdaptiveLayoutMode.Compact -> CompactNowPlayingLayout(
            track = track,
            playbackState = playbackState,
            playbackController = playbackController,
            uiState = uiState,
            artworkBitmap = artworkBitmap,
            brush = brush,
        )
        NowPlayingAdaptiveLayoutMode.Split -> WideNowPlayingLayout(
            track = track,
            playbackState = playbackState,
            playbackController = playbackController,
            uiState = uiState,
            artworkBitmap = artworkBitmap,
            brush = brush,
        )
    }
}
```

Keep `Surface(...).leftEdgeSwipeBack(onBack)` unchanged outside this block.

- [ ] **Step 3: Run focused compile and helper tests**

Run:

```bash
./gradlew :shared:compileKotlinJvm :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Search for forbidden adaptive dependency**

Run:

```bash
grep -R "miuix-navigation3-adaptive\|ListDetailPaneScaffold\|androidx.navigation3.adaptive" -n gradle shared/src androidApp/src || true
```

Expected: no output.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt
git commit -m "feat: add adaptive now playing layout"
```

### Task 4: Final Verification and Evidence

**Files:**
- Modify: `openspec/changes/adaptive-now-playing-screen/tasks.md`
- Modify: `progress.md`
- Track: `docs/superpowers/specs/2026-07-07-adaptive-now-playing-screen-design.md`
- Track: `docs/superpowers/plans/2026-07-07-adaptive-now-playing-screen.md`
- Track: `openspec/changes/adaptive-now-playing-screen/proposal.md`
- Track: `openspec/changes/adaptive-now-playing-screen/design.md`
- Track: `openspec/changes/adaptive-now-playing-screen/specs/now-playing-ui/spec.md`

**Interfaces:**
- Consumes: completed Tasks 1-3.
- Produces: final verification evidence and commit.

- [ ] **Step 1: Validate OpenSpec**

Run:

```bash
openspec validate adaptive-now-playing-screen --strict
```

Expected: `Change 'adaptive-now-playing-screen' is valid`.

- [ ] **Step 2: Run focused helper tests**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run broad JVM/desktop/Android verification**

Run:

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Run iOS availability and simulator verification**

Run:

```bash
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

Expected: xcodebuild prints an Xcode version and simulator tests return `BUILD SUCCESSFUL`. If unavailable, record the exact blocker.

- [ ] **Step 5: Run hygiene checks**

Run:

```bash
git diff --check
grep -R "miuix-navigation3-adaptive\|ListDetailPaneScaffold\|androidx.navigation3.adaptive" -n gradle shared/src androidApp/src || true
```

Expected: `git diff --check` has no output, and grep has no output.

- [ ] **Step 6: Update OpenSpec tasks**

Mark all tasks complete in `openspec/changes/adaptive-now-playing-screen/tasks.md` and add exact command evidence below the relevant tasks.

- [ ] **Step 7: Update progress.md**

Add a top handoff entry:

```text
## Handoff - 2026-07-07 adaptive now playing screen

Route: openspec+superpowers
Owner: implementation
Input: adaptive-now-playing-screen spec/plan
Output: compact-preserving adaptive Now Playing split layout
Verification:
- <exact command>: pass/fail (<exact result>)
Changed files:
- <paths>
Next owner: user for manual wide/compact visual validation
Blockers: <none or exact blocker>
Commit: pending
```

- [ ] **Step 8: Commit docs and evidence**

Before committing, describe staged diffs to the user/coordinator. Then run:

```bash
git add openspec/changes/adaptive-now-playing-screen docs/superpowers/specs/2026-07-07-adaptive-now-playing-screen-design.md docs/superpowers/plans/2026-07-07-adaptive-now-playing-screen.md progress.md
git commit -m "docs: record adaptive now playing verification"
```
