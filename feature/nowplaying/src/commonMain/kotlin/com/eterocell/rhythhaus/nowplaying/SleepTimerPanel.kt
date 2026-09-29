package com.eterocell.rhythhaus.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.SleepTimerMode
import com.eterocell.rhythhaus.SleepTimerState
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.hausClickable
import org.jetbrains.compose.resources.stringResource
import rhythhaus.feature.nowplaying.generated.resources.Res
import rhythhaus.feature.nowplaying.generated.resources.sleep_timer_15_minutes
import rhythhaus.feature.nowplaying.generated.resources.sleep_timer_30_minutes
import rhythhaus.feature.nowplaying.generated.resources.sleep_timer_3_tracks
import rhythhaus.feature.nowplaying.generated.resources.sleep_timer_45_minutes
import rhythhaus.feature.nowplaying.generated.resources.sleep_timer_5_tracks
import rhythhaus.feature.nowplaying.generated.resources.sleep_timer_60_minutes
import rhythhaus.feature.nowplaying.generated.resources.sleep_timer_cancel
import rhythhaus.feature.nowplaying.generated.resources.sleep_timer_current_track
import rhythhaus.feature.nowplaying.generated.resources.sleep_timer_fade
import rhythhaus.feature.nowplaying.generated.resources.sleep_timer_fade_format
import rhythhaus.feature.nowplaying.generated.resources.sleep_timer_fade_off
import rhythhaus.feature.nowplaying.generated.resources.sleep_timer_fade_on
import rhythhaus.feature.nowplaying.generated.resources.sleep_timer_stop_after
import rhythhaus.feature.nowplaying.generated.resources.sleep_timer_stop_in
import rhythhaus.feature.nowplaying.generated.resources.sleep_timer_timed_summary_minutes
import rhythhaus.feature.nowplaying.generated.resources.sleep_timer_timed_summary_seconds
import rhythhaus.feature.nowplaying.generated.resources.sleep_timer_title
import rhythhaus.feature.nowplaying.generated.resources.sleep_timer_tracks_summary
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text

internal const val NowPlayingSleepTimerPanelTestTag =
    "NowPlayingSleepTimerPanel"
internal const val NowPlayingSleepTimerFadeTestTag = "NowPlayingSleepTimerFade"
internal const val NowPlayingSleepTimer15MinutesTestTag =
    "NowPlayingSleepTimer15Minutes"
internal const val NowPlayingSleepTimer30MinutesTestTag =
    "NowPlayingSleepTimer30Minutes"
internal const val NowPlayingSleepTimer45MinutesTestTag =
    "NowPlayingSleepTimer45Minutes"
internal const val NowPlayingSleepTimer60MinutesTestTag =
    "NowPlayingSleepTimer60Minutes"
internal const val NowPlayingSleepTimerCurrentTrackTestTag =
    "NowPlayingSleepTimerCurrentTrack"
internal const val NowPlayingSleepTimer3TracksTestTag =
    "NowPlayingSleepTimer3Tracks"
internal const val NowPlayingSleepTimer5TracksTestTag =
    "NowPlayingSleepTimer5Tracks"
internal const val NowPlayingSleepTimerActiveSummaryTestTag =
    "NowPlayingSleepTimerActiveSummary"
internal const val NowPlayingSleepTimerCancelTestTag =
    "NowPlayingSleepTimerCancel"

/**
 * Listener controls for the process-local sleep timer. The timer remains
 * controller-owned; this panel only presents the immutable projection and
 * dispatches narrow arm/cancel requests.
 */
@Composable
internal fun SleepTimerPanel(
    sleepTimerState: SleepTimerState,
    onArmSleepTimer: (minutes: Int, fadeEnabled: Boolean) -> Unit,
    onArmSleepTimerAfterCompletions: (count: Int, fadeEnabled: Boolean) -> Unit,
    onCancelSleepTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var nextTimerFadeEnabled by remember {
        mutableStateOf(sleepTimerState.fadeEnabled)
    }
    LaunchedEffect(sleepTimerState.mode, sleepTimerState.fadeEnabled) {
        if (sleepTimerState.mode != null) {
            nextTimerFadeEnabled = sleepTimerState.fadeEnabled
        }
    }
    val fadeOn = stringResource(Res.string.sleep_timer_fade_on)
    val fadeOff = stringResource(Res.string.sleep_timer_fade_off)
    val configuredFadeLabel = if (nextTimerFadeEnabled) fadeOn else fadeOff
    val activeSummary =
        sleepTimerActiveSummary(sleepTimerState, fadeOn, fadeOff)

    Card(
        modifier =
            modifier.fillMaxWidth().testTag(NowPlayingSleepTimerPanelTestTag),
        cornerRadius = 20.dp,
        colors = CardDefaults.defaultColors(color = HausColors.current.panel),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(Res.string.sleep_timer_title),
                color = HausColors.current.ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
            )
            if (sleepTimerState.mode != null) {
                activeSummary?.let { summary ->
                    Text(
                        summary,
                        modifier =
                            Modifier.testTag(
                                    NowPlayingSleepTimerActiveSummaryTestTag)
                                .semantics { contentDescription = summary },
                        color = HausColors.current.muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp,
                    )
                }
                SleepTimerActionButton(
                    label = stringResource(Res.string.sleep_timer_cancel),
                    onClick = onCancelSleepTimer,
                    modifier =
                        Modifier.testTag(NowPlayingSleepTimerCancelTestTag),
                    destructive = true,
                )
            }
            SleepTimerFadeToggle(
                label = stringResource(Res.string.sleep_timer_fade),
                stateLabel = configuredFadeLabel,
                fadeEnabled = nextTimerFadeEnabled,
                interactive = true,
                onClick = { nextTimerFadeEnabled = !nextTimerFadeEnabled },
            )
            Text(
                stringResource(Res.string.sleep_timer_stop_in),
                color = HausColors.current.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SleepTimerActionButton(
                    label = stringResource(Res.string.sleep_timer_15_minutes),
                    onClick = { onArmSleepTimer(15, nextTimerFadeEnabled) },
                    modifier =
                        Modifier.weight(1f)
                            .testTag(NowPlayingSleepTimer15MinutesTestTag),
                )
                SleepTimerActionButton(
                    label = stringResource(Res.string.sleep_timer_30_minutes),
                    onClick = { onArmSleepTimer(30, nextTimerFadeEnabled) },
                    modifier =
                        Modifier.weight(1f)
                            .testTag(NowPlayingSleepTimer30MinutesTestTag),
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SleepTimerActionButton(
                    label = stringResource(Res.string.sleep_timer_45_minutes),
                    onClick = { onArmSleepTimer(45, nextTimerFadeEnabled) },
                    modifier =
                        Modifier.weight(1f)
                            .testTag(NowPlayingSleepTimer45MinutesTestTag),
                )
                SleepTimerActionButton(
                    label = stringResource(Res.string.sleep_timer_60_minutes),
                    onClick = { onArmSleepTimer(60, nextTimerFadeEnabled) },
                    modifier =
                        Modifier.weight(1f)
                            .testTag(NowPlayingSleepTimer60MinutesTestTag),
                )
            }
            Text(
                stringResource(Res.string.sleep_timer_stop_after),
                color = HausColors.current.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SleepTimerActionButton(
                    label =
                        stringResource(Res.string.sleep_timer_current_track),
                    onClick = {
                        onArmSleepTimerAfterCompletions(1, nextTimerFadeEnabled)
                    },
                    modifier =
                        Modifier.weight(1f)
                            .testTag(NowPlayingSleepTimerCurrentTrackTestTag),
                    maxLines = 2,
                )
                SleepTimerActionButton(
                    label = stringResource(Res.string.sleep_timer_3_tracks),
                    onClick = {
                        onArmSleepTimerAfterCompletions(3, nextTimerFadeEnabled)
                    },
                    modifier =
                        Modifier.weight(1f)
                            .testTag(NowPlayingSleepTimer3TracksTestTag),
                    maxLines = 2,
                )
                SleepTimerActionButton(
                    label = stringResource(Res.string.sleep_timer_5_tracks),
                    onClick = {
                        onArmSleepTimerAfterCompletions(5, nextTimerFadeEnabled)
                    },
                    modifier =
                        Modifier.weight(1f)
                            .testTag(NowPlayingSleepTimer5TracksTestTag),
                    maxLines = 2,
                )
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun SleepTimerFadeToggle(
    label: String,
    stateLabel: String,
    fadeEnabled: Boolean,
    interactive: Boolean,
    onClick: () -> Unit,
) {
    val interactionModifier =
        if (interactive) Modifier.hausClickable(onClick) else Modifier
    Box(
        Modifier.fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (fadeEnabled) HausColors.current.pulse
                else HausColors.current.paper,
            )
            .testTag(NowPlayingSleepTimerFadeTestTag)
            .semantics {
                contentDescription = label
                stateDescription = stateLabel
                toggleableState =
                    if (fadeEnabled) ToggleableState.On else ToggleableState.Off
            }
            .then(interactionModifier)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(
                Res.string.sleep_timer_fade_format, label, stateLabel),
            color =
                if (fadeEnabled) HausColors.current.paper
                else HausColors.current.ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SleepTimerActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    maxLines: Int = 1,
) {
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (destructive) HausColors.current.pulse.copy(alpha = 0.15f)
                else HausColors.current.paper,
            )
            .semantics { contentDescription = label }
            .hausClickable(onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color =
                if (destructive) HausColors.current.pulse
                else HausColors.current.ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun sleepTimerActiveSummary(
    state: SleepTimerState,
    fadeOn: String,
    fadeOff: String,
): String? {
    val fadeLabel = if (state.fadeEnabled) fadeOn else fadeOff
    return when (state.mode) {
        SleepTimerMode.Timed -> {
            val remainingMillis = state.remainingMillis ?: return null
            if (remainingMillis > 60_000L) {
                stringResource(
                    Res.string.sleep_timer_timed_summary_minutes,
                    remainingMillis.roundedUpBy(60_000L),
                    fadeLabel,
                )
            } else {
                stringResource(
                    Res.string.sleep_timer_timed_summary_seconds,
                    remainingMillis.roundedUpBy(1_000L),
                    fadeLabel,
                )
            }
        }

        SleepTimerMode.TrackCount -> {
            val remainingTracks = state.remainingTracks ?: return null
            stringResource(
                Res.string.sleep_timer_tracks_summary,
                remainingTracks.coerceAtLeast(0),
                fadeLabel,
            )
        }

        null -> null
    }
}

private fun Long.roundedUpBy(divisor: Long): Long =
    if (this <= 0L) 0L else ((this - 1L) / divisor) + 1L
