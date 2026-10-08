package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.impl.JvmFolderSourceAccess
import com.eterocell.rhythhaus.library.impl.PlatformScanEvent
import com.eterocell.rhythhaus.library.impl.createPlatformSourceAccess
import com.eterocell.rhythhaus.library.impl.normalizedSourceLocalKey
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PlatformSourceAccessJvmTest {
    @Test
    fun jvmFactoryCreatesFolderAccessAndReportsAvailabilityCausally() {
        val access = createPlatformSourceAccess()
        assertIs<JvmFolderSourceAccess>(access)

        val folder = Files.createTempDirectory("rhythhaus-jvm-access").toFile()
        folder.deleteOnExit()
        assertEquals(
            LibrarySourceAccessStatus.Available,
            access.accessStatus(folderSource(folder.absolutePath)),
        )
        assertEquals(
            LibrarySourceAccessStatus.LostAccess,
            access.accessStatus(
                folderSource("/nonexistent/rhythhaus-missing-folder")),
        )
        assertEquals(
            LibrarySourceAccessStatus.LostAccess,
            access.accessStatus(
                LibrarySource(
                    id = "saf",
                    platformKind = LibraryPlatformKind.AndroidSafTree,
                    displayName = "Saf",
                    handle = "content://tree/root",
                    createdAtEpochMillis = 1,
                ),
            ),
        )
    }

    @Test
    fun jvmScanYieldsVisitedFoldersCandidatesAndSkippedUnsupported() {
        val root = Files.createTempDirectory("rhythhaus-jvm-scan").toFile()
        root.deleteOnExit()
        val album =
            Files.createDirectory(root.toPath().resolve("Album One")).toFile()
        album.deleteOnExit()
        Files.createFile(root.toPath().resolve("Album One/01 First.mp3"))
        Files.createFile(root.toPath().resolve("Album One/cover.jpg"))
        Files.createFile(root.toPath().resolve("readme.txt"))

        val access = createPlatformSourceAccess()
        val source = folderSource(root.absolutePath)
        val events = access.scan(source).toList()

        val foldersVisited =
            events.filterIsInstance<PlatformScanEvent.FolderVisited>().map {
                it.displayPath
            }
        assertTrue(
            source.displayName in foldersVisited,
            "root folder must be reported with its display name: $foldersVisited")
        assertTrue(
            "Album One" in foldersVisited,
            "nested folder must be reported: $foldersVisited")

        val candidates =
            events.filterIsInstance<PlatformScanEvent.AudioCandidate>().map {
                it.candidate
            }
        val first = candidates.single { it.displayName == "01 First.mp3" }
        assertEquals("Album One/01 First.mp3", first.sourceLocalKey)
        assertEquals("Album One/01 First.mp3", first.displayPath)
        assertEquals(
            AudioSource.FilePath("$root/${"Album One/01 First.mp3"}"),
            first.audioSource,
        )

        val skipped =
            events.filterIsInstance<PlatformScanEvent.Skipped>().map {
                it.sourceLocalKey to it.reason
            }
        assertTrue(
            ("Album One/cover.jpg" to "Unsupported audio type") in skipped,
            "unsupported audio files must be skipped: $skipped")
        assertTrue(
            ("readme.txt" to "Unsupported audio type") in skipped,
            "non-audio files must be skipped: $skipped")
    }

    @Test
    fun jvmScanOfMissingOrForeignFolderFailsClosed() {
        val access = createPlatformSourceAccess()
        assertFails {
            access
                .scan(folderSource("/nonexistent/rhythhaus-missing-folder"))
                .toList()
        }
        assertFails {
            access
                .scan(
                    LibrarySource(
                        id = "saf",
                        platformKind = LibraryPlatformKind.AndroidSafTree,
                        displayName = "Saf",
                        handle = "content://tree/root",
                        createdAtEpochMillis = 1,
                    ),
                )
                .toList()
        }
    }

    @Test
    fun jvmScanOfDroppedFilesPreservesDropOrderAndOriginalPaths() {
        val root = Files.createTempDirectory("rhythhaus-jvm-drop-scan").toFile()
        try {
            val second =
                root.resolve("02 Second.mp3").apply {
                    writeBytes(byteArrayOf(2))
                }
            val first =
                root.resolve("01 First.flac").apply {
                    writeBytes(byteArrayOf(1))
                }
            val source =
                validateDesktopDrop(
                        paths = listOf(second.path, first.path),
                        createdAtEpochMillis = 1L,
                    )
                    .sources
                    .single()

            val candidates =
                createPlatformSourceAccess()
                    .scan(source)
                    .filterIsInstance<PlatformScanEvent.AudioCandidate>()
                    .map(PlatformScanEvent.AudioCandidate::candidate)
                    .toList()

            assertEquals(
                listOf(second.canonicalPath, first.canonicalPath).map {
                    it.normalizedSourceLocalKey()
                },
                candidates.map { it.sourceLocalKey },
            )
            assertEquals(
                listOf(
                    AudioSource.FilePath(second.canonicalPath),
                    AudioSource.FilePath(first.canonicalPath),
                ),
                candidates.map { it.audioSource },
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun deletedDroppedFileRemainsUntilExplicitRemoveMissing() {
        val root = Files.createTempDirectory("rhythhaus-drop-missing").toFile()
        try {
            val file =
                root.resolve("Original.mp3").apply {
                    writeBytes(byteArrayOf(1))
                }
            val source =
                validateDesktopDrop(listOf(file.path), 1L).sources.single()
            val repository = InMemoryLibraryRepository()
            var sequence = 0L
            val scanner =
                LibraryScanner(
                    repository = repository,
                    platformScanner = createPlatformSourceAccess(),
                    now = { ++sequence },
                    idFactory = { prefix -> "$prefix-${++sequence}" },
                )
            assertEquals(ScanStatus.Completed, scanner.scan(source).status)
            val track = repository.tracks().single()
            repository.setTrackFavorite(track.id, true)
            repository.recordTrackPlayed(track.id, 10L)
            assertTrue(file.delete())
            val rescan = scanner.scan(source)
            assertEquals(ScanStatus.Completed, rescan.status)
            assertEquals(1, rescan.changeSummary?.missingCount)
            assertEquals(track.id, repository.tracks().single().id)
            assertTrue(track.id in repository.favoriteTrackIds())
            assertEquals(1L, repository.playHistory()[track.id]?.playCount)
            assertEquals(
                RemoveMissingTracksResult.Removed(1),
                repository.removeMissingTracks(source.id, rescan.id))
            assertTrue(repository.tracks().isEmpty())
            assertTrue(repository.favoriteTrackIds().isEmpty())
            assertTrue(repository.playHistory().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun replacingDroppedPathWithSymlinkPreservesTrackAndUserState() {
        val root = Files.createTempDirectory("rhythhaus-drop-link").toFile()
        try {
            val file =
                root.resolve("Original.mp3").apply {
                    writeBytes(byteArrayOf(1))
                }
            val target =
                root.resolve("Target.mp3").apply { writeBytes(byteArrayOf(2)) }
            val source =
                validateDesktopDrop(listOf(file.path), 1L).sources.single()
            val repository = InMemoryLibraryRepository()
            var sequence = 0L
            val scanner =
                LibraryScanner(
                    repository = repository,
                    platformScanner = createPlatformSourceAccess(),
                    now = { ++sequence },
                    idFactory = { prefix -> "$prefix-${++sequence}" },
                )
            assertEquals(ScanStatus.Completed, scanner.scan(source).status)
            val track = repository.tracks().single()
            repository.setTrackFavorite(track.id, true)
            repository.recordTrackPlayed(track.id, 10L)
            assertTrue(file.delete())
            Files.createSymbolicLink(file.toPath(), target.toPath())
            val rescan = scanner.scan(source)
            assertEquals(ScanStatus.Completed, rescan.status)
            assertEquals(0, rescan.changeSummary?.missingCount)
            assertEquals(track.id, repository.tracks().single().id)
            assertEquals(
                track.audioSource, repository.tracks().single().audioSource)
            assertTrue(track.id in repository.favoriteTrackIds())
            assertEquals(1L, repository.playHistory()[track.id]?.playCount)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun existingNonRegularDroppedPathCannotAuthorizeMissingRemoval() {
        val root =
            Files.createTempDirectory("rhythhaus-drop-unreadable").toFile()
        try {
            val file =
                root.resolve("Original.mp3").apply {
                    writeBytes(byteArrayOf(1))
                }
            val source =
                validateDesktopDrop(listOf(file.path), 1L).sources.single()
            val repository = InMemoryLibraryRepository()
            var sequence = 0L
            val scanner =
                LibraryScanner(
                    repository = repository,
                    platformScanner = createPlatformSourceAccess(),
                    now = { ++sequence },
                    idFactory = { prefix -> "$prefix-${++sequence}" },
                )
            scanner.scan(source)
            val track = repository.tracks().single()
            repository.setTrackFavorite(track.id, true)
            repository.recordTrackPlayed(track.id, 10L)
            assertTrue(file.delete())
            assertTrue(file.mkdir())
            val rescan = scanner.scan(source)
            assertEquals(ScanStatus.Completed, rescan.status)
            assertEquals(0, rescan.changeSummary?.missingCount)
            assertEquals(
                RemoveMissingTracksResult.Removed(0),
                repository.removeMissingTracks(source.id, rescan.id))
            assertEquals(track.id, repository.tracks().single().id)
            assertTrue(track.id in repository.favoriteTrackIds())
            assertEquals(1L, repository.playHistory()[track.id]?.playCount)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun danglingDroppedSymlinkPreservesTrackUntilPathIsActuallyRemoved() {
        val root = Files.createTempDirectory("rhythhaus-drop-dangling").toFile()
        try {
            val file =
                root.resolve("Original.mp3").apply {
                    writeBytes(byteArrayOf(1))
                }
            val source =
                validateDesktopDrop(listOf(file.path), 1L).sources.single()
            val repository = InMemoryLibraryRepository()
            var sequence = 0L
            val scanner =
                LibraryScanner(
                    repository = repository,
                    platformScanner = createPlatformSourceAccess(),
                    now = { ++sequence },
                    idFactory = { prefix -> "$prefix-${++sequence}" },
                )
            scanner.scan(source)
            val track = repository.tracks().single()
            repository.setTrackFavorite(track.id, true)
            repository.recordTrackPlayed(track.id, 10L)
            assertTrue(file.delete())
            Files.createSymbolicLink(
                file.toPath(), root.resolve("Missing.mp3").toPath())
            val rescan = scanner.scan(source)
            assertEquals(0, rescan.changeSummary?.missingCount)
            assertEquals(
                RemoveMissingTracksResult.Removed(0),
                repository.removeMissingTracks(source.id, rescan.id))
            assertEquals(track.id, repository.tracks().single().id)
            assertTrue(track.id in repository.favoriteTrackIds())
            assertEquals(1L, repository.playHistory()[track.id]?.playCount)
            Files.delete(file.toPath())
            val missing = scanner.scan(source)
            assertEquals(1, missing.changeSummary?.missingCount)
            assertEquals(
                RemoveMissingTracksResult.Removed(1),
                repository.removeMissingTracks(source.id, missing.id))
        } finally {
            root.deleteRecursively()
        }
    }

    private fun folderSource(handle: String): LibrarySource =
        LibrarySource(
            id = "jvm-source",
            platformKind = LibraryPlatformKind.JvmFolder,
            displayName = "Music",
            handle = handle,
            createdAtEpochMillis = 1,
        )
}
