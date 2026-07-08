# Album/Artist Track List Top-Bar Artwork Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show representative embedded artwork in album and artist drill-down track-list top bars.

**Architecture:** Keep artwork derivation in the existing Library route layer and rendering in the existing drill-down Miuix top-bar chrome. Reuse ordered `Track.artworkBytes` candidates and `decodeArtworkCached`; do not introduce new models, schema, dependencies, or platform code.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Miuix `TopAppBar`, existing RhythHaus artwork cache.

## Global Constraints

- Shared Compose UI only for implementation.
- No Gradle dependency changes.
- No SQLDelight schema migration.
- No scanner, TagLib, playback engine, or platform-specific source changes.
- Preserve existing Miuix scroll behavior and `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)` on the drill-down `LazyColumn`.
- Use cached artwork decoding for top-bar artwork; full-size cached decoding is acceptable because the artwork fills the top bar rather than a compact thumbnail.
- If no candidate artwork decodes successfully, render no top-bar artwork placeholder and keep the existing glass title top bar.

---

### Task 1: Pass representative artwork into drill-down chrome

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt:107-163`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt:51-128`

**Interfaces:**
- Consumes: `Track.artworkBytes: ByteArray?` from existing album/artist track lists.
- Produces: `DrillDownView(..., topBarArtworkCandidates: List<ByteArray> = emptyList(), ...)` and forwards that value to `DrillDownMiuixScrollChrome`.

- [ ] **Step 1: Add the optional `topBarArtworkBytes` parameter to `DrillDownView`**

In `LibraryDetailContent.kt`, add the parameter immediately after `tracks` so the call site can pass route-specific representative artwork while existing defaults remain safe:

```kotlin
internal fun DrillDownView(
    title: String,
    subtitle: String,
    tracks: List<Track>,
    topBarArtworkCandidates: List<ByteArray> = emptyList(),
    selectedTrack: Track?,
```

- [ ] **Step 2: Forward the parameter to the top-bar chrome**

In the existing `DrillDownMiuixScrollChrome` call, pass the new value:

```kotlin
DrillDownMiuixScrollChrome(
    scrollBehavior = miuixScrollBehavior,
    title = title,
    topBarArtworkCandidates = topBarArtworkCandidates,
    onBack = onBack,
    backdrop = drillDownBackdrop,
    modifier = Modifier.align(Alignment.TopCenter),
)
```

- [ ] **Step 3: Derive album representative artwork at the route boundary**

In `LibraryRoutes.kt`, inside the `AlbumDetail` branch after `val albumTracks = album.tracks`, add:

```kotlin
val albumArtworkCandidates = albumTracks.mapNotNull { it.artworkBytes }
```

Then pass it to `DrillDownView`:

```kotlin
topBarArtworkCandidates = albumArtworkCandidates,
```

- [ ] **Step 4: Derive artist representative artwork at the route boundary**

In the `ArtistDetail` branch after `val artistTracks = artist.tracks`, add:

```kotlin
val artistArtworkCandidates = artistTracks.mapNotNull { it.artworkBytes }
```

Then pass it to `DrillDownView`:

```kotlin
topBarArtworkCandidates = artistArtworkCandidates,
```

- [ ] **Step 5: Run focused compilation**

Run: `./gradlew :shared:compileKotlinJvm --configuration-cache`
Expected: `BUILD SUCCESSFUL`.

### Task 2: Render cached artwork as the Miuix drill-down top-bar background

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt:1-126`

**Interfaces:**
- Consumes: `topBarArtworkCandidates: List<ByteArray>` from `DrillDownView`.
- Produces: top-bar artwork background in `DrillDownMiuixScrollChrome` using cached artwork decoding.

- [ ] **Step 1: Add imports**

Add the Compose image/layout imports and the existing decoder import needed by the chrome:

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.ContentScale
import com.eterocell.rhythhaus.ui.decodeArtworkCached
import rhythhaus.shared.generated.resources.album_artwork
```

`Box`, `fillMaxWidth`, `height`, `RoundedCornerShape`, and `clip` are already imported in this file.

- [ ] **Step 2: Add the optional artwork parameter to `DrillDownMiuixScrollChrome`**

Update the function signature:

```kotlin
internal fun DrillDownMiuixScrollChrome(
    scrollBehavior: ScrollBehavior,
    title: String,
    topBarArtworkCandidates: List<ByteArray> = emptyList(),
    onBack: () -> Unit,
    backdrop: LayerBackdrop?,
    modifier: Modifier = Modifier,
)
```

- [ ] **Step 3: Decode through the artwork cache**

At the start of `DrillDownMiuixScrollChrome`, before the outer `Box`, add:

```kotlin
val topBarArtwork = remember(topBarArtworkCandidates) {
    topBarArtworkCandidates.firstNotNullOfOrNull { it.decodeArtworkCached() }
}
```

- [ ] **Step 4: Render artwork as the top-bar background with chip overlays**

Add this `actions` argument to the existing `TopAppBar` call, before `scrollBehavior`:

```kotlin
actions = {
    if (topBarArtwork != null) {
        Image(
            bitmap = topBarArtwork,
            contentDescription = stringResource(Res.string.album_artwork),
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
    }
},
```

Keep `actionIconPadding = 0.dp`; do not add placeholder text or a fallback shape when `topBarArtwork` is null.

- [ ] **Step 5: Run focused compilation and library tests**

Run: `./gradlew :shared:compileKotlinJvm --configuration-cache`
Expected: `BUILD SUCCESSFUL`.

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryBrowserTest' --configuration-cache`
Expected: `BUILD SUCCESSFUL`.

### Task 3: Evidence, OpenSpec task status, and final verification

**Files:**
- Modify: `openspec/changes/drilldown-topbar-artwork/tasks.md`
- Modify: `progress.md`
- Modify: `roadmap.md`

**Interfaces:**
- Consumes: implementation and verification output from Tasks 1-2.
- Produces: durable handoff evidence and roadmap entry.

- [ ] **Step 1: Validate OpenSpec**

Run: `openspec validate drilldown-topbar-artwork --strict`
Expected: `Change 'drilldown-topbar-artwork' is valid`.

- [ ] **Step 2: Run focused and platform verification**

Run: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`
Expected: `BUILD SUCCESSFUL`.

Run: `/usr/bin/xcrun xcodebuild -version`
Expected: prints installed Xcode version and exits 0.

Run: `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`
Expected: `BUILD SUCCESSFUL`, unless an exact toolchain blocker is recorded.

Run: `git diff --check`
Expected: no output and exit 0.

- [ ] **Step 3: Update OpenSpec task evidence**

Mark Tasks 1-3 complete in `openspec/changes/drilldown-topbar-artwork/tasks.md` and add the commands that actually ran with pass/fail evidence.

- [ ] **Step 4: Update session state**

Prepend a handoff entry to `progress.md` with route, owner, scope, verification, changed files, next owner, blockers, and commit status.

- [ ] **Step 5: Update roadmap**

Append a concise checked entry to `roadmap.md` noting that album/artist drill-down track-list top bars now show representative embedded artwork when available, with fallback to current title/back behavior when unavailable.
