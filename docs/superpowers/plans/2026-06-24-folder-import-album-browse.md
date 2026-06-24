# Folder Import + Album/Artist Browse Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace single-file audio import with folder-based scanning, add album/artist two-level browsing, activate iOS app-local folder scanner

**Architecture:** Create SQLDelight schema and `SqlDelightLibraryRepository`, wire existing `PlatformFolderPickerLauncher` + `LibraryScanner` into UI, compute album/artist groupings from persisted `LibraryTrack` rows, replace flat track list with two-level drill-down navigation

**Tech Stack:** Kotlin 2.4.0, Compose Multiplatform 1.11.1, Miuix UI, SQLDelight 2.3.2, TagLib

## Global Constraints

- Kotlin 2.4.0, Compose Multiplatform 1.11.1, SQLDelight 2.3.2
- Shared-first: all domain logic and UI in `shared/src/commonMain`
- Platform seams via `expect`/`actual` in `androidMain`, `iosMain`, `jvmMain`
- iOS uses app-local folder (Documents/RhythHaus Music) — no document picker
- Existing `LibraryScanner`, `PlatformFolderPickerLauncher`, `PlatformAudioScanner` already built
- `InMemoryLibraryRepository` exists as reference — new `SqlDelightLibraryRepository` replaces it for production
- `./init.sh` must pass all platforms
- YAGNI: no dark mode, no Apple Music, no Windows/Linux, no remote sources

---

### Task 1: Database schema and SqlDelightLibraryRepository

**Files:**
- Create: `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/LibrarySource.sq`
- Create: `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/LibraryTrack.sq`
- Create: `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/ScanSession.sq`
- Create: `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/ScanError.sq`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.kt`
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepository.kt`
- Modify: `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.jvm.kt`
- Modify: `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.android.kt`
- Modify: `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.ios.kt`

**Produces:** SQLDelight-generated DAOs, `SqlDelightLibraryRepository` implementing `LibraryRepository` with real persistence

- [ ] **Step 1: Check existing RhythHausDatabase schema file**

Read `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/RhythHausDatabase.sq` to understand current schema state. If it exists, note the current contents. If not, create it.

- [ ] **Step 2: Create LibrarySource.sq**

```sql
CREATE TABLE IF NOT EXISTS librarySource (
    id TEXT NOT NULL PRIMARY KEY,
    platformKind TEXT NOT NULL,
    displayName TEXT NOT NULL,
    handle TEXT NOT NULL,
    createdAtEpochMillis INTEGER NOT NULL DEFAULT 0,
    lastScanAtEpochMillis INTEGER,
    accessStatus TEXT NOT NULL DEFAULT 'Available'
);

upsertSource:
INSERT OR REPLACE INTO librarySource(id, platformKind, displayName, handle, createdAtEpochMillis, lastScanAtEpochMillis, accessStatus)
VALUES (?, ?, ?, ?, ?, ?, ?);

selectAllSources:
SELECT * FROM librarySource;
```

- [ ] **Step 3: Create LibraryTrack.sq**

```sql
CREATE TABLE IF NOT EXISTS libraryTrack (
    id TEXT NOT NULL PRIMARY KEY,
    sourceId TEXT NOT NULL,
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
    updatedAtEpochMillis INTEGER NOT NULL
);

upsertTrack:
INSERT OR REPLACE INTO libraryTrack(id, sourceId, sourceLocalKey, audioSourceKind, audioSourceValue,
    displayName, title, artist, album, durationMillis, sizeBytes, modifiedAtEpochMillis,
    lastSeenScanId, createdAtEpochMillis, updatedAtEpochMillis)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

selectAllTracks:
SELECT * FROM libraryTrack ORDER BY title COLLATE NOCASE ASC;

selectTracksForSource:
SELECT * FROM libraryTrack WHERE sourceId = ? ORDER BY title COLLATE NOCASE ASC;

removeMissingTracks:
DELETE FROM libraryTrack WHERE sourceId = ? AND lastSeenScanId != ?;
```

- [ ] **Step 4: Create ScanSession.sq**

```sql
CREATE TABLE IF NOT EXISTS scanSession (
    id TEXT NOT NULL PRIMARY KEY,
    sourceId TEXT NOT NULL,
    status TEXT NOT NULL,
    startedAtEpochMillis INTEGER NOT NULL,
    completedAtEpochMillis INTEGER,
    foldersVisited INTEGER NOT NULL DEFAULT 0,
    filesVisited INTEGER NOT NULL DEFAULT 0,
    tracksAdded INTEGER NOT NULL DEFAULT 0,
    tracksUpdated INTEGER NOT NULL DEFAULT 0,
    filesSkipped INTEGER NOT NULL DEFAULT 0,
    terminalMessage TEXT
);

insertScanSession:
INSERT INTO scanSession(id, sourceId, status, startedAtEpochMillis, completedAtEpochMillis,
    foldersVisited, filesVisited, tracksAdded, tracksUpdated, filesSkipped, terminalMessage)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

updateScanSession:
UPDATE scanSession SET status = ?, completedAtEpochMillis = ?, foldersVisited = ?,
    filesVisited = ?, tracksAdded = ?, tracksUpdated = ?, filesSkipped = ?, terminalMessage = ?
WHERE id = ?;

selectScanErrorsForScan:
SELECT * FROM scanError WHERE scanId = ?;
```

- [ ] **Step 5: Create ScanError.sq**

```sql
CREATE TABLE IF NOT EXISTS scanError (
    id TEXT NOT NULL PRIMARY KEY,
    scanId TEXT NOT NULL,
    sourceLocalKey TEXT NOT NULL,
    displayPath TEXT NOT NULL,
    reason TEXT NOT NULL,
    recoverable INTEGER NOT NULL DEFAULT 1,
    createdAtEpochMillis INTEGER NOT NULL
);

insertScanError:
INSERT INTO scanError(id, scanId, sourceLocalKey, displayPath, reason, recoverable, createdAtEpochMillis)
VALUES (?, ?, ?, ?, ?, ?, ?);
```

- [ ] **Step 6: Update LibraryDatabase expect declaration**

The `expect class LibraryDatabase` should wrap a SQLDelight `RhythHausDatabase`. Verify the current file at `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.kt` and update the expect to expose the SQLDelight database handle.

```kotlin
package com.eterocell.rhythhaus.library

import app.cash.sqldelight.db.SqlDriver

expect class LibraryDatabase {
    val driver: SqlDriver
    val rhythmicHausDatabaseQueries: RhythHausDatabaseQueries
}
```

- [ ] **Step 7: Update platform actuals**

In `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.jvm.kt`, ensure the JVM actual supplies a `JdbcSqliteDriver`. In the Android actual, use `AndroidSqliteDriver`. In the iOS actual, use `NativeSqliteDriver`.

- [ ] **Step 8: Create SqlDelightLibraryRepository**

Create `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepository.kt` implementing `LibraryRepository` using SQLDelight queries:

```kotlin
package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource

class SqlDelightLibraryRepository(
    private val database: LibraryDatabase,
) : LibraryRepository {
    override fun upsertSource(source: LibrarySource) {
        database.rhythmicHausDatabaseQueries.upsertSource(
            id = source.id,
            platformKind = source.platformKind.name,
            displayName = source.displayName,
            handle = source.handle,
            createdAtEpochMillis = source.createdAtEpochMillis,
            lastScanAtEpochMillis = source.lastScanAtEpochMillis,
            accessStatus = source.accessStatus.name,
        )
    }

    override fun sources(): List<LibrarySource> =
        database.rhythmicHausDatabaseQueries.selectAllSources().executeAsList().map { it.toDomain() }

    override fun upsertTrack(track: LibraryTrack): TrackUpsertResult {
        val existing = database.rhythmicHausDatabaseQueries.selectAllTracks().executeAsList()
            .firstOrNull { it.sourceId == track.sourceId && it.sourceLocalKey == track.sourceLocalKey }
        val target = if (existing != null) track.copy(id = existing.id, createdAtEpochMillis = existing.createdAtEpochMillis) else track
        val audioSource = target.audioSource
        database.rhythmicHausDatabaseQueries.upsertTrack(
            id = target.id,
            sourceId = target.sourceId,
            sourceLocalKey = target.sourceLocalKey,
            audioSourceKind = when (audioSource) { is AudioSource.FilePath -> "FilePath"; is AudioSource.Uri -> "Uri" },
            audioSourceValue = when (audioSource) { is AudioSource.FilePath -> audioSource.path; is AudioSource.Uri -> audioSource.value },
            displayName = target.displayName,
            title = target.title,
            artist = target.artist,
            album = target.album,
            durationMillis = target.durationMillis,
            sizeBytes = target.sizeBytes,
            modifiedAtEpochMillis = target.modifiedAtEpochMillis,
            lastSeenScanId = target.lastSeenScanId,
            createdAtEpochMillis = target.createdAtEpochMillis,
            updatedAtEpochMillis = target.updatedAtEpochMillis,
        )
        return if (existing == null) TrackUpsertResult.Added else TrackUpsertResult.Updated
    }

    override fun tracks(): List<LibraryTrack> =
        database.rhythmicHausDatabaseQueries.selectAllTracks().executeAsList().map { it.toDomain() }

    override fun tracksForSource(sourceId: String): List<LibraryTrack> =
        database.rhythmicHausDatabaseQueries.selectTracksForSource(sourceId).executeAsList().map { it.toDomain() }

    override fun insertScanSession(session: ScanSession) {
        database.rhythmicHausDatabaseQueries.insertScanSession(
            id = session.id,
            sourceId = session.sourceId,
            status = session.status.name,
            startedAtEpochMillis = session.startedAtEpochMillis,
            completedAtEpochMillis = session.completedAtEpochMillis,
            foldersVisited = session.foldersVisited.toLong(),
            filesVisited = session.filesVisited.toLong(),
            tracksAdded = session.tracksAdded.toLong(),
            tracksUpdated = session.tracksUpdated.toLong(),
            filesSkipped = session.filesSkipped.toLong(),
            terminalMessage = session.terminalMessage,
        )
    }

    override fun updateScanSession(session: ScanSession) {
        database.rhythmicHausDatabaseQueries.updateScanSession(
            status = session.status.name,
            completedAtEpochMillis = session.completedAtEpochMillis,
            foldersVisited = session.foldersVisited.toLong(),
            filesVisited = session.filesVisited.toLong(),
            tracksAdded = session.tracksAdded.toLong(),
            tracksUpdated = session.tracksUpdated.toLong(),
            filesSkipped = session.filesSkipped.toLong(),
            terminalMessage = session.terminalMessage,
            id = session.id,
        )
    }

    override fun insertScanError(error: ScanError) {
        database.rhythmicHausDatabaseQueries.insertScanError(
            id = error.id,
            scanId = error.scanId,
            sourceLocalKey = error.sourceLocalKey,
            displayPath = error.displayPath,
            reason = error.reason,
            recoverable = if (error.recoverable) 1L else 0L,
            createdAtEpochMillis = error.createdAtEpochMillis,
        )
    }

    override fun scanErrors(scanId: String): List<ScanError> =
        database.rhythmicHausDatabaseQueries.selectScanErrorsForScan(scanId).executeAsList().map { it.toDomain() }

    override fun removeMissingTracks(sourceId: String, latestScanId: String): Int {
        database.rhythmicHausDatabaseQueries.removeMissingTracks(sourceId, latestScanId)
        return 0 // SQLDelight doesn't return affected rows easily; acceptable for now
    }
}
```

Add the `.toDomain()` extension functions in the same file to map SQLDelight row types to domain `LibrarySource`, `LibraryTrack`, `ScanSession`, `ScanError`.

- [ ] **Step 9: Verify SQLDelight code generation**

```bash
./gradlew :shared:generateCommonMainRhythHausDatabaseInterface --configuration-cache
```

Expected: SUCCESS — SQLDelight generates `RhythHausDatabase`, DAOs, and row types.

- [ ] **Step 10: Run existing repository contract tests against SqlDelight**

```bash
./gradlew :shared:jvmTest --tests "com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest" --configuration-cache
```

Expected: Tests pass using `SqlDelightLibraryRepository` with an in-memory SQLite driver.

- [ ] **Step 11: Commit**

```bash
git add shared/src/commonMain/sqldelight/ shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepository.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.kt shared/src/*/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.*.kt
git commit -m "feat: add SQLDelight schema and SqlDelightLibraryRepository"
```

---

### Task 2: Wire folder picker into UI

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`

**Consumes:** `PlatformFolderPickerLauncher`, `LibraryScanner`, `SqlDelightLibraryRepository` from Task 1

**Produces:** "Add music folder" button replaces "Import local audio", folder picker launches on click

- [ ] **Step 1: Replace AudioImportLauncher with PlatformFolderPickerLauncher in App.kt**

In `App()`, replace `rememberAudioImportLauncher` with `rememberPlatformFolderPickerLauncher`:

```kotlin
val folderPickerLauncher = rememberPlatformFolderPickerLauncher { result ->
    when (result) {
        is PlatformFolderPickResult.Success -> {
            importMessage = "Added: ${result.source.displayName}. Scanning..."
            // Scanner wiring in Task 3
            importMessage = "Imported source: ${result.source.displayName}"
        }
        is PlatformFolderPickResult.Unavailable -> importMessage = result.message
        is PlatformFolderPickResult.Failure -> importMessage = result.cause?.let { "${result.message}: $it" } ?: result.message
    }
}
```

Replace the old `importLauncher` variable and its usage in `ImportAudioCard` with `folderPickerLauncher`.

- [ ] **Step 2: Update ImportAudioCard composable**

Change the card copy from "Import local audio" / "Add more local audio" to "Add music folder" / "Manage music folders". Update the `Text` composable descriptions accordingly.

Replace `importLauncher::launch` with `folderPickerLauncher::launch` in the `Button`'s `onClick`.

- [ ] **Step 3: Test UI compiles**

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: SUCCESS — no compile errors from the folder picker wiring.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
git commit -m "feat: wire PlatformFolderPickerLauncher into UI"
```

---

### Task 3: Wire scanner and replace importedFiles with repository

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`

**Consumes:** `SqlDelightLibraryRepository` (Task 1), `PlatformFolderPickerLauncher` (Task 2), `LibraryScanner` (existing)

**Produces:** Scanner runs on folder pick success, tracks persisted, UI refreshes from repository

- [ ] **Step 1: Initialize repository and scanner in App()**

```kotlin
val libraryDb = remember { createLibraryDatabase() } // expect fun from LibraryDatabase
val repository = remember { SqlDelightLibraryRepository(libraryDb) }
val platformAccess = remember { createPlatformSourceAccess() } // expect fun wrapping PlatformSourceAccess + PlatformAudioScanner
val scanner = remember { LibraryScanner(
    repository = repository,
    platformScanner = platformAccess as PlatformAudioScanner,
    metadataReader = metadataReader,
    now = { currentTimeMillis() },
    idFactory = { uuid4().toString() },
) }
```

Note: `createPlatformSourceAccess()` and `currentTimeMillis()` / `uuid4()` need expect/actual declarations. For JVM/Android, `currentTimeMillis() = System.currentTimeMillis()`. For iOS, use `platform.Foundation.NSDate`. For `uuid4()`, use `kotlin.uuid.Uuid.random().toString()` (Kotlin 2.4.0+) or a simple expect.

- [ ] **Step 2: Trigger scanner on folder pick success**

In the `folderPickerLauncher` callback:

```kotlin
is PlatformFolderPickResult.Success -> {
    val source = result.source
    val session = scanner.scan(source)
    importMessage = "Scan complete: ${session.tracksAdded} new, ${session.tracksUpdated} updated"
    // Reload tracks from repository
    val scannedTracks = repository.tracks()
    // Update UI state...
}
```

- [ ] **Step 3: Replace importedFiles state with repository-backed state**

Replace `var importedFiles by remember { mutableStateOf(emptyList<ImportedAudioFile>()) }` with:

```kotlin
var libraryTracks by remember { mutableStateOf(repository.tracks()) }
```

Remove `enrichImportedAudioFiles`, `mergeImportedFiles`, `importedLibrarySnapshot`. Build `LibrarySnapshot` directly from `libraryTracks`.

- [ ] **Step 4: Test full scanner pipeline**

```bash
./gradlew :shared:jvmTest --tests "*LibraryScannerTest" --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: Scanner tests pass, compilation succeeds.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
git commit -m "feat: wire LibraryScanner into UI, replace importedFiles with repository"
```

---

### Task 4: Add album/artist grouping and two-level browsing

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryBrowser.kt`

**Consumes:** `repository.tracks()` from Task 3

**Produces:** Segmented picker (Albums/Artists), album grid, artist list, drill-down track list

- [ ] **Step 1: Create LibraryBrowser.kt with grouping functions**

```kotlin
package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.LibraryTrack

enum class BrowseMode { Albums, Artists }

data class AlbumGroup(val album: String, val tracks: List<LibraryTrack>, val artist: String? = tracks.firstOrNull()?.artist)

data class ArtistGroup(val artist: String, val tracks: List<LibraryTrack>, val albumCount: Int = tracks.map { it.album }.distinct().size)

fun groupByAlbum(tracks: List<LibraryTrack>): List<AlbumGroup> =
    tracks.groupBy { it.album }
        .map { (album, tracks) -> AlbumGroup(album, tracks.sortedBy { it.title.lowercase() }) }
        .sortedBy { it.album.lowercase() }

fun groupByArtist(tracks: List<LibraryTrack>): List<ArtistGroup> =
    tracks.groupBy { it.artist }
        .map { (artist, tracks) -> ArtistGroup(artist, tracks.sortedBy { it.title.lowercase() }) }
        .sortedBy { it.artist.lowercase() }
```

- [ ] **Step 2: Add BrowseMode state and segmented picker to LibraryHomeScreen**

```kotlin
var browseMode by remember { mutableStateOf(BrowseMode.Albums) }
```

Add a Miuix segmented control or two side-by-side buttons at the top of the track list area:

```kotlin
Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
) {
    BrowseMode.entries.forEach { mode ->
        FilterChip(
            selected = browseMode == mode,
            onClick = { browseMode = mode },
            label = { Text(mode.name) },
        )
    }
}
```

- [ ] **Step 3: Implement album grid view**

When `browseMode == BrowseMode.Albums`, show a `LazyVerticalGrid` of album cards:

```kotlin
val albums = remember(libraryTracks) { groupByAlbum(libraryTracks) }
LazyVerticalGrid(columns = GridCells.Fixed(2), ...) {
    items(albums) { album ->
        AlbumCard(album, onClick = { selectedAlbum = album })
    }
}
```

Each `AlbumCard` shows the album name, artist, track count, and an accent-colored background square.

- [ ] **Step 4: Implement artist list view**

When `browseMode == BrowseMode.Artists`, show a `LazyColumn` of artist items:

```kotlin
val artists = remember(libraryTracks) { groupByArtist(libraryTracks) }
LazyColumn {
    items(artists) { artist ->
        ArtistRow(artist, onClick = { selectedArtist = artist })
    }
}
```

- [ ] **Step 5: Implement drill-down track list**

When an album or artist is selected, show a filtered `LazyColumn` of tracks:

```kotlin
if (selectedAlbum != null) {
    TrackListScreen(
        title = selectedAlbum!!.album,
        tracks = selectedAlbum!!.tracks,
        onBack = { selectedAlbum = null },
        onTrackSelected = { track -> ... },
    )
}
```

The track list reuses the existing `NowPlayingCard` at the bottom.

- [ ] **Step 6: Wire playback from drill-down**

When a track is selected from album/artist view, update the `selectedTrackId` and current track list to the filtered tracks.

- [ ] **Step 7: Test compilation**

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: SUCCESS.

- [ ] **Step 8: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryBrowser.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
git commit -m "feat: add album/artist grouping and two-level browsing"
```

---

### Task 5: Activate iOS import

**Files:**
- Modify: `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/AudioImport.ios.kt`
- Modify: `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/AudioImport.ios.kt`

**Consumes:** `PlatformFolderPickerLauncher` from iOS (already implemented in `PlatformSourceAccess.ios.kt`)

**Produces:** iOS app no longer shows "Unavailable" — folder scanner works

- [ ] **Step 1: Replace iOS AudioImportLauncher stub**

In `AudioImport.ios.kt`, the old `rememberAudioImportLauncher` returns `isAvailable = false`. Replace it to delegate to the folder picker:

```kotlin
@Composable
actual fun rememberAudioImportLauncher(onResult: (AudioImportResult) -> Unit): AudioImportLauncher {
    val folderPicker = rememberPlatformFolderPickerLauncher { pickResult ->
        // Delegate folder pick results through the old AudioImportResult interface
        // for backward compatibility during transition
    }
    // ... forward calls
}
```

Actually — since we're replacing the old launcher entirely (Task 2), the iOS `AudioImportLauncher` stub can simply be deleted. The `rememberPlatformFolderPickerLauncher` in `PlatformSourceAccess.ios.kt` already works.

- [ ] **Step 2: Verify iOS app-local scanner compiles**

```bash
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

Expected: SUCCESS — iOS tests pass, scanner module compiles.

- [ ] **Step 3: Commit**

```bash
git add shared/src/iosMain/kotlin/com/eterocell/rhythhaus/AudioImport.ios.kt
git commit -m "feat: activate iOS app-local folder import"
```

---

### Task 6: Clean up old import path

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AudioImport.kt`
- Modify: `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/AudioImport.android.kt`
- Modify: `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/AudioImport.jvm.kt`
- Modify: `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/AudioImport.ios.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/SharedCommonTest.kt`

**Produces:** Old `AudioImportResult`, `AudioImportLauncher`, `ImportedAudioFile` removed. `rememberAudioImportLauncher` deleted from all platforms.

- [ ] **Step 1: Remove AudioImport types from AudioImport.kt**

Remove `AudioImportResult`, `ImportedAudioFile`, `AudioImportLauncher`, `rememberAudioImportLauncher` (expect), `importedLibrarySnapshot`, `toTrack`, `toDisplayTitle`, `importedAccent`, `stableHash` from `AudioImport.kt`. Keep `mergeImportedFiles` if still used during transition, otherwise remove.

- [ ] **Step 2: Remove platform AudioImport actuals**

Delete or empty out `AudioImport.android.kt`, `AudioImport.jvm.kt`, `AudioImport.ios.kt`.

- [ ] **Step 3: Update tests**

Update `SharedCommonTest.kt` and any test files that reference `ImportedAudioFile`, `enrichImportedAudioFiles`, `importedLibrarySnapshot`. Replace with `LibraryTrack`-based test helpers.

- [ ] **Step 4: Run full test suite**

```bash
./gradlew :shared:jvmTest --configuration-cache
```

Expected: SUCCESS — all tests pass with old types removed.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: remove old single-file import path"
```

---

### Task 7: Full harness verification

**Files:**
- Modify: `progress.md`
- Modify: `openspec/changes/scan-local-audio-folders/tasks.md`

- [ ] **Step 1: Run full harness**

```bash
./init.sh
```

Expected: BUILD SUCCESSFUL — Android, macOS/JVM, iOS Simulator all compile and test.

- [ ] **Step 2: Update OpenSpec task status**

Update `openspec/changes/scan-local-audio-folders/tasks.md`: mark tasks 2.1-2.4, 4.4, 5.1-5.4, 6.1-6.5 as completed.

- [ ] **Step 3: Update progress.md**

Append handoff record with completed tasks, verification results, and next safe action.

- [ ] **Step 4: Commit**

```bash
git add openspec/changes/scan-local-audio-folders/tasks.md progress.md
git commit -m "chore: update OpenSpec tasks and progress for folder import"
```
