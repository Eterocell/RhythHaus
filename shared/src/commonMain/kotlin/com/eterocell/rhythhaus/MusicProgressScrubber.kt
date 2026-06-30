package com.eterocell.rhythhaus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text

internal fun scrubberFractionForOffset(offsetX: Float, widthPx: Float): Float {
    if (widthPx <= 0f) return 0f
    return (offsetX / widthPx).coerceIn(0f, 1f)
}

internal fun scrubberPositionForFraction(fraction: Float, durationMillis: Long): Long {
    if (durationMillis <= 0L) return 0L
    return (durationMillis * fraction.coerceIn(0f, 1f)).toLong().coerceIn(0L, durationMillis)
}

internal class MusicScrubInteractionState(
    positionMillis: Long,
    durationMillis: Long,
) {
    private var playbackPositionMillis: Long = positionMillis.coerceAtLeast(0L)
    private var playbackDurationMillis: Long = durationMillis.coerceAtLeast(0L)
    private var scrubFraction: Float? = null

    val displayPositionMillis: Long
        get() = scrubFraction?.let { scrubberPositionForFraction(it, playbackDurationMillis) }
            ?: playbackPositionMillis.coerceIn(0L, playbackDurationMillis.takeIf { it > 0L } ?: Long.MAX_VALUE)

    val displayFraction: Float
        get() {
            val duration = playbackDurationMillis
            if (duration <= 0L) return 0f
            return scrubFraction ?: (playbackPositionMillis.coerceIn(0L, duration).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        }

    fun updatePlaybackPosition(positionMillis: Long, durationMillis: Long) {
        playbackPositionMillis = positionMillis.coerceAtLeast(0L)
        playbackDurationMillis = durationMillis.coerceAtLeast(0L)
    }

    fun startScrub(fraction: Float) {
        scrubFraction = fraction.coerceIn(0f, 1f)
    }

    fun updateScrub(fraction: Float) {
        scrubFraction = fraction.coerceIn(0f, 1f)
    }

    fun finishScrub(): Long? {
        val target = scrubFraction?.let { scrubberPositionForFraction(it, playbackDurationMillis) }
        scrubFraction = null
        return target
    }

    fun cancelScrub() {
        scrubFraction = null
    }
}

@Composable
internal fun MusicProgressScrubber(
    positionMillis: Long,
    durationMillis: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionState = remember { MusicScrubInteractionState(positionMillis, durationMillis) }
    interactionState.updatePlaybackPosition(positionMillis, durationMillis)

    var widthPx by remember { mutableFloatStateOf(0f) }
    val displayFraction = interactionState.displayFraction
    val displayPositionMillis = interactionState.displayPositionMillis
    val density = LocalDensity.current
    val thumbOffset = with(density) { (widthPx * displayFraction).toDp() - 7.dp }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .onSizeChanged { widthPx = it.width.toFloat() }
                .pointerInput(durationMillis, onSeek) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        fun fractionFor(x: Float) = scrubberFractionForOffset(x, widthPx)
                        fun finishIfReleased(activePointerId: PointerId): Boolean {
                            val change = currentEvent.changes.firstOrNull { it.id == activePointerId }
                            return change?.changedToUpIgnoreConsumed() == true
                        }

                        interactionState.startScrub(fractionFor(down.position.x))
                        var activePointerId = down.id
                        var canceled = false
                        var released = finishIfReleased(activePointerId)

                        while (!released) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == activePointerId }
                            if (change == null) {
                                canceled = true
                                break
                            }

                            if (change.pressed) {
                                if (change.positionChanged() || change.positionChange().x != 0f) {
                                    interactionState.updateScrub(fractionFor(change.position.x))
                                }
                                change.consume()
                            } else {
                                released = true
                            }
                        }

                        if (canceled) {
                            interactionState.cancelScrub()
                        } else {
                            interactionState.finishScrub()?.let(onSeek)
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(HausLine),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(displayFraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(HausPulse),
            )
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset.coerceAtLeast(0.dp))
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(HausPulse)
                    .border(width = 2.dp, color = HausPaper, shape = CircleShape),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatMillis(displayPositionMillis),
                color = HausMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = formatMillis(durationMillis),
                color = HausMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
