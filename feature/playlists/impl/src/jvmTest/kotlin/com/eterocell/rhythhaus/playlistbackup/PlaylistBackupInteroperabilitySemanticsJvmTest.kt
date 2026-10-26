package com.eterocell.rhythhaus.playlistbackup

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.eterocell.rhythhaus.library.PlaylistSummary
import com.eterocell.rhythhaus.library.ui.PlaylistFeatureDestination
import com.eterocell.rhythhaus.library.ui.PlaylistFeatureDismissal
import com.eterocell.rhythhaus.library.ui.PlaylistFeatureDismissalDispatch
import com.eterocell.rhythhaus.library.ui.PlaylistFeatureDismissalPublisher
import com.eterocell.rhythhaus.library.ui.rememberPlaylistFeatureAppearanceSource
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PlaylistBackupInteroperabilitySemanticsJvmTest {
    @Test
    fun inactiveSectionCannotClearTheSharedOperationError() = runComposeUiTest {
        setContent {
            PlaylistBackupSettingsSection(
                state =
                    PlaylistBackupUiState(
                        error = PlaylistBackupUiError.ReadFailed),
                launcherAvailable = false,
                labels = PlaylistBackupSettingsLabels("Cancel", "Close"),
                onExport = {},
                onOpen = {},
                onAction = {},
            )
        }
        onNodeWithText("Close").assertIsNotEnabled()
    }

    @Test
    fun confirmationFailureStaysVisibleAndStalePreviewCannotBeResubmitted() =
        runComposeUiTest {
            val state =
                mutableStateOf(
                    PlaylistBackupUiState(
                        preview =
                            PlaylistBackupPreview(
                                1,
                                emptyList(),
                                emptyList(),
                                PlaylistBackupCounts(1, 0, 0),
                                true)))
            var confirms = 0
            val destination = PlaylistFeatureDestination("feedback")
            setContent {
                PlaylistBackupSettingsHost(
                    state = state.value,
                    launcherAvailable = true,
                    showEntryActions = false,
                    destination = destination,
                    appearanceSource =
                        rememberPlaylistFeatureAppearanceSource(destination),
                    dismissalPublisher =
                        object : PlaylistFeatureDismissalPublisher {
                            override fun publish(
                                dismissal: PlaylistFeatureDismissal?,
                                dispatch:
                                    (
                                        PlaylistFeatureDismissal) -> PlaylistFeatureDismissalDispatch,
                            ): () -> Unit = {}
                        },
                    labels = PlaylistBackupSettingsLabels("Cancel", "Close"),
                    onExport = {},
                    onOpen = {},
                    onAction = {},
                    onDismissPreview = {},
                    onConfirmPreview = {
                        confirms++
                        state.value =
                            state.value.copy(
                                error = PlaylistBackupUiError.RepositoryFailed)
                    },
                    onDismissResult = {},
                )
            }
            onNodeWithTag("playlist-backup-preview-confirm").performClick()
            onNodeWithTag("playlist-backup-preview-error").assertIsDisplayed()
            onNodeWithTag("playlist-backup-preview-confirm").performClick()
            assertEquals(
                2,
                confirms,
                "repository failure permits retry of the same preview")
            runOnIdle {
                state.value =
                    state.value.copy(error = PlaylistBackupUiError.StalePreview)
            }
            onNodeWithTag("playlist-backup-preview-error").assertIsDisplayed()
            onNodeWithTag("playlist-backup-preview-confirm")
                .assertIsNotEnabled()
        }

    @Test
    fun jsonActionsNeverDispatchInteroperabilityOperations() =
        runComposeUiTest {
            var jsonExports = 0
            var jsonImports = 0
            val exports = mutableListOf<Pair<String, PlaylistDocumentFormat>>()
            val imports = mutableListOf<PlaylistDocumentFormat>()
            setContent {
                PlaylistBackupSettingsSection(
                    state = PlaylistBackupUiState(),
                    launcherAvailable = true,
                    labels = PlaylistBackupSettingsLabels("Cancel", "Close"),
                    onExport = { jsonExports++ },
                    onOpen = { jsonImports++ },
                    onAction = {},
                    staticPlaylists = listOf(playlist("saved")),
                    onExportPlaylistFormat = { id, format ->
                        exports += id to format
                    },
                    onOpenPlaylistFormat = { imports += it },
                )
            }

            onNodeWithTag("playlist-backup-json-export").performClick()
            onNodeWithTag("playlist-backup-json-import").performClick()

            assertEquals(1, jsonExports)
            assertEquals(1, jsonImports)
            assertEquals(emptyList(), exports)
            assertEquals(emptyList(), imports)
        }

    @Test
    fun interchangeActionsUseSelectedStaticPlaylistAndEachFormat() =
        runComposeUiTest {
            val exports = mutableListOf<Pair<String, PlaylistDocumentFormat>>()
            val imports = mutableListOf<PlaylistDocumentFormat>()
            setContent {
                PlaylistBackupSettingsSection(
                    state = PlaylistBackupUiState(),
                    launcherAvailable = true,
                    labels = PlaylistBackupSettingsLabels("Cancel", "Close"),
                    onExport = {},
                    onOpen = {},
                    onAction = {},
                    staticPlaylists = listOf(playlist("one"), playlist("two")),
                    onExportPlaylistFormat = { id, format ->
                        exports += id to format
                    },
                    onOpenPlaylistFormat = { imports += it },
                )
            }

            onNodeWithTag("playlist-backup-static-two").performClick()
            waitForIdle()
            listOf(
                    "playlist-backup-format-m3u" to PlaylistDocumentFormat.M3u,
                    "playlist-backup-format-m3u8" to
                        PlaylistDocumentFormat.M3u8,
                    "playlist-backup-format-pls" to PlaylistDocumentFormat.Pls,
                )
                .forEach { (tag, format) ->
                    onNodeWithTag(tag).performClick()
                    waitForIdle()
                    onNodeWithTag(PlaylistBackupStaticExportTag).performClick()
                    onNodeWithTag(PlaylistBackupFormatImportTag).performClick()
                    waitForIdle()
                    assertEquals("two" to format, exports.last())
                    assertEquals(format, imports.last())
                }
            assertEquals(3, exports.size)
            assertEquals(3, imports.size)
        }

    @Test
    fun staticExportStaysDisabledWithoutSavedPlaylistOrDuringBusyOperation() =
        runComposeUiTest {
            setContent {
                PlaylistBackupSettingsSection(
                    state = PlaylistBackupUiState(),
                    launcherAvailable = true,
                    labels = PlaylistBackupSettingsLabels("Cancel", "Close"),
                    onExport = {},
                    onOpen = {},
                    onAction = {},
                    onExportPlaylistFormat = { _, _ -> },
                    onOpenPlaylistFormat = {},
                )
            }
            onNodeWithTag(PlaylistBackupStaticExportTag).assertIsNotEnabled()

            setContent {
                PlaylistBackupSettingsSection(
                    state =
                        PlaylistBackupUiState(
                            operation = PlaylistBackupOperation.Exporting,
                        ),
                    launcherAvailable = true,
                    labels = PlaylistBackupSettingsLabels("Cancel", "Close"),
                    onExport = {},
                    onOpen = {},
                    onAction = {},
                    staticPlaylists = listOf(playlist("saved")),
                    onExportPlaylistFormat = { _, _ -> },
                    onOpenPlaylistFormat = {},
                )
            }
            onNodeWithTag(PlaylistBackupStaticExportTag).assertIsNotEnabled()
            onNodeWithTag(PlaylistBackupFormatImportTag).assertIsNotEnabled()
        }

    private fun playlist(id: String) =
        PlaylistSummary(id, "Playlist $id", 1L, 1L)
}
