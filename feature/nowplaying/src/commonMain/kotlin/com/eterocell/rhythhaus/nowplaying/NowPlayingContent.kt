package com.eterocell.rhythhaus.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Filter1
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.LyricLine
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackFailureKind
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.PlaybackStatus
import com.eterocell.rhythhaus.RepeatMode
import com.eterocell.rhythhaus.ShuffleMode
import com.eterocell.rhythhaus.SleepTimerState
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.nowplaying.ui.MusicProgressScrubber
import com.eterocell.rhythhaus.parseLyrics
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.ArtworkImage
import com.eterocell.rhythhaus.ui.ArtworkImageRole
import com.eterocell.rhythhaus.ui.BackChip
import com.eterocell.rhythhaus.ui.hausClickable
import com.eterocell.rhythhaus.ui.leftEdgeSwipeBack
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import rhythhaus.feature.nowplaying.generated.resources.Res
import rhythhaus.feature.nowplaying.generated.resources.favorite_action
import rhythhaus.feature.nowplaying.generated.resources.favorite_selected
import rhythhaus.feature.nowplaying.generated.resources.favorite_unselected
import rhythhaus.feature.nowplaying.generated.resources.lyrics_action
import rhythhaus.feature.nowplaying.generated.resources.lyrics_close
import rhythhaus.feature.nowplaying.generated.resources.lyrics_current_line
import rhythhaus.feature.nowplaying.generated.resources.lyrics_title
import rhythhaus.feature.nowplaying.generated.resources.next_track
import rhythhaus.feature.nowplaying.generated.resources.panel_queue_action
import rhythhaus.feature.nowplaying.generated.resources.panel_timer_action
import rhythhaus.feature.nowplaying.generated.resources.playback_error_access_lost
import rhythhaus.feature.nowplaying.generated.resources.playback_error_decoder_failure
import rhythhaus.feature.nowplaying.generated.resources.playback_error_missing_file
import rhythhaus.feature.nowplaying.generated.resources.playback_error_unknown
import rhythhaus.feature.nowplaying.generated.resources.playback_error_unsupported_format
import rhythhaus.feature.nowplaying.generated.resources.playback_status_buffering
import rhythhaus.feature.nowplaying.generated.resources.playback_status_error
import rhythhaus.feature.nowplaying.generated.resources.playback_status_loading
import rhythhaus.feature.nowplaying.generated.resources.playback_status_paused
import rhythhaus.feature.nowplaying.generated.resources.playback_status_playing
import rhythhaus.feature.nowplaying.generated.resources.playback_status_ready
import rhythhaus.feature.nowplaying.generated.resources.playback_status_stopped
import rhythhaus.feature.nowplaying.generated.resources.previous_track
import rhythhaus.feature.nowplaying.generated.resources.recovery_remove_from_queue
import rhythhaus.feature.nowplaying.generated.resources.recovery_retry
import rhythhaus.feature.nowplaying.generated.resources.recovery_skip
import rhythhaus.feature.nowplaying.generated.resources.repeat_mode_repeat_one
import rhythhaus.feature.nowplaying.generated.resources.repeat_mode_repeat_playlist
import rhythhaus.feature.nowplaying.generated.resources.repeat_mode_stop_after_current
import rhythhaus.feature.nowplaying.generated.resources.repeat_mode_stop_after_queue
import rhythhaus.feature.nowplaying.generated.resources.shuffle_off
import rhythhaus.feature.nowplaying.generated.resources.shuffle_on
import rhythhaus.feature.nowplaying.generated.resources.sleep_timer_fade_off
import rhythhaus.feature.nowplaying.generated.resources.sleep_timer_fade_on
import rhythhaus.feature.nowplaying.generated.resources.sleep_timer_title
import rhythhaus.feature.nowplaying.generated.resources.track_number_format
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text

internal const val NowPlayingPreviousTestTag = "NowPlayingPrevious"
internal const val NowPlayingPlayPauseTestTag = "NowPlayingPlayPause"
internal const val NowPlayingNextTestTag = "NowPlayingNext"
internal const val NowPlayingShuffleTestTag = "NowPlayingShuffle"
internal const val NowPlayingRepeatTestTag = "NowPlayingRepeat"
internal const val NowPlayingContentRootTestTag = "NowPlayingContentRoot"
internal const val NowPlayingCompactLayoutTestTag = "NowPlayingCompactLayout"
internal const val NowPlayingSplitLayoutTestTag = "NowPlayingSplitLayout"
internal const val NowPlayingTrackNumberTestTag = "NowPlayingTrackNumber"
internal const val NowPlayingStatusTestTag = "NowPlayingStatus"
internal const val NowPlayingProgressTestTag = "NowPlayingProgress"
internal const val NowPlayingTitleTestTag = "NowPlayingTitle"
internal const val NowPlayingSubtitleTestTag = "NowPlayingSubtitle"
internal const val NowPlayingRetryFailureTestTag = "NowPlayingRetryFailure"
internal const val NowPlayingSkipFailureTestTag = "NowPlayingSkipFailure"
internal const val NowPlayingRemoveFailureTestTag = "NowPlayingRemoveFailure"
internal const val NowPlayingFavoriteTestTag = "NowPlayingFavorite"
internal const val NowPlayingLyricsButtonTestTag = "NowPlayingLyricsButton"
internal const val NowPlayingLyricsDialogTestTag = "NowPlayingLyricsDialog"
internal const val NowPlayingLyricsCloseTestTag = "NowPlayingLyricsClose"
internal const val NowPlayingLyricsListTestTag = "NowPlayingLyricsList"
internal const val NowPlayingLyricLineTestTag = "NowPlayingLyricLine"
internal const val NowPlayingTimerButtonTestTag = "NowPlayingTimerButton"
internal const val NowPlayingQueueButtonTestTag = "NowPlayingQueueButton"
internal const val NowPlayingPanelBackTestTag = "NowPlayingPanelBack"
internal const val NowPlayingTimerPanelTestTag = "NowPlayingTimerPanel"
internal const val NowPlayingQueuePanelTestTag = "NowPlayingQueuePanel"

/** Immutable shared-resolved labels used by [NowPlayingContent]. */
public data class NowPlayingScreenLabels(
    /** Localized play action. */
    public val play: String,
    /** Localized pause action. */
    public val pause: String,
    /** Localized artwork description. */
    public val albumArtwork: String,
    /** Localized current track subtitle. */
    public val currentTrackArtistAlbum: String,
)

private data class NowPlayingUiState(
    val durationMillis: Long,
    val positionMillis: Long,
    val statusText: String,
    val isPlaying: Boolean,
    val shuffleEnabled: Boolean,
    val repeatContentDescription: String,
    val shuffleContentDescription: String,
)

/**
 * Whether the error-only recovery section is active: the controller reports an
 * error with a structured cause for the current queue occurrence.
 */
private val PlaybackState.errorRecoveryVisible: Boolean
    get() =
        status == PlaybackStatus.Error &&
            error != null &&
            currentOccurrence != null

/**
 * Renders expanded Now Playing and sends the generic left-edge callback to
 * [onBack].
 */
@Composable
@OptIn(ExperimentalComposeUiApi::class)
public fun NowPlayingContent(
    track: Track,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    labels: NowPlayingScreenLabels,
    artworkLoader: suspend (String) -> ByteArray?,
    onBack: () -> Unit,
    sleepTimerState: SleepTimerState,
    onArmSleepTimer: (minutes: Int, fadeEnabled: Boolean) -> Unit,
    onArmSleepTimerAfterCompletions: (count: Int, fadeEnabled: Boolean) -> Unit,
    onCancelSleepTimer: () -> Unit,
    activePanel: NowPlayingPanel,
    onPanelChange: (NowPlayingPanel) -> Unit,
    queueContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    favoriteTrackIds: Set<String> = emptySet(),
    onSetTrackFavorite: (String, Boolean) -> Unit = { _, _ -> },
    isCurrentTrackAvailableInLibrary: Boolean = false,
): Unit {
    val guardedOnSetTrackFavorite: (String, Boolean) -> Unit = { id, value ->
        if (playbackController.state.value.currentTrack?.id == id) {
            onSetTrackFavorite(id, value)
        }
    }
    val brush =
        Brush.linearGradient(
            listOf(Color(track.accent.start), Color(track.accent.end)))
    val durationMillis =
        playbackState.durationMillis ?: track.durationSeconds * 1_000L
    val uiState =
        NowPlayingUiState(
            durationMillis = durationMillis,
            positionMillis =
                playbackState.positionMillis.coerceIn(0L, durationMillis),
            statusText =
                playbackState.error?.message
                    ?: statusLabel(playbackState.status),
            isPlaying = playbackState.isPlaying,
            shuffleEnabled = playbackState.shuffleMode == ShuffleMode.On,
            repeatContentDescription = repeatLabel(playbackState.repeatMode),
            shuffleContentDescription =
                if (playbackState.shuffleMode == ShuffleMode.On)
                    stringResource(Res.string.shuffle_on)
                else stringResource(Res.string.shuffle_off),
        )
    val lyrics =
        remember(track.lyrics) {
            track.lyrics?.let(::parseLyrics).orEmpty()
        }
    val lyricsText = track.lyrics
    LaunchedEffect(track.id, activePanel, lyricsText) {
        if (activePanel == NowPlayingPanel.Lyrics &&
            lyricsText.isNullOrBlank()) {
            onPanelChange(NowPlayingPanel.Controls)
        }
    }
    val visiblePanel =
        if (activePanel == NowPlayingPanel.Lyrics &&
            lyricsText.isNullOrBlank()) {
            NowPlayingPanel.Controls
        } else {
            activePanel
        }
    Surface(
        modifier =
            modifier
                .fillMaxSize()
                .testTag(NowPlayingContentRootTestTag)
                .leftEdgeSwipeBack(onBack),
        color = HausColors.current.paper) {
            BoxWithConstraints(
                Modifier.fillMaxSize()
                    .focusRestorer()
                    .focusGroup()
                    .safeContentPadding(),
            ) {
                val layoutMode =
                    nowPlayingAdaptiveLayoutModeFor(
                        maxWidth.value, maxHeight.value)
                val queueSidebarVisible =
                    visiblePanel == NowPlayingPanel.Queue &&
                        layoutMode == NowPlayingAdaptiveLayoutMode.Split &&
                        nowPlayingQueueSidebarFor(maxWidth.value)
                if ((visiblePanel == NowPlayingPanel.Controls &&
                    layoutMode == NowPlayingAdaptiveLayoutMode.Split) ||
                    queueSidebarVisible) {
                    val queueSidebar: (@Composable () -> Unit)? =
                        if (queueSidebarVisible) {
                            {
                                Box(Modifier.fillMaxSize()) {
                                    queueContent()
                                }
                            }
                        } else {
                            null
                        }
                    WideNowPlayingLayout(
                        track,
                        playbackState,
                        playbackController,
                        labels,
                        artworkLoader,
                        uiState,
                        brush,
                        favoriteTrackIds,
                        guardedOnSetTrackFavorite,
                        isCurrentTrackAvailableInLibrary,
                        sleepTimerState,
                        onOpenLyrics = {
                            onPanelChange(NowPlayingPanel.Lyrics)
                        },
                        onOpenTimer = {
                            onPanelChange(NowPlayingPanel.Timer)
                        },
                        onOpenQueue = {
                            onPanelChange(NowPlayingPanel.Queue)
                        },
                        queueContent = queueSidebar,
                    )
                } else {
                    when (visiblePanel) {
                        NowPlayingPanel.Controls ->
                            CompactNowPlayingLayout(
                                track,
                                playbackState,
                                playbackController,
                                labels,
                                artworkLoader,
                                uiState,
                                brush,
                                favoriteTrackIds,
                                guardedOnSetTrackFavorite,
                                isCurrentTrackAvailableInLibrary,
                                sleepTimerState,
                                onOpenLyrics = {
                                    onPanelChange(NowPlayingPanel.Lyrics)
                                },
                                onOpenTimer = {
                                    onPanelChange(NowPlayingPanel.Timer)
                                },
                                onOpenQueue = {
                                    onPanelChange(NowPlayingPanel.Queue)
                                },
                            )
                        NowPlayingPanel.Lyrics ->
                            LyricsPanel(
                                lines = lyrics,
                                rawLyrics = checkNotNull(lyricsText),
                                positionMillis = uiState.positionMillis,
                                onDismiss = {
                                    onPanelChange(NowPlayingPanel.Controls)
                                },
                            )
                        NowPlayingPanel.Timer ->
                            PlayerSubpanel(
                                title =
                                    stringResource(
                                        Res.string.sleep_timer_title),
                                onBack = {
                                    onPanelChange(NowPlayingPanel.Controls)
                                },
                                modifier =
                                    Modifier.testTag(
                                        NowPlayingTimerPanelTestTag),
                            ) {
                                SleepTimerPanel(
                                    sleepTimerState = sleepTimerState,
                                    onArmSleepTimer = onArmSleepTimer,
                                    onArmSleepTimerAfterCompletions =
                                        onArmSleepTimerAfterCompletions,
                                    onCancelSleepTimer = onCancelSleepTimer,
                                )
                            }
                        NowPlayingPanel.Queue ->
                            Box(
                                Modifier.fillMaxSize()
                                    .testTag(NowPlayingQueuePanelTestTag)) {
                                    queueContent()
                                }
                    }
                }
            }
        }
}

@Composable
private fun NowPlayingArtworkPane(
    track: Track,
    labels: NowPlayingScreenLabels,
    artworkLoader: suspend (String) -> ByteArray?,
    brush: Brush,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        cornerRadius = 32.dp,
        colors = CardDefaults.defaultColors(color = HausColors.current.ink)) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(1f).background(brush),
                contentAlignment = Alignment.Center) {
                    val artwork =
                        rememberNowPlayingArtwork(
                            track.id, track.artworkBytes, artworkLoader)
                    ArtworkImage(
                        artwork,
                        labels.albumArtwork,
                        ArtworkImageRole.Hero,
                        Modifier.fillMaxSize(),
                        ContentScale.Crop) {
                            Text(
                                track.title.take(3).uppercase(),
                                color = Color.White.copy(alpha = 0.48f),
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Black)
                        }
                }
        }
}

@Composable
private fun NowPlayingControlsPane(
    track: Track,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    labels: NowPlayingScreenLabels,
    uiState: NowPlayingUiState,
    modifier: Modifier = Modifier,
    favoriteTrackIds: Set<String>,
    onSetTrackFavorite: (String, Boolean) -> Unit,
    isCurrentTrackAvailableInLibrary: Boolean,
    sleepTimerState: SleepTimerState,
    onOpenLyrics: () -> Unit,
    onOpenTimer: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    val timerSummary =
        sleepTimerActiveSummary(
            sleepTimerState,
            stringResource(Res.string.sleep_timer_fade_on),
            stringResource(Res.string.sleep_timer_fade_off),
        )
    Column(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                track.title,
                modifier = Modifier.testTag(NowPlayingTitleTestTag),
                color = HausColors.current.ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            Text(
                labels.currentTrackArtistAlbum,
                modifier = Modifier.testTag(NowPlayingSubtitleTestTag),
                color = HausColors.current.muted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            track.trackNumber?.let {
                Text(
                    stringResource(Res.string.track_number_format, it),
                    modifier = Modifier.testTag(NowPlayingTrackNumberTestTag),
                    color = HausColors.current.muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!track.lyrics.isNullOrBlank()) {
                    PanelActionButton(
                        label = stringResource(Res.string.lyrics_action),
                        onClick = onOpenLyrics,
                        modifier =
                            Modifier.testTag(NowPlayingLyricsButtonTestTag),
                    )
                }
                PanelActionButton(
                    label = stringResource(Res.string.panel_timer_action),
                    onClick = onOpenTimer,
                    modifier =
                        Modifier.testTag(NowPlayingTimerButtonTestTag)
                            .semantics {
                                timerSummary?.let { stateDescription = it }
                            },
                )
                PanelActionButton(
                    label = stringResource(Res.string.panel_queue_action),
                    onClick = onOpenQueue,
                    modifier = Modifier.testTag(NowPlayingQueueButtonTestTag),
                )
            }
            timerSummary?.let { summary ->
                Text(
                    summary,
                    color = HausColors.current.muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (isCurrentTrackAvailableInLibrary) {
                val checked = track.id in favoriteTrackIds
                val actionLabel = stringResource(Res.string.favorite_action)
                val stateLabel =
                    if (checked) stringResource(Res.string.favorite_selected)
                    else stringResource(Res.string.favorite_unselected)
                Box(
                    Modifier.size(48.dp)
                        .testTag(NowPlayingFavoriteTestTag)
                        .semantics {
                            contentDescription = actionLabel
                            stateDescription = stateLabel
                            toggleableState =
                                if (checked) ToggleableState.On
                                else ToggleableState.Off
                        }
                        .hausClickable {
                            onSetTrackFavorite(track.id, !checked)
                        },
                    contentAlignment = Alignment.Center) {
                        Icon(
                            if (checked) Icons.Filled.Favorite
                            else Icons.Filled.FavoriteBorder,
                            null,
                            tint = HausColors.current.pulse,
                            modifier = Modifier.size(26.dp))
                    }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            uiState.statusText,
            modifier = Modifier.testTag(NowPlayingStatusTestTag),
            color = HausColors.current.muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1)
        if (playbackState.errorRecoveryVisible) {
            val failureError = playbackState.error!!
            Spacer(Modifier.height(10.dp))
            Text(
                recoveryFailureSummary(failureError.kind),
                color = HausColors.current.danger,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            val scope = rememberCoroutineScope()
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                    RecoveryActionButton(
                        stringResource(Res.string.recovery_retry),
                        playbackController::retryFailedTrack,
                        primary = true,
                        modifier =
                            Modifier.testTag(NowPlayingRetryFailureTestTag),
                    )
                    RecoveryActionButton(
                        stringResource(Res.string.recovery_skip),
                        playbackController::skipFailedTrack,
                        modifier =
                            Modifier.testTag(NowPlayingSkipFailureTestTag),
                    )
                    RecoveryActionButton(
                        stringResource(Res.string.recovery_remove_from_queue),
                        {
                            scope.launch {
                                playbackController.removeFailedTrack()
                            }
                        },
                        destructive = true,
                        modifier =
                            Modifier.testTag(NowPlayingRemoveFailureTestTag),
                    )
                }
        }
        Spacer(Modifier.height(12.dp))
        MusicProgressScrubber(
            uiState.positionMillis,
            uiState.durationMillis,
            playbackController::seekTo,
            Modifier.fillMaxWidth().testTag(NowPlayingProgressTestTag))
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
                PlaybackModeButton(
                    uiState.shuffleEnabled,
                    uiState.shuffleContentDescription,
                    playbackController::toggleShuffleMode,
                    Modifier.testTag(NowPlayingShuffleTestTag)) {
                        Icon(
                            Icons.Filled.Shuffle,
                            null,
                            tint =
                                if (uiState.shuffleEnabled)
                                    HausColors.current.onPulse
                                else HausColors.current.ink,
                            modifier = Modifier.size(22.dp))
                    }
                Spacer(Modifier.width(12.dp))
                val repeatSelected =
                    playbackState.repeatMode == RepeatMode.RepeatPlaylist ||
                        playbackState.repeatMode == RepeatMode.RepeatOne
                PlaybackModeButton(
                    repeatSelected,
                    uiState.repeatContentDescription,
                    playbackController::cycleRepeatMode,
                    Modifier.testTag(NowPlayingRepeatTestTag)) {
                        val icon =
                            when (playbackState.repeatMode) {
                                RepeatMode.RepeatOne -> Icons.Filled.RepeatOne
                                RepeatMode.StopAfterCurrent ->
                                    Icons.Filled.Filter1
                                else -> Icons.Filled.Repeat
                            }
                        Icon(
                            icon,
                            null,
                            tint =
                                if (repeatSelected) HausColors.current.onPulse
                                else HausColors.current.ink,
                            modifier = Modifier.size(22.dp))
                    }
            }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
                TransportButton(
                    48.dp,
                    RoundedCornerShape(14.dp),
                    HausColors.current.panel,
                    Icons.Filled.SkipPrevious,
                    stringResource(Res.string.previous_track),
                    playbackController::skipToPrevious,
                    testTag = NowPlayingPreviousTestTag)
                TransportButton(
                    64.dp,
                    CircleShape,
                    HausColors.current.pulse,
                    if (uiState.isPlaying) Icons.Filled.Pause
                    else Icons.Filled.PlayArrow,
                    if (uiState.isPlaying) labels.pause else labels.play,
                    playbackController::togglePlayPause,
                    HausColors.current.onPulse,
                    34.dp,
                    focusColor = HausColors.current.onPulse,
                    outerColor = HausColors.current.outline,
                    testTag = NowPlayingPlayPauseTestTag)
                TransportButton(
                    48.dp,
                    RoundedCornerShape(14.dp),
                    HausColors.current.panel,
                    Icons.Filled.SkipNext,
                    stringResource(Res.string.next_track),
                    playbackController::skipToNext,
                    testTag = NowPlayingNextTestTag)
            }
    }
}

@Composable
private fun TransportButton(
    size: androidx.compose.ui.unit.Dp,
    shape: androidx.compose.ui.graphics.Shape,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color = HausColors.current.ink,
    iconSize: androidx.compose.ui.unit.Dp = 26.dp,
    focusColor: Color = HausColors.current.outline,
    outerColor: Color? = null,
    testTag: String? = null,
) {
    Box(
        Modifier.size(size)
            .clip(shape)
            .background(color)
            .then(testTag?.let(Modifier::testTag) ?: Modifier)
            .hausClickable(
                onClick = onClick,
                focusColor = focusColor,
                outerColor = outerColor,
            ),
        contentAlignment = Alignment.Center) {
            Icon(
                icon,
                description,
                tint = tint,
                modifier = Modifier.size(iconSize))
        }
}

/**
 * Recovery action in the error-only failure section: 44dp tall, 14dp corner
 * radius, 14sp bold label. Primary uses ink/paper, destructive uses the shared
 * danger tint, and neutral uses the shared panel surface.
 */
@Composable
private fun RecoveryActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    destructive: Boolean = false,
) {
    val background =
        when {
            destructive -> HausColors.current.dangerPanel
            primary -> HausColors.current.ink
            else -> HausColors.current.panel
        }
    val contentColor =
        when {
            destructive -> HausColors.current.danger
            primary -> HausColors.current.paper
            else -> HausColors.current.ink
        }
    val focusColor =
        when {
            destructive -> HausColors.current.danger
            primary -> HausColors.current.paper
            else -> HausColors.current.outline
        }
    val outerColor =
        if (destructive || primary) HausColors.current.outline else null
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .hausClickable(
                onClick = onClick,
                focusColor = focusColor,
                outerColor = outerColor,
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center) {
            Text(
                label,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
}

/** Localized listener wording that summarizes [kind]. */
@Composable
private fun recoveryFailureSummary(kind: PlaybackFailureKind): String =
    when (kind) {
        PlaybackFailureKind.MissingFile ->
            stringResource(Res.string.playback_error_missing_file)
        PlaybackFailureKind.AccessLost ->
            stringResource(Res.string.playback_error_access_lost)
        PlaybackFailureKind.UnsupportedFormat ->
            stringResource(Res.string.playback_error_unsupported_format)
        PlaybackFailureKind.DecoderFailure ->
            stringResource(Res.string.playback_error_decoder_failure)
        PlaybackFailureKind.Unknown ->
            stringResource(Res.string.playback_error_unknown)
    }

@Composable
private fun CompactNowPlayingLayout(
    track: Track,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    labels: NowPlayingScreenLabels,
    artworkLoader: suspend (String) -> ByteArray?,
    uiState: NowPlayingUiState,
    brush: Brush,
    favoriteTrackIds: Set<String>,
    onSetTrackFavorite: (String, Boolean) -> Unit,
    isCurrentTrackAvailableInLibrary: Boolean,
    sleepTimerState: SleepTimerState,
    onOpenLyrics: () -> Unit,
    onOpenTimer: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    Column(
        Modifier.testTag(NowPlayingCompactLayoutTestTag)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(18.dp))
            NowPlayingArtworkPane(
                track, labels, artworkLoader, brush, Modifier.fillMaxWidth())
            Spacer(Modifier.height(18.dp))
            // The error-only recovery section can outgrow the bounded compact
            // viewport; keep the pane scrollable so the scrubber and transport
            // controls below it stay reachable. Outside error the pane keeps
            // its fixed natural height and gains no scroll semantics.
            val controlsModifier =
                if (playbackState.errorRecoveryVisible) {
                    Modifier.fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                } else {
                    Modifier.fillMaxWidth()
                }
            NowPlayingControlsPane(
                track,
                playbackState,
                playbackController,
                labels,
                uiState,
                controlsModifier,
                favoriteTrackIds,
                onSetTrackFavorite,
                isCurrentTrackAvailableInLibrary,
                sleepTimerState,
                onOpenLyrics,
                onOpenTimer,
                onOpenQueue,
            )
        }
}

@Composable
private fun WideNowPlayingLayout(
    track: Track,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    labels: NowPlayingScreenLabels,
    artworkLoader: suspend (String) -> ByteArray?,
    uiState: NowPlayingUiState,
    brush: Brush,
    favoriteTrackIds: Set<String>,
    onSetTrackFavorite: (String, Boolean) -> Unit,
    isCurrentTrackAvailableInLibrary: Boolean,
    sleepTimerState: SleepTimerState,
    onOpenLyrics: () -> Unit,
    onOpenTimer: () -> Unit,
    onOpenQueue: () -> Unit,
    queueContent: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.testTag(NowPlayingSplitLayoutTestTag)
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.fillMaxHeight().weight(1f).widthIn(min = 320.dp),
                contentAlignment = Alignment.Center) {
                    NowPlayingArtworkPane(
                        track,
                        labels,
                        artworkLoader,
                        brush,
                        Modifier.fillMaxWidth().aspectRatio(1f))
                }
            Box(
                Modifier.fillMaxHeight().weight(1f).widthIn(min = 320.dp),
                contentAlignment = Alignment.Center) {
                    // The timer setup panel may exceed a 600×400 dp split
                    // viewport even without an error recovery section.
                    val controlsModifier =
                        Modifier.fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    NowPlayingControlsPane(
                        track,
                        playbackState,
                        playbackController,
                        labels,
                        uiState,
                        controlsModifier,
                        favoriteTrackIds,
                        onSetTrackFavorite,
                        isCurrentTrackAvailableInLibrary,
                        sleepTimerState,
                        onOpenLyrics,
                        onOpenTimer,
                        onOpenQueue,
                    )
                }
            queueContent?.let { content ->
                Box(
                    Modifier.width(280.dp)
                        .fillMaxHeight()
                        .testTag(NowPlayingQueuePanelTestTag)) {
                        content()
                    }
            }
        }
}

@Composable
private fun LyricsPanel(
    lines: List<LyricLine>,
    rawLyrics: String,
    positionMillis: Long,
    onDismiss: () -> Unit,
) {
    val visibleLines =
        remember(lines, rawLyrics) {
            lines.ifEmpty { listOf(LyricLine(null, rawLyrics.trim())) }
        }
    val activeIndex = activeTimedLyricIndex(visibleLines, positionMillis)
    val listState = rememberLazyListState()
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) listState.animateScrollToItem(activeIndex)
    }
    Card(
        modifier =
            Modifier.fillMaxSize()
                .padding(20.dp)
                .testTag(NowPlayingLyricsDialogTestTag),
        cornerRadius = 24.dp,
        colors = CardDefaults.defaultColors(color = HausColors.current.panel),
    ) {
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val closeLabel = stringResource(Res.string.lyrics_close)
                Text(
                    stringResource(Res.string.lyrics_title),
                    color = HausColors.current.ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
                BackChip(
                    onClick = onDismiss,
                    modifier =
                        Modifier.testTag(NowPlayingLyricsCloseTestTag)
                            .semantics {
                                contentDescription = closeLabel
                            },
                )
            }
            LazyColumn(
                modifier =
                    Modifier.fillMaxWidth()
                        .weight(1f)
                        .testTag(NowPlayingLyricsListTestTag),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(visibleLines) { index, line ->
                    val active = index == activeIndex
                    val activeLineLabel =
                        stringResource(Res.string.lyrics_current_line)
                    Text(
                        line.text,
                        modifier =
                            Modifier.fillMaxWidth()
                                .testTag("$NowPlayingLyricLineTestTag-$index")
                                .semantics {
                                    if (active) {
                                        stateDescription = activeLineLabel
                                    }
                                },
                        color =
                            if (active) HausColors.current.pulse
                            else HausColors.current.ink,
                        fontSize = if (active) 18.sp else 16.sp,
                        fontWeight =
                            if (active) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerSubpanel(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackChip(
                onClick = onBack,
                modifier = Modifier.testTag(NowPlayingPanelBackTestTag),
            )
            Text(
                title,
                color = HausColors.current.ink,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Box(Modifier.fillMaxWidth().weight(1f)) { content() }
    }
}

@Composable
private fun PanelActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(HausColors.current.panel)
            .hausClickable(onClick)
            .semantics { contentDescription = label }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = HausColors.current.pulse,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun activeTimedLyricIndex(
    lines: List<LyricLine>,
    positionMillis: Long,
): Int = lines.indexOfLast { line ->
    line.timestampMillis?.let { it <= positionMillis } == true
}

@Composable
private fun PlaybackModeButton(
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) HausColors.current.pulse
                else HausColors.current.panel)
            .hausClickable(
                onClick = onClick,
                focusColor =
                    if (selected) HausColors.current.onPulse
                    else HausColors.current.outline,
                outerColor = if (selected) HausColors.current.outline else null,
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
        content = content)
}

@Composable
private fun statusLabel(status: PlaybackStatus): String =
    when (status) {
        PlaybackStatus.Idle -> stringResource(Res.string.playback_status_ready)
        PlaybackStatus.Loading ->
            stringResource(Res.string.playback_status_loading)
        PlaybackStatus.Buffering ->
            stringResource(Res.string.playback_status_buffering)
        PlaybackStatus.Playing ->
            stringResource(Res.string.playback_status_playing)
        PlaybackStatus.Paused ->
            stringResource(Res.string.playback_status_paused)
        PlaybackStatus.Stopped ->
            stringResource(Res.string.playback_status_stopped)
        PlaybackStatus.Error -> stringResource(Res.string.playback_status_error)
    }

@Composable
private fun repeatLabel(mode: RepeatMode): String =
    when (mode) {
        RepeatMode.StopAfterQueue ->
            stringResource(Res.string.repeat_mode_stop_after_queue)
        RepeatMode.RepeatPlaylist ->
            stringResource(Res.string.repeat_mode_repeat_playlist)
        RepeatMode.RepeatOne ->
            stringResource(Res.string.repeat_mode_repeat_one)
        RepeatMode.StopAfterCurrent ->
            stringResource(Res.string.repeat_mode_stop_after_current)
    }
