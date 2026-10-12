package com.eterocell.rhythhaus.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import com.eterocell.rhythhaus.ui.HausDialog
import com.eterocell.rhythhaus.ui.RhythHausTopAppBar
import org.jetbrains.compose.resources.stringResource
import rhythhaus.feature.settings.generated.resources.Res
import rhythhaus.feature.settings.generated.resources.about
import rhythhaus.feature.settings.generated.resources.add_device_audio
import rhythhaus.feature.settings.generated.resources.appearance
import rhythhaus.feature.settings.generated.resources.configured_folders
import rhythhaus.feature.settings.generated.resources.crossfade_3_seconds
import rhythhaus.feature.settings.generated.resources.crossfade_5_seconds
import rhythhaus.feature.settings.generated.resources.crossfade_8_seconds
import rhythhaus.feature.settings.generated.resources.crossfade_error
import rhythhaus.feature.settings.generated.resources.crossfade_explanation
import rhythhaus.feature.settings.generated.resources.crossfade_off
import rhythhaus.feature.settings.generated.resources.crossfade_saving
import rhythhaus.feature.settings.generated.resources.crossfade_title
import rhythhaus.feature.settings.generated.resources.gapless_title
import rhythhaus.feature.settings.generated.resources.gapless_off
import rhythhaus.feature.settings.generated.resources.gapless_on
import rhythhaus.feature.settings.generated.resources.gapless_explanation
import rhythhaus.feature.settings.generated.resources.gapless_saving
import rhythhaus.feature.settings.generated.resources.gapless_error
import rhythhaus.feature.settings.generated.resources.device_audio
import rhythhaus.feature.settings.generated.resources.device_audio_add
import rhythhaus.feature.settings.generated.resources.device_audio_settings
import rhythhaus.feature.settings.generated.resources.device_audio_unavailable
import rhythhaus.feature.settings.generated.resources.manage_music
import rhythhaus.feature.settings.generated.resources.notification_permission_body
import rhythhaus.feature.settings.generated.resources.notification_permission_request
import rhythhaus.feature.settings.generated.resources.notification_permission_settings
import rhythhaus.feature.settings.generated.resources.notification_permission_title
import rhythhaus.feature.settings.generated.resources.recover_source_format
import rhythhaus.feature.settings.generated.resources.remove_folder
import rhythhaus.feature.settings.generated.resources.remove_folder_message
import rhythhaus.feature.settings.generated.resources.remove_source_format
import rhythhaus.feature.settings.generated.resources.replaygain_album
import rhythhaus.feature.settings.generated.resources.replaygain_error
import rhythhaus.feature.settings.generated.resources.replaygain_explanation
import rhythhaus.feature.settings.generated.resources.replaygain_off
import rhythhaus.feature.settings.generated.resources.replaygain_saving
import rhythhaus.feature.settings.generated.resources.replaygain_title
import rhythhaus.feature.settings.generated.resources.replaygain_track
import rhythhaus.feature.settings.generated.resources.rescan_source_format
import rhythhaus.feature.settings.generated.resources.review_onboarding
import rhythhaus.feature.settings.generated.resources.source_access_available
import rhythhaus.feature.settings.generated.resources.source_access_lost
import rhythhaus.feature.settings.generated.resources.source_last_scanned
import rhythhaus.feature.settings.generated.resources.source_never_scanned
import rhythhaus.feature.settings.generated.resources.source_status_format
import rhythhaus.feature.settings.generated.resources.theme_dark_description
import rhythhaus.feature.settings.generated.resources.theme_dark_label
import rhythhaus.feature.settings.generated.resources.theme_light_description
import rhythhaus.feature.settings.generated.resources.theme_light_label
import rhythhaus.feature.settings.generated.resources.theme_system_description
import rhythhaus.feature.settings.generated.resources.theme_system_label
import rhythhaus.feature.settings.generated.resources.unnamed_folder
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference

/** Shared-owned wording and actions injected into [SettingsScreen]. */
public data class SettingsSharedLabels(
    /** Settings route title resolved by Shared. */
    public val title: String,
    /** Shared-owned add-folder action wording. */
    public val addMusicFolder: String,
    /** Shared-owned unavailable-picker wording. */
    public val folderPickerUnavailable: String,
    /** Shared-owned clear-library action wording. */
    public val clearLibrary: String,
    /** Shared-owned generic cancellation wording. */
    public val cancel: String,
    /** Shared-owned generic removal wording. */
    public val remove: String,
)

/** Host-neutral recovery state for the optional device-audio source. */
public enum class MediaStoreAudioRecovery {
    /** Access is available and the user can explicitly register the source. */
    Add,
    /** The host can request audio permission. */
    Requestable,
    /** Permission recovery requires system app settings. */
    SettingsRequired,
}

/**
 * Immutable, feature-safe rendering projection for one authoritative Library
 * source.
 */
public data class SettingsSourceItem(
    /** Stable source identifier returned to Shared callbacks. */
    public val id: String,
    /** User-visible source name already selected by Shared. */
    public val displayName: String,
    /** Whether the source remains accessible to the platform. */
    public val accessAvailable: Boolean,
    /** Whether the source has completed at least one scan. */
    public val hasBeenScanned: Boolean,
)

/**
 * Feature-safe projection of a denied host media-notification permission.
 *
 * Settings renders its recovery card only for these two actionable denied
 * variants; an unavailable, granted, or non-Android permission surface never
 * reaches this screen. The projection carries no Android or Shared framework
 * type, so the feature stays host-neutral.
 */
public enum class MediaNotificationRecovery {
    /** The host permission dialog can be requested again. */
    Requestable,

    /** Only the host notification settings can restore the permission. */
    SettingsRequired,
}

/** Neutral listener choices mapped to the playback policy by Shared. */
public enum class SettingsReplayGainMode {
    /** Keep the original application gain. */
    Off,
    /** Select the track's ReplayGain tags. */
    Track,
    /**
     * Select album tags, falling back to track tags when album gain is absent.
     */
    Album,
}

/** Neutral overlap choices mapped to the playback policy by Shared. */
public enum class SettingsCrossfadeDuration {
    /** Preserve ordinary non-overlapping playback. */
    Off,
    /** Overlap eligible tracks for up to three seconds. */
    Seconds3,
    /** Overlap eligible tracks for up to five seconds. */
    Seconds5,
    /** Overlap eligible tracks for up to eight seconds. */
    Seconds8,
}

public enum class SettingsGaplessMode { Off, On }

internal enum class SettingsSourceAccess {
    Available,
    Lost,
}

internal enum class SettingsSourceScan {
    NeverScanned,
    HasBeenScanned,
}

internal data class SettingsSourcePresentation(
    val displayNameFallbackRequired: Boolean,
    val access: SettingsSourceAccess,
    val scan: SettingsSourceScan,
)

internal fun SettingsSourceItem.presentation(): SettingsSourcePresentation =
    SettingsSourcePresentation(
        displayNameFallbackRequired = displayName.isBlank(),
        access =
            if (accessAvailable) SettingsSourceAccess.Available
            else SettingsSourceAccess.Lost,
        scan =
            if (hasBeenScanned) SettingsSourceScan.HasBeenScanned
            else SettingsSourceScan.NeverScanned)

internal data class SettingsLayoutPolicy(
    val horizontalPagePadding: Dp,
    val verticalPagePadding: Dp,
    val itemSpacing: Dp,
    val bottomContentPadding: Dp,
    val topBarTitlePadding: Dp,
    val topBarNavigationIconPadding: Dp,
    val appearanceHorizontalInsidePadding: Dp,
    val appearanceVerticalInsidePadding: Dp,
)

internal val CompactSettingsLayoutPolicy =
    SettingsLayoutPolicy(16.dp, 8.dp, 12.dp, 8.dp, 0.dp, 0.dp, 0.dp, 16.dp)
internal const val SettingsPickerTestTag = "settings-picker"
internal const val SettingsClearTestTag = "settings-clear"
internal const val SettingsAboutTestTag = "settings-about"
internal const val SettingsReviewOnboardingTestTag =
    "settings-review-onboarding"
internal const val SettingsRescanPrefix = "settings-rescan-"
internal const val SettingsRecoverPrefix = "settings-recover-"
internal const val SettingsRemovePrefix = "settings-remove-"
internal const val SettingsRemoveConfirmTestTag = "settings-remove-confirm"
internal const val SettingsRemoveDismissTestTag = "settings-remove-dismiss"
internal const val SettingsRootTestTag = "settings-root"
internal const val SettingsNotificationRecoveryTestTag =
    "settings-notification-recovery"
internal const val SettingsPointerShieldTestTag = "settings-pointer-shield"
internal const val SettingsListTestTag = "settings-list"
internal const val SettingsThemeTestTag = "settings-theme"
internal const val SettingsReplayGainPrefix = "settings-replaygain-"
internal const val SettingsReplayGainExplanationTestTag =
    "settings-replaygain-explanation"
internal const val SettingsReplayGainSavingTestTag =
    "settings-replaygain-saving"
internal const val SettingsReplayGainErrorTestTag = "settings-replaygain-error"
internal const val SettingsCrossfadePrefix = "settings-crossfade-"
internal const val SettingsCrossfadeExplanationTestTag =
    "settings-crossfade-explanation"
internal const val SettingsCrossfadeSavingTestTag = "settings-crossfade-saving"
internal const val SettingsCrossfadeErrorTestTag = "settings-crossfade-error"
internal const val SettingsGaplessPrefix = "settings-gapless-"

/**
 * Renders Settings from scalar state, source projections, callbacks, and
 * caller-owned slots.
 *
 * @param labels Shared-owned wording and actions injected into this screen.
 * @param currentThemeMode the theme mode currently in effect.
 * @param replayGainMode the last restored or durably accepted normalization
 *   mode.
 * @param replayGainBusy excludes normalization input during restoration or
 *   save.
 * @param replayGainError shows a localized recoverable preference-write error.
 * @param crossfadeDuration the last restored or durably accepted overlap
 *   choice.
 * @param crossfadeBusy excludes overlap input during restoration or save.
 * @param crossfadeError shows a localized recoverable overlap-save error.
 * @param sources the authoritative projection of configured library sources.
 * @param sourcePickerActionVisible whether to render the add-folder action.
 * @param sourcePickerAvailable whether the platform folder picker can launch.
 * @param importMessage transient import feedback, or null when absent.
 * @param mutationsEnabled gates every mutation: disabled controls neither
 *   dispatch nor accept input.
 * @param hasImportedTracks whether the clear action is rendered.
 * @param playlistBackupContent the playlist-backup settings slot.
 * @param activeScanContent the live scan surface slot, or null while idle.
 * @param scanOutcomeContent the terminal scan-outcome panel slot rendered
 *   directly after [activeScanContent], or null while absent.
 * @param clearLibraryDialog the Shared-owned clear confirmation dialog slot,
 *   rendered only when supplied.
 * @param notificationRecovery the actionable denied host media-notification
 *   permission to recover, or null when no card, action, or accessibility node
 *   may be rendered.
 * @param onThemeModeSelected dispatches the selected theme mode.
 * @param onReplayGainModeSelected requests a mode without changing selection
 *   optimistically; the caller publishes only a successfully saved mode.
 * @param onCrossfadeDurationSelected requests a duration without changing the
 *   accepted selection optimistically.
 * @param onAddMusicFolder dispatches the add-folder action.
 * @param onRescanSource dispatches a rescan with the source id.
 * @param onRecoverSource dispatches a lost-access recovery (folder re-pick)
 *   with the source id whose access is lost.
 * @param onRemoveSource dispatches a removal with the source id.
 * @param onRequestClearLibrary requests the Shared clear-library confirmation.
 * @param onAboutClick navigates to the feature-owned About page.
 * @param onReviewOnboarding navigates to the onboarding review page.
 * @param onRequestNotificationPermission re-requests host notification
 *   permission for the [MediaNotificationRecovery.Requestable] variant.
 * @param onOpenNotificationSettings opens the host notification settings for
 *   the [MediaNotificationRecovery.SettingsRequired] variant.
 * @param onDismiss navigates back.
 * @param modifier applied to the root container.
 * @param onExclusiveModalChanged reports foreground destructive confirmations.
 */
@Composable
public fun SettingsScreen(
    labels: SettingsSharedLabels,
    currentThemeMode: RhythHausThemeMode,
    replayGainMode: SettingsReplayGainMode,
    replayGainBusy: Boolean,
    replayGainError: Boolean,
    crossfadeDuration: SettingsCrossfadeDuration,
    crossfadeBusy: Boolean,
    crossfadeError: Boolean,
    gaplessMode: SettingsGaplessMode = SettingsGaplessMode.Off,
    gaplessBusy: Boolean = false,
    gaplessError: Boolean = false,
    sources: List<SettingsSourceItem>,
    sourcePickerActionVisible: Boolean,
    sourcePickerAvailable: Boolean,
    importMessage: String?,
    mutationsEnabled: Boolean,
    hasImportedTracks: Boolean,
    playlistBackupContent: @Composable () -> Unit,
    activeScanContent: (@Composable () -> Unit)?,
    scanOutcomeContent: (@Composable () -> Unit)? = null,
    clearLibraryDialog: (@Composable () -> Unit)?,
    notificationRecovery: MediaNotificationRecovery?,
    mediaStoreAudioRecovery: MediaStoreAudioRecovery? = null,
    onThemeModeSelected: (RhythHausThemeMode) -> Unit,
    onReplayGainModeSelected: (SettingsReplayGainMode) -> Unit,
    onCrossfadeDurationSelected: (SettingsCrossfadeDuration) -> Unit,
    onGaplessModeSelected: (SettingsGaplessMode) -> Unit = {},
    onAddMusicFolder: () -> Unit,
    onRescanSource: (String) -> Unit,
    onRecoverSource: (String) -> Unit,
    onRemoveSource: (String) -> Unit,
    onRequestClearLibrary: () -> Unit,
    onAboutClick: () -> Unit,
    onReviewOnboarding: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onRequestMediaStoreAudio: () -> Unit = {},
    onOpenMediaStoreSettings: () -> Unit = {},
    onAddMediaStoreAudio: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState? = null,
    onExclusiveModalChanged: (Boolean) -> Unit = {},
) {
    var sourcePendingRemoval by remember {
        mutableStateOf<SettingsSourceItem?>(null)
    }
    val currentModalCallback by rememberUpdatedState(onExclusiveModalChanged)
    SideEffect {
        currentModalCallback(
            clearLibraryDialog != null || sourcePendingRemoval != null)
    }
    DisposableEffect(Unit) {
        onDispose { currentModalCallback(false) }
    }
    val policy = CompactSettingsLayoutPolicy
    val effectiveListState = listState ?: rememberLazyListState()
    Box(
        modifier
            .fillMaxSize()
            .background(HausColors.current.paper)
            .testTag(SettingsRootTestTag)) {
            Box(
                Modifier.matchParentSize()
                    .testTag(SettingsPointerShieldTestTag)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial)
                                    .changes
                                    .forEach {
                                        it.consume()
                                    }
                            }
                        }
                    })
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = HausColors.current.paper) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = HausColors.current.paper,
                        contentWindowInsets = WindowInsets(0.dp)) {
                            LazyColumn(
                                state = effectiveListState,
                                modifier =
                                    Modifier.fillMaxSize()
                                        .testTag(SettingsListTestTag)
                                        .safeContentPadding()
                                        .padding(
                                            horizontal =
                                                policy.horizontalPagePadding,
                                            vertical =
                                                policy.verticalPagePadding),
                                contentPadding =
                                    PaddingValues(
                                        bottom = policy.bottomContentPadding),
                                verticalArrangement =
                                    Arrangement.spacedBy(policy.itemSpacing),
                            ) {
                                item {
                                    RhythHausTopAppBar(
                                        labels.title,
                                        onDismiss,
                                        titlePadding =
                                            policy.topBarTitlePadding,
                                        navigationIconPadding =
                                            policy.topBarNavigationIconPadding)
                                }
                                notificationRecovery?.let { variant ->
                                    item {
                                        MediaNotificationRecoveryCard(
                                            variant,
                                            onRequestNotificationPermission,
                                            onOpenNotificationSettings)
                                    }
                                }
                                mediaStoreAudioRecovery?.let { variant ->
                                    item {
                                        MediaStoreAudioRecoveryCard(
                                            variant = variant,
                                            enabled = mutationsEnabled,
                                            onRequest =
                                                onRequestMediaStoreAudio,
                                            onOpenSettings =
                                                onOpenMediaStoreSettings,
                                            onAdd = onAddMediaStoreAudio,
                                        )
                                    }
                                }
                                item {
                                    AppearanceDropdown(
                                        currentThemeMode,
                                        onThemeModeSelected,
                                        policy
                                            .appearanceHorizontalInsidePadding,
                                        policy.appearanceVerticalInsidePadding)
                                }
                                item {
                                    ReplayGainPreference(
                                        current = replayGainMode,
                                        busy = replayGainBusy,
                                        modalVisible =
                                            clearLibraryDialog != null ||
                                                sourcePendingRemoval != null,
                                        saveFailed = replayGainError,
                                        onSelected = onReplayGainModeSelected)
                                }
                                item {
                                    CrossfadePreference(
                                        current = crossfadeDuration,
                                        busy = crossfadeBusy,
                                        modalVisible =
                                            clearLibraryDialog != null ||
                                                sourcePendingRemoval != null,
                                        saveFailed = crossfadeError,
                                        onSelected =
                                            onCrossfadeDurationSelected)
                                }
                                item {
                                    GaplessPreference(
                                        current = gaplessMode,
                                        busy = gaplessBusy,
                                        modalVisible = clearLibraryDialog != null || sourcePendingRemoval != null,
                                        saveFailed = gaplessError,
                                        onSelected = onGaplessModeSelected)
                                }
                                item {
                                    Text(
                                        stringResource(Res.string.manage_music),
                                        color = HausColors.current.ink,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black)
                                }
                                activeScanContent?.let { content ->
                                    item { content() }
                                }
                                scanOutcomeContent?.let { item { it() } }
                                if (sources.isNotEmpty()) {
                                    item {
                                        Text(
                                            stringResource(
                                                Res.string.configured_folders),
                                            color = HausColors.current.muted,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold)
                                    }
                                    items(
                                        sources,
                                        key = SettingsSourceItem::id) { source
                                            ->
                                            ConfiguredSourceRow(
                                                source,
                                                mutationsEnabled,
                                                { onRescanSource(source.id) },
                                                onRecover = {
                                                    onRecoverSource(source.id)
                                                }) {
                                                    sourcePendingRemoval =
                                                        source
                                                }
                                        }
                                }
                                if (sourcePickerActionVisible)
                                    item {
                                        val pickerEnabled =
                                            sourcePickerAvailable &&
                                                mutationsEnabled
                                        val pickerLabel =
                                            if (sourcePickerAvailable) {
                                                labels.addMusicFolder
                                            } else {
                                                labels.folderPickerUnavailable
                                            }
                                        val pickerModifier =
                                            Modifier.fillMaxWidth()
                                                .height(48.dp)
                                        if (!pickerEnabled) {
                                            Box(
                                                pickerModifier
                                                    .testTag(
                                                        SettingsPickerTestTag)
                                                    .clearAndSetSemantics {
                                                        contentDescription =
                                                            pickerLabel
                                                        disabled()
                                                    }) {
                                                    Button(
                                                        onClick =
                                                            onAddMusicFolder,
                                                        enabled = false,
                                                        modifier =
                                                            pickerModifier,
                                                        cornerRadius = 16.dp,
                                                        colors =
                                                            ButtonDefaults
                                                                .buttonColors(
                                                                    color =
                                                                        HausColors
                                                                            .current
                                                                            .ink,
                                                                    contentColor =
                                                                        HausColors
                                                                            .current
                                                                            .paper,
                                                                    disabledColor =
                                                                        HausColors
                                                                            .current
                                                                            .muted
                                                                            .copy(
                                                                                alpha =
                                                                                    .28f),
                                                                    disabledContentColor =
                                                                        HausColors
                                                                            .current
                                                                            .muted)) {
                                                            Text(
                                                                pickerLabel,
                                                                fontSize =
                                                                    14.sp,
                                                                fontWeight =
                                                                    FontWeight
                                                                        .Bold)
                                                        }
                                                }
                                        } else
                                            Button(
                                                onClick = onAddMusicFolder,
                                                enabled = true,
                                                modifier =
                                                    pickerModifier.testTag(
                                                        SettingsPickerTestTag),
                                                cornerRadius = 16.dp,
                                                colors =
                                                    ButtonDefaults.buttonColors(
                                                        color =
                                                            HausColors.current
                                                                .ink,
                                                        contentColor =
                                                            HausColors.current
                                                                .paper,
                                                        disabledColor =
                                                            HausColors.current
                                                                .muted
                                                                .copy(
                                                                    alpha =
                                                                        .28f),
                                                        disabledContentColor =
                                                            HausColors.current
                                                                .muted)) {
                                                    Text(
                                                        pickerLabel,
                                                        fontSize = 14.sp,
                                                        fontWeight =
                                                            FontWeight.Bold)
                                                }
                                    }
                                importMessage?.let { message ->
                                    item {
                                        Text(
                                            message,
                                            color = HausColors.current.muted,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            fontWeight = FontWeight.Medium)
                                    }
                                }
                                item {
                                    HorizontalDivider(
                                        modifier =
                                            Modifier.padding(vertical = 4.dp),
                                        thickness = 1.dp,
                                        color =
                                            HausColors.current.line.copy(
                                                alpha = 0.6f))
                                }
                                item { playlistBackupContent() }
                                if (hasImportedTracks)
                                    item {
                                        val clearModifier =
                                            Modifier.fillMaxWidth()
                                                .height(48.dp)
                                        if (!mutationsEnabled) {
                                            Box(
                                                clearModifier
                                                    .testTag(
                                                        SettingsClearTestTag)
                                                    .clearAndSetSemantics {
                                                        contentDescription =
                                                            labels.clearLibrary
                                                        disabled()
                                                    }) {
                                                    Button(
                                                        onClick =
                                                            onRequestClearLibrary,
                                                        enabled = false,
                                                        modifier =
                                                            clearModifier,
                                                        cornerRadius = 16.dp,
                                                        colors =
                                                            ButtonDefaults
                                                                .buttonColors(
                                                                    color =
                                                                        HausColors
                                                                            .current
                                                                            .pulse
                                                                            .copy(
                                                                                alpha =
                                                                                    .15f),
                                                                    contentColor =
                                                                        HausColors
                                                                            .current
                                                                            .pulse)) {
                                                            Text(
                                                                labels
                                                                    .clearLibrary,
                                                                fontSize =
                                                                    14.sp,
                                                                fontWeight =
                                                                    FontWeight
                                                                        .Bold)
                                                        }
                                                }
                                        } else
                                            Button(
                                                onClick = onRequestClearLibrary,
                                                enabled = true,
                                                modifier =
                                                    clearModifier.testTag(
                                                        SettingsClearTestTag),
                                                cornerRadius = 16.dp,
                                                colors =
                                                    ButtonDefaults.buttonColors(
                                                        color =
                                                            HausColors.current
                                                                .pulse
                                                                .copy(
                                                                    alpha =
                                                                        .15f),
                                                        contentColor =
                                                            HausColors.current
                                                                .pulse)) {
                                                    Text(
                                                        labels.clearLibrary,
                                                        fontSize = 14.sp,
                                                        fontWeight =
                                                            FontWeight.Bold)
                                                }
                                    }
                                item { ReviewOnboardingRow(onReviewOnboarding) }
                                item { AboutRow(onAboutClick) }
                            }
                        }
                }
            clearLibraryDialog?.invoke()
            sourcePendingRemoval?.let { source ->
                RemoveSourceDialog(
                    source,
                    labels,
                    mutationsEnabled,
                    { sourcePendingRemoval = null }) {
                        onRemoveSource(source.id)
                        sourcePendingRemoval = null
                    }
            }
        }
}

@Composable
private fun ReviewOnboardingRow(onClick: () -> Unit) {
    val label = stringResource(Res.string.review_onboarding)
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .testTag(SettingsReviewOnboardingTestTag)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = label
            }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Info,
            null,
            tint = HausColors.current.ink,
            modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            Modifier.weight(1f),
            HausColors.current.ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold)
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            tint = HausColors.current.muted,
            modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun AboutRow(onClick: () -> Unit) =
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .testTag(SettingsAboutTestTag)
            .semantics(mergeDescendants = true) { role = Role.Button }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Info,
            null,
            tint = HausColors.current.ink,
            modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(Res.string.about),
            Modifier.weight(1f),
            HausColors.current.ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold)
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            tint = HausColors.current.muted,
            modifier = Modifier.size(22.dp))
    }

@Composable
private fun ConfiguredSourceRow(
    source: SettingsSourceItem,
    mutationsEnabled: Boolean,
    onRescan: () -> Unit,
    onRecover: () -> Unit,
    onRemove: () -> Unit
) {
    val presentation = source.presentation()
    val displayName =
        if (presentation.displayNameFallbackRequired) {
            stringResource(Res.string.unnamed_folder)
        } else source.displayName
    val access =
        stringResource(
            if (presentation.access == SettingsSourceAccess.Available) {
                Res.string.source_access_available
            } else Res.string.source_access_lost)
    val scan =
        stringResource(
            if (presentation.scan == SettingsSourceScan.HasBeenScanned) {
                Res.string.source_last_scanned
            } else Res.string.source_never_scanned)
    val alpha = if (mutationsEnabled) 1f else .42f
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 72.dp)
            .background(
                HausColors.current.panel.copy(alpha = .54f),
                RoundedCornerShape(16.dp))
            .border(1.dp, HausColors.current.line, RoundedCornerShape(16.dp))
            .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        displayName,
                        color = HausColors.current.ink,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(
                            Res.string.source_status_format, access, scan),
                        color = HausColors.current.muted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2)
                }
            val lostAccess = presentation.access == SettingsSourceAccess.Lost
            IconButton(
                onClick = if (lostAccess) onRecover else onRescan,
                enabled = mutationsEnabled,
                modifier =
                    Modifier.testTag(
                            if (lostAccess) SettingsRecoverPrefix + source.id
                            else SettingsRescanPrefix + source.id)
                        .semantics {
                            if (!mutationsEnabled) disabled()
                        },
                backgroundColor = Color.Transparent,
                minWidth = 44.dp,
                minHeight = 44.dp) {
                    Icon(
                        if (lostAccess) Icons.Default.FolderOpen
                        else Icons.Default.Refresh,
                        stringResource(
                            if (lostAccess) Res.string.recover_source_format
                            else Res.string.rescan_source_format,
                            displayName),
                        tint = HausColors.current.ink.copy(alpha = alpha),
                        modifier = Modifier.size(20.dp))
                }
            IconButton(
                onRemove,
                enabled = mutationsEnabled,
                modifier =
                    Modifier.testTag(SettingsRemovePrefix + source.id)
                        .semantics {
                            if (!mutationsEnabled) disabled()
                        },
                backgroundColor = Color.Transparent,
                minWidth = 44.dp,
                minHeight = 44.dp) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        stringResource(
                            Res.string.remove_source_format, displayName),
                        tint = HausColors.current.pulse.copy(alpha = alpha),
                        modifier = Modifier.size(20.dp))
                }
        }
}

@Composable
private fun MediaNotificationRecoveryCard(
    variant: MediaNotificationRecovery,
    onRequestPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    val action =
        when (variant) {
            MediaNotificationRecovery.Requestable -> onRequestPermission
            MediaNotificationRecovery.SettingsRequired ->
                onOpenNotificationSettings
        }
    val actionLabel =
        stringResource(
            when (variant) {
                MediaNotificationRecovery.Requestable ->
                    Res.string.notification_permission_request
                MediaNotificationRecovery.SettingsRequired ->
                    Res.string.notification_permission_settings
            })
    Column(
        Modifier.fillMaxWidth()
            .background(
                HausColors.current.panel.copy(alpha = .54f),
                RoundedCornerShape(16.dp))
            .border(1.dp, HausColors.current.line, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(Res.string.notification_permission_title),
                color = HausColors.current.ink,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Black)
            Text(
                stringResource(Res.string.notification_permission_body),
                color = HausColors.current.muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium)
            Button(
                onClick = action,
                modifier =
                    Modifier.fillMaxWidth()
                        .height(48.dp)
                        .testTag(SettingsNotificationRecoveryTestTag),
                cornerRadius = 16.dp,
                colors =
                    ButtonDefaults.buttonColors(
                        color = HausColors.current.ink,
                        contentColor = HausColors.current.paper,
                        disabledColor =
                            HausColors.current.muted.copy(alpha = .28f),
                        disabledContentColor = HausColors.current.muted)) {
                    Text(
                        actionLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold)
                }
        }
}

@Composable
private fun MediaStoreAudioRecoveryCard(
    variant: MediaStoreAudioRecovery,
    enabled: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    onAdd: () -> Unit,
) {
    val action =
        when (variant) {
            MediaStoreAudioRecovery.Add -> onAdd
            MediaStoreAudioRecovery.Requestable -> onRequest
            MediaStoreAudioRecovery.SettingsRequired -> onOpenSettings
        }
    val actionLabel =
        stringResource(
            when (variant) {
                MediaStoreAudioRecovery.Add -> Res.string.device_audio_add
                MediaStoreAudioRecovery.Requestable ->
                    Res.string.add_device_audio
                MediaStoreAudioRecovery.SettingsRequired ->
                    Res.string.device_audio_settings
            })
    Column(
        Modifier.fillMaxWidth()
            .background(
                HausColors.current.panel.copy(alpha = .54f),
                RoundedCornerShape(16.dp))
            .border(1.dp, HausColors.current.line, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(Res.string.device_audio),
                color = HausColors.current.ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black)
            Text(
                stringResource(Res.string.device_audio_unavailable),
                color = HausColors.current.muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium)
            Button(
                onClick = action,
                enabled = enabled,
                modifier =
                    Modifier.fillMaxWidth()
                        .height(48.dp)
                        .testTag("settings-device-audio-action"),
                cornerRadius = 16.dp,
                colors =
                    ButtonDefaults.buttonColors(
                        color = HausColors.current.ink,
                        contentColor = HausColors.current.paper)) {
                    Text(
                        actionLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold)
                }
        }
}

@Composable
private fun RemoveSourceDialog(
    source: SettingsSourceItem,
    labels: SettingsSharedLabels,
    mutationsEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val fullName =
        source.displayName.ifBlank { stringResource(Res.string.unnamed_folder) }
    val visibleName =
        if (fullName.length <= 64) fullName else fullName.take(63) + "…"
    HausDialog(
        title = stringResource(Res.string.remove_folder),
        onDismiss = onDismiss,
        dismissLabel = labels.cancel,
        body = {
            Text(
                stringResource(Res.string.remove_folder),
                color = HausColors.current.ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            Text(
                visibleName,
                Modifier.clearAndSetSemantics { contentDescription = fullName },
                color = HausColors.current.ink,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(Res.string.remove_folder_message),
                color = HausColors.current.muted,
                fontSize = 14.sp,
                lineHeight = 20.sp)
        },
        actions = {
            Spacer(Modifier.weight(1f))
            Button(
                onDismiss,
                modifier =
                    Modifier.height(44.dp)
                        .testTag(SettingsRemoveDismissTestTag),
                cornerRadius = 12.dp,
                insideMargin =
                    PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        color = HausColors.current.muted.copy(alpha = .15f),
                        contentColor = HausColors.current.muted)) {
                    Text(
                        labels.cancel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium)
                }
            Spacer(Modifier.width(12.dp))
            Button(
                onConfirm,
                enabled = mutationsEnabled,
                modifier =
                    Modifier.height(44.dp)
                        .testTag(SettingsRemoveConfirmTestTag),
                cornerRadius = 12.dp,
                insideMargin =
                    PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        color = HausColors.current.pulse,
                        contentColor = HausColors.current.paper,
                        disabledColor =
                            HausColors.current.muted.copy(alpha = .28f),
                        disabledContentColor = HausColors.current.muted)) {
                    Text(
                        labels.remove,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium)
                }
        })
}

@Composable
private fun ReplayGainPreference(
    current: SettingsReplayGainMode,
    busy: Boolean,
    modalVisible: Boolean,
    saveFailed: Boolean,
    onSelected: (SettingsReplayGainMode) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .background(
                HausColors.current.panel.copy(alpha = .54f),
                RoundedCornerShape(16.dp))
            .border(1.dp, HausColors.current.line, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(Res.string.replaygain_title),
                modifier = Modifier.semantics { heading() },
                color = HausColors.current.ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black)
            Row(
                Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsReplayGainMode.entries.forEach { mode ->
                        val label = mode.label()
                        val isSelected = mode == current
                        val interaction =
                            if (busy || modalVisible) {
                                Modifier.clearAndSetSemantics {
                                    contentDescription = label
                                    selected = isSelected
                                    role = Role.RadioButton
                                    disabled()
                                }
                            } else {
                                Modifier.selectable(
                                    selected = isSelected,
                                    role = Role.RadioButton,
                                    onClick = { onSelected(mode) })
                            }
                        Row(
                            Modifier.weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) HausColors.current.ink
                                    else HausColors.current.paper)
                                .testTag(mode.testTag())
                                .then(interaction)
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement =
                                Arrangement.spacedBy(6.dp)) {
                                val color =
                                    (if (isSelected) HausColors.current.paper
                                        else HausColors.current.ink)
                                        .copy(
                                            alpha =
                                                if (busy || modalVisible) .5f
                                                else 1f)
                                Icon(
                                    if (isSelected)
                                        Icons.Default.RadioButtonChecked
                                    else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = color)
                                Text(
                                    label,
                                    color = color,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                    }
                }
            Text(
                stringResource(Res.string.replaygain_explanation),
                modifier =
                    Modifier.testTag(SettingsReplayGainExplanationTestTag),
                color = HausColors.current.muted,
                fontSize = 12.sp,
                lineHeight = 17.sp)
            if (busy) {
                Text(
                    stringResource(Res.string.replaygain_saving),
                    modifier =
                        Modifier.testTag(SettingsReplayGainSavingTestTag)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    color = HausColors.current.muted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp)
            }
            if (saveFailed) {
                val message = stringResource(Res.string.replaygain_error)
                Text(
                    message,
                    modifier =
                        Modifier.testTag(SettingsReplayGainErrorTestTag)
                            .semantics {
                                liveRegion = LiveRegionMode.Polite
                                error(message)
                            },
                    color = HausColors.current.pulse,
                    fontSize = 12.sp,
                    lineHeight = 17.sp)
            }
        }
}

@Composable
private fun SettingsReplayGainMode.label(): String =
    stringResource(
        when (this) {
            SettingsReplayGainMode.Off -> Res.string.replaygain_off
            SettingsReplayGainMode.Track -> Res.string.replaygain_track
            SettingsReplayGainMode.Album -> Res.string.replaygain_album
        })

private fun SettingsReplayGainMode.testTag(): String =
    when (this) {
        SettingsReplayGainMode.Off -> "settings-replaygain-off"
        SettingsReplayGainMode.Track -> "settings-replaygain-track"
        SettingsReplayGainMode.Album -> "settings-replaygain-album"
    }

@Composable
private fun CrossfadePreference(
    current: SettingsCrossfadeDuration,
    busy: Boolean,
    modalVisible: Boolean,
    saveFailed: Boolean,
    onSelected: (SettingsCrossfadeDuration) -> Unit,
) {
    val interactionEnabled = !busy && !modalVisible
    Column(
        Modifier.fillMaxWidth()
            .background(
                HausColors.current.panel.copy(alpha = .54f),
                RoundedCornerShape(16.dp))
            .border(1.dp, HausColors.current.line, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(Res.string.crossfade_title),
                modifier = Modifier.semantics { heading() },
                color = HausColors.current.ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black)
            Column(
                Modifier.fillMaxWidth().selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(2) { rowIndex ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)) {
                                for (index in
                                    rowIndex * 2 until rowIndex * 2 + 2) {
                                    val duration =
                                        SettingsCrossfadeDuration.entries[index]
                                    val label = duration.label()
                                    val isSelected = duration == current
                                    val interaction =
                                        if (interactionEnabled) {
                                            Modifier.selectable(
                                                selected = isSelected,
                                                role = Role.RadioButton,
                                                onClick = {
                                                    onSelected(duration)
                                                })
                                        } else {
                                            Modifier.clearAndSetSemantics {
                                                contentDescription = label
                                                selected = isSelected
                                                role = Role.RadioButton
                                                disabled()
                                            }
                                        }
                                    Row(
                                        Modifier.weight(1f)
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected)
                                                    HausColors.current.ink
                                                else HausColors.current.paper)
                                            .testTag(duration.testTag())
                                            .then(interaction)
                                            .padding(
                                                horizontal = 8.dp,
                                                vertical = 10.dp),
                                        verticalAlignment =
                                            Alignment.CenterVertically,
                                        horizontalArrangement =
                                            Arrangement.spacedBy(6.dp)) {
                                            val color =
                                                (if (isSelected)
                                                        HausColors.current.paper
                                                    else HausColors.current.ink)
                                                    .copy(
                                                        alpha =
                                                            if (interactionEnabled)
                                                                1f
                                                            else .5f)
                                            Icon(
                                                if (isSelected)
                                                    Icons.Default
                                                        .RadioButtonChecked
                                                else
                                                    Icons.Default
                                                        .RadioButtonUnchecked,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = color)
                                            Text(
                                                label,
                                                color = color,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold)
                                        }
                                }
                            }
                    }
                }
            Text(
                stringResource(Res.string.crossfade_explanation),
                modifier =
                    Modifier.testTag(SettingsCrossfadeExplanationTestTag),
                color = HausColors.current.muted,
                fontSize = 12.sp,
                lineHeight = 17.sp)
            if (busy) {
                Text(
                    stringResource(Res.string.crossfade_saving),
                    modifier =
                        Modifier.testTag(SettingsCrossfadeSavingTestTag)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    color = HausColors.current.muted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp)
            }
            if (saveFailed) {
                val message = stringResource(Res.string.crossfade_error)
                Text(
                    message,
                    modifier =
                        Modifier.testTag(SettingsCrossfadeErrorTestTag)
                            .semantics {
                                liveRegion = LiveRegionMode.Polite
                                error(message)
                            },
                    color = HausColors.current.pulse,
                    fontSize = 12.sp,
                    lineHeight = 17.sp)
            }
        }
}

@Composable
private fun GaplessPreference(
    current: SettingsGaplessMode,
    busy: Boolean,
    modalVisible: Boolean,
    saveFailed: Boolean,
    onSelected: (SettingsGaplessMode) -> Unit,
) {
    val enabled = !busy && !modalVisible
    Column(
        Modifier.fillMaxWidth().background(HausColors.current.panel.copy(alpha = .54f), RoundedCornerShape(16.dp))
            .border(1.dp, HausColors.current.line, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(Res.string.gapless_title), modifier = Modifier.semantics { heading() },
            color = HausColors.current.ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Text(stringResource(Res.string.gapless_explanation), color = HausColors.current.muted, fontSize = 13.sp)
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsGaplessMode.entries.forEach { mode ->
                val selected = mode == current
                val label = stringResource(if (mode == SettingsGaplessMode.On) Res.string.gapless_on else Res.string.gapless_off)
                Row(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (selected) HausColors.current.ink else HausColors.current.paper)
                    .testTag(SettingsGaplessPrefix + mode.name.lowercase())
                    .then(if (enabled) Modifier.selectable(selected, Role.RadioButton) { onSelected(mode) }
                        else Modifier.clearAndSetSemantics { contentDescription = label; this.selected = selected; role = Role.RadioButton; disabled() })
                    .padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val tint = (if (selected) HausColors.current.paper else HausColors.current.ink).copy(alpha = if (enabled) 1f else .5f)
                    Icon(if (selected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null, modifier = Modifier.size(18.dp), tint = tint)
                    Text(label, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (busy) Text(stringResource(Res.string.gapless_saving), Modifier.testTag("settings-gapless-saving").semantics { liveRegion = LiveRegionMode.Polite }, color = HausColors.current.muted, fontSize = 12.sp)
        if (saveFailed) Text(stringResource(Res.string.gapless_error), Modifier.testTag("settings-gapless-error").semantics { liveRegion = LiveRegionMode.Assertive; error(stringResource(Res.string.gapless_error)) }, color = HausColors.current.muted, fontSize = 12.sp)
    }
}

@Composable
private fun SettingsCrossfadeDuration.label(): String =
    stringResource(
        when (this) {
            SettingsCrossfadeDuration.Off -> Res.string.crossfade_off
            SettingsCrossfadeDuration.Seconds3 -> Res.string.crossfade_3_seconds
            SettingsCrossfadeDuration.Seconds5 -> Res.string.crossfade_5_seconds
            SettingsCrossfadeDuration.Seconds8 -> Res.string.crossfade_8_seconds
        })

private fun SettingsCrossfadeDuration.testTag(): String =
    when (this) {
        SettingsCrossfadeDuration.Off -> "settings-crossfade-off"
        SettingsCrossfadeDuration.Seconds3 -> "settings-crossfade-3"
        SettingsCrossfadeDuration.Seconds5 -> "settings-crossfade-5"
        SettingsCrossfadeDuration.Seconds8 -> "settings-crossfade-8"
    }

@Composable
private fun AppearanceDropdown(
    current: RhythHausThemeMode,
    onSelected: (RhythHausThemeMode) -> Unit,
    horizontal: Dp,
    vertical: Dp
) {
    val options = RhythHausThemeMode.settingsOptions
    OverlayDropdownPreference(
        items = options.map { it.label() },
        selectedIndex = options.indexOf(current).coerceAtLeast(0),
        title = stringResource(Res.string.appearance),
        summary = current.description(),
        modifier = Modifier.fillMaxWidth().testTag(SettingsThemeTestTag),
        insideMargin =
            PaddingValues(horizontal = horizontal, vertical = vertical),
        renderInRootScaffold = false,
        onSelectedIndexChange = { options.getOrNull(it)?.let(onSelected) })
}

/** Localized system-owned source name for Shared source presentations. */
@Composable
public fun deviceAudioSourceDisplayName(): String =
    stringResource(Res.string.device_audio)

@Composable
private fun RhythHausThemeMode.label(): String =
    stringResource(
        when (this) {
            RhythHausThemeMode.System -> Res.string.theme_system_label
            RhythHausThemeMode.Light -> Res.string.theme_light_label
            RhythHausThemeMode.Dark -> Res.string.theme_dark_label
        })

@Composable
private fun RhythHausThemeMode.description(): String =
    stringResource(
        when (this) {
            RhythHausThemeMode.System -> Res.string.theme_system_description
            RhythHausThemeMode.Light -> Res.string.theme_light_description
            RhythHausThemeMode.Dark -> Res.string.theme_dark_description
        })
