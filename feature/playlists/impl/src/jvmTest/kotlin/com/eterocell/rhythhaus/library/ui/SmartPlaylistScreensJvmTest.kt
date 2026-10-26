package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlaylistSummary
import com.eterocell.rhythhaus.library.SmartPlaylistRule
import com.eterocell.rhythhaus.library.SmartPlaylistSummary
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SmartPlaylistScreensJvmTest {
    @Test
    fun allSixRulesRequireAvailableOptionsAndValidRecencyCounts() {
        val source = PlaylistSummary("source", "Mix", 1, 1)
        val snapshot = PlaylistSnapshot(playlists = listOf(source))
        val tracks =
            listOf(
                LibraryTrack(
                    "a",
                    "source",
                    "a",
                    AudioSource.FilePath("a.mp3"),
                    "a.mp3",
                    "Song",
                    "Artist",
                    "Album",
                    1000,
                    100,
                    null,
                    null,
                    1,
                    1))
        listOf(
                SmartPlaylistRule.Favorites,
                SmartPlaylistRule.RecentlyPlayed(10),
                SmartPlaylistRule.RecentlyPlayed(25),
                SmartPlaylistRule.RecentlyAdded(50),
                SmartPlaylistRule.Artist("Artist"),
                SmartPlaylistRule.Album("Artist", "Album"),
                SmartPlaylistRule.SavedPlaylist("source"),
            )
            .forEach { rule ->
                kotlin.test.assertTrue(
                    validSmartDraft(
                        SmartDraft(
                            PlaylistDismissalAppearance("draft"),
                            "Named",
                            rule),
                        snapshot,
                        tracks),
                    "$rule")
            }
        kotlin.test.assertFalse(
            validSmartDraft(
                SmartDraft(
                    PlaylistDismissalAppearance("draft"),
                    "Named",
                    SmartPlaylistRule.SavedPlaylist("gone")),
                snapshot,
                tracks))
        kotlin.test.assertFalse(
            validSmartDraft(
                SmartDraft(
                    PlaylistDismissalAppearance("draft"),
                    "Named",
                    SmartPlaylistRule.RecentlyAdded(11)),
                snapshot,
                tracks))
    }

    @Test
    fun existingArtistOrAlbumCanBeRenamedAfterLastMatchingTrackDisappears() {
        for (original in
            listOf(
                SmartPlaylistRule.Artist("Former artist"),
                SmartPlaylistRule.Album("Former artist", "Former album"),
                SmartPlaylistRule.SavedPlaylist("deleted-static-source"),
            )) {
            val renamed =
                SmartDraft(
                    PlaylistDismissalAppearance("edit"), "New name", original)
            kotlin.test.assertTrue(
                validSmartDraft(
                    renamed, PlaylistSnapshot(), emptyList(), original))
            kotlin.test.assertFalse(
                validSmartDraft(renamed, PlaylistSnapshot(), emptyList()))
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun smartCreateRetriesFailureWithoutDuplicatingPendingWritesAndIgnoresDismissedCallback() =
        runComposeUiTest {
            val locale = Locale.getDefault()
            Locale.setDefault(Locale.ENGLISH)
            try {
                val completions = mutableListOf<(PlaylistStateAction) -> Unit>()
                val destination = PlaylistFeatureDestination("smart-hub")
                setContent {
                    Box(Modifier.size(600.dp, 400.dp)) {
                        PlaylistHubScreen(
                            state = PlaylistState(hasConfirmedSnapshot = true),
                            destination = destination,
                            appearanceSource =
                                rememberPlaylistFeatureAppearanceSource(
                                    destination),
                            dismissalPublisher = noDismissalPublisher(),
                            playlistsLabel = "Playlists",
                            loadingLabel = "Loading",
                            loadFailedLabel = "Failed",
                            retryLabel = "Retry",
                            mutationFailedLabel = "Failed",
                            onBack = {},
                            onOpenPlaylist = {},
                            onCreate = { _, _ -> },
                            libraryTracks = emptyList(),
                            onOpenSmartPlaylist = {},
                            onCreateSmartPlaylist = { _, _, done ->
                                completions += done
                            },
                            onRetry = {},
                            documentActions = {},
                        )
                    }
                }
                onAllNodes(hasText("Create playlist"))[0].performClick()
                onAllNodes(hasText("Smart playlist", substring = false))[0]
                    .performClick()
                onAllNodes(hasContentDescription("Playlist name"))
                    .assertCountEquals(1)
                onAllNodes(hasTestTag("smart-playlist-name"))[0]
                    .performTextInput("Favorites mix")
                onAllNodes(hasText("Save"))[0].performClick()
                onAllNodes(hasText("Save"))[0].performClick()
                assertEquals(1, completions.size)
                completions[0](PlaylistStateAction.MutationFailed("db"))
                waitForIdle()
                onAllNodes(hasText("Could not save playlist changes"))
                    .assertCountEquals(1)
                onAllNodes(hasText("Save"))[0].performClick()
                assertEquals(2, completions.size)
                onAllNodes(hasText("Cancel"))[0].performClick()
                onAllNodes(hasText("Create playlist"))[0].performClick()
                onAllNodes(hasText("Smart playlist", substring = false))[0]
                    .performClick()
                onAllNodes(hasText("Create smart playlist"))[0].performClick()
                completions[1](
                    PlaylistStateAction.SnapshotConfirmed(PlaylistSnapshot()))
                waitForIdle()
                onAllNodes(hasText("Save")).assertCountEquals(1)
            } finally {
                Locale.setDefault(locale)
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun smartRuleSelectionShowsOnlyItsParametersAndKeepsSaveReachable() =
        runComposeUiTest {
            val oldLocale = Locale.getDefault()
            Locale.setDefault(Locale.ENGLISH)
            try {
                val tracks =
                    List(30) { index ->
                        val id = index.toString().padStart(2, '0')
                        LibraryTrack(
                            "track-$id",
                            "source",
                            "track-$id",
                            AudioSource.FilePath("track-$id.mp3"),
                            "track-$id.mp3",
                            "Track $id",
                            "Artist $id",
                            "Album $id",
                            1_000,
                            100,
                            null,
                            null,
                            1,
                            1,
                        )
                    }
                val destination = PlaylistFeatureDestination("long-smart-rule")
                var created: Pair<String, SmartPlaylistRule>? = null
                val snapshot = PlaylistSnapshot()
                setContent {
                    Box(Modifier.size(600.dp, 400.dp)) {
                        PlaylistHubScreen(
                            state = PlaylistState(hasConfirmedSnapshot = true),
                            destination = destination,
                            appearanceSource =
                                rememberPlaylistFeatureAppearanceSource(
                                    destination),
                            dismissalPublisher = noDismissalPublisher(),
                            playlistsLabel = "Playlists",
                            loadingLabel = "Loading",
                            loadFailedLabel = "Failed",
                            retryLabel = "Retry",
                            mutationFailedLabel = "Failed",
                            onBack = {},
                            onOpenPlaylist = {},
                            onCreate = { _, _ -> },
                            libraryTracks = tracks,
                            onOpenSmartPlaylist = {},
                            onCreateSmartPlaylist = { name, rule, done ->
                                created = name to rule
                                done(
                                    PlaylistStateAction.SnapshotConfirmed(
                                        snapshot.copy(
                                            smartPlaylists =
                                                listOf(
                                                    SmartPlaylistSummary(
                                                        "created",
                                                        name,
                                                        rule,
                                                        1,
                                                        1,
                                                    )))))
                            },
                            onRetry = {},
                            documentActions = {},
                        )
                    }
                }

                onAllNodes(hasText("Create playlist"))[0].performClick()
                onAllNodes(hasText("Smart playlist"))[0].performClick()
                onAllNodes(hasTestTag("smart-playlist-name"))[0]
                    .performTextInput("Long artist list")
                onAllNodes(hasContentDescription("Artist 29"))
                    .assertCountEquals(0)
                onAllNodes(hasContentDescription("Album 29"))
                    .assertCountEquals(0)

                onAllNodes(hasContentDescription("Artist")).assertCountEquals(1)
                onAllNodes(hasContentDescription("Artist"))[0]
                    .performScrollTo()
                    .performClick()
                waitForIdle()
                onAllNodes(hasContentDescription("Artist 00"))
                    .assertCountEquals(1)
                onAllNodes(hasContentDescription("Artist 29"))[0]
                    .performScrollTo()
                    .assertIsDisplayed()
                    .performClick()
                waitForIdle()
                onAllNodes(hasContentDescription("Album 29"))
                    .assertCountEquals(0)
                onAllNodes(hasText("Save"))[0]
                    .assertIsDisplayed()
                    .performClick()

                assertEquals(
                    "Long artist list" to SmartPlaylistRule.Artist("Artist 29"),
                    created,
                )
            } finally {
                Locale.setDefault(oldLocale)
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun smartEditorKeepsDraftAndActionsReachableAcrossLargeTextReflow() =
        runComposeUiTest {
            val oldLocale = Locale.getDefault()
            Locale.setDefault(Locale.ENGLISH)
            try {
                var width by mutableStateOf(390.dp)
                var draft by
                    mutableStateOf(
                        SmartDraft(
                            PlaylistDismissalAppearance("adaptive-editor")))
                var confirmedName: String? = null
                setContent {
                    CompositionLocalProvider(
                        LocalDensity provides Density(1f, 2f)) {
                            Box(Modifier.size(width, 640.dp)) {
                                SmartRuleDialog(
                                    draft = draft,
                                    libraryTracks = emptyList(),
                                    snapshot = PlaylistSnapshot(),
                                    title = "Create smart playlist",
                                    onChange = { draft = it },
                                    onDismiss = {},
                                    onConfirm = { confirmedName = draft.name },
                                )
                            }
                        }
                }
                onAllNodes(hasTestTag("smart-playlist-name"))[0]
                    .performTextInput("Retained draft")
                val compactPanel =
                    onAllNodes(hasTestTag("HausDialogPanel"))[0]
                        .fetchSemanticsNode()
                        .boundsInRoot
                assertTrue(
                    compactPanel.height >= 512f,
                    "compact rule editing must use most of the available height")
                onAllNodes(hasText("Save"))[0].assertIsDisplayed()
                onAllNodes(hasText("Cancel"))[0].assertIsDisplayed()
                runOnIdle { width = 900.dp }
                waitForIdle()
                val widePanel =
                    onAllNodes(hasTestTag("HausDialogPanel"))[0]
                        .fetchSemanticsNode()
                        .boundsInRoot
                assertTrue(
                    widePanel.width <= 640f,
                    "wide rule editing must not stretch across the window")
                assertTrue(
                    widePanel.height <= 592f,
                    "editor must leave room around the panel in a short window")
                assertEquals("Retained draft", draft.name)
                onAllNodes(hasText("Cancel"))[0].assertIsDisplayed()
                onAllNodes(hasText("Save"))[0]
                    .assertIsDisplayed()
                    .performClick()
                assertEquals("Retained draft", confirmedName)
            } finally {
                Locale.setDefault(oldLocale)
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun duplicateSourceRowsRetainDistinctSelectionAndDoNotExposeStaticEntryActions() =
        runComposeUiTest {
            val destination = PlaylistFeatureDestination("smart-detail")
            var selected: SavedPlaylistPlaybackRequest? = null
            val smart =
                SmartPlaylistSummary(
                    "smart", "Duplicates", SmartPlaylistRule.Favorites, 1, 1)
            val track =
                PlayableTrack(
                    "a",
                    "Song",
                    "Artist",
                    "Album",
                    1000,
                    AudioSource.FilePath("a.mp3"))
            setContent {
                SmartPlaylistDetailScreen(
                    playlist = smart,
                    projection =
                        SmartPlaylistProjection(
                            listOf(
                                SmartPlaylistRow("smart:1", "a"),
                                SmartPlaylistRow("smart:2", "a"))),
                    playableTracksById = mapOf("a" to track),
                    libraryTracks = emptyList(),
                    state =
                        PlaylistState(
                            confirmedSnapshot =
                                PlaylistSnapshot(
                                    smartPlaylists = listOf(smart)),
                            hasConfirmedSnapshot = true),
                    destination = destination,
                    appearanceSource =
                        rememberPlaylistFeatureAppearanceSource(destination),
                    dismissalPublisher = noDismissalPublisher(),
                    mutationFailedLabel = "Failed",
                    onBack = {},
                    onRetry = {},
                    onUpdate = { _, _, _ -> },
                    onDelete = {},
                    onDeleteConfirmed = {},
                    onPlayEntry = { selected = it },
                )
            }
            onAllNodes(hasText("Song")).assertCountEquals(2)
            onAllNodes(hasText("Song"))[1].performClick()
            assertEquals("smart:2", selected?.selectedOccurrenceId)
            assertEquals(
                listOf("smart:1", "smart:2"),
                selected?.occurrences?.map { it.id })
            onAllNodes(hasText("Add tracks")).assertCountEquals(0)
            onAllNodes(hasText("Reorder")).assertCountEquals(0)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun smartRuleDialogAtLargeTextKeepsLongParameterListsAndActionsReachable() =
        runComposeUiTest {
            val oldLocale = Locale.getDefault()
            Locale.setDefault(Locale.ENGLISH)
            try {
                val tracks =
                    List(24) { index ->
                        val id = index.toString().padStart(2, '0')
                        LibraryTrack(
                            "track-$id",
                            "source",
                            "track-$id",
                            AudioSource.FilePath("track-$id.mp3"),
                            "track-$id.mp3",
                            "Track $id",
                            "Artist $id",
                            "Album $id",
                            1_000,
                            100,
                            null,
                            null,
                            1,
                            1,
                        )
                    }
                val saved =
                    List(18) { index ->
                        val id = index.toString().padStart(2, '0')
                        PlaylistSummary(
                            "saved-$id",
                            "Collected set $id for the archive",
                            1,
                            1)
                    }
                val lastArtist = "Artist 23"
                val lastAlbum = "Artist 23 — Album 23"
                val lastSaved = "Collected set 17 for the archive"
                var draft by
                    mutableStateOf(
                        SmartDraft(PlaylistDismissalAppearance("large-text")))
                var confirmed: SmartDraft? = null
                var dismissed = false
                setContent {
                    CompositionLocalProvider(
                        LocalDensity provides Density(1f, 2f)) {
                            Box(Modifier.size(600.dp, 640.dp)) {
                                SmartRuleDialog(
                                    draft = draft,
                                    libraryTracks = tracks,
                                    snapshot =
                                        PlaylistSnapshot(playlists = saved),
                                    title = "Create smart playlist",
                                    onChange = { draft = it },
                                    onDismiss = { dismissed = true },
                                    onConfirm = { confirmed = draft },
                                )
                            }
                        }
                }
                fun assertActionsReachable() {
                    onAllNodes(hasText("Save"))[0]
                        .assertIsDisplayed()
                        .assertIsEnabled()
                    onAllNodes(hasText("Cancel"))[0]
                        .assertIsDisplayed()
                        .assertIsEnabled()
                }

                onAllNodes(hasTestTag("smart-playlist-name"))[0]
                    .performTextInput("Large text rules")
                onAllNodes(hasContentDescription(lastArtist))
                    .assertCountEquals(0)
                onAllNodes(hasContentDescription(lastAlbum))
                    .assertCountEquals(0)
                onAllNodes(hasContentDescription(lastSaved))
                    .assertCountEquals(0)
                assertActionsReachable()

                onAllNodes(hasContentDescription("Artist"))[0]
                    .performScrollTo()
                    .performClick()
                waitForIdle()
                onAllNodes(hasContentDescription(lastAlbum))
                    .assertCountEquals(0)
                onAllNodes(hasContentDescription(lastSaved))
                    .assertCountEquals(0)
                onAllNodes(hasContentDescription(lastArtist))[0]
                    .performScrollTo()
                    .assertIsDisplayed()
                    .performClick()
                waitForIdle()
                assertEquals(SmartPlaylistRule.Artist(lastArtist), draft.rule)
                assertActionsReachable()

                onAllNodes(hasContentDescription("Album"))[0]
                    .performScrollTo()
                    .performClick()
                waitForIdle()
                onAllNodes(hasContentDescription(lastArtist))
                    .assertCountEquals(0)
                onAllNodes(hasContentDescription(lastSaved))
                    .assertCountEquals(0)
                onAllNodes(hasContentDescription(lastAlbum))[0]
                    .performScrollTo()
                    .assertIsDisplayed()
                    .performClick()
                waitForIdle()
                assertEquals(
                    SmartPlaylistRule.Album("Artist 23", "Album 23"),
                    draft.rule)
                assertActionsReachable()

                onAllNodes(hasContentDescription("Saved playlist"))[0]
                    .performScrollTo()
                    .performClick()
                waitForIdle()
                onAllNodes(hasContentDescription(lastArtist))
                    .assertCountEquals(0)
                onAllNodes(hasContentDescription(lastAlbum))
                    .assertCountEquals(0)
                onAllNodes(hasContentDescription(lastSaved))[0]
                    .performScrollTo()
                    .assertIsDisplayed()
                    .performClick()
                waitForIdle()
                assertEquals(
                    SmartPlaylistRule.SavedPlaylist("saved-17"), draft.rule)
                assertActionsReachable()
                onAllNodes(hasText("Save"))[0].performClick()
                assertEquals(
                    SmartDraft(
                        PlaylistDismissalAppearance("large-text"),
                        "Large text rules",
                        SmartPlaylistRule.SavedPlaylist("saved-17")),
                    confirmed,
                )
                onAllNodes(hasText("Cancel"))[0].performClick()
                assertTrue(dismissed)
            } finally {
                Locale.setDefault(oldLocale)
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun smartDetailEditKeepsOnePendingUpdateAndIgnoresDismissedCallback() =
        runComposeUiTest {
            val oldLocale = Locale.getDefault()
            Locale.setDefault(Locale.ENGLISH)
            try {
                val smart =
                    SmartPlaylistSummary(
                        "smart",
                        "Duplicates",
                        SmartPlaylistRule.Favorites,
                        1,
                        1)
                val updated =
                    smart.copy(rule = SmartPlaylistRule.RecentlyPlayed(50))
                val destination =
                    PlaylistFeatureDestination("smart-edit-pending")
                val publisher = PlatformDismissalPublisher()
                val requests = mutableListOf<Pair<String, SmartPlaylistRule>>()
                val completions = mutableListOf<(PlaylistStateAction) -> Unit>()
                setContent {
                    Box(Modifier.size(600.dp, 640.dp)) {
                        SmartPlaylistDetailScreen(
                            playlist = smart,
                            projection =
                                SmartPlaylistProjection(
                                    listOf(SmartPlaylistRow("smart:1", "a"))),
                            playableTracksById =
                                mapOf(
                                    "a" to
                                        PlayableTrack(
                                            "a",
                                            "Song",
                                            "Artist",
                                            "Album",
                                            1000,
                                            AudioSource.FilePath("a.mp3"))),
                            libraryTracks = emptyList(),
                            state =
                                PlaylistState(
                                    confirmedSnapshot =
                                        PlaylistSnapshot(
                                            smartPlaylists = listOf(smart)),
                                    hasConfirmedSnapshot = true),
                            destination = destination,
                            appearanceSource =
                                rememberPlaylistFeatureAppearanceSource(
                                    destination),
                            dismissalPublisher = publisher,
                            mutationFailedLabel = "Failed",
                            onBack = {},
                            onRetry = {},
                            onUpdate = { name, rule, done ->
                                requests += name to rule
                                completions += done
                            },
                            onDelete = {},
                            onDeleteConfirmed = {},
                            onPlayEntry = {},
                        )
                    }
                }
                onAllNodes(hasText("Edit smart playlist")).assertCountEquals(1)
                onAllNodes(hasText("Edit smart playlist"))[0].performClick()
                waitForIdle()
                onAllNodes(hasContentDescription("Favorites"))[0]
                    .assertIsSelected()
                onAllNodes(hasContentDescription("Recently played"))[0]
                    .performScrollTo()
                    .performClick()
                waitForIdle()
                onAllNodes(hasContentDescription("50"))[0]
                    .performScrollTo()
                    .performClick()
                waitForIdle()

                onAllNodes(hasText("Save"))[0]
                    .assertIsDisplayed()
                    .performClick()
                assertEquals(
                    listOf<Pair<String, SmartPlaylistRule>>(
                        "Duplicates" to SmartPlaylistRule.RecentlyPlayed(50)),
                    requests)

                onAllNodes(hasText("Save"))[0].performClick()
                onAllNodes(hasContentDescription("Recently added"))[0]
                    .performScrollTo()
                    .performClick()
                waitForIdle()
                assertEquals(1, requests.size)
                onAllNodes(hasContentDescription("Recently played"))[0]
                    .assertIsSelected()
                onAllNodes(hasContentDescription("Recently added"))[0]
                    .assertIsNotSelected()
                onAllNodes(hasContentDescription("50"))[0].assertIsSelected()

                completions[0](PlaylistStateAction.MutationFailed("db"))
                waitForIdle()
                onAllNodes(hasText("Could not save playlist changes"))
                    .assertCountEquals(1)
                onAllNodes(hasContentDescription("Recently played"))[0]
                    .assertIsSelected()
                onAllNodes(hasContentDescription("50"))[0].assertIsSelected()
                onAllNodes(hasText("Save"))[0].performClick()
                assertEquals(2, requests.size)
                assertEquals(requests[0], requests[1])

                assertEquals(
                    PlaylistFeatureDismissalDispatch.Started,
                    publisher.dismissCurrent())
                waitForIdle()
                onAllNodes(hasText("Save")).assertCountEquals(0)
                completions[1](PlaylistStateAction.MutationFailed("db"))
                waitForIdle()
                onAllNodes(hasText("Save")).assertCountEquals(0)
                onAllNodes(hasText("Could not save playlist changes"))
                    .assertCountEquals(0)

                onAllNodes(hasText("Edit smart playlist"))[0].performClick()
                waitForIdle()
                onAllNodes(hasText("Save")).assertCountEquals(1)
                completions[1](
                    PlaylistStateAction.SnapshotConfirmed(
                        PlaylistSnapshot(smartPlaylists = listOf(updated))))
                waitForIdle()
                onAllNodes(hasText("Save")).assertCountEquals(1)
                onAllNodes(hasText("Could not save playlist changes"))
                    .assertCountEquals(0)
                onAllNodes(hasText("Save"))[0].performClick()
                assertEquals(
                    "Duplicates" to SmartPlaylistRule.Favorites, requests[2])
                completions[2](
                    PlaylistStateAction.SnapshotConfirmed(
                        PlaylistSnapshot(smartPlaylists = listOf(updated))))
                waitForIdle()
                onAllNodes(hasText("Save")).assertCountEquals(0)
            } finally {
                Locale.setDefault(oldLocale)
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun smartDetailDeleteKeepsOnePendingDeleteAndIgnoresDismissedCallback() =
        runComposeUiTest {
            val oldLocale = Locale.getDefault()
            Locale.setDefault(Locale.ENGLISH)
            try {
                val smart =
                    SmartPlaylistSummary(
                        "smart",
                        "Duplicates",
                        SmartPlaylistRule.Favorites,
                        1,
                        1)
                val destination =
                    PlaylistFeatureDestination("smart-delete-pending")
                val publisher = PlatformDismissalPublisher()
                val requests = mutableListOf<(PlaylistStateAction) -> Unit>()
                var confirmedSnapshot: PlaylistSnapshot? = null
                setContent {
                    Box(Modifier.size(600.dp, 640.dp)) {
                        SmartPlaylistDetailScreen(
                            playlist = smart,
                            projection = SmartPlaylistProjection(emptyList()),
                            playableTracksById = emptyMap(),
                            libraryTracks = emptyList(),
                            state =
                                PlaylistState(
                                    confirmedSnapshot =
                                        PlaylistSnapshot(
                                            smartPlaylists = listOf(smart)),
                                    hasConfirmedSnapshot = true),
                            destination = destination,
                            appearanceSource =
                                rememberPlaylistFeatureAppearanceSource(
                                    destination),
                            dismissalPublisher = publisher,
                            mutationFailedLabel = "Failed",
                            onBack = {},
                            onRetry = {},
                            onUpdate = { _, _, _ -> },
                            onDelete = { requests += it },
                            onDeleteConfirmed = { confirmedSnapshot = it },
                            onPlayEntry = {},
                        )
                    }
                }
                val confirmDelete =
                    hasText("Delete smart playlist") and
                        hasAnyAncestor(hasTestTag("HausDialogPanel"))
                onAllNodes(hasText("Delete smart playlist"))
                    .assertCountEquals(1)
                onAllNodes(hasText("Delete smart playlist"))[0].performClick()
                waitForIdle()
                onAllNodes(
                        hasText(
                            "Delete this smart playlist?", substring = true))
                    .assertCountEquals(1)
                onAllNodes(confirmDelete).assertCountEquals(1)
                onAllNodes(confirmDelete)[0].performClick()
                assertEquals(1, requests.size)
                onAllNodes(confirmDelete)[0].performClick()
                assertEquals(1, requests.size)

                assertEquals(
                    PlaylistFeatureDismissalDispatch.Started,
                    publisher.dismissCurrent())
                waitForIdle()
                onAllNodes(hasText("Cancel")).assertCountEquals(0)
                requests[0](PlaylistStateAction.MutationFailed("db"))
                waitForIdle()
                assertNull(confirmedSnapshot)
                onAllNodes(hasText("Could not save playlist changes"))
                    .assertCountEquals(0)

                onAllNodes(hasText("Delete smart playlist"))[0].performClick()
                waitForIdle()
                onAllNodes(hasText("Cancel")).assertCountEquals(1)
                onAllNodes(confirmDelete)[0].performClick()
                assertEquals(2, requests.size)
                requests[0](
                    PlaylistStateAction.SnapshotConfirmed(PlaylistSnapshot()))
                waitForIdle()
                assertNull(confirmedSnapshot)
                onAllNodes(hasText("Cancel")).assertCountEquals(1)
                requests[1](
                    PlaylistStateAction.SnapshotConfirmed(PlaylistSnapshot()))
                waitForIdle()
                assertEquals(PlaylistSnapshot(), confirmedSnapshot)
                onAllNodes(hasText("Cancel")).assertCountEquals(0)
            } finally {
                Locale.setDefault(oldLocale)
            }
        }

    private fun noDismissalPublisher() =
        object : PlaylistFeatureDismissalPublisher {
            override fun publish(
                dismissal: PlaylistFeatureDismissal?,
                dispatch:
                    (
                        PlaylistFeatureDismissal) -> PlaylistFeatureDismissalDispatch
            ): () -> Unit = {}
        }

    private class PlatformDismissalPublisher :
        PlaylistFeatureDismissalPublisher {
        private data class Registration(
            val dismissal: PlaylistFeatureDismissal?,
            val dispatch:
                (PlaylistFeatureDismissal) -> PlaylistFeatureDismissalDispatch,
        )

        private var current: Registration? = null

        override fun publish(
            dismissal: PlaylistFeatureDismissal?,
            dispatch:
                (PlaylistFeatureDismissal) -> PlaylistFeatureDismissalDispatch
        ): () -> Unit {
            val registration = Registration(dismissal, dispatch)
            current = registration
            return { if (current === registration) current = null }
        }

        fun dismissCurrent(): PlaylistFeatureDismissalDispatch {
            val registration = checkNotNull(current)
            return registration.dispatch(checkNotNull(registration.dismissal))
        }
    }
}
