package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.impl.JvmFolderSourceAccess
import com.eterocell.rhythhaus.library.impl.PlatformScanEvent
import com.eterocell.rhythhaus.library.impl.createPlatformSourceAccess
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
        val album = Files.createDirectory(root.toPath().resolve("Album One")).toFile()
        album.deleteOnExit()
        Files.createFile(root.toPath().resolve("Album One/01 First.mp3"))
        Files.createFile(root.toPath().resolve("Album One/cover.jpg"))
        Files.createFile(root.toPath().resolve("readme.txt"))

        val access = createPlatformSourceAccess()
        val source = folderSource(root.absolutePath)
        val events = access.scan(source).toList()

        val foldersVisited =
            events.filterIsInstance<PlatformScanEvent.FolderVisited>()
                .map { it.displayPath }
        assertTrue(
            source.displayName in foldersVisited,
            "root folder must be reported with its display name: $foldersVisited")
        assertTrue(
            "Album One" in foldersVisited,
            "nested folder must be reported: $foldersVisited")

        val candidates =
            events.filterIsInstance<PlatformScanEvent.AudioCandidate>()
                .map { it.candidate }
        val first = candidates.single { it.displayName == "01 First.mp3" }
        assertEquals("Album One/01 First.mp3", first.sourceLocalKey)
        assertEquals("Album One/01 First.mp3", first.displayPath)
        assertEquals(
            AudioSource.FilePath(
                "$root/${"Album One/01 First.mp3"}"),
            first.audioSource,
        )

        val skipped =
            events.filterIsInstance<PlatformScanEvent.Skipped>()
                .map { it.sourceLocalKey to it.reason }
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
            access.scan(
                    folderSource("/nonexistent/rhythhaus-missing-folder"))
                .toList()
        }
        assertFails {
            access.scan(
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

    private fun folderSource(handle: String): LibrarySource =
        LibrarySource(
            id = "jvm-source",
            platformKind = LibraryPlatformKind.JvmFolder,
            displayName = "Music",
            handle = handle,
            createdAtEpochMillis = 1,
        )
}
