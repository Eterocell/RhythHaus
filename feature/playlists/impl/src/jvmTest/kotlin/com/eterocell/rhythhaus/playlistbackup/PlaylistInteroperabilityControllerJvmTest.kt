package com.eterocell.rhythhaus.playlistbackup

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.*
import com.eterocell.rhythhaus.library.ui.*
import java.nio.file.Files
import kotlin.test.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class PlaylistInteroperabilityControllerJvmTest {
    @Test
    fun importsPreserveDuplicatesAndRejectStalePreviewsAndUnreadableMedia() =
        runBlocking {
            val directory = Files.createTempDirectory("interop-smoke")
            val media =
                Files.write(directory.resolve("track.mp3"), byteArrayOf(1, 2))
            val database =
                LibraryDatabase(directory.resolve("library.db").toFile())
            try {
                database.database.librarySourceQueries.upsertSource(
                    "source",
                    "JvmFolder",
                    "Music",
                    directory.toString(),
                    1,
                    null,
                    "Available")
                database.database.libraryTrackQueries.upsertTrack(
                    id = "track",
                    sourceId = "source",
                    sourceLocalKey = "track.mp3",
                    audioSourceKind = "FilePath",
                    audioSourceValue = media.toString(),
                    displayName = "track.mp3",
                    title = "Song",
                    artist = "Artist",
                    album = "Album",
                    durationMillis = 120000,
                    sizeBytes = 2,
                    modifiedAtEpochMillis = 1,
                    lastSeenScanId = null,
                    createdAtEpochMillis = 1,
                    updatedAtEpochMillis = 1,
                    trackNumber = null,
                    discNumber = null,
                    artworkBytes = null,
                    artworkMimeType = null)
                val repository = SqlDelightPlaylistRepository(database)
                val original =
                    repository.createWithEntries(
                        "Ordered", listOf("track", "track"))
                val track =
                    LibraryTrack(
                        "track",
                        "source",
                        "track.mp3",
                        AudioSource.FilePath(media.toString()),
                        "track.mp3",
                        "Song",
                        "Artist",
                        "Album",
                        120000,
                        2,
                        1,
                        null,
                        1,
                        1)
                var revision = 7L
                val launcher = CaptureLauncher()
                val controller =
                    createPlaylistBackupController(
                        PlaylistStateOwner(repository, Dispatchers.Unconfined),
                        Dispatchers.Unconfined,
                        launcher,
                        object : PlaylistBackupRevisionGuard {
                            override suspend fun <T> withCurrentRevision(
                                expectedRevision: Long,
                                block: suspend () -> T
                            ): PlaylistBackupRevisionGuardResult<T> =
                                if (revision == expectedRevision)
                                    PlaylistBackupRevisionGuardResult.Current(
                                        block())
                                else PlaylistBackupRevisionGuardResult.Stale
                        })
                for (format in
                    listOf(
                        PlaylistDocumentFormat.M3u,
                        PlaylistDocumentFormat.M3u8,
                        PlaylistDocumentFormat.Pls)) {
                    val snapshot = loadPlaylistSnapshot(repository)
                    val saving =
                        controller.beginExport(
                            PlaylistBackupUiState(),
                            snapshot,
                            listOf(track),
                            1,
                            format,
                            original.id,
                            ::isJvmPlaylistTrackReadable)
                    assertEquals(
                        PlaylistBackupOperation.Saving, saving.operation)
                    assertEquals(format, launcher.format)
                    val document =
                        directory.resolve(
                            playlistDocumentFileName("Ordered", format))
                    Files.write(document, launcher.bytes)
                    val idle =
                        controller.receiveSave(
                            saving, PlaylistBackupDocumentSaveResult.Success)
                    val opening = controller.beginOpen(idle, format)
                    val preview =
                        controller.receiveOpen(
                            opening,
                            PlaylistBackupDocumentOpenResult.Success(
                                Files.readAllBytes(document)),
                            listOf(track),
                            repository.playlists().map { it.name },
                            "Imported",
                            revision)
                    assertEquals(2, preview.preview!!.totals.restorable)
                    val confirmed = controller.confirm(preview, snapshot)
                    assertNull(confirmed.state.error)
                    val existingIds = snapshot.playlists.map { it.id }.toSet()
                    val created =
                        confirmed.confirmedSnapshot!!.playlists.single {
                            it.id !in existingIds
                        }
                    assertEquals(
                        listOf("track", "track"),
                        repository.entries(created.id).map { it.trackId })
                }
                val before = repository.playlists().size
                val opening =
                    controller.beginOpen(
                        PlaylistBackupUiState(), PlaylistDocumentFormat.Pls)
                val preview =
                    controller.receiveOpen(
                        opening,
                        PlaylistBackupDocumentOpenResult.Success(
                            launcher.bytes),
                        listOf(track),
                        repository.playlists().map { it.name },
                        "Imported",
                        revision)
                revision++
                assertEquals(
                    PlaylistBackupUiError.StalePreview,
                    controller
                        .confirm(preview, loadPlaylistSnapshot(repository))
                        .state
                        .error)
                assertEquals(before, repository.playlists().size)
                Files.delete(media)
                val failed =
                    controller.beginExport(
                        PlaylistBackupUiState(),
                        loadPlaylistSnapshot(repository),
                        listOf(track),
                        1,
                        PlaylistDocumentFormat.Pls,
                        original.id,
                        ::isJvmPlaylistTrackReadable)
                assertEquals(
                    PlaylistBackupUiError.ExportMissingTrack, failed.error)
            } finally {
                database.driver.close()
                directory.toFile().deleteRecursively()
            }
        }

    private class CaptureLauncher : PlaylistBackupDocumentLauncher {
        override val isAvailable = true
        var bytes = byteArrayOf()
        var format = PlaylistDocumentFormat.RhythHausJson

        override fun save(suggestedFileName: String, bytes: ByteArray) =
            error("Expected explicit format")

        override fun open() = error("Expected explicit format")

        override fun save(
            suggestedFileName: String,
            bytes: ByteArray,
            format: PlaylistDocumentFormat
        ) {
            this.bytes = bytes
            this.format = format
        }

        override fun open(format: PlaylistDocumentFormat) {
            this.format = format
        }
    }
}
