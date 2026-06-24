# Now Playing Panel + Track Ordering + Artwork Display Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform the NowPlayingCard into a clickable floating bar with expand-to-full-screen, order album/artist tracks by track number instead of alphabetically, and display album/track/artist artwork images everywhere instead of text placeholders.

**Architecture:** Three interconnected changes sharing a data-model prerequisite: `LibraryTrack` and `Track` gain `trackNumber`, `discNumber`, and `artworkBytes` fields, the SQL schema adds matching columns, the scanner flows TagLib-parsed values through, and then each UI feature (ordering, artwork display, floating bar) builds on that enriched model.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, SQLDelight, TagLib native C ABI reader

## Global Constraints

- Shared-first: all UI lives in `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Platform-specific artwork decoding via `expect fun ByteArray.decodeArtwork(): ImageBitmap?` (already exists with JVM/Android impls, iOS returns null)
- `AudioMetadata` is the scanner's simplified metadata intermediate — extend it to carry track number and artwork
- `LibraryTrack` is the persistence model — add fields + update SQL schema + update all queries
- `Track` is the Compose UI model — add fields and populate them in `librarySnapshot()` and `toUiTrack()`
- No Windows/Linux scope; no file watching; no repository-level reordering (the `InMemoryLibraryRepository` and `SqlDelightLibraryRepository` main listing should also switch to track-number order or at minimum include the fields)
- Verification: `./init.sh` must pass after every task

---

### Task 1: Extend data models with track number, disc number, and artwork bytes

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryModels.kt` (add fields to `LibraryTrack`)
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/MusicModels.kt` (add fields to `Track`)
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AudioMetadata.kt` (extend `AudioMetadata` + reader)
- Modify: `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/LibraryTrack.sq` (schema + queries)

**Interfaces:**
- Produces: `LibraryTrack(trackNumber: Int? = null, discNumber: Int? = null, artworkBytes: ByteArray? = null, artworkMimeType: String? = null)`
- Produces: `Track(trackNumber: Int? = null, discNumber: Int? = null, artworkBytes: ByteArray? = null)` (artworkBytes already exists)
- Produces: `AudioMetadata(trackNumber: Int? = null, discNumber: Int? = null, artworkBytes: ByteArray? = null, artworkMimeType: String? = null)`

- [ ] **Step 1: Add fields to AudioMetadata and update AudioMetadataReader**

Add track number, disc number, artwork fields to `AudioMetadata` in `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AudioMetadata.kt`:

```kotlin
data class AudioMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMillis: Long? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val artworkBytes: ByteArray? = null,
    val artworkMimeType: String? = null,
) {
    override fun equals(other: Any?): Boolean = other is AudioMetadata &&
        title == other.title && artist == other.artist && album == other.album &&
        durationMillis == other.durationMillis && trackNumber == other.trackNumber &&
        discNumber == other.discNumber && artworkMimeType == other.artworkMimeType &&
        artworkBytes.contentEquals(other.artworkBytes)

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + album.hashCode()
        result = 31 * result + (durationMillis?.hashCode() ?: 0)
        result = 31 * result + (trackNumber ?: 0)
        result = 31 * result + (discNumber ?: 0)
        result = 31 * result + (artworkMimeType?.hashCode() ?: 0)
        result = 31 * result + (artworkBytes?.contentHashCode() ?: 0)
        return result
    }
}
```

Update `private fun TagMetadata.toAudioMetadata()` to pass the new fields:

```kotlin
private fun TagMetadata.toAudioMetadata(): AudioMetadata = AudioMetadata(
    title = title.normalizedOrNull(),
    artist = artist.normalizedOrNull(),
    album = album.normalizedOrNull(),
    durationMillis = durationMillis?.takeIf { it > 0L },
    trackNumber = trackNumber?.takeIf { it > 0 },
    discNumber = discNumber?.takeIf { it > 0 },
    artworkBytes = artwork?.bytes?.takeIf { it.isNotEmpty() },
    artworkMimeType = artwork?.mimeType,
)
```

- [ ] **Step 2: Add fields to LibraryTrack**

In `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryModels.kt`, add to `LibraryTrack`:

```kotlin
data class LibraryTrack(
    // ... existing fields ...
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val artworkBytes: ByteArray? = null,
    val artworkMimeType: String? = null,
) {
    // existing toPlayableTrack remains unchanged
    
    override fun equals(other: Any?): Boolean = other is LibraryTrack &&
        id == other.id && sourceId == other.sourceId && sourceLocalKey == other.sourceLocalKey &&
        audioSource == other.audioSource && displayName == other.displayName &&
        title == other.title && artist == other.artist && album == other.album &&
        durationMillis == other.durationMillis && sizeBytes == other.sizeBytes &&
        modifiedAtEpochMillis == other.modifiedAtEpochMillis &&
        lastSeenScanId == other.lastSeenScanId &&
        createdAtEpochMillis == other.createdAtEpochMillis &&
        updatedAtEpochMillis == other.updatedAtEpochMillis &&
        trackNumber == other.trackNumber && discNumber == other.discNumber &&
        artworkMimeType == other.artworkMimeType &&
        artworkBytes.contentEquals(other.artworkBytes)

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + sourceId.hashCode()
        result = 31 * result + sourceLocalKey.hashCode()
        result = 31 * result + audioSource.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + album.hashCode()
        result = 31 * result + (durationMillis?.hashCode() ?: 0)
        result = 31 * result + (sizeBytes?.hashCode() ?: 0)
        result = 31 * result + (modifiedAtEpochMillis?.hashCode() ?: 0)
        result = 31 * result + (lastSeenScanId?.hashCode() ?: 0)
        result = 31 * result + createdAtEpochMillis.hashCode()
        result = 31 * result + updatedAtEpochMillis.hashCode()
        result = 31 * result + (trackNumber ?: 0)
        result = 31 * result + (discNumber ?: 0)
        result = 31 * result + (artworkMimeType?.hashCode() ?: 0)
        result = 31 * result + (artworkBytes?.contentHashCode() ?: 0)
        return result
    }
}
```

Update `LibraryTrack.toPlayableTrack()` to pass artwork:

```kotlin
fun toPlayableTrack(): PlayableTrack = PlayableTrack(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMillis = durationMillis,
    source = audioSource,
    artworkBytes = artworkBytes,
)
```

- [ ] **Step 3: Add fields to Track (UI model)**

In `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/MusicModels.kt`, update `Track`:

```kotlin
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val accent: TrackAccent,
    val source: AudioSource,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val artworkBytes: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean = other is Track &&
        id == other.id && title == other.title && artist == other.artist &&
        album == other.album && durationSeconds == other.durationSeconds &&
        accent == other.accent && source == other.source &&
        trackNumber == other.trackNumber && discNumber == other.discNumber &&
        artworkBytes.contentEquals(other.artworkBytes)

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + album.hashCode()
        result = 31 * result + durationSeconds
        result = 31 * result + accent.hashCode()
        result = 31 * result + source.hashCode()
        result = 31 * result + (trackNumber ?: 0)
        result = 31 * result + (discNumber ?: 0)
        result = 31 * result + (artworkBytes?.contentHashCode() ?: 0)
        return result
    }
}
```

- [ ] **Step 4: Update SQLDelight schema and queries**

In `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/LibraryTrack.sq`, add columns:

```sql
CREATE TABLE IF NOT EXISTS library_track (
    id TEXT NOT NULL PRIMARY KEY,
    sourceId TEXT NOT NULL REFERENCES library_source(id) ON DELETE CASCADE,
    sourceLocalKey TEXT NOT NULL,
    audioSourceKind TEXT NOT NULL,
    audioSourceValue TEXT NOT NULL,
    displayName TEXT NOT NULL,
    title TEXT NOT NULL DEFAULT 'Untitled audio',
    artist TEXT NOT NULL DEFAULT 'Local file',
    album TEXT NOT NULL DEFAULT 'Imported audio',
    durationMillis INTEGER,
    sizeBytes INTEGER,
    modifiedAtEpochMillis INTEGER,
    lastSeenScanId TEXT,
    createdAtEpochMillis INTEGER NOT NULL,
    updatedAtEpochMillis INTEGER NOT NULL,
    trackNumber INTEGER,
    discNumber INTEGER,
    artworkBytes BLOB,
    artworkMimeType TEXT
);
```

Update `upsertTrack`:

```sql
upsertTrack:
INSERT OR REPLACE INTO library_track(id, sourceId, sourceLocalKey, audioSourceKind, audioSourceValue,
    displayName, title, artist, album, durationMillis, sizeBytes, modifiedAtEpochMillis,
    lastSeenScanId, createdAtEpochMillis, updatedAtEpochMillis,
    trackNumber, discNumber, artworkBytes, artworkMimeType)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
```

Update `selectAllTracks` and `selectTracksForSource`:

```sql
selectAllTracks:
SELECT * FROM library_track ORDER BY title COLLATE NOCASE ASC, artist COLLATE NOCASE ASC;

selectTracksForSource:
SELECT * FROM library_track WHERE sourceId = ? ORDER BY title COLLATE NOCASE ASC, artist COLLATE NOCASE ASC;
```

- [ ] **Step 5: Update SqlDelightLibraryRepository to read/write new columns**

In `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepository.kt`:

Update both query lambdas (upsert and select) for the new columns. The `upsertTrack` method needs the 4 new parameters. The `tracks()` and `tracksForSource()` mapper lambdas need to read the new columns and construct `LibraryTrack` with them.

- [ ] **Step 6: Update LibraryScanner to pass metadata through**

In `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryScanner.kt`, update `toLibraryTrack()`:

```kotlin
private fun AudioScanCandidate.toLibraryTrack(
    scanId: String,
    timestamp: Long,
    trackId: String,
    metadataReader: AudioMetadataReader,
): LibraryTrack {
    val metadata = runCatching { metadataReader.read(audioSource) }.getOrNull()
    return LibraryTrack(
        id = trackId,
        sourceId = sourceId,
        sourceLocalKey = sourceLocalKey,
        audioSource = audioSource,
        displayName = displayName,
        title = metadata?.title ?: displayName.fallbackTitle(),
        artist = metadata?.artist ?: "Local file",
        album = metadata?.album ?: "Imported audio",
        durationMillis = metadata?.durationMillis,
        sizeBytes = sizeBytes,
        modifiedAtEpochMillis = modifiedAtEpochMillis,
        lastSeenScanId = scanId,
        createdAtEpochMillis = timestamp,
        updatedAtEpochMillis = timestamp,
        trackNumber = metadata?.trackNumber,
        discNumber = metadata?.discNumber,
        artworkBytes = metadata?.artworkBytes,
        artworkMimeType = metadata?.artworkMimeType,
    )
}
```

- [ ] **Step 7: Update librarySnapshot and toUiTrack to flow new fields**

In `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`, update `librarySnapshot()`:

```kotlin
private fun librarySnapshot(tracks: List<LibraryTrack>): LibrarySnapshot {
    val uiTracks = tracks.mapIndexed { index, track ->
        Track(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            durationSeconds = ((track.durationMillis ?: 0L) / 1_000L).toInt(),
            accent = libraryTrackAccent(index),
            source = track.audioSource,
            trackNumber = track.trackNumber,
            discNumber = track.discNumber,
            artworkBytes = track.artworkBytes,
        )
    }
    // ... rest unchanged
}
```

In `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryBrowser.kt`, update `toUiTrack()`:

```kotlin
private fun LibraryTrack.toUiTrack(): Track = Track(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationSeconds = durationMillis?.div(1_000L)?.toInt() ?: 0,
    accent = TrackAccent(start = 0xFF111018, end = 0xFF776F66),
    source = audioSource,
    trackNumber = trackNumber,
    discNumber = discNumber,
    artworkBytes = artworkBytes,
)
```

- [ ] **Step 8: Update InMemoryLibraryRepository**

The `tracks()` method at line 48-50 and `upsertTrack` materialize `LibraryTrack`, but these are data-class copies — they should work. `tracks()` sort currently uses `compareBy { it.title.lowercase() }.thenBy { it.artist.lowercase() }` — keep as-is for now (Task 2 handles ordering).

- [ ] **Step 9: Fix test compilation**

Update test files that construct `LibraryTrack`, `Track`, or `AudioMetadata`:
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryModelsTest.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryScannerTest.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryRepositoryContractTest.kt`
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepositoryJvmTest.kt`

Each test file constructs test instances. Add `trackNumber = null, discNumber = null, artworkBytes = null, artworkMimeType = null` to every construction. Use a helper or default parameter values to minimize churn.

- [ ] **Step 10: Run verification**

```bash
./gradlew :shared:jvmTest --configuration-cache
```

Expected: All tests pass.

- [ ] **Step 11: Commit**

```bash
git add -A
git commit -m "feat: add track number, disc number, and artwork to data models and persistence"
```

---

### Task 2: Order album/artist tracks by track number

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryBrowser.kt` (ordering in all 4 grouping functions)

**Interfaces:**
- Consumes: `Track.trackNumber: Int?`, `Track.discNumber: Int?` (from Task 1)
- Produces: ordered track lists within `AlbumGroup` and `ArtistGroup`

- [ ] **Step 1: Change track ordering in groupByAlbum**

In `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryBrowser.kt`, line 26, change:

```kotlin
tracks = trackList.sortedBy { it.title.lowercase() }.map { it.toUiTrack() },
```

to:

```kotlin
tracks = trackList.sortedWith(
    compareBy<LibraryTrack> { it.discNumber ?: 0 }
        .thenBy { it.trackNumber ?: 0 }
        .thenBy { it.title.lowercase() }
).map { it.toUiTrack() },
```

- [ ] **Step 2: Change track ordering in groupByArtist**

Line 37:

```kotlin
tracks = trackList.sortedBy { it.title.lowercase() }.map { it.toUiTrack() },
```

to:

```kotlin
tracks = trackList.sortedWith(
    compareBy<LibraryTrack> { it.discNumber ?: 0 }
        .thenBy { it.trackNumber ?: 0 }
        .thenBy { it.title.lowercase() }
).map { it.toUiTrack() },
```

- [ ] **Step 3: Change track ordering in groupTracksByAlbum**

Line 49:

```kotlin
tracks = trackList.sortedBy { it.title.lowercase() },
```

to:

```kotlin
tracks = trackList.sortedWith(
    compareBy<Track> { it.discNumber ?: 0 }
        .thenBy { it.trackNumber ?: 0 }
        .thenBy { it.title.lowercase() }
),
```

- [ ] **Step 4: Change track ordering in groupTracksByArtist**

Line 60:

```kotlin
tracks = trackList.sortedBy { it.title.lowercase() },
```

to:

```kotlin
tracks = trackList.sortedWith(
    compareBy<Track> { it.discNumber ?: 0 }
        .thenBy { it.trackNumber ?: 0 }
        .thenBy { it.title.lowercase() }
),
```

- [ ] **Step 5: Run verification**

```bash
./gradlew :shared:jvmTest --configuration-cache
```

Expected: All tests pass.

- [ ] **Step 6: Commit**

```bash
git add LibraryBrowser.kt
git commit -m "feat: order album/artist tracks by disc and track number"
```

---

### Task 3: Show artwork images instead of text placeholders

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` (AlbumMark, AlbumCard, ArtistRow composables)

**Interfaces:**
- Consumes: `Track.artworkBytes: ByteArray?` (from Task 1), `ByteArray.decodeArtwork(): ImageBitmap?` (existing expect/actual)
- Produces: artwork-aware composables that fall back to text placeholders when no artwork

- [ ] **Step 1: Update AlbumMark to show artwork when available**

In `App.kt`, replace the `AlbumMark` composable (lines 899-928):

```kotlin
@Composable
private fun AlbumMark(track: Track, selected: Boolean) {
    val artworkBitmap = remember(track.artworkBytes) {
        track.artworkBytes?.decodeArtwork()
    }
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(track.accent.start), Color(track.accent.end)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f)),
            )
        } else if (artworkBitmap != null) {
            Image(
                bitmap = artworkBitmap,
                contentDescription = "Album art",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = track.title.firstOrNull()?.uppercase() ?: "♪",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
            )
        }
    }
}
```

Needs import: `import androidx.compose.ui.layout.ContentScale`

- [ ] **Step 2: Update AlbumCard to show artwork**

In `App.kt`, replace the inner Box of `AlbumCard` (lines 1088-1106). Add artwork extraction from the first track:

```kotlin
val albumArtwork = remember(album.tracks) {
    album.tracks.firstNotNullOfOrNull { it.artworkBytes?.decodeArtwork() }
}
Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(120.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(
            Brush.linearGradient(
                listOf(HausInk, HausPulse),
            ),
        ),
    contentAlignment = Alignment.Center,
) {
    if (albumArtwork != null) {
        Image(
            bitmap = albumArtwork,
            contentDescription = "Album artwork",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    } else {
        Text(
            text = album.album.take(2).uppercase(),
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
        )
    }
}
```

- [ ] **Step 3: Update ArtistRow to show artwork**

In `App.kt`, replace the inner Box of `ArtistRow` (lines 1144-1161). Extract artwork from the first track that has it:

```kotlin
val artistArtwork = remember(artist.tracks) {
    artist.tracks.firstNotNullOfOrNull { it.artworkBytes?.decodeArtwork() }
}
Box(
    modifier = Modifier
        .size(54.dp)
        .clip(CircleShape)
        .background(
            Brush.linearGradient(
                listOf(HausInk, HausPulse),
            ),
        ),
    contentAlignment = Alignment.Center,
) {
    if (artistArtwork != null) {
        Image(
            bitmap = artistArtwork,
            contentDescription = "Artist artwork",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    } else {
        Text(
            text = artist.artist.firstOrNull()?.uppercase() ?: "♪",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
        )
    }
}
```

- [ ] **Step 4: Run verification**

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Expected: BUILD SUCCESSFUL. (iOS artwork decode returns null, so it won't show images on iOS simulator but won't crash.)

- [ ] **Step 5: Commit**

```bash
git add App.kt
git commit -m "feat: display album/track/artist artwork images instead of text placeholders"
```

---

### Task 4: Create NowPlayingBar floating bar composable

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`

**Interfaces:**
- Consumes: `Track`, `PlaybackState`, `PlaybackController`
- Produces: `NowPlayingBar` composable — a slim floating bar with artwork, track info, play/pause button, and mini progress indicator; clickable to expand

- [ ] **Step 1: Create NowPlayingBar.kt with the floating bar composable**

```kotlin
package com.eterocell.rhythhaus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NowPlayingBar(
    track: Track,
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brush = Brush.linearGradient(
        colors = listOf(Color(track.accent.start), Color(track.accent.end)),
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset.Infinite,
    )
    val artworkBitmap = remember(track.artworkBytes) {
        track.artworkBytes?.decodeArtwork()
    }
    val progressFraction = playbackState.progressFraction
    val isPlaying = playbackState.isPlaying

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onExpand),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 8.dp,
        color = HausPanel,
    ) {
        Column {
            // Mini progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(HausLine),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .fillMaxHeight()
                        .background(HausPulse),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Artwork / fallback
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(track.accent.start), Color(track.accent.end)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (artworkBitmap != null) {
                        Image(
                            bitmap = artworkBitmap,
                            contentDescription = "Album art",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text(
                            text = track.title.firstOrNull()?.uppercase() ?: "♪",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                        )
                    }
                }

                // Track info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = HausInk,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${track.artist} · ${track.album}",
                        color = HausMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Play/pause button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(HausInk)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isPlaying) "⏸" else "▶",
                        color = HausPaper,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Fix any import/API issues.

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt
git commit -m "feat: add NowPlayingBar floating bar composable"
```

---

### Task 5: Create NowPlayingScreen expanded screen

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`

**Interfaces:**
- Consumes: `Track`, `PlaybackState`, `PlaybackController`
- Produces: `NowPlayingScreen` composable — full-screen view with large artwork, track metadata, seek bar, and transport controls (prev/play-pause/next + stop)

- [ ] **Step 1: Create NowPlayingScreen.kt**

```kotlin
package com.eterocell.rhythhaus

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NowPlayingScreen(
    track: Track,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brush = Brush.linearGradient(
        colors = listOf(Color(track.accent.start), Color(track.accent.end)),
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset.Infinite,
    )
    val durationMillis = playbackState.durationMillis ?: track.durationSeconds * 1_000L
    val positionMillis = playbackState.positionMillis.coerceIn(0L, durationMillis)
    val progressFraction = if (durationMillis > 0) positionMillis.toFloat() / durationMillis else 0f
    val statusText = playbackState.error?.message ?: playbackState.status.label
    val artworkBitmap = remember(track.artworkBytes) {
        track.artworkBytes?.decodeArtwork()
    }
    val isPlaying = playbackState.isPlaying

    Surface(modifier = modifier.fillMaxSize(), color = HausPaper) {
        Column(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            // Back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(HausInk)
                        .clickable(onClick = onBack)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "← LIBRARY",
                        color = HausPaper,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.8.sp,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Large artwork
            Card(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 32.dp,
                colors = CardDefaults.cardColors(containerColor = HausInk),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(brush),
                    contentAlignment = Alignment.Center,
                ) {
                    if (artworkBitmap != null) {
                        Image(
                            bitmap = artworkBitmap,
                            contentDescription = "Album artwork",
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

            Spacer(Modifier.height(18.dp))

            // Track info
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = track.title,
                    color = HausInk,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${track.artist} · ${track.album}",
                    color = HausMuted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (track.trackNumber != null) {
                    Text(
                        text = "Track ${track.trackNumber}",
                        color = HausMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Status
            Text(
                text = statusText,
                color = HausMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )

            Spacer(Modifier.height(12.dp))

            // Seek bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Slider(
                    value = progressFraction,
                    onValueChange = { fraction ->
                        playbackController.seekTo((durationMillis * fraction).toLong())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = HausPulse,
                        activeTrackColor = HausPulse,
                        inactiveTrackColor = HausLine,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatMillis(positionMillis),
                        color = HausMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = formatMillis(durationMillis),
                        color = HausMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Transport controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
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

                // Play/Pause (large, highlighted)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(HausPulse)
                        .clickable { playbackController.togglePlayPause() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isPlaying) "⏸" else "▶",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                    )
                }

                // Next track
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(HausPanel)
                        .clickable {
                            val queue = playbackState.queue
                            val currentId = playbackState.currentTrack?.id
                            val currentIndex = queue.indexOfFirst { it.id == currentId }
                            val nextTrack = queue.getOrNull(currentIndex + 1) ?: queue.firstOrNull()
                            nextTrack?.let { playbackController.selectTrack(it.id, autoPlay = true) }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "⏭", color = HausInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Fix import/API issues.

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt
git commit -m "feat: add NowPlayingScreen expanded view with artwork and controls"
```

---

### Task 6: Wire NowPlayingBar and NowPlayingScreen into LibraryHomeScreen

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` (LibraryHomeScreen, DrillDownView)

**Interfaces:**
- Consumes: `NowPlayingBar`, `NowPlayingScreen` (from Tasks 4-5)
- Produces: refactored `LibraryHomeScreen` with floating bar overlay; `DrillDownView` also uses floating bar

- [ ] **Step 1: Refactor LibraryHomeScreen to use NowPlayingBar overlay and NowPlayingScreen expansion**

Replace the inline `NowPlayingCard` usage (lines 247-265) in `LibraryHomeScreen` with a floating bar overlay and expansion state. The `LibraryHomeScreen` needs a new state variable `showNowPlayingScreen: Boolean`.

At the top of `LibraryHomeScreen`, add:

```kotlin
var showNowPlayingScreen by remember { mutableStateOf(false) }
```

If `showNowPlayingScreen` is true, show `NowPlayingScreen` instead of the normal content. Otherwise, show the normal library content with `NowPlayingBar` overlaid at the bottom.

Remove the `NowPlayingCard` item from the `LazyColumn` (lines 247-265). Replace the outer `Surface` with a `Box` to allow the floating bar overlay:

```kotlin
if (showNowPlayingScreen && selectedTrack != null) {
    NowPlayingScreen(
        track = selectedTrack,
        playbackState = playbackState,
        playbackController = playbackController,
        onBack = { showNowPlayingScreen = false },
    )
} else {
    Box(modifier = modifier.fillMaxSize()) {
        // Existing library content (unchanged except remove NowPlayingCard item)
        Surface(modifier = Modifier.fillMaxSize(), color = HausPaper) {
            LazyColumn(
                // ... same content, but remove the item { NowPlayingCard(...) } block
            )
        }

        // Floating NowPlayingBar at the bottom
        if (selectedTrack != null) {
            NowPlayingBar(
                track = selectedTrack,
                playbackState = playbackState,
                onPlayPause = {
                    val playableTracks = snapshot.tracks.map { it.toPlayableTrack() }
                    if (playbackState.currentTrack?.id != selectedTrack.id || playbackState.status == PlaybackStatus.Idle) {
                        playbackController.setQueue(playableTracks, selectedTrack.id)
                    }
                    playbackController.togglePlayPause()
                },
                onExpand = { showNowPlayingScreen = true },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
```

- [ ] **Step 2: Update DrillDownView similarly**

In `DrillDownView` (lines 930-990), remove the `NowPlayingCard` item and add the floating bar. Add `showNowPlayingScreen` state. Wrap the content:

```kotlin
@Composable
private fun DrillDownView(
    title: String,
    subtitle: String,
    tracks: List<Track>,
    selectedTrack: Track?,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    onBack: () -> Unit,
    onTrackSelected: (String) -> Unit,
    onPlayPause: (Track) -> Unit,
) {
    var selectedTrackId by remember { mutableStateOf(selectedTrack?.id) }
    var showNowPlayingScreen by remember { mutableStateOf(false) }

    if (showNowPlayingScreen && selectedTrack != null) {
        NowPlayingScreen(
            track = selectedTrack,
            playbackState = playbackState,
            playbackController = playbackController,
            onBack = { showNowPlayingScreen = false },
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(modifier = Modifier.fillMaxSize(), color = HausPaper) {
                LazyColumn(
                    // ... same but remove the item { NowPlayingCard(...) } block
                )
            }

            // Floating bar
            if (selectedTrack != null) {
                NowPlayingBar(
                    track = selectedTrack,
                    playbackState = playbackState,
                    onPlayPause = { onPlayPause(selectedTrack) },
                    onExpand = { showNowPlayingScreen = true },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}
```

- [ ] **Step 3: Run full verification**

```bash
./init.sh
```

Expected: BUILD SUCCESSFUL across all platforms.

- [ ] **Step 4: Commit**

```bash
git add App.kt
git commit -m "feat: wire NowPlayingBar floating bar and NowPlayingScreen into LibraryHomeScreen"
```

---

## Completion checklist

- [ ] Task 1: Data models extended with trackNumber, discNumber, artworkBytes — all tests pass
- [ ] Task 2: Album/artist tracks ordered by disc+track number — all tests pass
- [ ] Task 3: AlbumMark, AlbumCard, ArtistRow show artwork images with text fallback
- [ ] Task 4: NowPlayingBar floating bar composable created and compiles
- [ ] Task 5: NowPlayingScreen expanded view created and compiles
- [ ] Task 6: LibraryHomeScreen and DrillDownView wired with floating bar + expansion
- [ ] `./init.sh` passes on all platforms
- [ ] `progress.md` updated with handoff evidence

## Risks and notes

- **SQL schema migration**: SQLDelight with `CREATE TABLE IF NOT EXISTS` won't add columns to existing tables on app reinstall. For development, delete the app data or uninstall/reinstall. A future migration system is out of scope.
- **iOS artwork**: `decodeArtwork()` returns `null` on iOS. The UI falls back to text placeholders gracefully, but iOS won't show artwork images until the UIKit bridge is implemented.
- **InMemoryLibraryRepository tracks() sort**: Keep alphabetical for now, but note that the SQLDelight repo also uses alphabetical ordering — a follow-up task could change default ordering to track number there too.
