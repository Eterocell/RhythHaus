package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MusicProgressScrubberTest {
    @Test
    fun offsetMappingClampsToTrackBounds() {
        assertEquals(0f, scrubberFractionForOffset(offsetX = -10f, widthPx = 200f))
        assertEquals(0.25f, scrubberFractionForOffset(offsetX = 50f, widthPx = 200f))
        assertEquals(1f, scrubberFractionForOffset(offsetX = 240f, widthPx = 200f))
        assertEquals(0f, scrubberFractionForOffset(offsetX = 10f, widthPx = 0f))
    }

    @Test
    fun fractionMappingClampsToDurationBounds() {
        assertEquals(0L, scrubberPositionForFraction(fraction = -0.25f, durationMillis = 240_000L))
        assertEquals(60_000L, scrubberPositionForFraction(fraction = 0.25f, durationMillis = 240_000L))
        assertEquals(240_000L, scrubberPositionForFraction(fraction = 1.25f, durationMillis = 240_000L))
        assertEquals(0L, scrubberPositionForFraction(fraction = 0.5f, durationMillis = 0L))
    }

    @Test
    fun scrubbingPreviewIgnoresPlaybackProgressUntilRelease() {
        val state = MusicScrubInteractionState(positionMillis = 10_000L, durationMillis = 100_000L)

        state.startScrub(0.4f)
        state.updatePlaybackPosition(positionMillis = 12_000L, durationMillis = 100_000L)

        assertEquals(40_000L, state.displayPositionMillis)
        assertEquals(0.4f, state.displayFraction)
        assertEquals(40_000L, state.finishScrub())
    }

    @Test
    fun dragReleaseProducesOneFinalSeekTargetAndThenReturnsToPlaybackState() {
        val state = MusicScrubInteractionState(positionMillis = 10_000L, durationMillis = 100_000L)

        state.startScrub(0.2f)
        state.updateScrub(0.7f)
        val target = state.finishScrub()
        state.updatePlaybackPosition(positionMillis = target ?: -1L, durationMillis = 100_000L)

        assertEquals(70_000L, target)
        assertEquals(70_000L, state.displayPositionMillis)
        assertEquals(0.7f, state.displayFraction)
    }

    @Test
    fun canceledScrubDoesNotProduceSeekTarget() {
        val state = MusicScrubInteractionState(positionMillis = 10_000L, durationMillis = 100_000L)

        state.startScrub(0.8f)
        state.cancelScrub()

        assertNull(state.finishScrub())
        assertEquals(10_000L, state.displayPositionMillis)
        assertEquals(0.1f, state.displayFraction)
    }

    @Test
    fun tapAndDragContractsEmitOnlyCommittedSeekTargets() {
        val duration = 200_000L
        val tapTarget = scrubberPositionForFraction(
            scrubberFractionForOffset(offsetX = 50f, widthPx = 200f),
            duration,
        )
        assertEquals(50_000L, tapTarget)

        val state = MusicScrubInteractionState(positionMillis = 0L, durationMillis = duration)
        val emittedTargets = mutableListOf<Long>()

        state.startScrub(scrubberFractionForOffset(offsetX = 20f, widthPx = 200f))
        state.updateScrub(scrubberFractionForOffset(offsetX = 80f, widthPx = 200f))
        state.updateScrub(scrubberFractionForOffset(offsetX = 160f, widthPx = 200f))
        state.finishScrub()?.let(emittedTargets::add)

        assertEquals(listOf(160_000L), emittedTargets)
    }
}
