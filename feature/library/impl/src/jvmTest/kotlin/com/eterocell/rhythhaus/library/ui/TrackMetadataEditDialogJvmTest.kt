package com.eterocell.rhythhaus.library.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.TrackMetadataEditorData
import com.eterocell.rhythhaus.library.TrackMetadataOverride
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class TrackMetadataEditDialogJvmTest {
    @Test
    fun restoreAllChangesOnlyTheDraftUntilSave() = runComposeUiTest {
        Locale.setDefault(Locale.ENGLISH)
        var draft by
            mutableStateOf(
                TrackMetadataDraft(
                    "Edited", "Edited artist", "Edited album", "2", "3"))
        var saved: TrackMetadataOverride? = null
        setContent {
            TrackMetadataEditDialog(
                data(), draft, false, false, { draft = it }, {}, { saved = it })
        }
        onNodeWithTag("metadata-restore-all").performScrollTo().performClick()
        assertEquals(TrackMetadataDraft(), draft)
        assertNull(saved)
        onNodeWithTag("metadata-save").performClick()
        assertEquals(TrackMetadataOverride(), saved)
    }

    @Test
    fun invalidNumberExplainsFailureAndPreventsPersistence() =
        runComposeUiTest {
            Locale.setDefault(Locale.ENGLISH)
            var saved: TrackMetadataOverride? = null
            setContent {
                TrackMetadataEditDialog(
                    data(),
                    TrackMetadataDraft(trackNumber = "0"),
                    false,
                    false,
                    {},
                    {},
                    { saved = it })
            }
            onNodeWithText(
                    "Track and disc numbers must be positive whole numbers.")
                .assertExists()
            onNodeWithTag("metadata-save").assertIsNotEnabled()
            assertNull(saved)
        }

    private fun data() =
        TrackMetadataEditorData(
            LibraryTrack(
                id = "track",
                sourceId = "source",
                sourceLocalKey = "track.mp3",
                audioSource = AudioSource.FilePath("/music/track.mp3"),
                displayName = "track.mp3",
                title = "Raw",
                artist = "Artist",
                album = "Album",
                durationMillis = null,
                sizeBytes = null,
                modifiedAtEpochMillis = null,
                lastSeenScanId = null,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L,
            ),
            TrackMetadataOverride(),
        )
}
