package com.eterocell.rhythhaus.library

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Deterministically asserts the library resource ownership ledger by reading
 * the actual catalog XML files. The feature catalog owns the complete expected
 * library key set in both locales; `library_queue` and `album_artwork` remain
 * Shared-owned; `selected` is absent from both Shared catalogs; and no key from
 * the library keys or the 12 injected Shared label keys is duplicated across
 * the feature and Shared catalogs.
 */
class LibraryResourceOwnershipJvmTest {
    private val libraryKeys =
        listOf(
            "folder_picker_error_access",
            "folder_picker_error_select",
            "folder_picker_error_prepare",
            "folder_picker_no_folder_selected",
            "unknown_artist",
            "artist_artwork",
            "album_accessibility_format",
            "artist_accessibility_format",
            "album_track_count_format",
            "track_count_format",
            "album_detail_subtitle_format",
            "artist_detail_subtitle_format",
            "artist_album_tracks_format",
            "browse_mode_albums",
            "browse_mode_artists",
            "browse_mode_songs",
            "scanning",
            "scan_progress_format",
            "import_card_title",
            "import_card_title_with_tracks",
            "import_card_description",
            "scan_completed",
            "scan_cancelled",
            "scan_failed",
            "scan_summary_format",
            "rescan",
            "retry_scan",
            "remove_missing",
            "view_scan_report",
            "hide_scan_report",
            "scan_report_empty",
            "scan_report_error_format",
        )

    private val injected12 =
        listOf(
            "add_music_folder",
            "folder_picker_unavailable",
            "clear_library",
            "cancel",
            "playlists",
            "playlists_accessibility",
            "library_queue",
            "album_art",
            "album_artwork",
            "now_playing_badge",
            "select_track_format",
            "track_artist_album_format",
        )

    private val featureValues =
        resourceCatalog(
            "feature/library/impl/src/commonMain/composeResources/values/strings.xml")
    private val featureValuesZh =
        resourceCatalog(
            "feature/library/impl/src/commonMain/composeResources/values-zh/strings.xml")
    private val sharedValues =
        resourceCatalog(
            "shared/src/commonMain/composeResources/values/strings.xml")
    private val sharedValuesZh =
        resourceCatalog(
            "shared/src/commonMain/composeResources/values-zh/strings.xml")

    @Test
    fun featureOwnsExactlyTheExpectedLibraryKeysInBothLocales() {
        assertEquals(libraryKeys.toSet(), featureKeys(featureValues))
        assertEquals(libraryKeys.toSet(), featureKeys(featureValuesZh))
    }

    @Test
    fun featureCatalogsHaveFullEnZhParity() {
        assertEquals(featureKeys(featureValues), featureKeys(featureValuesZh))
    }

    @Test
    fun libraryQueueAndAlbumArtworkRemainSharedOwned() {
        val sharedKeys = featureKeys(sharedValues)
        val featureKeys = featureKeys(featureValues)
        assertTrue("library_queue" in sharedKeys)
        assertTrue("album_artwork" in sharedKeys)
        assertFalse("library_queue" in featureKeys)
        assertFalse("album_artwork" in featureKeys)
    }

    @Test
    fun selectedIsAbsentFromBothSharedCatalogs() {
        assertFalse("selected" in featureKeys(sharedValues))
        assertFalse("selected" in featureKeys(sharedValuesZh))
    }

    @Test
    fun libraryAndInjectedKeysAreOwnedByExactlyOneCatalog() {
        val featureKeys = featureKeys(featureValues)
        val sharedKeys = featureKeys(sharedValues)
        val sharedZhKeys = featureKeys(sharedValuesZh)
        (libraryKeys + injected12).forEach { key ->
            val featureOwned = key in featureKeys
            val sharedOwned = key in sharedKeys
            assertTrue(
                featureOwned != sharedOwned,
                "key $key must be owned by exactly one of feature or shared")
            if (key in libraryKeys) {
                assertTrue(
                    featureOwned, "library key $key must be feature-owned")
                assertFalse(
                    key in sharedZhKeys,
                    "library key $key must not be in shared ZH")
            } else {
                assertTrue(
                    sharedOwned, "injected key $key must be Shared-owned")
            }
        }
    }

    private fun featureKeys(catalog: File): Set<String> =
        Regex("""<string\s+name="([^"]+)"""")
            .findAll(catalog.readText())
            .map { it.groupValues[1] }
            .toSet()

    private fun resourceCatalog(relativePath: String): File {
        var dir = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile
        }
        val root = checkNotNull(dir) { "repo root not found" }
        return File(root, relativePath).also {
            assertTrue(it.isFile, "missing resource catalog: ${it.path}")
        }
    }
}
