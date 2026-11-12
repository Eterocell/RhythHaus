package com.eterocell.rhythhaus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.LibraryScanner
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.PlaylistRepository
import com.eterocell.rhythhaus.library.ui.PlaylistStateOwner
import com.eterocell.rhythhaus.mediastore.MediaStoreAudioPermissionController
import com.eterocell.rhythhaus.mediastore.UnavailableMediaStoreAudioPermissionController
import com.eterocell.rhythhaus.notificationpermission.MediaNotificationPermissionController
import com.eterocell.rhythhaus.notificationpermission.UnavailableMediaNotificationPermissionController
import com.eterocell.rhythhaus.onboarding.OnboardingPreferenceStore
import com.eterocell.rhythhaus.session.PlaybackSessionReconciler
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.theme.ThemePreferenceOwner
import com.eterocell.rhythhaus.theme.ThemePreferenceStore
import kotlinx.coroutines.flow.Flow
import org.koin.compose.koinInject

@Composable
@Preview
fun App(
    notificationPermissionController: MediaNotificationPermissionController =
        UnavailableMediaNotificationPermissionController,
    mediaStorePermissionController: MediaStoreAudioPermissionController =
        UnavailableMediaStoreAudioPermissionController,
    desktopDropEvents: Flow<List<String>>? = null,
    desktopDropActive: Boolean = false,
    onDesktopDropAvailabilityChanged: (Boolean) -> Unit = {},
) {
    val controller = koinInject<PlaybackController>()
    val outputStatusProvider =
        koinInject<PlatformPlaybackEngine>() as? PlaybackOutputStatusProvider
    val tagLibReader = koinInject<TagLibReader>()
    val repository = koinInject<LibraryRepository>()
    val processState = koinInject<AppProcessState>()
    val playlistRepository = koinInject<PlaylistRepository>()
    val platformAccess = koinInject<PlatformSourceAccess>()
    val scanner = koinInject<LibraryScanner>()
    val themePreferenceStore = koinInject<ThemePreferenceStore>()
    val themePreferenceOwner = koinInject<ThemePreferenceOwner>()
    val replayGainPreferenceOwner =
        koinInject<com.eterocell.rhythhaus.playback.ReplayGainPreferenceOwner>()
    val crossfadePreferenceOwner =
        koinInject<com.eterocell.rhythhaus.playback.CrossfadePreferenceOwner>()
    val gaplessPreferenceOwner =
        koinInject<com.eterocell.rhythhaus.playback.GaplessPreferenceOwner>()
    val automaticResumePreferenceOwner =
        koinInject<
            com.eterocell.rhythhaus.playback.AutomaticResumePreferenceOwner>()
    val onboardingPreferenceStore = koinInject<OnboardingPreferenceStore>()
    val playbackReconciler = koinInject<PlaybackSessionReconciler>()
    val playlistStateOwner = koinInject<PlaylistStateOwner>()
    AppLibraryContentHost(
        library =
            AppLibraryServices(
                repository = repository,
                playlistRepository = playlistRepository,
                platformAccess = platformAccess,
                scanner = scanner,
                tagLibReader = tagLibReader,
                processState = processState,
                playbackReconciler = playbackReconciler,
                playlistStateOwner = playlistStateOwner,
            ),
        playback =
            AppPlaybackServices(
                controller = controller,
                outputStatusProvider = outputStatusProvider,
                replayGainPreferenceOwner = replayGainPreferenceOwner,
                crossfadePreferenceOwner = crossfadePreferenceOwner,
                gaplessPreferenceOwner = gaplessPreferenceOwner,
                automaticResumePreferenceOwner = automaticResumePreferenceOwner,
            ),
        preferences =
            AppPreferenceServices(
                themePreferenceStore = themePreferenceStore,
                themePreferenceOwner = themePreferenceOwner,
                onboardingPreferenceStore = onboardingPreferenceStore,
            ),
        platform =
            AppPlatformServices(
                notificationPermissionController =
                    notificationPermissionController,
                mediaStorePermissionController = mediaStorePermissionController,
                desktopDropEvents = desktopDropEvents,
                desktopDropActive = desktopDropActive,
                onDesktopDropAvailabilityChanged =
                    onDesktopDropAvailabilityChanged,
            ),
    )
}
