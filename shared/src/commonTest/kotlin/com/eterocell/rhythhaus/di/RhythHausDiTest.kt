package com.eterocell.rhythhaus.di

import com.eterocell.rhythhaus.AudioMetadataReader
import com.eterocell.rhythhaus.PlatformPlaybackEngine
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackProcessLifecycle
import com.eterocell.rhythhaus.library.InMemoryLibraryRepository
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.LibraryScanner
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.PlatformScanEvent
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.ScanStatus
import com.eterocell.rhythhaus.session.PlaybackCheckpoint
import com.eterocell.rhythhaus.session.PlaybackSessionController
import com.eterocell.rhythhaus.session.PlaybackSessionCoordinator
import com.eterocell.rhythhaus.session.PlaybackSessionReconciler
import com.eterocell.rhythhaus.session.PlaybackSessionSnapshot
import com.eterocell.rhythhaus.session.PlaybackSessionStore
import com.eterocell.rhythhaus.session.RevisionedPlaybackSessionSnapshot
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.taglib.TagReadResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class RhythHausDiTest {
    @Test
    fun concurrentAndRepeatedRestoreOnceInvocationsRunOneCoordinatorRestore() =
        runBlocking {
            val controller = CountingRestoreController()
            val processScope = detachedScope(coroutineContext)
            val lifecycle =
                PlaybackProcessLifecycle(
                    coordinator =
                        PlaybackSessionCoordinator(
                            controller, EmptySessionStore, processScope),
                    processScope = processScope,
                )

            coroutineScope {
                repeat(20) { launch { lifecycle.restoreOnce(emptyList()) } }
            }
            lifecycle.restoreOnce(emptyList())

            assertEquals(1, controller.restoreCount)
            processScope.cancel()
        }

    @Test
    fun cancelledFirstWaiterDoesNotReplaceSharedRestoreAttempt() = runBlocking {
        val controller = CountingRestoreController(blockRestore = true)
        val processScope = detachedScope(coroutineContext)
        val lifecycle =
            PlaybackProcessLifecycle(
                coordinator =
                    PlaybackSessionCoordinator(
                        controller, EmptySessionStore, processScope),
                processScope = processScope,
            )

        val first = launch { lifecycle.restoreOnce(emptyList()) }
        controller.restoreStarted.await()
        first.cancelAndJoin()

        val second = async { lifecycle.restoreOnce(emptyList()) }
        controller.allowRestore.complete(Unit)
        second.await()

        assertEquals(1, controller.restoreCount)
        processScope.cancel()
    }

    @Test
    fun failedSharedRestoreIsRetainedAndObservedByLaterCallers() = runBlocking {
        var restoreCount = 0
        val processScope = detachedScope(coroutineContext)
        val lifecycle =
            PlaybackProcessLifecycle(
                restoreAction = {
                    restoreCount++
                    throw IllegalStateException("restore failed")
                },
                processScope = processScope,
            )

        assertFailsWith<IllegalStateException> {
            lifecycle.restoreOnce(emptyList())
        }
        assertFailsWith<IllegalStateException> {
            lifecycle.restoreOnce(emptyList())
        }
        assertEquals(1, restoreCount)
        processScope.cancel()
    }

    @Test
    fun processScopeCancellationTerminatesSharedRestoreAndLaterCallerDoesNotHang() =
        runBlocking {
            val controller = CountingRestoreController(blockRestore = true)
            val processScope = detachedScope(coroutineContext)
            val lifecycle =
                PlaybackProcessLifecycle(
                    coordinator =
                        PlaybackSessionCoordinator(
                            controller, EmptySessionStore, processScope),
                    processScope = processScope,
                )

            val first = async {
                runCatching { lifecycle.restoreOnce(emptyList()) }
                    .exceptionOrNull()
            }
            controller.restoreStarted.await()
            processScope.cancel()
            assertEquals(true, first.await() is CancellationException)

            val second = async {
                runCatching { lifecycle.restoreOnce(emptyList()) }
                    .exceptionOrNull()
            }
            assertEquals(true, second.await() is CancellationException)
            assertEquals(1, controller.restoreCount)
        }

    @Test
    fun playbackSessionBindingsAreProcessSingletonsAndInterfaceAliases() {
        stopKoin()
        val application = startKoin {
            allowOverride(true)
            modules(
                rhythHausModule(),
                module {
                    single<PlaybackSessionStore> { EmptySessionStore }
                },
            )
        }

        try {
            val koin = application.koin
            assertSame(
                koin.get<PlatformPlaybackEngine>(),
                koin.get<PlatformPlaybackEngine>())
            assertSame(
                koin.get<PlaybackController>(), koin.get<PlaybackController>())
            assertSame(
                koin.get<PlaybackController>(),
                koin.get<PlaybackSessionController>())
            assertSame(EmptySessionStore, koin.get<PlaybackSessionStore>())
            assertSame(
                koin.get<PlaybackSessionCoordinator>(),
                koin.get<PlaybackSessionCoordinator>())
            assertSame(
                koin.get<PlaybackSessionCoordinator>(),
                koin.get<PlaybackSessionReconciler>())
            assertSame(
                koin.get<PlaybackProcessLifecycle>(),
                koin.get<PlaybackProcessLifecycle>())
        } finally {
            application.koin.get<CoroutineScope>().cancel()
            stopKoin()
        }
    }

    @Test
    fun koinResolvesLibraryScannerFromTestSafeDependencies() {
        stopKoin()
        val koinApplication = startKoin {
            modules(
                module {
                    single<TagLibReader> { FakeTagLibReader }
                    single { AudioMetadataReader(tagLibReader = get()) }
                    single<LibraryRepository> { InMemoryLibraryRepository() }
                    single<PlatformSourceAccess> { FakePlatformSourceAccess }
                    single {
                        val platformAccess = get<PlatformSourceAccess>()
                        LibraryScanner(
                            repository = get(),
                            platformScanner = platformAccess,
                            metadataReader = get(),
                            now = { 100L },
                            idFactory = { prefix -> "$prefix-id" },
                        )
                    }
                },
            )
        }

        try {
            val scanner = koinApplication.koin.get<LibraryScanner>()
            val source =
                LibrarySource(
                    id = "source-1",
                    platformKind = LibraryPlatformKind.JvmFolder,
                    displayName = "Music",
                    handle = "/Music",
                    createdAtEpochMillis = 1L,
                )

            val result = scanner.scan(source)

            assertNotNull(scanner)
            assertEquals(ScanStatus.Completed, result.status)
            assertEquals(0, result.filesVisited)
        } finally {
            stopKoin()
        }
    }
}

private class CountingRestoreController(
    private val blockRestore: Boolean = false,
    private val restoreFailure: Throwable? = null,
) : PlaybackSessionController {
    override val checkpoints: Flow<PlaybackCheckpoint> = emptyFlow()
    val restoreStarted = CompletableDeferred<Unit>()
    val allowRestore = CompletableDeferred<Unit>()
    var restoreCount: Int = 0
        private set

    override fun sessionSnapshot(): PlaybackSessionSnapshot =
        PlaybackSessionSnapshot()

    override suspend fun restoreSession(
        snapshot: PlaybackSessionSnapshot,
        tracks: List<PlayableTrack>,
    ): RevisionedPlaybackSessionSnapshot {
        restoreCount++
        restoreStarted.complete(Unit)
        if (blockRestore) allowRestore.await()
        restoreFailure?.let { throw it }
        return RevisionedPlaybackSessionSnapshot(
            PlaybackSessionSnapshot(), null)
    }

    override suspend fun reconcileSession(
        tracks: List<PlayableTrack>
    ): RevisionedPlaybackSessionSnapshot =
        RevisionedPlaybackSessionSnapshot(PlaybackSessionSnapshot(), null)

    override suspend fun awaitCheckpointFence() = Unit

    override fun setCommandsEnabled(enabled: Boolean) = Unit
}

private object EmptySessionStore : PlaybackSessionStore {
    override suspend fun read(): PlaybackSessionSnapshot =
        PlaybackSessionSnapshot()

    override suspend fun save(snapshot: PlaybackSessionSnapshot) = Unit
}

private fun detachedScope(
    context: kotlin.coroutines.CoroutineContext
): CoroutineScope = CoroutineScope(context.minusKey(Job) + SupervisorJob())

private object FakePlatformSourceAccess : PlatformSourceAccess {
    override fun scan(source: LibrarySource): Sequence<PlatformScanEvent> =
        emptySequence()
}

private object FakeTagLibReader : TagLibReader {
    override fun readPath(path: String): TagReadResult =
        TagReadResult.Unsupported("not used")

    override fun readProperties(path: String): Map<String, String> = emptyMap()
}
