package com.eterocell.rhythhaus

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.google.common.util.concurrent.ListenableFuture
import java.lang.reflect.Proxy
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout

class AndroidPlaybackMediaSessionTest {

    @Test
    fun productionAndroidFactorySupportsCrossfadeWithoutAnotherEngine() {
        val engine = createAndroidPlaybackEngine()
        try {
            assertTrue(engine is CrossfadePlaybackEngine)
        } finally {
            engine.release()
        }
    }

    @Test
    fun serviceAdmissionStagesSilentlyAndPromotesMetadataWithoutStoppingOutgoing() {
        val fixture = ServicePlaybackFixture()
        val outgoing = fixture.load(10L, "Outgoing")
        fixture.command(AndroidServiceEngineCommand.Play(101L))

        val denied =
            fixture.router
                .dispatch(
                    controllerUid = 7,
                    engineHint = true,
                    command =
                        AndroidServiceEngineCommand.PrepareIncoming(
                            fixture.item(11L, "Incoming")),
                )
                .get()
        assertEquals(AndroidServiceEngineResult.PermissionDenied, denied)
        assertEquals(1, fixture.players.count { !it.released })

        val staged =
            fixture.command(
                AndroidServiceEngineCommand.PrepareIncoming(
                    fixture.item(11L, "Incoming")))
        val incoming = fixture.players.last()
        assertEquals(
            "Outgoing", fixture.facade!!.currentMediaItem!!.mediaMetadata.title)
        assertTrue(outgoing.playWhenReady)
        assertFalse(incoming.playWhenReady)
        assertEquals(0f, incoming.volume)
        assertFalse(staged.isDone)
        incoming.ready(8_000L)
        assertEquals(8_000L, staged.get().durationMillis)
        assertEquals(
            listOf("status:10:Playing"),
            fixture.events.mapNotNull { it.statusLabel() })

        fixture.command(AndroidServiceEngineCommand.StartIncoming(202L)).get()
        assertEquals(
            "Incoming", fixture.facade!!.currentMediaItem!!.mediaMetadata.title)
        assertTrue(outgoing.playWhenReady)
        assertTrue(incoming.playWhenReady)
        assertEquals(0f, incoming.volume)
        assertEquals(
            listOf(10L, 11L),
            fixture.events
                .filter { it.status == PlaybackStatus.Playing }
                .map { it.token.generation })

        fixture
            .command(
                AndroidServiceEngineCommand.RetireOutgoing(
                    Media3RequestToken(10L, 1L)))
            .get()
        assertTrue(outgoing.released)
        assertFalse(incoming.released)
        assertTrue(incoming.playWhenReady)
        fixture.playback.release()
    }

    @Test
    fun sessionFacadePausesBothAndResumesAtRetainedPerVoiceGains() {
        val fixture = ServicePlaybackFixture()
        val outgoing = fixture.load(20L, "Outgoing")
        fixture.command(AndroidServiceEngineCommand.Play(201L))
        fixture.command(
            AndroidServiceEngineCommand.PrepareIncoming(
                fixture.item(21L, "Incoming")))
        val incoming = fixture.players.last()
        incoming.ready(8_000L)
        fixture.command(AndroidServiceEngineCommand.StartIncoming(202L))
        fixture.command(
            AndroidServiceEngineCommand.CrossfadeGains(0.21f, 0.34f))

        fixture.facade!!.pause()
        assertFalse(outgoing.playWhenReady)
        assertFalse(incoming.playWhenReady)
        fixture.facade!!.play()
        assertTrue(outgoing.playWhenReady)
        assertTrue(incoming.playWhenReady)
        assertEquals(listOf(0.21f), outgoing.gainsAtPlay.takeLast(1))
        assertEquals(listOf(0.34f), incoming.gainsAtPlay.takeLast(1))

        fixture.facade!!.seekTo(1_200L)
        assertTrue(outgoing.released)
        assertEquals(1_200L, incoming.position)
        assertEquals(0.34f, incoming.volume)
        fixture.playback.release()
    }

    @Test
    fun failedIncomingPreparationRetainsCurrentPlaybackAndPublishesNoIncomingEvent() {
        val fixture = ServicePlaybackFixture()
        val outgoing = fixture.load(30L, "Outgoing")
        fixture.command(AndroidServiceEngineCommand.Play(301L))
        fixture.failNextPrepare = true

        val result =
            fixture
                .command(
                    AndroidServiceEngineCommand.PrepareIncoming(
                        fixture.item(31L, "Broken")))
                .get()

        assertNotNull(result.failure)
        assertEquals(
            "Outgoing", fixture.facade!!.currentMediaItem!!.mediaMetadata.title)
        assertTrue(outgoing.playWhenReady)
        assertFalse(outgoing.released)
        assertTrue(fixture.players.last().released)
        assertTrue(fixture.events.all { it.token.generation == 30L })
        fixture.playback.release()
    }

    @Test
    fun engineFailedPreparationLeavesOutgoingPlaybackAndItsRunIntact() =
        runBlocking {
            val fixture = ServicePlaybackFixture()
            val engine = fixture.engine()
            val load =
                async(start = CoroutineStart.UNDISPATCHED) {
                    engine.loadPaused(serviceTrack("Outgoing"), 32L)
                }
            val outgoing = fixture.players.last()
            outgoing.ready(12_000L)
            load.await()
            engine.play(321L)
            fixture.failNextPrepare = true

            assertFailsWith<PlaybackFailureException> {
                engine.prepareIncoming(serviceTrack("Broken"), 33L)
            }

            assertTrue(outgoing.playWhenReady)
            assertFalse(outgoing.released)
            assertTrue(fixture.players.last().released)
            assertEquals(
                "Outgoing",
                fixture.facade!!.currentMediaItem!!.mediaMetadata.title)
            outgoing.listeners
                .single()
                .onPlaybackStateChanged(Player.STATE_ENDED)
            assertEquals(
                321L, fixture.events.single { it.completed }.playbackRunId)
            engine.release()
        }

    @Test
    fun cancellingPreparationRejectsLateNativeReadinessAndPreservesOutgoing() {
        val fixture = ServicePlaybackFixture()
        val outgoing = fixture.load(40L, "Outgoing")
        fixture.command(AndroidServiceEngineCommand.Play(401L))
        val staged =
            fixture.command(
                AndroidServiceEngineCommand.PrepareIncoming(
                    fixture.item(41L, "Incoming")))
        val incoming = fixture.players.last()
        val queuedCallback = incoming.listeners.single()

        fixture.command(
            AndroidServiceEngineCommand.CancelIncoming(
                Media3RequestToken(41L, 1L)))
        queuedCallback.onPlaybackStateChanged(Player.STATE_READY)

        assertTrue(staged.isDone)
        assertNotNull(staged.get().failure)
        assertTrue(incoming.released)
        assertTrue(outgoing.playWhenReady)
        assertEquals(
            "Outgoing", fixture.facade!!.currentMediaItem!!.mediaMetadata.title)
        assertTrue(fixture.events.all { it.token.generation == 40L })
        fixture.playback.release()
    }

    @Test
    fun retiringNativeCompletionKeepsOriginalGenerationAndPlaybackRun() {
        val fixture = ServicePlaybackFixture()
        val outgoing = fixture.load(50L, "Outgoing")
        fixture.command(AndroidServiceEngineCommand.Play(501L))
        val queuedOutgoingCallback = outgoing.listeners.single()
        fixture.command(
            AndroidServiceEngineCommand.PrepareIncoming(
                fixture.item(51L, "Incoming")))
        fixture.players.last().ready(8_000L)
        fixture.command(AndroidServiceEngineCommand.StartIncoming(502L))
        fixture.command(AndroidServiceEngineCommand.Play(503L))

        queuedOutgoingCallback.onPlaybackStateChanged(Player.STATE_ENDED)

        val completed = fixture.events.single { it.completed }
        assertEquals(50L, completed.token.generation)
        assertEquals(501L, completed.playbackRunId)
        assertEquals(
            "Incoming", fixture.facade!!.currentMediaItem!!.mediaMetadata.title)
        fixture.playback.release()
    }

    @Test
    fun cancelAfterNativePlayingRestoresOutgoingIdentityRunAndGainWithoutReplay() {
        val fixture = ServicePlaybackFixture()
        val outgoing = fixture.load(52L, "Outgoing")
        fixture.command(AndroidServiceEngineCommand.Gain(0.42f))
        fixture.command(AndroidServiceEngineCommand.Play(521L))
        fixture.command(
            AndroidServiceEngineCommand.PrepareIncoming(
                fixture.item(53L, "Incoming")))
        val incoming = fixture.players.last()
        incoming.ready(8_000L)
        fixture.command(AndroidServiceEngineCommand.CrossfadeGains(0.21f, 0f))
        fixture.command(AndroidServiceEngineCommand.StartIncoming(531L))
        val lateIncomingCallback = incoming.listeners.single()
        val outgoingPlayCount = outgoing.gainsAtPlay.size

        fixture
            .command(
                AndroidServiceEngineCommand.CancelIncoming(
                    Media3RequestToken(53L, 1L)))
            .get()
        lateIncomingCallback.onIsPlayingChanged(true)
        outgoing.listeners.single().onPlaybackStateChanged(Player.STATE_ENDED)

        assertTrue(incoming.released)
        assertFalse(outgoing.released)
        assertTrue(outgoing.playWhenReady)
        assertEquals(0.42f, outgoing.volume)
        assertEquals(outgoingPlayCount, outgoing.gainsAtPlay.size)
        assertEquals(
            "Outgoing", fixture.facade!!.currentMediaItem!!.mediaMetadata.title)
        assertEquals(521L, fixture.events.single { it.completed }.playbackRunId)
        assertEquals(
            1,
            fixture.events.count {
                it.token.generation == 53L &&
                    it.status == PlaybackStatus.Playing
            })
        fixture.playback.release()
    }

    @Test
    fun incomingStartFailureKeepsOutgoingUntilControllerChoosesRollback() {
        val fixture = ServicePlaybackFixture()
        val outgoing = fixture.load(54L, "Outgoing")
        fixture.command(AndroidServiceEngineCommand.Play(541L))
        fixture.command(
            AndroidServiceEngineCommand.PrepareIncoming(
                fixture.item(55L, "Incoming")))
        val incoming = fixture.players.last()
        incoming.ready(8_000L)
        incoming.failPlay = true

        assertNotNull(
            fixture
                .command(AndroidServiceEngineCommand.StartIncoming(551L))
                .get()
                .failure)
        assertFalse(outgoing.released)
        assertTrue(outgoing.playWhenReady)
        assertEquals(
            "Incoming", fixture.facade!!.currentMediaItem!!.mediaMetadata.title)

        fixture
            .command(
                AndroidServiceEngineCommand.CancelIncoming(
                    Media3RequestToken(55L, 1L)))
            .get()
        assertTrue(incoming.released)
        assertFalse(outgoing.released)
        assertEquals(
            "Outgoing", fixture.facade!!.currentMediaItem!!.mediaMetadata.title)
        fixture.playback.release()
    }

    @Test
    fun sourceOwnedRunCallbacksCannotRelabelEventsAfterReplay() {
        val fixture = ServicePlaybackFixture()
        val current = fixture.load(56L, "Current")
        fixture.command(AndroidServiceEngineCommand.Play(561L))
        val oldCallback = current.listeners.single()
        fixture.command(AndroidServiceEngineCommand.Play(562L))
        fixture.events.clear()

        oldCallback.onIsPlayingChanged(false)
        oldCallback.onPlaybackStateChanged(Player.STATE_ENDED)
        fixture.playback.publishProgress()
        current.listeners.single().onPlaybackStateChanged(Player.STATE_ENDED)

        assertTrue(fixture.events.none { it.playbackRunId == 561L })
        assertEquals(562L, fixture.events.single { it.completed }.playbackRunId)
        assertTrue(
            fixture.events.any {
                it.positionMillis == 0L && it.playbackRunId == 562L
            })
        fixture.playback.release()
    }

    @Test
    fun crossfadeCommandsAreFencedToIncomingAndOutgoingSourceTokens() {
        val fixture = ServicePlaybackFixture()
        val outgoing = fixture.load(57L, "Outgoing")
        fixture.command(AndroidServiceEngineCommand.Play(571L))
        fixture.command(
            AndroidServiceEngineCommand.PrepareIncoming(
                fixture.item(58L, "Incoming")))
        val incoming = fixture.players.last()
        incoming.ready(8_000L)

        assertNull(
            fixture
                .command(
                    AndroidServiceEngineCommand.CrossfadeGains(
                        0.4f, 0f, Media3RequestToken(58L, 1L)))
                .get()
                .failure)
        fixture.command(AndroidServiceEngineCommand.StartIncoming(581L))
        assertNotNull(
            fixture
                .command(
                    AndroidServiceEngineCommand.CrossfadeGains(
                        0.1f, 0.9f, Media3RequestToken(57L, 1L)))
                .get()
                .failure)
        fixture.command(
            AndroidServiceEngineCommand.CancelIncoming(
                Media3RequestToken(59L, 1L)))
        fixture.command(
            AndroidServiceEngineCommand.RetireOutgoing(
                Media3RequestToken(58L, 1L)))

        assertFalse(outgoing.released)
        assertFalse(incoming.released)
        assertEquals(0.4f, outgoing.volume)
        assertEquals(0f, incoming.volume)
        fixture.command(
            AndroidServiceEngineCommand.RetireOutgoing(
                Media3RequestToken(57L, 1L)))
        assertTrue(outgoing.released)
        assertFalse(incoming.released)
        fixture.playback.release()
    }

    @Test
    fun sameUidWithoutEngineHintCannotPrepareStartOrReleaseNativeVoices() {
        val fixture = ServicePlaybackFixture()
        val outgoing = fixture.load(59L, "Outgoing")
        fixture.command(AndroidServiceEngineCommand.Play(591L))

        listOf(
                AndroidServiceEngineCommand.PrepareIncoming(
                    fixture.item(60L, "Incoming")),
                AndroidServiceEngineCommand.StartIncoming(601L),
                AndroidServiceEngineCommand.Release,
            )
            .forEach { command ->
                assertEquals(
                    AndroidServiceEngineResult.PermissionDenied,
                    fixture.router.dispatch(42, false, command).get())
            }

        assertEquals(1, fixture.players.count { !it.released })
        assertTrue(outgoing.playWhenReady)
        fixture.playback.release()
    }

    @Test
    fun engineCommandsStageFadeRollbackAndRetireThroughServiceAdmission() =
        runBlocking {
            val fixture = ServicePlaybackFixture()
            val engine = fixture.engine()
            val listener = RecordingAndroidPlaybackListener()
            engine.listener = listener
            engine.setPlaybackGain(0.42f)
            val loaded =
                async(start = CoroutineStart.UNDISPATCHED) {
                    engine.loadPaused(serviceTrack("Outgoing"), 90L)
                }
            val outgoing = fixture.players.last()
            assertEquals(0.42f, outgoing.volume)
            outgoing.ready(12_000L)
            assertEquals(LoadedPlayback(90L, 12_000L), loaded.await())
            engine.play(901L)
            val staged =
                async(start = CoroutineStart.UNDISPATCHED) {
                    engine.prepareIncoming(serviceTrack("Incoming"), 91L)
                }
            val incoming = fixture.players.last()
            incoming.ready(8_000L)
            assertEquals(LoadedPlayback(91L, 8_000L), staged.await())
            engine.setCrossfadeGains(0.21f, 0f)
            engine.startIncoming(911L)
            engine.setCrossfadeGains(0.21f, 0.3f)

            assertEquals(0.21f, outgoing.volume)
            assertEquals(0.3f, incoming.volume)
            assertTrue(outgoing.playWhenReady)
            assertTrue(incoming.playWhenReady)
            engine.cancelPreparedIncoming()
            assertEquals(
                "Outgoing",
                fixture.facade!!.currentMediaItem!!.mediaMetadata.title)
            assertEquals(0.42f, outgoing.volume)
            assertTrue(incoming.released)

            val next =
                async(start = CoroutineStart.UNDISPATCHED) {
                    engine.prepareIncoming(serviceTrack("Accepted"), 92L)
                }
            val accepted = fixture.players.last()
            accepted.ready(8_000L)
            next.await()
            engine.startIncoming(921L)
            engine.setCrossfadeGains(0.1f, 0.4f)
            engine.finishCrossfade()
            engine.cancelPreparedIncoming()

            assertTrue(outgoing.released)
            assertFalse(accepted.released)
            assertEquals(
                "Accepted",
                fixture.facade!!.currentMediaItem!!.mediaMetadata.title)
            assertTrue(listener.events.contains("status:92:Playing"))
            engine.release()
            assertTrue(accepted.released)
        }

    @Test
    fun engineSuppressesStaleRunAndSourceEventsAndTerminalTrailingStatus() =
        runBlocking {
            val fixture = ServicePlaybackFixture()
            val engine = fixture.engine()
            val listener = RecordingAndroidPlaybackListener()
            engine.listener = listener
            val load =
                async(start = CoroutineStart.UNDISPATCHED) {
                    engine.loadPaused(serviceTrack("Current"), 93L)
                }
            fixture.players.last().ready(12_000L)
            load.await()
            engine.play(931L)
            val token =
                requireNotNull(
                    Media3RequestToken.decode(
                        fixture.players.last().item!!.mediaId))
            engine.play(932L)
            listener.events.clear()

            engine.serviceEvent(
                AndroidServicePlaybackEvent(
                    token, 931L, status = PlaybackStatus.Paused))
            engine.serviceEvent(
                AndroidServicePlaybackEvent(token, 931L, completed = true))
            engine.serviceEvent(
                AndroidServicePlaybackEvent(
                    Media3RequestToken(93L, token.nonce + 1),
                    932L,
                    status = PlaybackStatus.Playing))
            engine.serviceEvent(
                AndroidServicePlaybackEvent(
                    token, 932L, failure = PlaybackError("decoder failed")))
            engine.serviceEvent(
                AndroidServicePlaybackEvent(
                    token, 932L, status = PlaybackStatus.Paused))
            engine.serviceEvent(
                AndroidServicePlaybackEvent(
                    token, 932L, positionMillis = 7_000L))
            engine.serviceEvent(
                AndroidServicePlaybackEvent(token, 932L, completed = true))

            assertEquals(listOf("error:93"), listener.events)
            engine.release()
        }

    @Test
    fun retiringNativeFailureBeforeIncomingPlayingReachesEngineAndSurvivesRollback() =
        runBlocking {
            val fixture = ServicePlaybackFixture()
            val engine = fixture.engine()
            val listener = RecordingAndroidPlaybackListener()
            engine.listener = listener
            try {
                engine.setPlaybackGain(0.42f)
                val load =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        engine.loadPaused(serviceTrack("Outgoing"), 120L)
                    }
                val outgoing = fixture.players.last()
                outgoing.ready(12_000L)
                load.await()
                engine.play(1201L)
                val outgoingCallback = outgoing.listeners.single()
                val delayedProgress =
                    mutableListOf<AndroidServicePlaybackEvent>()
                fixture.forwardEvent = { delayedProgress += it }
                assertTrue(fixture.playback.publishProgress())
                assertEquals(1201L, delayedProgress.single().playbackRunId)
                fixture.forwardEvent = engine::serviceEvent

                val staged =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        engine.prepareIncoming(serviceTrack("Incoming"), 121L)
                    }
                val incoming = fixture.players.last()
                incoming.ready(8_000L)
                staged.await()
                incoming.confirmPlaying = false
                engine.setCrossfadeGains(0.21f, 0f)
                listener.events.clear()
                engine.startIncoming(1211L)

                outgoingCallback.onPlayerError(
                    HostPlaybackException("outgoing decoder failed"))
                outgoingCallback.onPlayerError(
                    HostPlaybackException("duplicate decoder failure"))

                val failure = fixture.events.single { it.failure != null }
                assertEquals(Media3RequestToken(120L, 1L), failure.token)
                assertEquals(1201L, failure.playbackRunId)
                assertEquals(listOf("error:120"), listener.events)
                assertEquals(120L, listener.failures.single().first)
                assertEquals(
                    PlaybackFailureKind.DecoderFailure,
                    listener.failures.single().second.kind)
                assertEquals(
                    "outgoing decoder failed",
                    listener.failures.single().second.cause)
                assertFalse(outgoing.playWhenReady)
                assertFalse(outgoing.released)
                assertTrue(incoming.playWhenReady)
                assertFalse(incoming.released)
                assertEquals(
                    "Incoming",
                    fixture.facade!!.currentMediaItem!!.mediaMetadata.title)

                engine.cancelPreparedIncoming()
                delayedProgress.forEach(engine::serviceEvent)
                outgoingCallback.onIsPlayingChanged(true)
                outgoingCallback.onPlaybackStateChanged(Player.STATE_ENDED)

                assertEquals(listOf("error:120"), listener.events)
                assertEquals(
                    "Outgoing",
                    fixture.facade!!.currentMediaItem!!.mediaMetadata.title)
                assertEquals(0.42f, outgoing.volume)
                assertFalse(outgoing.playWhenReady)
                assertFalse(outgoing.released)
                assertTrue(incoming.released)
                assertEquals(1, outgoing.gainsAtPlay.size)
            } finally {
                engine.release()
                fixture.playback.release()
            }
        }

    @Test
    fun retiringNativeFailureAfterIncomingPlayingWaitsForExplicitControllerRetirement() =
        runBlocking {
            val fixture = ServicePlaybackFixture()
            val engine = fixture.engine()
            val listener = RecordingAndroidPlaybackListener()
            engine.listener = listener
            try {
                val load =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        engine.loadPaused(serviceTrack("Outgoing"), 122L)
                    }
                val outgoing = fixture.players.last()
                outgoing.ready(12_000L)
                load.await()
                engine.play(1221L)
                val outgoingCallback = outgoing.listeners.single()
                val staged =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        engine.prepareIncoming(serviceTrack("Incoming"), 123L)
                    }
                val incoming = fixture.players.last()
                incoming.ready(8_000L)
                staged.await()
                engine.startIncoming(1231L)
                engine.setCrossfadeGains(0.2f, 0.4f)
                assertTrue(listener.events.contains("status:123:Playing"))
                listener.events.clear()

                outgoingCallback.onPlayerError(
                    HostPlaybackException("retiring decoder failed"))

                assertEquals(listOf("error:122"), listener.events)
                assertFalse(outgoing.released)
                assertFalse(outgoing.playWhenReady)
                assertFalse(incoming.released)
                assertTrue(incoming.playWhenReady)
                assertEquals(0.4f, incoming.volume)
                assertEquals(
                    "Incoming",
                    fixture.facade!!.currentMediaItem!!.mediaMetadata.title)

                engine.finishCrossfade()
                engine.cancelPreparedIncoming()
                outgoingCallback.onPlayerError(
                    HostPlaybackException("late retiring decoder failure"))

                assertEquals(listOf("error:122"), listener.events)
                assertTrue(outgoing.released)
                assertFalse(incoming.released)
                assertTrue(incoming.playWhenReady)
                assertEquals(
                    "Incoming",
                    fixture.facade!!.currentMediaItem!!.mediaMetadata.title)
            } finally {
                engine.release()
                fixture.playback.release()
            }
        }

    @Test
    fun obsoleteRetiringNativeRunErrorCannotPauseVoicesOrReachEngine() =
        runBlocking {
            val fixture = ServicePlaybackFixture()
            val engine = fixture.engine()
            val listener = RecordingAndroidPlaybackListener()
            engine.listener = listener
            try {
                val load =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        engine.loadPaused(serviceTrack("Outgoing"), 124L)
                    }
                val outgoing = fixture.players.last()
                outgoing.ready(12_000L)
                load.await()
                engine.play(1241L)
                val obsoleteCallback = outgoing.listeners.single()
                engine.pause()
                engine.play(1242L)
                val activeCallback = outgoing.listeners.single()
                val staged =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        engine.prepareIncoming(serviceTrack("Incoming"), 125L)
                    }
                val incoming = fixture.players.last()
                incoming.ready(8_000L)
                staged.await()
                engine.startIncoming(1251L)
                listener.events.clear()

                obsoleteCallback.onPlayerError(
                    HostPlaybackException("obsolete run failure"))

                assertTrue(listener.events.isEmpty())
                assertTrue(fixture.events.none { it.failure != null })
                assertTrue(outgoing.playWhenReady)
                assertTrue(incoming.playWhenReady)
                assertFalse(outgoing.released)
                assertFalse(incoming.released)

                activeCallback.onPlayerError(
                    HostPlaybackException("active retiring failure"))

                assertEquals(listOf("error:124"), listener.events)
                assertEquals(
                    1242L,
                    fixture.events.single { it.failure != null }.playbackRunId)
                assertTrue(incoming.playWhenReady)
            } finally {
                engine.release()
                fixture.playback.release()
            }
        }

    @Test
    fun queuedRetiringNativeFailureCannotReachEngineAfterExplicitRetirement() =
        runBlocking {
            val fixture = ServicePlaybackFixture()
            val engine = fixture.engine()
            val listener = RecordingAndroidPlaybackListener()
            engine.listener = listener
            try {
                val load =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        engine.loadPaused(serviceTrack("Outgoing"), 126L)
                    }
                val outgoing = fixture.players.last()
                outgoing.ready(12_000L)
                load.await()
                engine.play(1261L)
                val outgoingCallback = outgoing.listeners.single()
                val staged =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        engine.prepareIncoming(serviceTrack("Incoming"), 127L)
                    }
                val incoming = fixture.players.last()
                incoming.ready(8_000L)
                staged.await()
                engine.startIncoming(1271L)
                listener.events.clear()
                val queuedEvents = mutableListOf<AndroidServicePlaybackEvent>()
                fixture.forwardEvent = { queuedEvents += it }

                outgoingCallback.onPlayerError(
                    HostPlaybackException("queued retiring failure"))
                assertEquals(
                    Media3RequestToken(126L, 1L), queuedEvents.single().token)
                assertEquals(1261L, queuedEvents.single().playbackRunId)
                assertNotNull(queuedEvents.single().failure)
                engine.finishCrossfade()
                fixture.forwardEvent = engine::serviceEvent
                queuedEvents.forEach(engine::serviceEvent)
                outgoingCallback.onPlayerError(
                    HostPlaybackException("disposed source failure"))

                assertTrue(listener.events.isEmpty())
                assertTrue(listener.failures.isEmpty())
                assertTrue(outgoing.released)
                assertFalse(incoming.released)
                assertTrue(incoming.playWhenReady)
                assertEquals(
                    "Incoming",
                    fixture.facade!!.currentMediaItem!!.mediaMetadata.title)
            } finally {
                engine.release()
                fixture.playback.release()
            }
        }

    @Test
    fun retiringNativeFailureKeepsIncomingProgressScheduledAfterControllerRetirement() =
        runBlocking {
            val fixture = ServicePlaybackFixture()
            val engine = fixture.engine()
            val listener = RecordingAndroidPlaybackListener()
            engine.listener = listener
            try {
                val load =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        engine.loadPaused(serviceTrack("Outgoing"), 128L)
                    }
                val outgoing = fixture.players.last()
                outgoing.ready(12_000L)
                load.await()
                engine.play(1281L)
                val outgoingCallback = outgoing.listeners.single()
                val staged =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        engine.prepareIncoming(serviceTrack("Incoming"), 129L)
                    }
                val incoming = fixture.players.last()
                incoming.ready(8_000L)
                staged.await()
                engine.startIncoming(1291L)
                listener.events.clear()
                listener.progress.clear()

                outgoingCallback.onPlayerError(
                    HostPlaybackException("retiring decoder failed"))
                engine.finishCrossfade()
                incoming.position = 700L
                assertTrue(fixture.runNextProgressTick())
                incoming.position = 950L
                assertTrue(fixture.runNextProgressTick())

                assertEquals(
                    listOf("error:128", "progress:129", "progress:129"),
                    listener.events)
                assertEquals(
                    listOf<Triple<Long, Long, Long?>>(
                        Triple(129L, 700L, 8_000L), Triple(129L, 950L, 8_000L)),
                    listener.progress)
                assertTrue(outgoing.released)
                assertFalse(incoming.released)
                assertTrue(incoming.playWhenReady)
                assertEquals(
                    "Incoming",
                    fixture.facade!!.currentMediaItem!!.mediaMetadata.title)
            } finally {
                engine.release()
                fixture.playback.release()
            }
        }

    @Test
    fun currentNativeFailureCancelsProgressWithoutPublishingRetainedOutgoing() =
        runBlocking {
            val fixture = ServicePlaybackFixture()
            val engine = fixture.engine()
            val listener = RecordingAndroidPlaybackListener()
            engine.listener = listener
            try {
                val load =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        engine.loadPaused(serviceTrack("Outgoing"), 130L)
                    }
                val outgoing = fixture.players.last()
                outgoing.ready(12_000L)
                load.await()
                engine.play(1301L)
                val staged =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        engine.prepareIncoming(serviceTrack("Incoming"), 131L)
                    }
                val incoming = fixture.players.last()
                incoming.ready(8_000L)
                staged.await()
                engine.startIncoming(1311L)
                listener.events.clear()
                listener.progress.clear()

                incoming.listeners
                    .single()
                    .onPlayerError(
                        HostPlaybackException("current decoder failed"))
                outgoing.position = 7_000L
                incoming.position = 500L

                assertFalse(fixture.runNextProgressTick())
                assertEquals(listOf("error:131"), listener.events)
                assertTrue(listener.progress.isEmpty())
                assertTrue(outgoing.playWhenReady)
                assertFalse(outgoing.released)
                assertFalse(incoming.playWhenReady)
                assertFalse(incoming.released)
                engine.finishCrossfade()
                assertFalse(fixture.runNextProgressTick())
            } finally {
                engine.release()
                fixture.playback.release()
            }
        }

    @Test
    fun retiringNativeCompletionCannotCancelIncomingProgressButCurrentCompletionDoes() =
        runBlocking {
            val fixture = ServicePlaybackFixture()
            val engine = fixture.engine()
            val listener = RecordingAndroidPlaybackListener()
            engine.listener = listener
            try {
                val load =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        engine.loadPaused(serviceTrack("Outgoing"), 132L)
                    }
                val outgoing = fixture.players.last()
                outgoing.ready(12_000L)
                load.await()
                engine.play(1321L)
                val outgoingCallback = outgoing.listeners.single()
                val staged =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        engine.prepareIncoming(serviceTrack("Incoming"), 133L)
                    }
                val incoming = fixture.players.last()
                incoming.ready(8_000L)
                staged.await()
                engine.startIncoming(1331L)
                listener.events.clear()
                listener.progress.clear()

                outgoing.playWhenReady = false
                outgoingCallback.onPlaybackStateChanged(Player.STATE_ENDED)
                engine.finishCrossfade()
                incoming.position = 750L
                assertTrue(fixture.runNextProgressTick())
                assertEquals(
                    listOf<Triple<Long, Long, Long?>>(
                        Triple(133L, 750L, 8_000L)),
                    listener.progress)

                incoming.playWhenReady = false
                incoming.listeners
                    .single()
                    .onPlaybackStateChanged(Player.STATE_ENDED)

                assertFalse(fixture.runNextProgressTick())
                assertEquals(
                    listOf("completed:132", "progress:133", "completed:133"),
                    listener.events)
                assertEquals(
                    listOf<Triple<Long, Long, Long?>>(
                        Triple(133L, 750L, 8_000L)),
                    listener.progress)
            } finally {
                engine.release()
                fixture.playback.release()
            }
        }

    @Test
    fun delayedPriorRunStatusCannotCancelResumedCurrentProgress() =
        runBlocking {
            val fixture = ServicePlaybackFixture()
            val engine = fixture.engine()
            val listener = RecordingAndroidPlaybackListener()
            engine.listener = listener
            try {
                val load =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        engine.loadPaused(serviceTrack("Current"), 134L)
                    }
                val current = fixture.players.last()
                current.ready(12_000L)
                load.await()
                engine.play(1341L)
                engine.pause()
                val priorPause =
                    fixture.events.single { it.status == PlaybackStatus.Paused }
                assertEquals(1341L, priorPause.playbackRunId)
                assertFalse(fixture.runNextProgressTick())
                engine.play(1342L)
                listener.events.clear()
                listener.progress.clear()

                fixture.deliverQueuedEvent(priorPause)
                current.position = 1_000L
                assertTrue(fixture.runNextProgressTick())

                assertEquals(listOf("progress:134"), listener.events)
                assertEquals(
                    listOf<Triple<Long, Long, Long?>>(
                        Triple(134L, 1_000L, 12_000L)),
                    listener.progress)
                assertEquals(1342L, fixture.events.last().playbackRunId)
                assertTrue(current.playWhenReady)
            } finally {
                engine.release()
                fixture.playback.release()
            }
        }

    @Test
    fun engineIncomingFailureKeepsRollbackAuthorityUntilExplicitRetirement() =
        runBlocking {
            val fixture = ServicePlaybackFixture()
            val engine = fixture.engine()
            val load =
                async(start = CoroutineStart.UNDISPATCHED) {
                    engine.loadPaused(serviceTrack("Outgoing"), 97L)
                }
            val outgoing = fixture.players.last()
            outgoing.ready(12_000L)
            load.await()
            engine.play(971L)
            val staged =
                async(start = CoroutineStart.UNDISPATCHED) {
                    engine.prepareIncoming(serviceTrack("Incoming"), 98L)
                }
            val incoming = fixture.players.last()
            incoming.ready(8_000L)
            staged.await()
            engine.startIncoming(981L)
            val incomingToken =
                Media3RequestToken.decode(incoming.item!!.mediaId)!!
            engine.serviceEvent(
                AndroidServicePlaybackEvent(
                    incomingToken,
                    981L,
                    failure = PlaybackError("incoming decoder failed")))

            assertFalse(outgoing.released)
            engine.cancelPreparedIncoming()
            assertTrue(incoming.released)
            assertFalse(outgoing.released)
            assertEquals(
                "Outgoing",
                fixture.facade!!.currentMediaItem!!.mediaMetadata.title)

            val acceptedLoad =
                async(start = CoroutineStart.UNDISPATCHED) {
                    engine.prepareIncoming(serviceTrack("Accepted"), 99L)
                }
            val accepted = fixture.players.last()
            accepted.ready(8_000L)
            acceptedLoad.await()
            engine.startIncoming(991L)
            engine.finishCrossfade()
            val acceptedToken =
                Media3RequestToken.decode(accepted.item!!.mediaId)!!
            engine.serviceEvent(
                AndroidServicePlaybackEvent(
                    acceptedToken,
                    991L,
                    failure = PlaybackError("accepted decoder failed")))
            engine.cancelPreparedIncoming()
            assertTrue(outgoing.released)
            assertFalse(accepted.released)
            assertEquals(
                "Accepted",
                fixture.facade!!.currentMediaItem!!.mediaMetadata.title)
            engine.release()
        }

    @Test
    fun cancelledEnginePreparationCannotTouchLaterStagedWinner() = runBlocking {
        val fixture = ServicePlaybackFixture()
        val engine = fixture.engine()
        val load =
            async(start = CoroutineStart.UNDISPATCHED) {
                engine.loadPaused(serviceTrack("Current"), 94L)
            }
        fixture.players.last().ready(12_000L)
        load.await()
        engine.play(941L)
        val cancelled =
            async(start = CoroutineStart.UNDISPATCHED) {
                engine.prepareIncoming(serviceTrack("Cancelled"), 95L)
            }
        val obsolete = fixture.players.last()
        cancelled.cancel()
        cancelled.join()
        val replacement =
            async(start = CoroutineStart.UNDISPATCHED) {
                engine.prepareIncoming(serviceTrack("Replacement"), 96L)
            }
        val winner = fixture.players.last()
        winner.ready(8_000L)

        assertTrue(obsolete.released)
        assertFalse(winner.released)
        assertEquals(LoadedPlayback(96L, 8_000L), replacement.await())
        engine.startIncoming(961L)
        assertTrue(winner.playWhenReady)
        engine.finishCrossfade()
        engine.release()
    }

    @Test
    fun systemFocusAndNoisyPauseBothVoicesWithoutResettingGain() {
        listOf(
                Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS,
                Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY)
            .forEach { reason ->
                val fixture = ServicePlaybackFixture()
                val outgoing = fixture.load(60L, "Outgoing")
                fixture.command(AndroidServiceEngineCommand.Play(601L))
                fixture.command(
                    AndroidServiceEngineCommand.PrepareIncoming(
                        fixture.item(61L, "Incoming")))
                val incoming = fixture.players.last()
                incoming.ready(8_000L)
                fixture.command(AndroidServiceEngineCommand.StartIncoming(602L))
                fixture.command(
                    AndroidServiceEngineCommand.CrossfadeGains(0.2f, 0.3f))

                incoming.listeners
                    .single()
                    .onPlayWhenReadyChanged(false, reason)

                assertFalse(outgoing.playWhenReady)
                assertFalse(incoming.playWhenReady)
                assertEquals(0.2f, outgoing.volume)
                assertEquals(0.3f, incoming.volume)
                fixture.playback.release()
            }
    }

    @Test
    fun stopClearAndReleaseDisposeBothNativeVoices() {
        listOf(
                AndroidServiceEngineCommand.Stop,
                AndroidServiceEngineCommand.Clear,
                AndroidServiceEngineCommand.Release)
            .forEach { command ->
                val fixture = ServicePlaybackFixture()
                val outgoing = fixture.load(70L, "Outgoing")
                fixture.command(AndroidServiceEngineCommand.Play(701L))
                fixture.command(
                    AndroidServiceEngineCommand.PrepareIncoming(
                        fixture.item(71L, "Incoming")))
                val incoming = fixture.players.last()
                incoming.ready(8_000L)
                fixture.command(AndroidServiceEngineCommand.StartIncoming(702L))
                fixture.command(
                    AndroidServiceEngineCommand.CrossfadeGains(0.2f, 0.3f))

                fixture.command(command).get()

                assertTrue(outgoing.released)
                assertTrue(incoming.released)
                assertEquals(1f, outgoing.volume)
                assertEquals(1f, incoming.volume)
                fixture.playback.release()
            }
    }

    @Test
    fun crossfadeGainValidationRejectsEitherInvalidVoiceBeforeMutatingPlayers() {
        val fixture = ServicePlaybackFixture()
        val outgoing = fixture.load(80L, "Outgoing")
        fixture.command(
            AndroidServiceEngineCommand.PrepareIncoming(
                fixture.item(81L, "Incoming")))
        val incoming = fixture.players.last()
        incoming.ready(8_000L)
        fixture.command(AndroidServiceEngineCommand.StartIncoming(802L))
        fixture.command(AndroidServiceEngineCommand.CrossfadeGains(0.2f, 0.3f))

        listOf(Float.NaN, Float.POSITIVE_INFINITY, -0.01f, 1.01f).forEach {
            invalid ->
            assertNotNull(
                fixture
                    .command(
                        AndroidServiceEngineCommand.CrossfadeGains(
                            invalid, 0.4f))
                    .get()
                    .failure)
            assertNotNull(
                fixture
                    .command(
                        AndroidServiceEngineCommand.CrossfadeGains(
                            0.4f, invalid))
                    .get()
                    .failure)
            assertEquals(0.2f, outgoing.volume)
            assertEquals(0.3f, incoming.volume)
        }
        fixture.playback.release()
    }

    @Test
    fun externalMediaSessionPlayAndSeekCannotBypassControllerRunAdmission() {
        val nativeActions = mutableListOf<String>()
        val requestedActions = mutableListOf<String>()
        var internalController = false
        val delegate =
            Proxy.newProxyInstance(
                Player::class.java.classLoader,
                arrayOf(Player::class.java),
            ) { _, method, arguments ->
                when (method.name) {
                    "play" -> nativeActions += "play"
                    "seekTo" -> nativeActions += "seek:${arguments?.last()}"
                    else -> error("Unexpected Player method: ${method.name}")
                }
                null
            } as Player
        val player =
            SkipRoutingPlayer(
                delegate, isInternalController = { internalController })
        RhythHausTransportBridge.onPlay = { requestedActions += "play" }
        RhythHausTransportBridge.onSeek = { requestedActions += "seek:$it" }
        try {
            player.play()
            player.seekTo(2_000L)
            assertEquals(listOf("play", "seek:2000"), requestedActions)
            assertEquals(emptyList(), nativeActions)

            internalController = true
            player.play()
            player.seekTo(3_000L)
            assertEquals(listOf("play", "seek:3000"), nativeActions)
            assertEquals(listOf("play", "seek:2000"), requestedActions)
        } finally {
            RhythHausTransportBridge.onPlay = null
            RhythHausTransportBridge.onSeek = null
        }
    }

    @Test
    fun externalPlayWhenReadyAndSeekShortcutsRouteThroughController() {
        val nativeActions = mutableListOf<String>()
        val requestedActions = mutableListOf<String>()
        val delegate =
            Proxy.newProxyInstance(
                Player::class.java.classLoader,
                arrayOf(Player::class.java),
            ) { _, method, arguments ->
                when (method.name) {
                    "getCurrentPosition" -> 4_000L
                    "getSeekBackIncrement" -> 1_000L
                    "getSeekForwardIncrement" -> 1_500L
                    "getDuration" -> 8_000L
                    "setPlayWhenReady",
                    "seekBack",
                    "seekForward",
                    "seekToDefaultPosition" -> {
                        nativeActions += method.name
                        null
                    }
                    else ->
                        error(
                            "Unexpected Player method: ${method.name}, $arguments")
                }
            } as Player
        val player =
            SkipRoutingPlayer(delegate, isInternalController = { false })
        RhythHausTransportBridge.onPlay = { requestedActions += "play" }
        RhythHausTransportBridge.onPause = { requestedActions += "pause" }
        RhythHausTransportBridge.onSeek = { requestedActions += "seek:$it" }
        try {
            player.playWhenReady = true
            player.playWhenReady = false
            player.seekBack()
            player.seekForward()
            player.seekToDefaultPosition()
            player.seekToDefaultPosition(0)
            assertEquals(
                listOf(
                    "play",
                    "pause",
                    "seek:3000",
                    "seek:5500",
                    "seek:0",
                    "seek:0"),
                requestedActions,
            )
            assertEquals(emptyList(), nativeActions)
        } finally {
            RhythHausTransportBridge.onPlay = null
            RhythHausTransportBridge.onPause = null
            RhythHausTransportBridge.onSeek = null
        }
    }

    @Test
    fun externalSessionCannotChangeAppGainOrPlayWhenUserTransportIsDisabled() {
        val fixture = ServicePlaybackFixture()
        val current = fixture.load(85L, "Current")
        fixture.command(AndroidServiceEngineCommand.Gain(0.42f))
        val external =
            SkipRoutingPlayer(
                current.player,
                isInternalController = { false },
                playback = fixture.playback)
        var requestedPlay = false
        RhythHausTransportBridge.onPlay = { requestedPlay = true }
        RhythHausTransportBridge.setTransportEnabled(false)
        try {
            external.volume = 1f
            external.play()
            external.seekTo(3_000L)
            assertEquals(0.42f, current.volume)
            assertEquals(0L, current.position)
            assertFalse(current.playWhenReady)
            assertFalse(requestedPlay)
        } finally {
            RhythHausTransportBridge.onPlay = null
            RhythHausTransportBridge.setTransportEnabled(true)
            fixture.playback.release()
        }
    }

    @Test
    fun serviceReleaseDetachesBothVoicesButKeepsTheSingleSessionReusable() {
        val fixture = ServicePlaybackFixture()
        val outgoing = fixture.load(86L, "Outgoing")
        fixture.command(AndroidServiceEngineCommand.Play(861L))
        fixture.command(
            AndroidServiceEngineCommand.PrepareIncoming(
                fixture.item(87L, "Incoming")))
        val incoming = fixture.players.last()
        incoming.ready(8_000L)
        fixture.command(AndroidServiceEngineCommand.StartIncoming(871L))
        fixture.command(AndroidServiceEngineCommand.Release)
        assertTrue(outgoing.released)
        assertTrue(incoming.released)

        val replacement = fixture.load(88L, "Replacement")
        fixture.command(AndroidServiceEngineCommand.Play(881L))
        assertTrue(replacement.playWhenReady)
        assertEquals(
            "Replacement",
            fixture.facade!!.currentMediaItem!!.mediaMetadata.title)
        fixture.playback.release()
    }

    @Test
    fun engineSeekToEndInvalidatesTheNativeRunAndAcceptsRunlessProgress() =
        runBlocking {
            val fixture = ServicePlaybackFixture()
            val engine = fixture.engine()
            val listener = RecordingAndroidPlaybackListener()
            engine.listener = listener
            val load =
                async(start = CoroutineStart.UNDISPATCHED) {
                    engine.loadPaused(serviceTrack("Current"), 89L)
                }
            val native = fixture.players.last()
            native.ready(12_000L)
            load.await()
            engine.play(891L)
            val callback = native.listeners.single()
            listener.events.clear()

            engine.seekTo(12_000L)
            callback.onPlaybackStateChanged(Player.STATE_ENDED)

            assertEquals(12_000L, native.position)
            assertEquals(listOf("progress:89"), listener.events)
            engine.release()
        }

    @Test
    fun mediaItemCarriesTrackMetadataForAndroidSystemControls() {
        val artwork = byteArrayOf(1, 2, 3)
        val track =
            PlayableTrack(
                id = "track-1",
                title = "Night Drive",
                artist = "Rhyth Haus",
                album = "Local Sessions",
                durationMillis = 181_000L,
                source =
                    AudioSource.Uri("content://media/external/audio/media/42"),
                artworkBytes = artwork,
            )

        val mediaMetadata = buildAndroidPlaybackMediaMetadata(track)

        assertEquals("Night Drive", mediaMetadata.title.toString())
        assertEquals("Rhyth Haus", mediaMetadata.artist.toString())
        assertEquals("Local Sessions", mediaMetadata.albumTitle.toString())
        assertTrue(artwork.contentEquals(mediaMetadata.artworkData))
        assertEquals(
            MediaMetadata.PICTURE_TYPE_FRONT_COVER,
            mediaMetadata.artworkDataType,
        )
    }

    @Test
    fun androidOldTokenCannotAcknowledgeSameTrackNewGeneration() = runBlocking {
        val fixture = ServicePlaybackFixture()
        val engine = fixture.engine()
        val first =
            async(start = CoroutineStart.UNDISPATCHED) {
                engine.loadPaused(serviceTrack("Same"), 10L)
            }
        val oldCallback = fixture.players.last().listeners.single()
        val second =
            async(start = CoroutineStart.UNDISPATCHED) {
                engine.loadPaused(serviceTrack("Same"), 11L)
            }

        oldCallback.onPlaybackStateChanged(Player.STATE_READY)
        assertFalse(second.isCompleted)
        fixture.players.last().ready(2_000L)
        assertFailsWith<CancellationException> { first.await() }
        assertEquals(LoadedPlayback(11L, 2_000L), second.await())
        engine.release()
    }

    @Test
    fun androidMediaItemCarriesUniqueGenerationRequestToken() = runBlocking {
        val fixture = ServicePlaybackFixture()
        val engine = fixture.engine()
        val first =
            async(start = CoroutineStart.UNDISPATCHED) {
                engine.loadPaused(serviceTrack("Same"), 20L)
            }
        val outgoing = fixture.players.last()
        outgoing.ready(12_000L)
        first.await()
        engine.play(201L)
        val second =
            async(start = CoroutineStart.UNDISPATCHED) {
                engine.prepareIncoming(serviceTrack("Same"), 21L)
            }
        val incoming = fixture.players.last()
        incoming.ready(8_000L)
        second.await()

        assertFalse(outgoing.item!!.mediaId == incoming.item!!.mediaId)
        assertEquals(
            20L,
            Media3RequestToken.decode(outgoing.item!!.mediaId)!!.generation)
        assertEquals(
            21L,
            Media3RequestToken.decode(incoming.item!!.mediaId)!!.generation)
        engine.release()
    }

    @Test
    fun gainSetBeforeConnectionReachesServicePlayerAndReturnsToUnityAfterReset() =
        runBlocking {
            val fixture = ServicePlaybackFixture()
            val engine = fixture.engine()
            engine.setPlaybackGain(0.27f)
            assertTrue(fixture.players.isEmpty())
            val first =
                async(start = CoroutineStart.UNDISPATCHED) {
                    engine.loadPaused(serviceTrack("First"), 22L)
                }
            val firstPlayer = fixture.players.last()
            assertEquals(0.27f, firstPlayer.volume)
            firstPlayer.ready(12_000L)
            first.await()
            val replacement =
                async(start = CoroutineStart.UNDISPATCHED) {
                    engine.loadPaused(serviceTrack("Replacement"), 23L)
                }
            val replacementPlayer = fixture.players.last()
            assertEquals(0.27f, replacementPlayer.volume)
            replacementPlayer.ready(12_000L)
            replacement.await()
            engine.stop()
            assertTrue(replacementPlayer.released)
            assertEquals(1f, replacementPlayer.volume)
            val next =
                async(start = CoroutineStart.UNDISPATCHED) {
                    engine.loadPaused(serviceTrack("Next"), 24L)
                }
            assertEquals(1f, fixture.players.last().volume)
            fixture.players.last().ready(12_000L)
            next.await()
            engine.release()
        }

    @Test
    fun playbackGainRejectsNonFiniteAndOutOfRangeValues() {
        val gain = AndroidPlaybackGainState()

        assertFailsWith<IllegalArgumentException> { gain.set(Float.NaN) }
        assertFailsWith<IllegalArgumentException> {
            gain.set(Float.NEGATIVE_INFINITY)
        }
        assertFailsWith<IllegalArgumentException> { gain.set(-0.01f) }
        assertFailsWith<IllegalArgumentException> { gain.set(1.01f) }
    }

    @Test
    fun releaseResetStaysAtUnityWhenGainChangesBeforeQueuedTeardownRuns() =
        runBlocking {
            val executor = RecordingAndroidControllerExecutor()
            val fixture = ServicePlaybackFixture()
            val engine = fixture.engine(executor = executor)
            engine.setPlaybackGain(0.27f)
            val load =
                async(start = CoroutineStart.UNDISPATCHED) {
                    engine.loadPaused(serviceTrack("Current"), 25L)
                }
            executor.runAll()
            val servicePlayer = fixture.players.last()
            servicePlayer.ready(12_000L)
            executor.runAll()
            load.await()
            assertEquals(0.27f, servicePlayer.volume)
            engine.release()
            engine.setPlaybackGain(0.61f)
            executor.runAll()
            assertEquals(1f, servicePlayer.volume)
            assertTrue(servicePlayer.released)
            fixture.playback.release()
        }

    @Test
    fun connectionFailureSettlesActiveLoadAndAllowsReplacement() = runBlocking {
        val fixture = ServicePlaybackFixture()
        var failConnection = true
        val engine =
            fixture.engine(
                sender = { command ->
                    if (failConnection) error("connection failed")
                    else fixture.command(command)
                })
        assertFailsWith<PlaybackFailureException> {
            engine.loadPaused(serviceTrack("Failed"), 30L)
        }
        failConnection = false
        val second =
            async(start = CoroutineStart.UNDISPATCHED) {
                engine.loadPaused(serviceTrack("Replacement"), 31L)
            }
        fixture.players.last().ready(2_000L)
        assertEquals(
            LoadedPlayback(31L, 2_000L), withTimeout(1_000) { second.await() })
        engine.release()
    }

    @Test
    fun matchingTokenPlayerErrorSettlesLoadAndLaterLoadCanBecomeReady() =
        runBlocking {
            supervisorScope {
                val fixture = ServicePlaybackFixture()
                val engine = fixture.engine()
                val first =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        engine.loadPaused(serviceTrack("Failed"), 40L)
                    }
                val token =
                    Media3RequestToken.decode(
                        fixture.players.last().item!!.mediaId)!!
                engine.serviceEvent(
                    AndroidServicePlaybackEvent(
                        token, failure = PlaybackError("decoder failed")))
                assertFailsWith<PlaybackFailureException> { first.await() }
                val second =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        engine.loadPaused(serviceTrack("Replacement"), 41L)
                    }
                fixture.players.last().ready(3_000L)
                assertEquals(LoadedPlayback(41L, 3_000L), second.await())
                engine.release()
            }
        }

    @Test
    fun cancellingOldLoadCannotInvalidateOrCompleteReplacement() = runBlocking {
        val fixture = ServicePlaybackFixture()
        val engine = fixture.engine()
        val first =
            async(start = CoroutineStart.UNDISPATCHED) {
                engine.loadPaused(serviceTrack("Old"), 50L)
            }
        val obsolete = fixture.players.last()
        val oldToken = Media3RequestToken.decode(obsolete.item!!.mediaId)!!
        val second =
            async(start = CoroutineStart.UNDISPATCHED) {
                engine.loadPaused(serviceTrack("Winner"), 51L)
            }
        val winner = fixture.players.last()
        fixture.command(AndroidServiceEngineCommand.CancelLoad(oldToken))
        assertFailsWith<CancellationException> { first.await() }
        winner.ready(2_000L)
        assertTrue(obsolete.released)
        assertFalse(winner.released)
        assertEquals(LoadedPlayback(51L, 2_000L), second.await())
        engine.release()
    }

    @Test
    fun releaseInvalidatesTokenAndCancelsOutstandingLoad() = runBlocking {
        val fixture = ServicePlaybackFixture()
        val engine = fixture.engine()
        val request =
            async(start = CoroutineStart.UNDISPATCHED) {
                engine.loadPaused(serviceTrack("Pending"), 60L)
            }
        val native = fixture.players.last()
        val oldCallback = native.listeners.single()
        engine.release()
        oldCallback.onPlaybackStateChanged(Player.STATE_READY)
        assertFailsWith<CancellationException> { request.await() }
        assertTrue(native.released)
        val replacementEngine = fixture.engine()
        val replacement =
            async(start = CoroutineStart.UNDISPATCHED) {
                replacementEngine.loadPaused(serviceTrack("Replacement"), 61L)
            }
        fixture.players.last().ready(1_000L)
        assertEquals(LoadedPlayback(61L, 1_000L), replacement.await())
        replacementEngine.release()
    }

    @Test
    fun readySourceRetainsObservableGenerationForLaterCallbacks() =
        runBlocking {
            val fixture = ServicePlaybackFixture()
            val engine = fixture.engine()
            val listener = RecordingAndroidPlaybackListener()
            engine.listener = listener
            val request =
                async(start = CoroutineStart.UNDISPATCHED) {
                    engine.loadPaused(serviceTrack("Current"), 70L)
                }
            val native = fixture.players.last()
            native.ready(1_234L)
            assertEquals(LoadedPlayback(70L, 1_234L), request.await())
            listener.events.clear()
            engine.play(701L)
            native.position = 300L
            fixture.playback.publishProgress()
            assertEquals(
                listOf("status:70:Playing", "progress:70", "progress:70"),
                listener.events)
            engine.release()
        }

    @Test
    fun replacementInvalidatesReadySourceAndRejectsLateCallbacks() =
        runBlocking {
            val fixture = ServicePlaybackFixture()
            val engine = fixture.engine()
            val listener = RecordingAndroidPlaybackListener()
            engine.listener = listener
            val first =
                async(start = CoroutineStart.UNDISPATCHED) {
                    engine.loadPaused(serviceTrack("Old"), 80L)
                }
            fixture.players.last().ready(2_000L)
            first.await()
            engine.play(801L)
            val oldToken =
                Media3RequestToken.decode(
                    fixture.players.last().item!!.mediaId)!!
            val second =
                async(start = CoroutineStart.UNDISPATCHED) {
                    engine.loadPaused(serviceTrack("Winner"), 81L)
                }
            fixture.players.last().ready(3_000L)
            second.await()
            listener.events.clear()
            engine.serviceEvent(
                AndroidServicePlaybackEvent(
                    oldToken, 801L, status = PlaybackStatus.Playing))
            engine.serviceEvent(
                AndroidServicePlaybackEvent(oldToken, 801L, completed = true))
            assertTrue(listener.events.isEmpty())
            engine.release()
        }

    @Test
    fun progressCaptureCannotBeRelabelledAcrossSourceReplacement() =
        runBlocking {
            val fixture = ServicePlaybackFixture()
            val engine = fixture.engine()
            val listener = RecordingAndroidPlaybackListener()
            engine.listener = listener
            val first =
                async(start = CoroutineStart.UNDISPATCHED) {
                    engine.loadPaused(serviceTrack("Old"), 90L)
                }
            val old = fixture.players.last()
            old.ready(3_000L)
            first.await()
            engine.play(901L)
            val captured =
                AndroidServicePlaybackEvent(
                    Media3RequestToken.decode(old.item!!.mediaId)!!,
                    901L,
                    positionMillis = 2_000L)
            val second =
                async(start = CoroutineStart.UNDISPATCHED) {
                    engine.loadPaused(serviceTrack("Winner"), 91L)
                }
            fixture.players.last().ready(3_000L)
            second.await()
            listener.events.clear()
            engine.serviceEvent(captured)
            assertTrue(listener.events.isEmpty())
            assertEquals(90L, captured.token.generation)
            engine.release()
        }

    @Test
    fun controllerOperationsAreDispatchedThroughProductionExecutorSeam() {
        val executor = RecordingAndroidControllerExecutor()
        val calls = mutableListOf<String>()
        val operations = AndroidControllerOperations(executor)

        operations.dispatch { calls += "load" }
        operations.dispatch { calls += "clear" }

        assertEquals(emptyList(), calls)
        assertEquals(2, executor.pendingCount)
        executor.runAll()
        assertEquals(listOf("load", "clear"), calls)
    }

    @Test
    fun androidFileNotFoundMapsToMissingFile() {
        val mapped =
            androidPlaybackError(
                errorCode = PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                message = "source file missing",
            )

        assertEquals(PlaybackFailureKind.MissingFile, mapped.kind)
        assertEquals("Android could not play this audio file.", mapped.message)
        assertEquals("source file missing", mapped.cause)
    }

    @Test
    fun androidPermissionFailureMapsToAccessLost() {
        val errorCode = PlaybackException.ERROR_CODE_IO_NO_PERMISSION

        assertEquals(
            PlaybackFailureKind.AccessLost, androidFailureKind(errorCode))
        assertEquals(
            PlaybackFailureKind.AccessLost,
            androidPlaybackError(
                    errorCode = errorCode,
                    message = "no permission to read source",
                )
                .kind,
        )
    }

    @Test
    fun androidUnsupportedContainerMapsToUnsupportedFormat() {
        val errorCode =
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED

        assertEquals(
            PlaybackFailureKind.UnsupportedFormat,
            androidFailureKind(errorCode))
    }

    @Test
    fun androidDecoderFailureMapsToDecoderFailure() {
        val errorCode = PlaybackException.ERROR_CODE_DECODING_FAILED
        val decoderErrorCodes =
            listOf(
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
                PlaybackException.ERROR_CODE_DECODING_FAILED,
                PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
                PlaybackException
                    .ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
                PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED,
            )

        decoderErrorCodes.forEach { code ->
            assertEquals(
                PlaybackFailureKind.DecoderFailure,
                androidFailureKind(code),
            )
        }
        assertEquals(
            PlaybackFailureKind.DecoderFailure,
            androidPlaybackError(
                    errorCode = errorCode,
                    message = "decoder could not decode samples",
                )
                .kind,
        )
    }

    @Test
    fun androidOpaquePlayerErrorRemainsUnknown() {
        val errorCode = PlaybackException.ERROR_CODE_UNSPECIFIED

        assertEquals(PlaybackFailureKind.Unknown, androidFailureKind(errorCode))
        assertEquals(
            PlaybackFailureKind.Unknown,
            androidPlaybackError(
                    errorCode = errorCode,
                    message = "unidentified failure",
                )
                .kind,
        )
    }

    @Test
    fun androidAbsentLocalSourceFileMapsToMissingFile() {
        val absent = Files.createTempFile("rhythhaus-absent-android", ".wav")
        Files.deleteIfExists(absent)
        try {
            val mapped =
                androidPlaybackError(
                    errorCode = PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                    message = "opaque io failure",
                    sourceFile = absent.toFile(),
                )

            assertEquals(PlaybackFailureKind.MissingFile, mapped.kind)
        } finally {
            Files.deleteIfExists(absent)
        }
    }

    @Test
    fun releaseIsSerializedAndSuppressesQueuedAndPostReleaseControllerWork() {
        val executor = RecordingAndroidControllerExecutor()
        val operations = AndroidControllerOperations(executor)
        val lifecycle = AndroidControllerLifecycle(operations)
        val calls = mutableListOf<String>()
        val releaseQueued = CountDownLatch(1)

        lifecycle.dispatchControllerWork { calls += "queued-before-release" }
        val releaseThread =
            thread(start = true) {
                lifecycle.release {
                    calls += "release-controller"
                    calls += "release-future"
                }
                releaseQueued.countDown()
            }
        assertTrue(releaseQueued.await(1, TimeUnit.SECONDS))
        releaseThread.join()
        lifecycle.dispatchControllerWork { calls += "queued-after-release" }

        assertEquals(emptyList(), calls)
        executor.runAll()

        assertEquals(listOf("release-controller", "release-future"), calls)
        assertEquals(0, executor.pendingCount)
    }

    @Test
    fun terminalPlayerErrorSuppressesTrailingIsPlayingFalsePaused() =
        runBlocking {
            withPlayingEngine(100L, 1001L) { fixture, engine, listener ->
                val token =
                    Media3RequestToken.decode(
                        fixture.players.last().item!!.mediaId)!!
                engine.serviceEvent(
                    AndroidServicePlaybackEvent(
                        token,
                        1001L,
                        failure = PlaybackError("decoder failed")))
                engine.serviceEvent(
                    AndroidServicePlaybackEvent(
                        token, 1001L, status = PlaybackStatus.Paused))
                assertEquals(listOf("error:100"), listener.events)
            }
        }

    @Test
    fun ordinaryIsPlayingFalseStillPublishesPaused() = runBlocking {
        withPlayingEngine(200L, 2001L) { fixture, engine, listener ->
            val token =
                Media3RequestToken.decode(
                    fixture.players.last().item!!.mediaId)!!
            engine.serviceEvent(
                AndroidServicePlaybackEvent(
                    token, 2001L, status = PlaybackStatus.Paused))
            assertEquals(listOf("status:200:Paused"), listener.events)
        }
    }

    @Test
    fun playingPublicationWaitsForMedia3ActualPlayingConfirmation() =
        runBlocking {
            val fixture = ServicePlaybackFixture()
            val engine = fixture.engine()
            val listener = RecordingAndroidPlaybackListener()
            engine.listener = listener
            val load =
                async(start = CoroutineStart.UNDISPATCHED) {
                    engine.loadPaused(serviceTrack("Current"), 250L)
                }
            val native = fixture.players.last()
            native.ready(12_000L)
            load.await()
            native.confirmPlaying = false
            listener.events.clear()
            engine.play(2501L)
            assertTrue(listener.events.isEmpty())
            native.listeners.single().onIsPlayingChanged(false)
            assertEquals(
                listOf("status:250:Paused", "progress:250"), listener.events)
            native.listeners.single().onIsPlayingChanged(true)
            assertEquals(
                listOf(
                    "status:250:Paused",
                    "progress:250",
                    "status:250:Playing",
                    "progress:250"),
                listener.events)
            engine.release()
        }

    @Test
    fun replacementRequestAfterTerminalFailurePublishesNormalStatuses() =
        runBlocking {
            val fixture = ServicePlaybackFixture()
            val engine = fixture.engine()
            val listener = RecordingAndroidPlaybackListener()
            engine.listener = listener
            val first =
                async(start = CoroutineStart.UNDISPATCHED) {
                    engine.loadPaused(serviceTrack("Failed"), 300L)
                }
            fixture.players.last().ready(12_000L)
            first.await()
            engine.play(3001L)
            val oldToken =
                Media3RequestToken.decode(
                    fixture.players.last().item!!.mediaId)!!
            listener.events.clear()
            engine.serviceEvent(
                AndroidServicePlaybackEvent(
                    oldToken, 3001L, failure = PlaybackError("decoder failed")))
            engine.serviceEvent(
                AndroidServicePlaybackEvent(
                    oldToken, 3001L, status = PlaybackStatus.Paused))
            assertEquals(listOf("error:300"), listener.events)
            val replacement =
                async(start = CoroutineStart.UNDISPATCHED) {
                    engine.loadPaused(serviceTrack("Replacement"), 301L)
                }
            fixture.players.last().ready(12_000L)
            replacement.await()
            listener.events.clear()
            engine.play(3011L)
            assertEquals(
                listOf("status:301:Playing", "progress:301"), listener.events)
            engine.release()
        }

    @Test
    fun duplicatePlayerErrorDoesNotRepublishForTerminalRequest() = runBlocking {
        withPlayingEngine(400L, 4001L) { fixture, engine, listener ->
            val token =
                Media3RequestToken.decode(
                    fixture.players.last().item!!.mediaId)!!
            val failure =
                AndroidServicePlaybackEvent(
                    token, 4001L, failure = PlaybackError("decoder failed"))
            engine.serviceEvent(failure)
            engine.serviceEvent(failure)
            assertEquals(listOf("error:400"), listener.events)
        }
    }

    @Test
    fun obsoletePlayerErrorAfterReplacementDoesNotRepublish() = runBlocking {
        withPlayingEngine(500L, 5001L) { fixture, engine, listener ->
            val token =
                Media3RequestToken.decode(
                    fixture.players.last().item!!.mediaId)!!
            engine.serviceEvent(
                AndroidServicePlaybackEvent(
                    Media3RequestToken(499L, token.nonce),
                    4991L,
                    failure = PlaybackError("old decoder failed")))
            engine.serviceEvent(
                AndroidServicePlaybackEvent(
                    token, 5001L, status = PlaybackStatus.Playing))
            assertEquals(listOf("status:500:Playing"), listener.events)
        }
    }

    private suspend fun withPlayingEngine(
        generation: Long,
        run: Long,
        action:
            (
                ServicePlaybackFixture,
                AndroidPlaybackEngine,
                RecordingAndroidPlaybackListener) -> Unit,
    ) = coroutineScope {
        val fixture = ServicePlaybackFixture()
        val engine = fixture.engine()
        val listener = RecordingAndroidPlaybackListener()
        engine.listener = listener
        try {
            val load =
                async(start = CoroutineStart.UNDISPATCHED) {
                    engine.loadPaused(serviceTrack("Current"), generation)
                }
            fixture.players.last().ready(12_000L)
            load.await()
            engine.play(run)
            listener.events.clear()
            action(fixture, engine, listener)
        } finally {
            engine.release()
            fixture.playback.release()
        }
    }

    private class ServicePlaybackFixture {
        val players = mutableListOf<ServiceNativePlayer>()
        val events = mutableListOf<AndroidServicePlaybackEvent>()
        var facade: SkipRoutingPlayer? = null
        var failNextPrepare = false
        var forwardEvent: ((AndroidServicePlaybackEvent) -> Unit)? = null
        private val scheduledProgress = ArrayDeque<Runnable>()
        private val progressPublisher: AndroidServiceProgressPublisher
        val playback =
            AndroidServicePlayback(
                createPlayer = {
                    ServiceNativePlayer(failNextPrepare)
                        .also {
                            failNextPrepare = false
                            players += it
                        }
                        .player
                },
                onCurrentPlayerChanged = ::currentPlayerChanged,
                onEvent = ::publishEvent,
            )
        val router = AndroidServiceCommandRouter(playback, serviceUid = 42)

        init {
            progressPublisher =
                AndroidServiceProgressPublisher(
                    playback,
                    postDelayed = { scheduledProgress.addLast(it) },
                    removeCallbacks = { task ->
                        scheduledProgress.removeAll { it === task }
                    },
                )
        }

        private fun currentPlayerChanged(player: Player) {
            facade = SkipRoutingPlayer(player, playback = playbackReference())
            progressPublisher.restart()
        }

        private fun publishEvent(event: AndroidServicePlaybackEvent) {
            events += event
            progressPublisher.onPlaybackEvent(event)
            forwardEvent?.invoke(event)
        }

        fun runNextProgressTick(): Boolean {
            val task = scheduledProgress.removeFirstOrNull() ?: return false
            task.run()
            return true
        }

        fun deliverQueuedEvent(event: AndroidServicePlaybackEvent) =
            publishEvent(event)

        private fun playbackReference(): AndroidServicePlayback = playback

        fun item(generation: Long, title: String): MediaItem =
            MediaItem.Builder()
                .setMediaId("$generation:1")
                .setMediaMetadata(
                    MediaMetadata.Builder().setTitle(title).build())
                .build()

        fun command(command: AndroidServiceEngineCommand) =
            router.dispatch(
                controllerUid = 42, engineHint = true, command = command)

        fun engine(
            executor: AndroidControllerExecutor = AndroidControllerExecutor {
                it()
            },
            sender:
                (AndroidServiceEngineCommand) -> ListenableFuture<
                        AndroidServiceEngineResult> =
                ::command,
        ): AndroidPlaybackEngine =
            AndroidPlaybackEngine(
                    controllerExecutor = executor,
                    sendServiceCommand = sender,
                    mediaItemFactory = { track, token ->
                        MediaItem.Builder()
                            .setMediaId(token.encode())
                            .setMediaMetadata(
                                buildAndroidPlaybackMediaMetadata(track))
                            .build()
                    },
                )
                .also { forwardEvent = it::serviceEvent }

        fun load(generation: Long, title: String): ServiceNativePlayer {
            val ready =
                command(
                    AndroidServiceEngineCommand.LoadPaused(
                        item(generation, title)))
            val player = players.last()
            player.ready(12_000L)
            assertEquals(12_000L, ready.get().durationMillis)
            events.clear()
            return player
        }
    }

    /**
     * Only the native decoder is replaced; admission, voice lifecycle and
     * facade are production code.
     */
    private class ServiceNativePlayer(private val failPrepare: Boolean) {
        val listeners = mutableListOf<Player.Listener>()
        val gainsAtPlay = mutableListOf<Float>()
        var volume = 1f
        var playWhenReady = false
        var released = false
        var position = 0L
        var duration = 0L
        var item: MediaItem? = null
        var failPlay = false
        var confirmPlaying = true
        val player: Player =
            Proxy.newProxyInstance(
                Player::class.java.classLoader, arrayOf(Player::class.java)) {
                    _,
                    method,
                    arguments ->
                    when (method.name) {
                        "addListener" -> {
                            listeners += arguments!!.single() as Player.Listener
                            null
                        }
                        "removeListener" -> {
                            listeners -= arguments!!.single() as Player.Listener
                            null
                        }
                        "setMediaItem" -> {
                            item = arguments!!.first() as MediaItem
                            null
                        }
                        "getCurrentMediaItem" -> item
                        "getDuration" -> duration
                        "getCurrentPosition" -> position
                        "getPlayWhenReady",
                        "isPlaying" -> playWhenReady
                        "setVolume" -> {
                            volume = arguments!!.single() as Float
                            null
                        }
                        "getVolume" -> volume
                        "prepare" -> {
                            if (failPrepare) error("decoder preparation failed")
                            null
                        }
                        "play" -> {
                            check(!failPlay) { "decoder start failed" }
                            gainsAtPlay += volume
                            playWhenReady = true
                            if (confirmPlaying)
                                listeners.toList().forEach {
                                    it.onIsPlayingChanged(true)
                                }
                            null
                        }
                        "pause",
                        "stop" -> {
                            playWhenReady = false
                            null
                        }
                        "seekTo" -> {
                            position = arguments!!.last() as Long
                            null
                        }
                        "release" -> {
                            released = true
                            playWhenReady = false
                            null
                        }
                        else ->
                            error("Unexpected Player method: ${method.name}")
                    }
                } as Player

        fun ready(durationMillis: Long) {
            duration = durationMillis
            listeners.toList().forEach {
                it.onPlaybackStateChanged(Player.STATE_READY)
            }
        }
    }

    /**
     * The explicit timestamp avoids Android's clock while preserving Media3's
     * native error type.
     */
    private class HostPlaybackException(message: String) :
        PlaybackException(
            message,
            null,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            Bundle.EMPTY,
            0L,
        )

    private fun AndroidServicePlaybackEvent.statusLabel(): String? =
        status?.let {
            "status:${token.generation}:$it"
        }

    private fun serviceTrack(title: String): PlayableTrack =
        PlayableTrack(
            id = title,
            title = title,
            artist = "Rhyth Haus",
            album = null,
            durationMillis = 12_000L,
            source = AudioSource.Uri("content://media/external/audio/media/42"),
        )

    private class RecordingAndroidControllerExecutor :
        AndroidControllerExecutor {
        private val pending = ArrayDeque<() -> Unit>()
        val pendingCount: Int
            get() = pending.size

        override fun execute(action: () -> Unit) {
            pending += action
        }

        fun runAll() {
            while (pending.isNotEmpty()) pending.removeFirst().invoke()
        }
    }

    private class RecordingAndroidPlaybackListener : PlaybackEngineListener {
        val events = mutableListOf<String>()
        val failures = mutableListOf<Pair<Long, PlaybackError>>()
        val progress = mutableListOf<Triple<Long, Long, Long?>>()

        override fun onPlaybackStatus(
            generation: Long,
            status: PlaybackStatus,
        ) {
            events += "status:$generation:$status"
        }

        override fun onPlaybackProgress(
            generation: Long,
            positionMillis: Long,
            durationMillis: Long?,
        ) {
            events += "progress:$generation"
            progress += Triple(generation, positionMillis, durationMillis)
        }

        override fun onPlaybackCompleted(
            generation: Long,
            playbackRunId: Long
        ) {
            events += "completed:$generation"
        }

        override fun onPlaybackError(
            generation: Long,
            error: PlaybackError,
        ) {
            events += "error:$generation"
            failures += generation to error
        }

        override fun onSkipToNext(generation: Long) = Unit

        override fun onSkipToPrevious(generation: Long) = Unit

        override fun onPlayRequested(generation: Long) = Unit

        override fun onPauseRequested(generation: Long) = Unit

        override fun onStopRequested(generation: Long) = Unit

        override fun onSeekRequested(generation: Long, positionMillis: Long) =
            Unit
    }
}
