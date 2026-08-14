package com.eterocell.gradle.architecture

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Asserts that `:shared` owns only its thin composition/facade role after the feature
 * modularization: `App()` composition, the root shell, cross-feature route/Back arbitration,
 * lifecycle, Koin assembly, the stable `MainViewController` iOS facade, and the
 * intentionally-retained session/theme/playback-factory/backup-ABI/selection/Now-Playing/
 * formatting helpers. Any source file outside this set is migrated implementation ownership;
 * any approved file missing is a regression.
 */
class ThinSharedInventoryTest {

    private val approved = setOf(
        // commonMain — facade + retained helpers
        "src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/di/RhythHausDi.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelection.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppState.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDialogs.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/TrackSelectionBar.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/TrackSelectionState.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/MusicModels.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingScreen.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/PlatformPlaybackEngineFactory.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/PlaybackProcessLifecycle.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocuments.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionStore.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/theme/ThemePreferenceStore.kt",
        // androidMain
        "src/androidMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocuments.android.kt",
        "src/androidMain/kotlin/com/eterocell/rhythhaus/theme/ThemePreferenceStore.android.kt",
        "src/androidMain/kotlin/com/eterocell/rhythhaus/PlatformPlaybackEngineFactory.android.kt",
        "src/androidMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionStore.android.kt",
        // jvmMain
        "src/jvmMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocuments.jvm.kt",
        "src/jvmMain/kotlin/com/eterocell/rhythhaus/PlatformPlaybackEngineFactory.jvm.kt",
        "src/jvmMain/kotlin/com/eterocell/rhythhaus/theme/ThemePreferenceStore.jvm.kt",
        "src/jvmMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionStore.jvm.kt",
        // iosMain
        "src/iosMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocuments.ios.kt",
        "src/iosMain/kotlin/com/eterocell/rhythhaus/MainViewController.kt",
        "src/iosMain/kotlin/com/eterocell/rhythhaus/theme/ThemePreferenceStore.ios.kt",
        "src/iosMain/kotlin/com/eterocell/rhythhaus/PlatformPlaybackEngineFactory.ios.kt",
        "src/iosMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionStore.ios.kt",
    )

    @Test
    fun sharedOwnsOnlyTheThinFacadeSourceSet() {
        val shared = File(
            System.getProperty("rhythhaus.rootDir") ?: error("rhythhaus.rootDir not set"),
            "shared",
        )
        assertTrue(shared.isDirectory, "Missing shared module at ${shared.path}")

        val mainSourceSets = listOf("commonMain", "androidMain", "jvmMain", "iosMain")
        val actual = mainSourceSets
            .flatMap { ss ->
                File(shared, "src/$ss/kotlin").walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .map { it.relativeTo(shared).path.replace(File.separatorChar, '/') }
            }
            .toSortedSet()

        val extras = actual - approved
        val missing = approved - actual
        assertEquals(
            emptySet(),
            extras,
            "shared owns migrated implementation ownership (remove or re-approve): $extras",
        )
        assertEquals(
            emptySet(),
            missing,
            "shared is missing an approved thin-facade file (restore or re-approve): $missing",
        )
    }
}
