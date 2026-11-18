package com.eterocell.rhythhaus.nowplaying.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.theme.HausColors
import kotlin.math.max
import org.jetbrains.compose.resources.stringResource
import rhythhaus.feature.nowplaying.generated.resources.Res
import rhythhaus.feature.nowplaying.generated.resources.playback_position
import rhythhaus.feature.nowplaying.generated.resources.playback_position_value_format
import top.yukonga.miuix.kmp.basic.Text

internal fun formatMillis(totalMillis: Long?): String {
    if (totalMillis == null) return "--:--"
    val totalSeconds = max(0L, totalMillis / 1_000L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

internal fun scrubberFractionForOffset(offsetX: Float, widthPx: Float): Float {
    if (widthPx <= 0f) return 0f
    return (offsetX / widthPx).coerceIn(0f, 1f)
}

internal fun scrubberPositionForFraction(
    fraction: Float,
    durationMillis: Long
): Long {
    if (durationMillis <= 0L) return 0L
    return (durationMillis * fraction.coerceIn(0f, 1f))
        .toLong()
        .coerceIn(0L, durationMillis)
}

internal const val MusicScrubberKeyboardStepMillis: Long = 5_000L

/**
 * Target position for one keyboard adjustment, or `null` when the duration is
 * unknown and seeking is not actionable.
 */
internal fun scrubberSeekTargetForDelta(
    positionMillis: Long,
    durationMillis: Long,
    deltaMillis: Long,
): Long? {
    if (durationMillis <= 0L) return null
    val current = positionMillis.coerceIn(0L, durationMillis)
    return (current + deltaMillis).coerceIn(0L, durationMillis)
}

/**
 * Applies one key press to the scrubber and reports whether the event was
 * handled. Seeking happens at most once, and only while the duration is known.
 */
internal fun handleScrubberKeyEvent(
    event: KeyEvent,
    positionMillis: Long,
    durationMillis: Long,
    onSeek: (Long) -> Unit,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val delta =
        when (event.key) {
            Key.DirectionLeft -> -MusicScrubberKeyboardStepMillis
            Key.DirectionRight -> MusicScrubberKeyboardStepMillis
            else -> return false
        }
    if (durationMillis <= 0L) return false
    val current = positionMillis.coerceIn(0L, durationMillis)
    val target = scrubberSeekTargetForDelta(current, durationMillis, delta)
    if (target != null && target != current) onSeek(target)
    return true
}

internal interface ScrubFractionState {
    var value: Float?
}

private class PlainScrubFractionState : ScrubFractionState {
    override var value: Float? = null
}

@Composable
private fun rememberComposeScrubFractionState(): ScrubFractionState = remember {
    object : ScrubFractionState {
        override var value: Float? by mutableStateOf(null)
    }
}

internal class MusicScrubInteractionState(
    positionMillis: Long,
    durationMillis: Long,
    private val scrubFractionState: ScrubFractionState =
        PlainScrubFractionState(),
) {
    private var playbackPositionMillis: Long = positionMillis.coerceAtLeast(0L)
    private var playbackDurationMillis: Long = durationMillis.coerceAtLeast(0L)
    private var scrubFraction: Float?
        get() = scrubFractionState.value
        set(value) {
            scrubFractionState.value = value
        }

    val displayPositionMillis: Long
        get() =
            scrubFraction?.let {
                scrubberPositionForFraction(it, playbackDurationMillis)
            }
                ?: playbackPositionMillis.coerceIn(
                    0L,
                    playbackDurationMillis.takeIf { it > 0L } ?: Long.MAX_VALUE)

    val displayFraction: Float
        get() {
            val duration = playbackDurationMillis
            if (duration <= 0L) return 0f
            return scrubFraction
                ?: (playbackPositionMillis.coerceIn(0L, duration).toFloat() /
                        duration.toFloat())
                    .coerceIn(0f, 1f)
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
        val fraction = scrubFraction
        scrubFraction = null
        if (playbackDurationMillis <= 0L) return null
        return fraction?.let {
            scrubberPositionForFraction(it, playbackDurationMillis)
        }
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
    val scrubFractionState = rememberComposeScrubFractionState()
    val interactionState = remember {
        MusicScrubInteractionState(
            positionMillis, durationMillis, scrubFractionState)
    }
    interactionState.updatePlaybackPosition(positionMillis, durationMillis)

    var widthPx by remember { mutableFloatStateOf(0f) }
    val displayFraction = interactionState.displayFraction
    val displayPositionMillis = interactionState.displayPositionMillis
    val density = LocalDensity.current
    val thumbOffset =
        with(density) { (widthPx * displayFraction).toDp() - 7.dp }
    val progressLabel = stringResource(Res.string.playback_position)
    val progressValue =
        stringResource(
            Res.string.playback_position_value_format,
            formatMillis(displayPositionMillis),
            formatMillis(durationMillis))
    val focusRequester = remember { FocusRequester() }
    // Playback ticks recompose the caller with a fresh lambda; keep the active
    // drag alive by reading the latest callback instead of keying on it.
    val currentOnSeek by rememberUpdatedState(onSeek)

    fun commitSeek(targetMillis: Long) {
        if (durationMillis <= 0L) return
        interactionState.cancelScrub()
        currentOnSeek(targetMillis.coerceIn(0L, durationMillis))
    }

    Column(
        modifier =
            modifier
                .onKeyEvent { event ->
                    handleScrubberKeyEvent(
                        event,
                        displayPositionMillis,
                        durationMillis,
                        ::commitSeek,
                    )
                }
                .focusRequester(focusRequester)
                .focusable()
                .semantics {
                    contentDescription = progressLabel
                    stateDescription = progressValue
                    if (durationMillis > 0L) {
                        progressBarRangeInfo =
                            ProgressBarRangeInfo(
                                current = displayFraction,
                                range = 0f..1f,
                            )
                        setProgress { fraction ->
                            commitSeek(
                                scrubberPositionForFraction(
                                    fraction, durationMillis))
                            true
                        }
                    }
                },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(44.dp)
                    .onSizeChanged { widthPx = it.width.toFloat() }
                    .pointerInput(durationMillis) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            focusRequester.requestFocus()
                            fun fractionFor(x: Float) =
                                scrubberFractionForOffset(x, widthPx)
                            fun finishIfReleased(
                                activePointerId: PointerId
                            ): Boolean {
                                val change =
                                    currentEvent.changes.firstOrNull {
                                        it.id == activePointerId
                                    }
                                return change?.changedToUpIgnoreConsumed() ==
                                    true
                            }

                            interactionState.startScrub(
                                fractionFor(down.position.x))
                            val activePointerId = down.id
                            var shouldSeekOnRelease = false

                            try {
                                var released = finishIfReleased(activePointerId)
                                while (!released) {
                                    val event = awaitPointerEvent()
                                    val change =
                                        event.changes.firstOrNull {
                                            it.id == activePointerId
                                        }
                                    if (change == null || change.isConsumed) {
                                        return@awaitEachGesture
                                    }

                                    if (change.pressed) {
                                        if (change.positionChanged() ||
                                            change.positionChange().x != 0f) {
                                            interactionState.updateScrub(
                                                fractionFor(change.position.x))
                                        }
                                        change.consume()
                                    } else {
                                        released = true
                                    }
                                }
                                shouldSeekOnRelease = true
                            } finally {
                                if (shouldSeekOnRelease) {
                                    interactionState
                                        .finishScrub()
                                        ?.let(currentOnSeek)
                                } else {
                                    interactionState.cancelScrub()
                                }
                            }
                        }
                    },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(HausColors.current.divider),
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth(displayFraction)
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(HausColors.current.primaryAction),
            )
            Box(
                modifier =
                    Modifier.offset(x = thumbOffset.coerceAtLeast(0.dp))
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(HausColors.current.primaryAction)
                        .border(
                            width = 2.dp,
                            color = HausColors.current.background,
                            shape = CircleShape),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatMillis(displayPositionMillis),
                color = HausColors.current.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = formatMillis(durationMillis),
                color = HausColors.current.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
