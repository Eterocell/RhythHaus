package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.impl.MediaStoreAudioRow
import com.eterocell.rhythhaus.library.impl.PlatformScanEvent
import com.eterocell.rhythhaus.library.impl.mediaStoreAudioEvents
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class MediaStoreAudioScanTest {
    @Test
    fun extensionlessSupportedMimeIsImportedAndUnknownMimeIsSkipped() {
        val events =
            mediaStoreAudioEvents(
                    source,
                    listOf(
                        row(1, "audio").copy(mimeType = "audio/mpeg"),
                        row(2, "unknown").copy(mimeType = "audio/unsupported"),
                    )) {}
                .toList()
        assertIs<PlatformScanEvent.AudioCandidate>(events[0])
        assertIs<PlatformScanEvent.Skipped>(events[1])
    }

    @Test
    fun unreadableFileIsReportedAndReadableSiblingsContinue() {
        val events =
            mediaStoreAudioEvents(
                    source,
                    listOf(row(1, "missing.mp3"), row(2, "readable.mp3"))) {
                        if (it.endsWith("/1"))
                            throw java.io.FileNotFoundException("Deleted")
                    }
                .toList()
        val skipped = assertIs<PlatformScanEvent.Skipped>(events[0])
        assertEquals(true, skipped.recoverable)
        assertEquals(
            "mediastore:2",
            assertIs<PlatformScanEvent.AudioCandidate>(events[1])
                .candidate
                .sourceLocalKey)
    }

    private val source =
        LibrarySource(
            "android-mediastore-audio",
            LibraryPlatformKind.AndroidMediaStoreAudio,
            "Device audio",
            "android-mediastore-audio",
            1)

    private fun row(id: Long, name: String) =
        MediaStoreAudioRow(
            id, name, "content://media/external/audio/media/$id", 42, 2000)

    @Test
    fun supportedRowsKeepIdentityAndUriAcrossRenames() {
        val before =
            mediaStoreAudioEvents(source, listOf(row(9, "before.mp3"))) {}
                .single()
        val after =
            mediaStoreAudioEvents(source, listOf(row(9, "after.mp3"))) {}
                .single()
        val original =
            assertIs<PlatformScanEvent.AudioCandidate>(before).candidate
        val renamed =
            assertIs<PlatformScanEvent.AudioCandidate>(after).candidate
        assertEquals("mediastore:9", original.sourceLocalKey)
        assertEquals(original.sourceLocalKey, renamed.sourceLocalKey)
        assertEquals(
            AudioSource.Uri("content://media/external/audio/media/9"),
            renamed.audioSource)
        assertEquals(42L, renamed.sizeBytes)
        assertEquals(2000L, renamed.modifiedAtEpochMillis)
    }

    @Test
    fun unsupportedRowsNeverOpenMediaAndEarlyExitDoesNotOpenSuccessors() {
        val opened = mutableListOf<String>()
        val events =
            mediaStoreAudioEvents(
                source,
                listOf(
                    row(1, "cover.jpg"),
                    row(2, "song.mp3"),
                    row(3, "later.flac"))) {
                    opened += it
                }
        val iterator = events.iterator()
        assertIs<PlatformScanEvent.Skipped>(iterator.next())
        assertEquals(emptyList(), opened)
        assertIs<PlatformScanEvent.AudioCandidate>(iterator.next())
        assertEquals(listOf("content://media/external/audio/media/2"), opened)
    }

    @Test
    fun unreadableRowFailsInsteadOfPublishingCompletedRemovalEvidence() {
        assertFailsWith<SecurityException> {
            mediaStoreAudioEvents(source, listOf(row(1, "song.mp3"))) {
                    throw SecurityException("Permission revoked")
                }
                .toList()
        }
    }

    @Test
    fun foreignSourceCannotEnterMediaStoreScanner() {
        assertFailsWith<IllegalArgumentException> {
            mediaStoreAudioEvents(
                    source.copy(
                        platformKind = LibraryPlatformKind.AndroidSafTree),
                    listOf(row(1, "song.mp3"))) {}
                .toList()
        }
    }
}
