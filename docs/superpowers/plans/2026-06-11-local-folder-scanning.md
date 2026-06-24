# Local Folder Scanning Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace single/multiple audio-file import as the primary library path with recursive local music folder/source scanning across Android, macOS/JVM, and iOS, backed by a persistent shared KMP database.

**Architecture:** Shared code owns library domain models, SQLDelight persistence, scan orchestration, and Compose library-manager UI. Platform actuals only choose/provision sources and enumerate audio candidates: Android SAF tree URI, macOS/JVM native folder + filesystem traversal, and iOS app-local music folder scanning.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Kotlin coroutines/StateFlow, SQLDelight 2.3.2, Android Storage Access Framework, JVM `java.nio.file`, Kotlin/Native Foundation APIs for iOS app-local storage.

---

## Source Design Inputs

- Superpowers design: `docs/superpowers/specs/2026-06-11-select-folder-scan-audio-design.md`
- OpenSpec change: `openspec/changes/scan-local-audio-folders/`
- Existing import/playback files:
  - `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AudioImport.kt`
  - `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
  - `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt`
  - `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/AudioImport.android.kt`
  - `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/AudioImport.jvm.kt`
  - `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/AudioImport.ios.kt`

## File Structure

Create these focused files instead of growing `App.kt` further:

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryModels.kt` — source, track, scan state, scan error, audio candidate models.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SupportedAudio.kt` — supported audio extension checks shared by platform scanners.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.kt` — database driver factory expect, database construction, row mapping helpers.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt` — repository interface and SQLDelight implementation.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryScanner.kt` — shared scan orchestration, progress, cancellation, remove-missing.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryManagerUi.kt` — shared Compose manager surface extracted from `App.kt`.
- `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/RhythHausDatabase.sq` — SQLDelight schema and queries.
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlatform.android.kt` — Android driver, SAF picker/source access, recursive traversal.
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlatform.jvm.kt` — JVM driver, native folder picker, recursive filesystem traversal.
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlatform.ios.kt` — Native driver, app-local music folder source and scanner.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryRepositoryTest.kt` — repository behavior tests.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryScannerTest.kt` — scanner orchestration tests with fakes.
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/JvmLibraryScannerTest.kt` — temp-folder filesystem scanner tests.

---

### Task 1: Create the OpenSpec change and verify it

**Files:**
- Already created: `openspec/changes/scan-local-audio-folders/proposal.md`
- Already created: `openspec/changes/scan-local-audio-folders/design.md`
- Already created: `openspec/changes/scan-local-audio-folders/tasks.md`
- Already created: `openspec/changes/scan-local-audio-folders/specs/local-library-scanning/spec.md`

- [ ] **Step 1: Validate OpenSpec before code changes**

Run:

```bash
openspec validate scan-local-audio-folders --strict
```

Expected:

```text
Change 'scan-local-audio-folders' is valid
```

- [ ] **Step 2: Commit OpenSpec planning artifacts if not already committed**

Run:

```bash
git status --short
git add openspec/changes/scan-local-audio-folders docs/superpowers/specs/2026-06-11-select-folder-scan-audio-design.md
git commit -m "docs: plan local folder scanning"
```

Expected: a semantic docs commit, unless these files are already committed.

---

### Task 2: Add SQLDelight build setup

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `shared/build.gradle.kts`
- Create: `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/RhythHausDatabase.sq`

- [ ] **Step 1: Add SQLDelight aliases**

Modify `gradle/libs.versions.toml`:

```toml
[versions]
sqldelight = "2.3.2"

[libraries]
sqldelight-runtime = { module = "app.cash.sqldelight:runtime", version.ref = "sqldelight" }
sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }
sqldelight-android-driver = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
sqldelight-native-driver = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }
sqldelight-sqlite-driver = { module = "app.cash.sqldelight:sqlite-driver", version.ref = "sqldelight" }

[plugins]
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
```

Do not duplicate existing table headers; insert each line into the current matching section.

- [ ] **Step 2: Apply SQLDelight plugin and dependencies**

Modify `shared/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        create("RhythHausDatabase") {
            packageName.set("com.eterocell.rhythhaus.library")
        }
    }
}
```

Add dependencies inside existing source sets:

```kotlin
sourceSets {
    androidMain.dependencies {
        implementation(libs.compose.uiToolingPreview)
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.media3.exoplayer)
        implementation(libs.sqldelight.android.driver)
    }
    jvmMain.dependencies {
        implementation(libs.sqldelight.sqlite.driver)
    }
    commonMain.dependencies {
        implementation(projects.taglib)
        implementation(libs.compose.runtime)
        implementation(libs.compose.foundation)
        implementation(libs.compose.material3)
        implementation(libs.compose.ui)
        implementation(libs.compose.components.resources)
        implementation(libs.compose.uiToolingPreview)
        implementation(libs.androidx.lifecycle.viewmodelCompose)
        implementation(libs.androidx.lifecycle.runtimeCompose)
        implementation(libs.kotlinx.coroutinesCore)
        implementation(libs.sqldelight.runtime)
        implementation(libs.sqldelight.coroutines)
    }
    commonTest.dependencies {
        implementation(libs.kotlin.test)
    }
    iosMain.dependencies {
        implementation(libs.sqldelight.native.driver)
    }
}
```

If `jvmMain` is currently declared only as a block for resources, merge the dependency block with the existing `resources.srcDir(nativeAudioResourceRoot)` block rather than creating two `jvmMain` blocks.

- [ ] **Step 3: Add initial schema**

Create `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/RhythHausDatabase.sq`:

```sql
CREATE TABLE library_source (
  id TEXT NOT NULL PRIMARY KEY,
  platform_kind TEXT NOT NULL,
  display_name TEXT NOT NULL,
  handle TEXT NOT NULL,
  created_at_epoch_millis INTEGER NOT NULL,
  last_scan_at_epoch_millis INTEGER,
  access_status TEXT NOT NULL
);

CREATE TABLE library_track (
  id TEXT NOT NULL PRIMARY KEY,
  source_id TEXT NOT NULL REFERENCES library_source(id) ON DELETE CASCADE,
  source_local_key TEXT NOT NULL,
  audio_source_kind TEXT NOT NULL,
  audio_source_value TEXT NOT NULL,
  display_name TEXT NOT NULL,
  title TEXT NOT NULL,
  artist TEXT NOT NULL,
  album TEXT NOT NULL,
  duration_millis INTEGER,
  size_bytes INTEGER,
  modified_at_epoch_millis INTEGER,
  last_seen_scan_id TEXT,
  created_at_epoch_millis INTEGER NOT NULL,
  updated_at_epoch_millis INTEGER NOT NULL
);

CREATE UNIQUE INDEX library_track_source_key ON library_track(source_id, source_local_key);

CREATE TABLE scan_session (
  id TEXT NOT NULL PRIMARY KEY,
  source_id TEXT NOT NULL REFERENCES library_source(id) ON DELETE CASCADE,
  status TEXT NOT NULL,
  started_at_epoch_millis INTEGER NOT NULL,
  completed_at_epoch_millis INTEGER,
  folders_visited INTEGER NOT NULL,
  files_visited INTEGER NOT NULL,
  tracks_added INTEGER NOT NULL,
  tracks_updated INTEGER NOT NULL,
  files_skipped INTEGER NOT NULL,
  terminal_message TEXT
);

CREATE TABLE scan_error (
  id TEXT NOT NULL PRIMARY KEY,
  scan_id TEXT NOT NULL REFERENCES scan_session(id) ON DELETE CASCADE,
  source_local_key TEXT NOT NULL,
  display_path TEXT NOT NULL,
  reason TEXT NOT NULL,
  recoverable INTEGER NOT NULL,
  created_at_epoch_millis INTEGER NOT NULL
);

selectSources:
SELECT * FROM library_source ORDER BY created_at_epoch_millis ASC;

upsertSource:
INSERT INTO library_source(id, platform_kind, display_name, handle, created_at_epoch_millis, last_scan_at_epoch_millis, access_status)
VALUES (?, ?, ?, ?, ?, ?, ?)
ON CONFLICT(id) DO UPDATE SET
  display_name = excluded.display_name,
  handle = excluded.handle,
  last_scan_at_epoch_millis = excluded.last_scan_at_epoch_millis,
  access_status = excluded.access_status;

selectTracks:
SELECT * FROM library_track ORDER BY title COLLATE NOCASE ASC, artist COLLATE NOCASE ASC;

selectTracksForSource:
SELECT * FROM library_track WHERE source_id = ? ORDER BY title COLLATE NOCASE ASC;

selectTrackBySourceKey:
SELECT * FROM library_track WHERE source_id = ? AND source_local_key = ?;

insertTrack:
INSERT INTO library_track(
  id, source_id, source_local_key, audio_source_kind, audio_source_value, display_name,
  title, artist, album, duration_millis, size_bytes, modified_at_epoch_millis,
  last_seen_scan_id, created_at_epoch_millis, updated_at_epoch_millis
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

updateTrack:
UPDATE library_track SET
  audio_source_kind = ?,
  audio_source_value = ?,
  display_name = ?,
  title = ?,
  artist = ?,
  album = ?,
  duration_millis = ?,
  size_bytes = ?,
  modified_at_epoch_millis = ?,
  last_seen_scan_id = ?,
  updated_at_epoch_millis = ?
WHERE id = ?;

selectMissingTracks:
SELECT * FROM library_track WHERE source_id = ? AND last_seen_scan_id != ?;

removeMissingTracks:
DELETE FROM library_track WHERE source_id = ? AND last_seen_scan_id != ?;

insertScanSession:
INSERT INTO scan_session(
  id, source_id, status, started_at_epoch_millis, completed_at_epoch_millis,
  folders_visited, files_visited, tracks_added, tracks_updated, files_skipped, terminal_message
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

updateScanSession:
UPDATE scan_session SET
  status = ?, completed_at_epoch_millis = ?, folders_visited = ?, files_visited = ?,
  tracks_added = ?, tracks_updated = ?, files_skipped = ?, terminal_message = ?
WHERE id = ?;

insertScanError:
INSERT INTO scan_error(id, scan_id, source_local_key, display_path, reason, recoverable, created_at_epoch_millis)
VALUES (?, ?, ?, ?, ?, ?, ?);

selectScanErrors:
SELECT * FROM scan_error WHERE scan_id = ? ORDER BY created_at_epoch_millis ASC;
```

- [ ] **Step 4: Verify generated database compiles**

Run:

```bash
./gradlew :shared:compileKotlinMetadata --configuration-cache
```

Expected: `BUILD SUCCESSFUL`. If SQLDelight task names differ, run `./gradlew :shared:tasks --all | grep -i sql` and use the generated compile task listed there.

- [ ] **Step 5: Commit setup**

Run:

```bash
git add gradle/libs.versions.toml shared/build.gradle.kts shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/RhythHausDatabase.sq
git commit -m "build: add library database setup"
```

---

### Task 3: Add shared library domain models

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryModels.kt`
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SupportedAudio.kt`
- Test: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryModelsTest.kt`

- [ ] **Step 1: Write model tests**

Create `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryModelsTest.kt`:

```kotlin
package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibraryModelsTest {
    @Test
    fun supportedAudioExtensionsAreCaseInsensitive() {
        assertTrue(isSupportedAudioName("Track.MP3"))
        assertTrue(isSupportedAudioName("mix.flac"))
        assertTrue(isSupportedAudioName("voice.m4a"))
        assertFalse(isSupportedAudioName("cover.jpg"))
        assertFalse(isSupportedAudioName("notes"))
    }

    @Test
    fun libraryTrackMapsToPlayableTrack() {
        val track = LibraryTrack(
            id = "track-1",
            sourceId = "source-1",
            sourceLocalKey = "Album/Track.mp3",
            audioSource = AudioSource.FilePath("/Music/Album/Track.mp3"),
            displayName = "Track.mp3",
            title = "Track",
            artist = "Artist",
            album = "Album",
            durationMillis = 123_000L,
            sizeBytes = 1000L,
            modifiedAtEpochMillis = 456L,
            lastSeenScanId = "scan-1",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 2L,
        )

        val playable = track.toPlayableTrack()

        assertEquals("track-1", playable.id)
        assertEquals("Track", playable.title)
        assertEquals("Artist", playable.artist)
        assertEquals("Album", playable.album)
        assertEquals(123_000L, playable.durationMillis)
        assertEquals(AudioSource.FilePath("/Music/Album/Track.mp3"), playable.source)
    }
}
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryModelsTest' --configuration-cache
```

Expected: fail because `LibraryTrack` and `isSupportedAudioName` do not exist.

- [ ] **Step 3: Add supported-audio helper**

Create `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SupportedAudio.kt`:

```kotlin
package com.eterocell.rhythhaus.library

private val supportedAudioExtensions = setOf(
    "wav", "wave", "aif", "aiff", "au", "mp3", "m4a", "aac", "flac", "ogg",
)

fun isSupportedAudioName(name: String): Boolean {
    val extension = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return extension in supportedAudioExtensions
}
```

- [ ] **Step 4: Add domain models**

Create `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryModels.kt`:

```kotlin
package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.PlayableTrack

enum class LibraryPlatformKind { AndroidSafTree, JvmFolder, IosAppLocal }
enum class LibrarySourceAccessStatus { Available, LostAccess }
enum class ScanStatus { Idle, Scanning, Cancelling, Completed, Cancelled, Failed }

data class LibrarySource(
    val id: String,
    val platformKind: LibraryPlatformKind,
    val displayName: String,
    val handle: String,
    val createdAtEpochMillis: Long,
    val lastScanAtEpochMillis: Long? = null,
    val accessStatus: LibrarySourceAccessStatus = LibrarySourceAccessStatus.Available,
)

data class LibraryTrack(
    val id: String,
    val sourceId: String,
    val sourceLocalKey: String,
    val audioSource: AudioSource,
    val displayName: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMillis: Long?,
    val sizeBytes: Long?,
    val modifiedAtEpochMillis: Long?,
    val lastSeenScanId: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
) {
    fun toPlayableTrack(): PlayableTrack = PlayableTrack(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMillis = durationMillis ?: 0L,
        source = audioSource,
    )
}

data class AudioScanCandidate(
    val sourceId: String,
    val sourceLocalKey: String,
    val displayPath: String,
    val displayName: String,
    val audioSource: AudioSource,
    val sizeBytes: Long? = null,
    val modifiedAtEpochMillis: Long? = null,
)

data class ScanSession(
    val id: String,
    val sourceId: String,
    val status: ScanStatus,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long? = null,
    val foldersVisited: Int = 0,
    val filesVisited: Int = 0,
    val tracksAdded: Int = 0,
    val tracksUpdated: Int = 0,
    val filesSkipped: Int = 0,
    val terminalMessage: String? = null,
)

data class ScanProgress(
    val session: ScanSession? = null,
    val latestItem: String? = null,
) {
    val isActive: Boolean = session?.status in setOf(ScanStatus.Scanning, ScanStatus.Cancelling)
}

data class ScanError(
    val id: String,
    val scanId: String,
    val sourceLocalKey: String,
    val displayPath: String,
    val reason: String,
    val recoverable: Boolean,
    val createdAtEpochMillis: Long,
)
```

- [ ] **Step 5: Run tests and commit**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryModelsTest' --configuration-cache
```

Expected: `BUILD SUCCESSFUL`.

Commit:

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryModels.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SupportedAudio.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryModelsTest.kt
git commit -m "feat: add local library domain models"
```

---

### Task 4: Add database driver factory and repository

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.kt`
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt`
- Create: `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.android.kt`
- Create: `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.jvm.kt`
- Create: `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.ios.kt`
- Test: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryRepositoryContractTest.kt`

- [ ] **Step 1: Add repository contract test using an in-memory fake**

Create `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryRepositoryContractTest.kt`:

```kotlin
package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryRepositoryContractTest {
    @Test
    fun upsertTrackDoesNotDuplicateSourceLocalKey() {
        val repository = InMemoryLibraryRepository()
        val source = testSource()
        repository.upsertSource(source)
        val first = testTrack(title = "First")
        val second = first.copy(id = "track-2", title = "Second", updatedAtEpochMillis = 20L)

        assertEquals(TrackUpsertResult.Added, repository.upsertTrack(first))
        assertEquals(TrackUpsertResult.Updated, repository.upsertTrack(second))

        val tracks = repository.tracks()
        assertEquals(1, tracks.size)
        assertEquals("Second", tracks.single().title)
    }

    @Test
    fun removeMissingDeletesTracksNotSeenInLatestScan() {
        val repository = InMemoryLibraryRepository()
        val source = testSource()
        repository.upsertSource(source)
        repository.upsertTrack(testTrack(id = "seen", sourceLocalKey = "seen.mp3", lastSeenScanId = "scan-2"))
        repository.upsertTrack(testTrack(id = "missing", sourceLocalKey = "missing.mp3", lastSeenScanId = "scan-1"))

        val removed = repository.removeMissingTracks(source.id, latestScanId = "scan-2")

        assertEquals(1, removed)
        assertEquals(listOf("seen"), repository.tracks().map { it.id })
    }
}

private fun testSource() = LibrarySource(
    id = "source-1",
    platformKind = LibraryPlatformKind.JvmFolder,
    displayName = "Music",
    handle = "/Music",
    createdAtEpochMillis = 1L,
)

private fun testTrack(
    id: String = "track-1",
    sourceLocalKey: String = "Track.mp3",
    title: String = "Track",
    lastSeenScanId: String = "scan-1",
) = LibraryTrack(
    id = id,
    sourceId = "source-1",
    sourceLocalKey = sourceLocalKey,
    audioSource = AudioSource.FilePath("/Music/$sourceLocalKey"),
    displayName = sourceLocalKey,
    title = title,
    artist = "Local file",
    album = "Imported audio",
    durationMillis = null,
    sizeBytes = null,
    modifiedAtEpochMillis = null,
    lastSeenScanId = lastSeenScanId,
    createdAtEpochMillis = 1L,
    updatedAtEpochMillis = 2L,
)
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryRepositoryContractTest' --configuration-cache
```

Expected: fail because repository types do not exist.

- [ ] **Step 3: Add repository API and in-memory implementation for tests**

Create `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt`:

```kotlin
package com.eterocell.rhythhaus.library

interface LibraryRepository {
    fun upsertSource(source: LibrarySource)
    fun sources(): List<LibrarySource>
    fun upsertTrack(track: LibraryTrack): TrackUpsertResult
    fun tracks(): List<LibraryTrack>
    fun tracksForSource(sourceId: String): List<LibraryTrack>
    fun insertScanSession(session: ScanSession)
    fun updateScanSession(session: ScanSession)
    fun insertScanError(error: ScanError)
    fun scanErrors(scanId: String): List<ScanError>
    fun removeMissingTracks(sourceId: String, latestScanId: String): Int
}

enum class TrackUpsertResult { Added, Updated }

class InMemoryLibraryRepository : LibraryRepository {
    private val sources = linkedMapOf<String, LibrarySource>()
    private val tracks = linkedMapOf<String, LibraryTrack>()
    private val scanSessions = linkedMapOf<String, ScanSession>()
    private val scanErrors = mutableListOf<ScanError>()

    override fun upsertSource(source: LibrarySource) {
        sources[source.id] = source
    }

    override fun sources(): List<LibrarySource> = sources.values.toList()

    override fun upsertTrack(track: LibraryTrack): TrackUpsertResult {
        val existing = tracks.values.firstOrNull { it.sourceId == track.sourceId && it.sourceLocalKey == track.sourceLocalKey }
        return if (existing == null) {
            tracks[track.id] = track
            TrackUpsertResult.Added
        } else {
            tracks[existing.id] = track.copy(id = existing.id, createdAtEpochMillis = existing.createdAtEpochMillis)
            TrackUpsertResult.Updated
        }
    }

    override fun tracks(): List<LibraryTrack> = tracks.values.sortedBy { it.title.lowercase() }

    override fun tracksForSource(sourceId: String): List<LibraryTrack> = tracks().filter { it.sourceId == sourceId }

    override fun insertScanSession(session: ScanSession) {
        scanSessions[session.id] = session
    }

    override fun updateScanSession(session: ScanSession) {
        scanSessions[session.id] = session
    }

    override fun insertScanError(error: ScanError) {
        scanErrors += error
    }

    override fun scanErrors(scanId: String): List<ScanError> = scanErrors.filter { it.scanId == scanId }

    override fun removeMissingTracks(sourceId: String, latestScanId: String): Int {
        val ids = tracks.values
            .filter { it.sourceId == sourceId && it.lastSeenScanId != latestScanId }
            .map { it.id }
        ids.forEach { tracks.remove(it) }
        return ids.size
    }
}
```

- [ ] **Step 4: Add database driver factory actuals**

Create `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.kt`:

```kotlin
package com.eterocell.rhythhaus.library

import app.cash.sqldelight.db.SqlDriver

expect class LibraryDatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

fun createLibraryDatabase(factory: LibraryDatabaseDriverFactory): RhythHausDatabase = RhythHausDatabase(factory.createDriver())
```

Create `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.android.kt`:

```kotlin
package com.eterocell.rhythhaus.library

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class LibraryDatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver = AndroidSqliteDriver(
        schema = RhythHausDatabase.Schema,
        context = context,
        name = "rhythhaus.db",
    )
}
```

Create `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.jvm.kt`:

```kotlin
package com.eterocell.rhythhaus.library

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class LibraryDatabaseDriverFactory(private val databaseFile: File = defaultDatabaseFile()) {
    actual fun createDriver(): SqlDriver {
        databaseFile.parentFile.mkdirs()
        return JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}").also { driver ->
            RhythHausDatabase.Schema.create(driver)
        }
    }
}

private fun defaultDatabaseFile(): File = File(System.getProperty("user.home"), "Library/Application Support/RhythHaus/rhythhaus.db")
```

Create `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.ios.kt`:

```kotlin
package com.eterocell.rhythhaus.library

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class LibraryDatabaseDriverFactory {
    actual fun createDriver(): SqlDriver = NativeSqliteDriver(
        schema = RhythHausDatabase.Schema,
        name = "rhythhaus.db",
    )
}
```

- [ ] **Step 5: Add SQL-backed repository**

In `LibraryRepository.kt`, add this class below `InMemoryLibraryRepository`:

```kotlin
class SqlDelightLibraryRepository(
    private val database: RhythHausDatabase,
) : LibraryRepository {
    private val queries = database.rhythHausDatabaseQueries

    override fun upsertSource(source: LibrarySource) {
        queries.upsertSource(
            id = source.id,
            platform_kind = source.platformKind.name,
            display_name = source.displayName,
            handle = source.handle,
            created_at_epoch_millis = source.createdAtEpochMillis,
            last_scan_at_epoch_millis = source.lastScanAtEpochMillis,
            access_status = source.accessStatus.name,
        )
    }

    override fun sources(): List<LibrarySource> = queries.selectSources().executeAsList().map { row ->
        LibrarySource(
            id = row.id,
            platformKind = LibraryPlatformKind.valueOf(row.platform_kind),
            displayName = row.display_name,
            handle = row.handle,
            createdAtEpochMillis = row.created_at_epoch_millis,
            lastScanAtEpochMillis = row.last_scan_at_epoch_millis,
            accessStatus = LibrarySourceAccessStatus.valueOf(row.access_status),
        )
    }

    override fun upsertTrack(track: LibraryTrack): TrackUpsertResult {
        val existing = queries.selectTrackBySourceKey(track.sourceId, track.sourceLocalKey).executeAsOneOrNull()
        return if (existing == null) {
            queries.insertTrack(
                id = track.id,
                source_id = track.sourceId,
                source_local_key = track.sourceLocalKey,
                audio_source_kind = track.audioSource.kindName,
                audio_source_value = track.audioSource.stableKey,
                display_name = track.displayName,
                title = track.title,
                artist = track.artist,
                album = track.album,
                duration_millis = track.durationMillis,
                size_bytes = track.sizeBytes,
                modified_at_epoch_millis = track.modifiedAtEpochMillis,
                last_seen_scan_id = track.lastSeenScanId,
                created_at_epoch_millis = track.createdAtEpochMillis,
                updated_at_epoch_millis = track.updatedAtEpochMillis,
            )
            TrackUpsertResult.Added
        } else {
            queries.updateTrack(
                audio_source_kind = track.audioSource.kindName,
                audio_source_value = track.audioSource.stableKey,
                display_name = track.displayName,
                title = track.title,
                artist = track.artist,
                album = track.album,
                duration_millis = track.durationMillis,
                size_bytes = track.sizeBytes,
                modified_at_epoch_millis = track.modifiedAtEpochMillis,
                last_seen_scan_id = track.lastSeenScanId,
                updated_at_epoch_millis = track.updatedAtEpochMillis,
                id = existing.id,
            )
            TrackUpsertResult.Updated
        }
    }

    override fun tracks(): List<LibraryTrack> = queries.selectTracks().executeAsList().map { it.toLibraryTrack() }
    override fun tracksForSource(sourceId: String): List<LibraryTrack> = queries.selectTracksForSource(sourceId).executeAsList().map { it.toLibraryTrack() }
    override fun insertScanSession(session: ScanSession) = upsertScanSession(session, insert = true)
    override fun updateScanSession(session: ScanSession) = upsertScanSession(session, insert = false)

    override fun insertScanError(error: ScanError) {
        queries.insertScanError(error.id, error.scanId, error.sourceLocalKey, error.displayPath, error.reason, if (error.recoverable) 1 else 0, error.createdAtEpochMillis)
    }

    override fun scanErrors(scanId: String): List<ScanError> = queries.selectScanErrors(scanId).executeAsList().map { row ->
        ScanError(row.id, row.scan_id, row.source_local_key, row.display_path, row.reason, row.recoverable != 0L, row.created_at_epoch_millis)
    }

    override fun removeMissingTracks(sourceId: String, latestScanId: String): Int {
        val missing = queries.selectMissingTracks(sourceId, latestScanId).executeAsList().size
        queries.removeMissingTracks(sourceId, latestScanId)
        return missing
    }

    private fun upsertScanSession(session: ScanSession, insert: Boolean) {
        if (insert) {
            queries.insertScanSession(session.id, session.sourceId, session.status.name, session.startedAtEpochMillis, session.completedAtEpochMillis, session.foldersVisited.toLong(), session.filesVisited.toLong(), session.tracksAdded.toLong(), session.tracksUpdated.toLong(), session.filesSkipped.toLong(), session.terminalMessage)
        } else {
            queries.updateScanSession(session.status.name, session.completedAtEpochMillis, session.foldersVisited.toLong(), session.filesVisited.toLong(), session.tracksAdded.toLong(), session.tracksUpdated.toLong(), session.filesSkipped.toLong(), session.terminalMessage, session.id)
        }
    }
}
```

Also add private mapping helpers in the same file:

```kotlin
private val AudioSource.kindName: String
    get() = when (this) {
        is AudioSource.FilePath -> "FilePath"
        is AudioSource.Uri -> "Uri"
    }

private fun String.toAudioSource(kind: String): AudioSource = when (kind) {
    "FilePath" -> AudioSource.FilePath(this)
    "Uri" -> AudioSource.Uri(this)
    else -> AudioSource.Uri(this)
}
```

Then add row mapping with the generated row type names from SQLDelight. If generated names differ, inspect `shared/build/generated/sqldelight` after compile and adjust imports only; do not change domain type names.

- [ ] **Step 6: Run repository tests and commit**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryRepositoryContractTest' --configuration-cache
```

Expected: `BUILD SUCCESSFUL`.

Commit:

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library shared/src/iosMain/kotlin/com/eterocell/rhythhaus/library shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryRepositoryContractTest.kt
git commit -m "feat: persist scanned library state"
```

---

### Task 5: Add shared scanner orchestration

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryScanner.kt`
- Test: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryScannerTest.kt`

- [ ] **Step 1: Write scanner tests**

Create `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryScannerTest.kt`:

```kotlin
package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LibraryScannerTest {
    @Test
    fun scannerImportsCandidatesAndRecordsSkippedFiles() {
        val repository = InMemoryLibraryRepository()
        val source = LibrarySource("source-1", LibraryPlatformKind.JvmFolder, "Music", "/Music", 1L)
        val platform = FakePlatformAudioScanner(
            candidates = listOf(
                PlatformScanEvent.FolderVisited("/Music"),
                PlatformScanEvent.AudioCandidate(AudioScanCandidate("source-1", "ok.mp3", "/Music/ok.mp3", "ok.mp3", AudioSource.FilePath("/Music/ok.mp3"))),
                PlatformScanEvent.Skipped("bad.txt", "/Music/bad.txt", "Unsupported file", true),
            ),
        )
        val scanner = LibraryScanner(repository, platform, now = { 100L }, idFactory = { prefix -> "$prefix-id" })

        val result = scanner.scan(source)

        assertEquals(ScanStatus.Completed, result.status)
        assertEquals(1, result.tracksAdded)
        assertEquals(1, result.filesSkipped)
        assertEquals(listOf("ok"), repository.tracks().map { it.title })
        assertEquals("Unsupported file", repository.scanErrors("scan-id").single().reason)
    }

    @Test
    fun cancellationStopsBeforeLaterCandidatesAndPreservesImportedTracks() {
        val repository = InMemoryLibraryRepository()
        val source = LibrarySource("source-1", LibraryPlatformKind.JvmFolder, "Music", "/Music", 1L)
        var cancel = false
        val platform = FakePlatformAudioScanner(
            candidates = listOf(
                PlatformScanEvent.AudioCandidate(AudioScanCandidate("source-1", "first.mp3", "first.mp3", "first.mp3", AudioSource.FilePath("/Music/first.mp3"))),
                PlatformScanEvent.AudioCandidate(AudioScanCandidate("source-1", "second.mp3", "second.mp3", "second.mp3", AudioSource.FilePath("/Music/second.mp3"))),
            ),
            afterFirst = { cancel = true },
        )
        val scanner = LibraryScanner(repository, platform, now = { 100L }, idFactory = { prefix -> "$prefix-id" })

        val result = scanner.scan(source, isCancelled = { cancel })

        assertEquals(ScanStatus.Cancelled, result.status)
        assertEquals(listOf("first"), repository.tracks().map { it.title })
    }
}
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryScannerTest' --configuration-cache
```

Expected: fail because scanner types do not exist.

- [ ] **Step 3: Add scanner implementation**

Create `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryScanner.kt`:

```kotlin
package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioMetadataReader

sealed interface PlatformScanEvent {
    data class FolderVisited(val displayPath: String) : PlatformScanEvent
    data class AudioCandidate(val candidate: AudioScanCandidate) : PlatformScanEvent
    data class Skipped(val sourceLocalKey: String, val displayPath: String, val reason: String, val recoverable: Boolean) : PlatformScanEvent
}

interface PlatformAudioScanner {
    fun scan(source: LibrarySource): Sequence<PlatformScanEvent>
}

class LibraryScanner(
    private val repository: LibraryRepository,
    private val platformScanner: PlatformAudioScanner,
    private val metadataReader: AudioMetadataReader = AudioMetadataReader(),
    private val now: () -> Long,
    private val idFactory: (String) -> String,
) {
    fun scan(source: LibrarySource, isCancelled: () -> Boolean = { false }): ScanSession {
        val scanId = idFactory("scan")
        var session = ScanSession(
            id = scanId,
            sourceId = source.id,
            status = ScanStatus.Scanning,
            startedAtEpochMillis = now(),
        )
        repository.upsertSource(source)
        repository.insertScanSession(session)

        for (event in platformScanner.scan(source)) {
            if (isCancelled()) {
                session = session.copy(status = ScanStatus.Cancelled, completedAtEpochMillis = now(), terminalMessage = "Scan cancelled")
                repository.updateScanSession(session)
                return session
            }
            when (event) {
                is PlatformScanEvent.FolderVisited -> session = session.copy(foldersVisited = session.foldersVisited + 1)
                is PlatformScanEvent.Skipped -> {
                    session = session.copy(filesVisited = session.filesVisited + 1, filesSkipped = session.filesSkipped + 1)
                    repository.insertScanError(
                        ScanError(
                            id = idFactory("scan-error"),
                            scanId = scanId,
                            sourceLocalKey = event.sourceLocalKey,
                            displayPath = event.displayPath,
                            reason = event.reason,
                            recoverable = event.recoverable,
                            createdAtEpochMillis = now(),
                        ),
                    )
                }
                is PlatformScanEvent.AudioCandidate -> {
                    session = session.copy(filesVisited = session.filesVisited + 1)
                    val track = event.candidate.toLibraryTrack(scanId, now(), idFactory("track"), metadataReader)
                    when (repository.upsertTrack(track)) {
                        TrackUpsertResult.Added -> session = session.copy(tracksAdded = session.tracksAdded + 1)
                        TrackUpsertResult.Updated -> session = session.copy(tracksUpdated = session.tracksUpdated + 1)
                    }
                }
            }
            repository.updateScanSession(session)
        }

        session = session.copy(status = ScanStatus.Completed, completedAtEpochMillis = now())
        repository.updateScanSession(session)
        return session
    }
}

private fun AudioScanCandidate.toLibraryTrack(
    scanId: String,
    timestamp: Long,
    trackId: String,
    metadataReader: AudioMetadataReader,
): LibraryTrack {
    val metadata = metadataReader.read(audioSource)
    val title = metadata?.title ?: displayName.substringBeforeLast('.', missingDelimiterValue = displayName).replace('_', ' ').replace('-', ' ').trim().ifBlank { "Untitled audio" }
    return LibraryTrack(
        id = trackId,
        sourceId = sourceId,
        sourceLocalKey = sourceLocalKey,
        audioSource = audioSource,
        displayName = displayName,
        title = title,
        artist = metadata?.artist ?: "Local file",
        album = metadata?.album ?: "Imported audio",
        durationMillis = metadata?.durationMillis,
        sizeBytes = sizeBytes,
        modifiedAtEpochMillis = modifiedAtEpochMillis,
        lastSeenScanId = scanId,
        createdAtEpochMillis = timestamp,
        updatedAtEpochMillis = timestamp,
    )
}

class FakePlatformAudioScanner(
    private val candidates: List<PlatformScanEvent>,
    private val afterFirst: () -> Unit = {},
) : PlatformAudioScanner {
    override fun scan(source: LibrarySource): Sequence<PlatformScanEvent> = sequence {
        candidates.forEachIndexed { index, event ->
            yield(event)
            if (index == 0) afterFirst()
        }
    }
}
```

- [ ] **Step 4: Run tests and commit**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryScannerTest' --configuration-cache
```

Expected: `BUILD SUCCESSFUL`.

Commit:

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryScanner.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryScannerTest.kt
git commit -m "feat: orchestrate local audio scans"
```

---

### Task 6: Add platform scanners and source pickers

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlatform.kt`
- Create/modify: `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlatform.android.kt`
- Create/modify: `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlatform.jvm.kt`
- Create/modify: `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlatform.ios.kt`
- Test: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/JvmLibraryScannerTest.kt`

- [ ] **Step 1: Add common platform contracts**

Create `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlatform.kt`:

```kotlin
package com.eterocell.rhythhaus.library

interface PlatformFolderPicker {
    val isAvailable: Boolean
    fun pickSource(onResult: (LibrarySourcePickResult) -> Unit)
}

sealed interface LibrarySourcePickResult {
    data class Success(val source: LibrarySource) : LibrarySourcePickResult
    data class Unavailable(val message: String) : LibrarySourcePickResult
    data class Failure(val message: String, val cause: String? = null) : LibrarySourcePickResult
    data object Cancelled : LibrarySourcePickResult
}
```

- [ ] **Step 2: Add JVM temp-folder scanner test**

Create `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/JvmLibraryScannerTest.kt`:

```kotlin
package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmLibraryScannerTest {
    @Test
    fun recursivelyFindsSupportedAudioFilesOnly() {
        val root = Files.createTempDirectory("rhythhaus-scan-test")
        val nested = Files.createDirectories(root.resolve("Album"))
        nested.resolve("Track.MP3").writeText("fake audio")
        nested.resolve("cover.jpg").writeText("image")
        val source = LibrarySource("source-1", LibraryPlatformKind.JvmFolder, "Music", root.toString(), 1L)

        val events = JvmFolderAudioScanner().scan(source).toList()

        val candidates = events.filterIsInstance<PlatformScanEvent.AudioCandidate>()
        assertEquals(1, candidates.size)
        assertEquals("Album/Track.MP3", candidates.single().candidate.sourceLocalKey)
        assertTrue(candidates.single().candidate.audioSource is AudioSource.FilePath)
    }
}
```

- [ ] **Step 3: Add JVM implementation**

Create `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlatform.jvm.kt`:

```kotlin
package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.pathString

class JvmFolderAudioScanner : PlatformAudioScanner {
    override fun scan(source: LibrarySource): Sequence<PlatformScanEvent> = sequence {
        val root = Path.of(source.handle)
        if (!Files.exists(root) || !root.isDirectory()) {
            yield(PlatformScanEvent.Skipped(source.handle, source.handle, "Folder is not accessible", recoverable = false))
            return@sequence
        }
        Files.walk(root).use { stream ->
            val iterator = stream.iterator()
            while (iterator.hasNext()) {
                val path = iterator.next()
                if (Files.isDirectory(path)) {
                    yield(PlatformScanEvent.FolderVisited(root.relativize(path).pathString.ifBlank { source.displayName }))
                } else if (isSupportedAudioName(path.name)) {
                    val relative = root.relativize(path).pathString.replace(File.separatorChar, '/')
                    yield(
                        PlatformScanEvent.AudioCandidate(
                            AudioScanCandidate(
                                sourceId = source.id,
                                sourceLocalKey = relative,
                                displayPath = path.pathString,
                                displayName = path.name,
                                audioSource = AudioSource.FilePath(path.toAbsolutePath().pathString),
                                sizeBytes = Files.size(path),
                                modifiedAtEpochMillis = Files.getLastModifiedTime(path).toMillis(),
                            ),
                        ),
                    )
                }
            }
        }
    }
}

class JvmFolderPicker(private val now: () -> Long, private val idFactory: (String) -> String) : PlatformFolderPicker {
    override val isAvailable: Boolean = true

    override fun pickSource(onResult: (LibrarySourcePickResult) -> Unit) {
        try {
            val dialog = FileDialog(null as Frame?, "Add music folder", FileDialog.LOAD).apply {
                System.setProperty("apple.awt.fileDialogForDirectories", "true")
            }
            dialog.isVisible = true
            val directory = dialog.directory?.let(::File)
            if (directory == null) {
                onResult(LibrarySourcePickResult.Cancelled)
            } else {
                onResult(
                    LibrarySourcePickResult.Success(
                        LibrarySource(
                            id = idFactory("source"),
                            platformKind = LibraryPlatformKind.JvmFolder,
                            displayName = directory.name.ifBlank { directory.absolutePath },
                            handle = directory.absolutePath,
                            createdAtEpochMillis = now(),
                        ),
                    ),
                )
            }
        } catch (throwable: Throwable) {
            onResult(LibrarySourcePickResult.Failure("Could not choose folder", throwable.message ?: throwable::class.simpleName))
        } finally {
            System.setProperty("apple.awt.fileDialogForDirectories", "false")
        }
    }
}
```

- [ ] **Step 4: Add Android implementation**

Before writing this file, add the AndroidX DocumentFile dependency used by recursive SAF traversal:

`gradle/libs.versions.toml`:

```toml
androidx-documentfile = "1.1.0"
androidx-documentfile = { module = "androidx.documentfile:documentfile", version.ref = "androidx-documentfile" }
```

`shared/build.gradle.kts` inside `androidMain.dependencies`:

```kotlin
implementation(libs.androidx.documentfile)
```

Create `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlatform.android.kt`:

```kotlin
package com.eterocell.rhythhaus.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.documentfile.provider.DocumentFile
import com.eterocell.rhythhaus.AudioSource

class AndroidSafAudioScanner(private val context: Context) : PlatformAudioScanner {
    override fun scan(source: LibrarySource): Sequence<PlatformScanEvent> = sequence {
        val rootUri = Uri.parse(source.handle)
        val root = DocumentFile.fromTreeUri(context, rootUri)
        if (root == null || !root.exists() || !root.isDirectory) {
            yield(PlatformScanEvent.Skipped(source.handle, source.displayName, "Folder is not accessible", recoverable = false))
            return@sequence
        }
        yieldAll(scanDocumentTree(source, root, prefix = ""))
    }

    private fun scanDocumentTree(source: LibrarySource, directory: DocumentFile, prefix: String): Sequence<PlatformScanEvent> = sequence {
        yield(PlatformScanEvent.FolderVisited(prefix.ifBlank { directory.name ?: source.displayName }))
        val children = try {
            directory.listFiles().toList()
        } catch (throwable: Throwable) {
            yield(PlatformScanEvent.Skipped(prefix, directory.uri.toString(), throwable.message ?: "Cannot list folder", recoverable = true))
            emptyList()
        }
        for (child in children) {
            val name = child.name ?: child.uri.lastPathSegment ?: child.uri.toString()
            val key = if (prefix.isBlank()) name else "$prefix/$name"
            when {
                child.isDirectory -> yieldAll(scanDocumentTree(source, child, key))
                child.isFile && isSupportedAudioName(name) -> yield(
                    PlatformScanEvent.AudioCandidate(
                        AudioScanCandidate(
                            sourceId = source.id,
                            sourceLocalKey = key,
                            displayPath = key,
                            displayName = name,
                            audioSource = AudioSource.Uri(child.uri.toString()),
                            sizeBytes = child.length().takeIf { it >= 0L },
                            modifiedAtEpochMillis = child.lastModified().takeIf { it > 0L },
                        ),
                    ),
                )
                child.isFile -> Unit
                else -> yield(PlatformScanEvent.Skipped(key, key, "Unsupported document entry", recoverable = true))
            }
        }
    }
}

@Composable
fun rememberAndroidSafFolderPicker(
    context: Context,
    now: () -> Long,
    idFactory: (String) -> String,
    onResult: (LibrarySourcePickResult) -> Unit,
): PlatformFolderPicker {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) {
            onResult(LibrarySourcePickResult.Cancelled)
        } else {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            onResult(
                LibrarySourcePickResult.Success(
                    LibrarySource(
                        id = idFactory("source"),
                        platformKind = LibraryPlatformKind.AndroidSafTree,
                        displayName = uri.lastPathSegment ?: "Android music folder",
                        handle = uri.toString(),
                        createdAtEpochMillis = now(),
                    ),
                ),
            )
        }
    }
    return remember(launcher) {
        object : PlatformFolderPicker {
            override val isAvailable: Boolean = true
            override fun pickSource(onResult: (LibrarySourcePickResult) -> Unit) = launcher.launch(null)
        }
    }
}
```

- [ ] **Step 5: Add iOS implementation**

Create `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlatform.ios.kt`:

```kotlin
package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource
import platform.Foundation.NSDirectoryEnumerator
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathDirectory
import platform.Foundation.NSSearchPathDomainMask
import platform.Foundation.NSSearchPathForDirectoriesInDomains

class IosAppLocalAudioScanner : PlatformAudioScanner {
    override fun scan(source: LibrarySource): Sequence<PlatformScanEvent> = sequence {
        val manager = NSFileManager.defaultManager
        if (!manager.fileExistsAtPath(source.handle)) {
            yield(PlatformScanEvent.Skipped(source.handle, source.handle, "App-local music folder is not accessible", recoverable = false))
            return@sequence
        }
        yield(PlatformScanEvent.FolderVisited(source.displayName))
        val enumerator = manager.enumeratorAtPath(source.handle) ?: return@sequence
        while (true) {
            val next = enumerator.nextObject() as? String ?: break
            val fullPath = "${source.handle}/$next"
            var isDirectory = false
            manager.fileExistsAtPath(fullPath, isDirectory = kotlinx.cinterop.ptr(kotlinx.cinterop.alloc<BooleanVar>().also { it.value = false }))
            val attributes = manager.attributesOfItemAtPath(fullPath, error = null)
            if (next.substringAfterLast('/').let(::isSupportedAudioName)) {
                yield(
                    PlatformScanEvent.AudioCandidate(
                        AudioScanCandidate(
                            sourceId = source.id,
                            sourceLocalKey = next,
                            displayPath = fullPath,
                            displayName = next.substringAfterLast('/'),
                            audioSource = AudioSource.FilePath(fullPath),
                            sizeBytes = (attributes?.get(NSFileSize) as? NSNumber)?.longLongValue,
                            modifiedAtEpochMillis = null,
                        ),
                    ),
                )
            } else if (next.endsWith('/')) {
                yield(PlatformScanEvent.FolderVisited(next))
            }
        }
    }
}

class IosAppLocalFolderPicker(private val now: () -> Long, private val idFactory: (String) -> String) : PlatformFolderPicker {
    override val isAvailable: Boolean = true
    override fun pickSource(onResult: (LibrarySourcePickResult) -> Unit) {
        val musicPath = appLocalMusicFolderPath()
        NSFileManager.defaultManager.createDirectoryAtPath(musicPath, withIntermediateDirectories = true, attributes = null, error = null)
        onResult(
            LibrarySourcePickResult.Success(
                LibrarySource(
                    id = idFactory("source"),
                    platformKind = LibraryPlatformKind.IosAppLocal,
                    displayName = "RhythHaus Music",
                    handle = musicPath,
                    createdAtEpochMillis = now(),
                ),
            ),
        )
    }
}

private fun appLocalMusicFolderPath(): String {
    val documents = NSSearchPathForDirectoriesInDomains(
        directory = NSSearchPathDirectory.NSDocumentDirectory,
        domainMask = NSSearchPathDomainMask.NSUserDomainMask,
        expandTilde = true,
    ).first() as String
    return "$documents/RhythHaus Music"
}
```

After writing the iOS file, immediately run `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`. If Kotlin/Native rejects the directory-detection interop above, simplify the scanner for this slice to enumerate all app-local paths and emit audio candidates for supported names only; folder progress can be added in a follow-up without changing the source model.


- [ ] **Step 6: Run focused platform checks and commit**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.JvmLibraryScannerTest' --configuration-cache
./gradlew :androidApp:assembleDebug --configuration-cache
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

Expected: all three commands report `BUILD SUCCESSFUL`.

Commit:

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlatform.kt shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlatform.android.kt shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlatform.jvm.kt shared/src/iosMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlatform.ios.kt shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/JvmLibraryScannerTest.kt gradle/libs.versions.toml shared/build.gradle.kts
git commit -m "feat: scan platform music sources"
```

---

### Task 7: Add shared library manager UI and wire `App`

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryManagerUi.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`

- [ ] **Step 1: Extract UI state**

Create `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryManagerUi.kt`:

```kotlin
package com.eterocell.rhythhaus.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class LibraryManagerUiState(
    val sources: List<LibrarySource>,
    val tracks: List<LibraryTrack>,
    val scanProgress: ScanProgress,
    val message: String? = null,
) {
    val hasTracks: Boolean = tracks.isNotEmpty()
}

@Composable
fun LibraryManagerCard(
    state: LibraryManagerUiState,
    folderPicker: PlatformFolderPicker,
    onAddSource: () -> Unit,
    onCancelScan: () -> Unit,
    onRescan: () -> Unit,
    onRemoveMissing: () -> Unit,
    onViewReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = if (state.hasTracks) "Manage local library" else "Add music folder", fontWeight = FontWeight.Black)
            Text(text = state.message ?: state.scanProgress.session?.status?.name ?: "Scan local audio recursively and keep your library on this device.")
            if (state.scanProgress.isActive) {
                val session = state.scanProgress.session
                Text(text = "Folders ${session?.foldersVisited ?: 0} • Files ${session?.filesVisited ?: 0} • Tracks ${(session?.tracksAdded ?: 0) + (session?.tracksUpdated ?: 0)} • Skipped ${session?.filesSkipped ?: 0}")
                Button(onClick = onCancelScan) { Text("Cancel scan") }
            } else {
                Button(onClick = onAddSource, enabled = folderPicker.isAvailable) { Text(if (state.hasTracks) "Add another source" else "Add music folder") }
                if (state.sources.isNotEmpty()) {
                    Button(onClick = onRescan) { Text("Rescan") }
                    Button(onClick = onRemoveMissing) { Text("Remove missing files") }
                    Button(onClick = onViewReport) { Text("View scan report") }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Wire `App.kt` to the new card**

Modify `App.kt` incrementally:

1. Import `LibraryManagerCard`, `LibraryManagerUiState`, `InMemoryLibraryRepository`, `LibraryScanner`, and platform picker/scanner factory functions.
2. Replace `rememberAudioImportLauncher` state with repository-backed library state.
3. Convert `LibraryTrack` rows to existing `Track` rows for `LibraryHomeScreen` until the UI is fully migrated.
4. Replace `ImportAudioCard` call with `LibraryManagerCard`.

Keep the old `ImportAudioCard` in the file until the new flow compiles, then delete it in the final migration task.

- [ ] **Step 3: Run Compose/common checks**

Run:

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin --configuration-cache
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit UI wiring**

Run:

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryManagerUi.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
git commit -m "feat: add local library manager UI"
```

---

### Task 8: Final migration and verification

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AudioImport.kt`
- Modify/delete: platform `AudioImport.*.kt` files if file import is removed
- Modify: `openspec/changes/scan-local-audio-folders/tasks.md`
- Modify: `progress.md`

- [ ] **Step 1: Decide old file import fate**

Use this decision rule:

- If folder scanning works on Android and macOS/JVM and iOS app-local scanning compiles, remove the old file import primary UI.
- If one platform still has a blocker, keep old file import as a secondary fallback and clearly label it “Import individual files.”

Record the decision in `openspec/changes/scan-local-audio-folders/tasks.md` under task 6.1.

- [ ] **Step 2: Run full verification**

Run:

```bash
./init.sh
openspec validate scan-local-audio-folders --strict
```

Expected:

```text
=== Harness verification complete ===
Change 'scan-local-audio-folders' is valid
```

If `./init.sh` fails, capture the exact failing command/output in `progress.md` and fix before claiming completion.

- [ ] **Step 3: Update OpenSpec task status**

Mark completed tasks in `openspec/changes/scan-local-audio-folders/tasks.md`. Leave any platform/manual validation work unchecked with exact notes.

- [ ] **Step 4: Update `progress.md` handoff evidence**

Append:

```text
## Handoff - 2026-06-11 local folder scanning

Route: openspec+superpowers
Owner: implementation
Scope: recursive folder/source scanning across Android, macOS/JVM, and iOS app-local storage with shared persistence and library manager UI.
Verification:
- ./init.sh: pass | fail (<exact output if fail>)
- openspec validate scan-local-audio-folders --strict: pass
Acceptance:
- Requirement matched: yes | no
- Scope controlled: yes | no
- Edge cases/risk reviewed: Android SAF access, macOS folder access, iOS app-local folder, metadata fallback, cancellation, remove-missing.
Changed files:
- <list actual changed files and why>
Next owner: user or implementation
Blockers: <none or exact blocker>
Commit: <commit hash and message>
```

- [ ] **Step 5: Commit final workflow changes**

Run:

```bash
git add shared gradle/libs.versions.toml openspec/changes/scan-local-audio-folders progress.md
git commit -m "feat: scan local audio folders"
```

---

## Plan Self-Review

Spec coverage:

- Recursive local source scanning: Task 5 and Task 6.
- Shared persistent database: Task 2 and Task 4.
- Scan progress and management: Task 5 and Task 7.
- Recoverable errors and metadata fallback: Task 5.
- Existing playback integration: Task 7 and Task 8.
- Android SAF, macOS folder, iOS app-local semantics: Task 6.

Known implementation caution:

- Task 6 includes concrete Android SAF and iOS app-local scanner code. Verify both with the listed Android/iOS Gradle commands before commit.
- SQLDelight generated row type names may require import/name adjustment after first generated compile; keep domain type names stable.
- If SQLDelight 2.3.2 is incompatible with Kotlin 2.4.0 or AGP 9.2.1 in this project, stop and record the exact Gradle error before changing database technology.
