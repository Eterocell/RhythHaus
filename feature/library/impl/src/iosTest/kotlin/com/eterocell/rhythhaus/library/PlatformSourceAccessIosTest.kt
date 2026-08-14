package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.library.impl.IOSAppLocalSourceAccess
import com.eterocell.rhythhaus.library.impl.PlatformScanEvent
import com.eterocell.rhythhaus.library.impl.createPlatformSourceAccess
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory

@OptIn(ExperimentalForeignApi::class)
class PlatformSourceAccessIosTest {
    @Test
    fun iosFactoryCreatesAppLocalAccessAndReportsMissingSourcesLost() {
        val access = createPlatformSourceAccess()
        assertIs<IOSAppLocalSourceAccess>(access)

        assertEquals(
            LibrarySourceAccessStatus.LostAccess,
            access.accessStatus(
                LibrarySource(
                    id = "missing",
                    platformKind = LibraryPlatformKind.IosAppLocal,
                    displayName = "Music",
                    handle = "/nonexistent-app-local-music-folder",
                    createdAtEpochMillis = 1,
                ),
            ),
        )
        assertEquals(
            LibrarySourceAccessStatus.LostAccess,
            access.accessStatus(
                LibrarySource(
                    id = "jvm",
                    platformKind = LibraryPlatformKind.JvmFolder,
                    displayName = "Music",
                    handle = "/tmp/music",
                    createdAtEpochMillis = 1,
                ),
            ),
        )
    }

    @Test
    fun nonAppLocalSourcesGuardTheirScan() {
        val access = createPlatformSourceAccess()
        assertFails {
            access
                .scan(
                    LibrarySource(
                        id = "jvm",
                        platformKind = LibraryPlatformKind.JvmFolder,
                        displayName = "Music",
                        handle = "/tmp/music",
                        createdAtEpochMillis = 1,
                    ),
                )
                .toList()
        }
    }

    @Test
    fun appLocalScanReportsFolderAndAudioCandidates() {
        val fileManager = NSFileManager.defaultManager
        val folder =
            NSTemporaryDirectory() +
                "rhythhaus-ios-scan-${Random.nextInt(0, Int.MAX_VALUE)}"
        fileManager.createDirectoryAtPath(
            path = folder,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        try {
            assertEquals(true, fileManager.fileExistsAtPath(folder))
            val songPath = "$folder/01 First.mp3"
            assertEquals(
                true,
                fileManager.createFileAtPath(
                    path = songPath, contents = null, attributes = null),
            )
            val coverPath = "$folder/cover.jpg"
            assertEquals(
                true,
                fileManager.createFileAtPath(
                    path = coverPath, contents = null, attributes = null),
            )

            val access = createPlatformSourceAccess()
            val source =
                LibrarySource(
                    id = "ios",
                    platformKind = LibraryPlatformKind.IosAppLocal,
                    displayName = "Music",
                    handle = folder,
                    createdAtEpochMillis = 1,
                )
            assertEquals(
                LibrarySourceAccessStatus.Available,
                access.accessStatus(source),
            )

            val events = access.scan(source).toList()
            val foldersVisited =
                events.filterIsInstance<PlatformScanEvent.FolderVisited>().map {
                    it.displayPath
                }
            assertTrue(
                "Music" in foldersVisited,
                "root folder must be reported with its display name: " +
                    foldersVisited)

            val candidates =
                events
                    .filterIsInstance<PlatformScanEvent.AudioCandidate>()
                    .map { it.candidate }
            val first = candidates.single { it.displayName == "01 First.mp3" }
            assertEquals("01 First.mp3", first.sourceLocalKey)
            assertEquals("01 First.mp3", first.displayPath)

            val skipped =
                events.filterIsInstance<PlatformScanEvent.Skipped>().map {
                    it.sourceLocalKey to it.reason
                }
            assertTrue(
                ("cover.jpg" to "Unsupported audio type") in skipped,
                "unsupported audio files must be skipped: $skipped")
        } finally {
            fileManager.removeItemAtPath(folder, error = null)
        }
    }
}
