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
        // Later app-owned facades, shell, and preference owners: docs/architecture.md
        // "Retained additions after the original inventory" records ownership authority.
        "src/androidMain/kotlin/com/eterocell/rhythhaus/library/DefaultLibrarySource.android.kt",
        "src/androidMain/kotlin/com/eterocell/rhythhaus/library/PlatformFolderPickerFacade.android.kt",
        "src/androidMain/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingPlatform.android.kt",
        "src/androidMain/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingPreferenceStore.android.kt",
        "src/androidMain/kotlin/com/eterocell/rhythhaus/playback/AutomaticResumePreferenceStore.android.kt",
        "src/androidMain/kotlin/com/eterocell/rhythhaus/playback/CrossfadePreferenceStore.android.kt",
        "src/androidMain/kotlin/com/eterocell/rhythhaus/playback/GaplessPreferenceStore.android.kt",
        "src/androidMain/kotlin/com/eterocell/rhythhaus/playback/ReplayGainPreferenceStore.android.kt",
        "src/androidMain/kotlin/com/eterocell/rhythhaus/systemui/AppSystemBarAppearance.android.kt",
        "src/androidMain/kotlin/com/eterocell/rhythhaus/theme/PlatformDynamicColor.android.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/AppLibraryOrchestrator.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/AppMetadataOverride.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/AppProcessState.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryOperationCoordinator.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/carplay/CarPlayFacade.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/library/DefaultLibrarySource.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/library/IOSLibraryImport.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlatformFolderPicker.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/AppNavigationChrome.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/AppShellHeader.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/AppShellLayout.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/AppShellWindowInsets.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/DesktopDropTargetPresentation.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/NowPlayingPanelPresentation.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/mediastore/MediaStoreAudioPermission.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/mediastore/MediaStoreSourceRegistration.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingPlatform.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingPreferenceStore.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/playback/AutomaticResumePolicy.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/playback/AutomaticResumePreferenceOwner.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/playback/AutomaticResumePreferenceStore.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/playback/CrossfadePreferenceOwner.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/playback/CrossfadePreferenceStore.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/playback/GaplessPreferenceOwner.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/playback/GaplessPreferenceStore.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/playback/ReplayGainPreferenceOwner.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/playback/ReplayGainPreferenceStore.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/systemui/AppSystemBarAppearance.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/theme/PlatformDynamicColor.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/theme/ThemePreferenceOwner.kt",
        "src/iosMain/kotlin/com/eterocell/rhythhaus/library/DefaultLibrarySource.ios.kt",
        "src/iosMain/kotlin/com/eterocell/rhythhaus/library/IOSLibraryImport.kt",
        "src/iosMain/kotlin/com/eterocell/rhythhaus/library/PlatformFolderPicker.ios.kt",
        "src/iosMain/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingPlatform.ios.kt",
        "src/iosMain/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingPreferenceStore.ios.kt",
        "src/iosMain/kotlin/com/eterocell/rhythhaus/playback/AutomaticResumePreferenceStore.ios.kt",
        "src/iosMain/kotlin/com/eterocell/rhythhaus/playback/CrossfadePreferenceStore.ios.kt",
        "src/iosMain/kotlin/com/eterocell/rhythhaus/playback/GaplessPreferenceStore.ios.kt",
        "src/iosMain/kotlin/com/eterocell/rhythhaus/playback/ReplayGainPreferenceStore.ios.kt",
        "src/iosMain/kotlin/com/eterocell/rhythhaus/systemui/AppSystemBarAppearance.ios.kt",
        "src/iosMain/kotlin/com/eterocell/rhythhaus/theme/PlatformDynamicColor.ios.kt",
        "src/jvmMain/kotlin/com/eterocell/rhythhaus/library/DefaultLibrarySource.jvm.kt",
        "src/jvmMain/kotlin/com/eterocell/rhythhaus/library/PlatformFolderPickerFacade.jvm.kt",
        "src/jvmMain/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingPlatform.jvm.kt",
        "src/jvmMain/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingPreferenceStore.jvm.kt",
        "src/jvmMain/kotlin/com/eterocell/rhythhaus/playback/AutomaticResumePreferenceStore.jvm.kt",
        "src/jvmMain/kotlin/com/eterocell/rhythhaus/playback/CrossfadePreferenceStore.jvm.kt",
        "src/jvmMain/kotlin/com/eterocell/rhythhaus/playback/GaplessPreferenceStore.jvm.kt",
        "src/jvmMain/kotlin/com/eterocell/rhythhaus/playback/ReplayGainPreferenceStore.jvm.kt",
        "src/jvmMain/kotlin/com/eterocell/rhythhaus/systemui/AppSystemBarAppearance.jvm.kt",
        "src/jvmMain/kotlin/com/eterocell/rhythhaus/theme/PlatformDynamicColor.jvm.kt",
        // commonMain — facade + retained helpers
        "src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/AppLibraryPublication.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/AppLibraryOperations.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/AppLibraryOperationsHost.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/AppLibraryContentHost.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/AppPlaylistBackupHost.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/AppOnboardingGate.kt",
        "src/commonMain/kotlin/com/eterocell/rhythhaus/theme/RhythHausTheme.kt",
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
        // Shared facade contract — Android-free media-notification permission state/controller
        "src/commonMain/kotlin/com/eterocell/rhythhaus/notificationpermission/MediaNotificationPermission.kt",
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
