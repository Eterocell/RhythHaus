package com.eterocell.rhythhaus.di

import com.eterocell.rhythhaus.AutomaticResumePolicyTarget
import com.eterocell.rhythhaus.PlatformPlaybackEngine
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackProcessLifecycle
import com.eterocell.rhythhaus.carplay.CarPlayFacade
import com.eterocell.rhythhaus.createPlatformPlaybackEngine
import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.libraryImplementationModule
import com.eterocell.rhythhaus.library.playlistsImplementationModule
import com.eterocell.rhythhaus.onboarding.OnboardingPreferenceStore
import com.eterocell.rhythhaus.onboarding.createOnboardingPreferenceStore
import com.eterocell.rhythhaus.session.PlaybackSessionController
import com.eterocell.rhythhaus.session.PlaybackSessionCoordinator
import com.eterocell.rhythhaus.session.PlaybackSessionPhase
import com.eterocell.rhythhaus.session.PlaybackSessionReconciler
import com.eterocell.rhythhaus.session.PlaybackSessionStore
import com.eterocell.rhythhaus.session.createPlaybackSessionStore
import com.eterocell.rhythhaus.theme.ThemePreferenceStore
import com.eterocell.rhythhaus.theme.createThemePreferenceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

fun rhythHausModule(): Module = module {
    single { com.eterocell.rhythhaus.AuthoritativeLibraryPublicationOwner() }
    single {
        com.eterocell.rhythhaus.AppProcessState(
            repository = get(),
            platformAccess = get(),
            controller = get(),
            lifecycle = get(),
            reconciler = get(),
            publications = get(),
            playlistOwner = get(),
            facade = get(),
            scope = get(),
        )
    }
    single<CoroutineScope> {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
    includes(libraryImplementationModule())
    // Shared is the sole composition root for the feature-owned playlist
    // bindings.
    includes(playlistsImplementationModule())
    single<PlatformPlaybackEngine> { createPlatformPlaybackEngine() }
    single {
        PlaybackController(
                engine = get(),
                artworkLoader = { trackId ->
                    get<LibraryRepository>().artworkForTrack(trackId)?.bytes
                },
            )
            .also { it.setCommandsEnabled(false) }
    }
    single {
        val playbackSession = get<PlaybackSessionCoordinator>()
        CarPlayFacade(
            libraryRepository = get(),
            playbackController = get(),
            processScope = get(),
            canDispatchPlaybackCommands = {
                when (playbackSession.phase.value) {
                    PlaybackSessionPhase.Ready,
                    PlaybackSessionPhase.FailedSafe -> true
                    PlaybackSessionPhase.NotRestored,
                    PlaybackSessionPhase.Restoring -> false
                }
            },
        )
    }
    single<PlaybackSessionController> { get<PlaybackController>() }
    single<PlaybackSessionStore> { createPlaybackSessionStore() }
    single {
        PlaybackSessionCoordinator(
            controller = get(),
            store = get(),
            processScope = get(),
        )
    }
    single<PlaybackSessionReconciler> { get<PlaybackSessionCoordinator>() }
    single {
        val coordinator = get<PlaybackSessionCoordinator>()
        val normalization =
            get<com.eterocell.rhythhaus.playback.ReplayGainPreferenceOwner>()
        val crossfade =
            get<com.eterocell.rhythhaus.playback.CrossfadePreferenceOwner>()
        val gapless =
            get<com.eterocell.rhythhaus.playback.GaplessPreferenceOwner>()
        val automaticResume =
            get<
                com.eterocell.rhythhaus.playback.AutomaticResumePreferenceOwner>()
        PlaybackProcessLifecycle(
            restoreAction = { tracks ->
                normalization.restore()
                crossfade.restore()
                gapless.restore()
                automaticResume.restore()
                coordinator.restoreOnce(tracks)
            },
            processScope = get(),
        )
    }
    single<ThemePreferenceStore> { createThemePreferenceStore() }
    single<com.eterocell.rhythhaus.playback.ReplayGainPreferenceStore> {
        com.eterocell.rhythhaus.playback.createReplayGainPreferenceStore()
    }
    single {
        com.eterocell.rhythhaus.playback.ReplayGainPreferenceOwner(
            store = get(),
            applyMode = get<PlaybackController>()::setReplayGainMode,
        )
    }
    single<com.eterocell.rhythhaus.playback.CrossfadePreferenceStore> {
        com.eterocell.rhythhaus.playback.createCrossfadePreferenceStore()
    }
    single {
        com.eterocell.rhythhaus.playback.CrossfadePreferenceOwner(
            store = get(),
            applyDuration = get<PlaybackController>()::setCrossfadeDuration,
        )
    }
    single<com.eterocell.rhythhaus.playback.GaplessPreferenceStore> {
        com.eterocell.rhythhaus.playback.createGaplessPreferenceStore()
    }
    single {
        com.eterocell.rhythhaus.playback.GaplessPreferenceOwner(
            store = get(),
            applyMode = { mode ->
                get<PlaybackController>()
                    .setGaplessEnabled(
                        mode ==
                            com.eterocell.rhythhaus.playback.GaplessPlaybackMode
                                .On)
            },
        )
    }
    single<com.eterocell.rhythhaus.playback.AutomaticResumePreferenceStore> {
        com.eterocell.rhythhaus.playback.createAutomaticResumePreferenceStore()
    }
    single {
        com.eterocell.rhythhaus.playback.AutomaticResumePreferenceOwner(
            store = get(),
            applyPolicy = { policy ->
                (get<PlatformPlaybackEngine>() as? AutomaticResumePolicyTarget)
                    ?.setAutomaticResumeAllowed(
                        policy ==
                            com.eterocell.rhythhaus.playback
                                .AutomaticResumePolicy
                                .FollowSystemRecommendation)
            },
        )
    }
    single<OnboardingPreferenceStore> { createOnboardingPreferenceStore() }
}

fun startRhythHausKoin() {
    if (KoinPlatform.getKoinOrNull() != null) return
    startKoin {
        modules(rhythHausModule())
    }
}

/** Returns the process-owned CarPlay facade for native platform clients. */
public fun sharedCarPlayFacade(): CarPlayFacade {
    startRhythHausKoin()
    return KoinPlatform.getKoin().get()
}

/**
 * Starts process-owned library/session initialization after native bridges are
 * registered.
 */
public fun startSharedAppProcess() {
    startRhythHausKoin()
    KoinPlatform.getKoin().get<com.eterocell.rhythhaus.AppProcessState>()
}
